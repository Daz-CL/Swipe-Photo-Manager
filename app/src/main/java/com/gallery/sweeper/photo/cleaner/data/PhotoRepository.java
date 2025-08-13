package com.gallery.sweeper.photo.cleaner.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.app.App;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoDao;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoGroupDao;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.permission.PermissionManager;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:37
 * 描述：
 */
public class PhotoRepository {
    private static final String TAG = "PhotoRepository";
    // 单例实例 - volatile确保多线程可见性
    private static volatile PhotoRepository instance;
    private final Context context;
    // DAO接口
    private final PhotoDao photoDao;
    private final PhotoGroupDao photoGroupDao;
    // 线程资源
    private final ExecutorService executor;
    private final ReentrantLock dbLock = new ReentrantLock(); // 数据库操作锁
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false); // 关闭状态标志
    // 事件总线用于通知状态变化
    private final EventBus eventBus = EventBus.getDefault();
    // 单例初始化控制
    private static final Object initLock = new Object();

    private PhotoRepository(Context context) {
        this.context = context.getApplicationContext();
        PhotoDatabase database = PhotoDatabase.getInstance(this.context);

        // 初始化DAO
        this.photoDao = database.photoDao();
        this.photoGroupDao = database.photoGroupDao();

        // 初始化事件总线
        initEventBus();

        // 创建单线程执行器（确保操作顺序性）
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PhotoRepository-Worker");
            thread.setPriority(Thread.NORM_PRIORITY - 1); // 降低优先级避免阻塞UI
            return thread;
        });
        XLog.i(TAG, "【系统】PhotoRepository初始化完成");

        // 启动定期清理
        //startPeriodicCleanup();
    }

    public static PhotoRepository getInstance() {
        if (instance == null) {
            synchronized (initLock) {
                if (instance == null) {
                    instance = new PhotoRepository(App.getInstance());
                    XLog.i(TAG, "【系统】创建PhotoRepository实例");
                }
            }
        }
        return instance;
    }

    /**
     * 初始化EventBus事件总线
     * <p>
     * 该方法用于注册当前对象到EventBus默认实例，确保能够接收和处理事件。
     * 方法会先检查当前对象是否已经注册，避免重复注册。
     * 如果未注册则进行注册操作，并输出调试日志。
     * <p>
     * 注意：该方法包含异常处理，如果注册过程中出现异常会打印异常堆栈信息。
     */
    private void initEventBus() {
        try {
            // 检查当前对象是否已注册到EventBus，避免重复注册
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听照片状态变化事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionEvent(PermissionRequiredEvent event) {
        //XLog.w(TAG, "【权限】发送权限请求通知 | 操作类型: " + event.getOperationType());
    }

    /**
     * 扫描设备媒体库中的图片，并将其信息同步到本地数据库。
     * <p>
     * 该方法会首先检查存储权限和系统关闭状态，然后在后台线程中执行实际的扫描操作。
     * 它通过查询 MediaStore 获取图片信息，验证文件是否存在，并将有效数据分批处理插入或更新到数据库中。
     * 同时，对于不存在的文件记录会从数据库中删除。
     * </p>
     *
     * @param context 上下文对象，用于访问 ContentResolver 和其他系统服务
     */
    public void scanMediaStore(Context context) {
        if (!PermissionManager.hasStoragePermission()) {
            XLog.e(TAG, "【权限】无存储权限，无法扫描媒体库");
            // 对于Android 11+特殊权限情况，需要引导用户去设置页面开启
            if (PermissionManager.needSpecialPermissionSetting()) {
                // 发送特殊事件，引导用户去设置页面
                EventBus.getDefault().post(new PermissionRequiredEvent(PermissionRequiredEvent.OPERATION_SPECIAL_PERMISSION));
            } else {
                PermissionManager.postPermissionRequired(PermissionRequiredEvent.OPERATION_SCAN);
            }
            return;
        }

        // 检查关闭状态
        if (isShuttingDown.get()) {
            XLog.e(TAG, "【系统】扫描操作被拒绝: 系统正在关闭");
            return;
        }
        executor.execute(() -> {
            XLog.w(TAG, "【扫描】===== 开始媒体库扫描 =====");
            long startTime = System.currentTimeMillis();
            AtomicInteger totalScanned = new AtomicInteger(0);
            AtomicInteger insertedCount = new AtomicInteger(0);
            AtomicInteger updatedCount = new AtomicInteger(0);
            AtomicInteger batchCounter = new AtomicInteger(0);
            AtomicInteger skippedFiles = new AtomicInteger(0);
            AtomicInteger deletedRecords = new AtomicInteger(0);

            // 月份缩写常量
            final String[] MONTH_ABBR = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            ContentResolver contentResolver = context.getContentResolver();
            String[] projection = {MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN};

            try (Cursor cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, null)) {

                // 检查游标有效性
                if (cursor == null) {
                    XLog.e(TAG, "【扫描】媒体库查询失败: 返回的Cursor为null");
                    return;
                }

                int cursorCount = cursor.getCount();
                XLog.i(TAG, "【扫描】媒体库查询成功 | 图片总数: " + cursorCount);

                if (cursorCount == 0) {
                    XLog.w(TAG, "【扫描】未找到任何图片 | 请检查权限和媒体库内容");
                    return;
                }

                // 初始化批处理
                int batchSize = calculateOptimalBatchSize(cursorCount);
                List<Photo> batch = new ArrayList<>(batchSize);
                XLog.d(TAG, "【扫描】批处理优化 | 批次大小: " + batchSize);

                // 获取列索引
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);

                // 遍历游标
                Calendar calendar = Calendar.getInstance();
                while (cursor.moveToNext()) {
                    int currentCount = totalScanned.incrementAndGet();

                    // 每100张记录一次进度
                    if (currentCount % 100 == 0) {
                        XLog.d(TAG, "【扫描】扫描进度: " + currentCount + "/" + cursorCount);
                    }

                    try {
                        // 提取数据
                        int mediaStoreId = cursor.getInt(idColumn);
                        String path = cursor.getString(pathColumn);
                        long dateTaken = cursor.getLong(dateTakenColumn);

                        // 路径验证
                        if (path == null || path.isEmpty()) {
                            XLog.w(TAG, "【扫描】跳过无效路径照片 | ID: " + mediaStoreId);
                            continue;
                        }

                        // 添加文件存在性检查
                        File file = new File(path);
                        if (!file.exists()) {
                            XLog.w(TAG, "【扫描】跳过不存在的文件 | ID: " + mediaStoreId + " | 路径: " + path);
                            skippedFiles.incrementAndGet();

                            // 检查数据库中是否有此记录（所有状态）
                            Photo existingPhoto = photoDao.getPhotoByIdSync(mediaStoreId);
                            if (existingPhoto != null) {
                                XLog.d(TAG, "【数据库】删除不存在文件的照片记录 | ID: " + mediaStoreId);
                                photoDao.deletePhoto(existingPhoto);
                                deletedRecords.incrementAndGet();
                            }
                            continue;
                        }

                        // 时间戳处理
                        if (dateTaken <= 0) {
                            dateTaken = System.currentTimeMillis();
                            XLog.w(TAG, "【扫描】时间戳无效 | 使用当前时间: " + path);
                        }

                        // 计算分组
                        calendar.setTimeInMillis(dateTaken);
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH是0基的

                        // 创建图片对象
                        Photo photo = new Photo(mediaStoreId, path, dateTaken, String.valueOf(year), MONTH_ABBR[month - 1]);
                        batch.add(photo);

                        // 批量提交
                        if (batch.size() >= batchSize) {
                            ProcessResult result = processBatch(batch);
                            insertedCount.addAndGet(result.inserted);
                            updatedCount.addAndGet(result.updated);
                            batch.clear();
                            batchCounter.incrementAndGet();
                        }
                    } catch (Exception e) {
                        XLog.e(TAG, "【错误】处理记录失败 | 位置: " + currentCount + " | 错误: " + e.getMessage());
                    }
                }

                // 处理最后一批
                if (!batch.isEmpty()) {
                    ProcessResult result = processBatch(batch);
                    insertedCount.addAndGet(result.inserted);
                    updatedCount.addAndGet(result.updated);
                    batchCounter.incrementAndGet();
                }

                // 性能统计
                long duration = System.currentTimeMillis() - startTime;
                XLog.w(TAG, "【扫描】媒体库扫描完成" +
                        "\n| 批次处理: " + batchCounter.get() +
                        "\n| 总扫描量: " + totalScanned.get() +
                        "\n| 跳过文件: " + skippedFiles.get() +
                        "\n| 删除记录: " + deletedRecords.get() +
                        "\n| 新增: " + insertedCount.get() +
                        "\n| 更新: " + updatedCount.get() +
                        "\n| 耗时: " + duration + "ms | 速度: " +
                        (totalScanned.get() > 0 ? (duration / totalScanned.get()) + "ms/张" : "N/A"));

                // 初始化分组
                //initializeGroups();
                XLog.i(TAG, "【分组】照片分组初始化完成");
            } catch (SecurityException e) {
                XLog.e(TAG, "【权限】权限不足 | 媒体库访问被拒绝: " + e.getMessage());
                PermissionManager.postPermissionRequired(PermissionRequiredEvent.OPERATION_SCAN); // 通知UI层
            } catch (Exception e) {
                XLog.e(TAG, "【错误】扫描未预期错误: " + e.getMessage());
            } finally {
                XLog.w(TAG, "【扫描】===== 媒体库扫描结束 =====");
            }
        });
    }


    /**
     * 计算最优的批处理大小
     * 根据总项目数量动态确定合适的批处理大小，以优化处理性能
     *
     * @param totalItems 总项目数量
     * @return 返回计算得出的最优批处理大小
     */
    private int calculateOptimalBatchSize(int totalItems) {
        if (totalItems <= 500) return 100;
        if (totalItems <= 2000) return 200;
        return 300; // 最大批次大小
    }


    /**
     * 处理一批照片数据，将其插入或更新到数据库中。
     * <p>
     * 该方法会对传入的照片列表进行分类处理：如果照片在数据库中已存在，则更新其信息；
     * 如果是新照片，则以默认状态插入数据库。整个过程使用数据库锁保证线程安全。
     *
     * @param batch 待处理的照片列表，不能为空但可以为null
     * @return 处理结果对象，包含插入、更新和总数等统计信息
     */
    private ProcessResult processBatch(List<Photo> batch) {
        ProcessResult result = new ProcessResult();
        if (batch == null || batch.isEmpty()) {
            XLog.w(TAG, "【批处理】空批次");
            return result;
        }

        // 获取数据库锁（避免并发写入冲突）
        dbLock.lock();
        try {
            long start = System.currentTimeMillis();
            int batchSize = batch.size();
            XLog.d(TAG, "【批处理】开始 | 数量: " + batchSize);

            // 1. 查询数据库中已存在的照片，用于判断是新增还是更新
            List<Long> existingIds = new ArrayList<>();
            for (Photo photo : batch) {
                existingIds.add(photo.mediaStoreId);
            }
            List<Photo> existingPhotos = photoDao.getPhotosByIdsSync(existingIds);
            Map<Long, Photo> existingMap = new HashMap<>();
            for (Photo photo : existingPhotos) {
                existingMap.put(photo.mediaStoreId, photo);
            }

            // 2. 分离出需要新增和需要更新的照片列表
            List<Photo> newPhotos = new ArrayList<>();
            List<Photo> updatePhotos = new ArrayList<>();

            for (Photo photo : batch) {
                Photo existing = existingMap.get(photo.mediaStoreId);
                if (existing != null) {
                    // 保留原有状态
                    photo.status = existing.status;
                    updatePhotos.add(photo);
                } else {
                    // 新照片使用默认状态
                    photo.status = Photo.Status.NORMAL;
                    newPhotos.add(photo);
                }
            }

            // 3. 执行数据库操作：分别插入新照片和更新已有照片
            if (!newPhotos.isEmpty()) {
                try {
                    photoDao.insertPhotos(newPhotos);
                    XLog.d(TAG, "【数据库】新照片插入完成 | 数量: " + newPhotos.size());
                    result.inserted = newPhotos.size();
                } catch (Exception e) {
                    XLog.e(TAG, "【错误】新照片插入失败: " + e.getMessage());
                }
            }

            if (!updatePhotos.isEmpty()) {
                try {
                    photoDao.updatePhotos(updatePhotos);
                    XLog.d(TAG, "【数据库】照片更新完成 | 数量: " + updatePhotos.size());
                    result.updated = updatePhotos.size();
                } catch (Exception e) {
                    XLog.e(TAG, "【错误】照片更新失败: " + e.getMessage());
                }
            }

            // 4. 记录处理结果并输出性能日志
            result.total = batchSize;

            // 性能日志
            long duration = System.currentTimeMillis() - start;
            XLog.d(TAG, "【批处理】完成 | 总数: " + batchSize +
                    " | 插入: " + result.inserted +
                    " | 更新: " + result.updated +
                    " | 耗时: " + duration + "ms");
            return result;
        } catch (OutOfMemoryError oom) {
            XLog.e(TAG, "【错误】内存不足错误 | 批次大小: " + batch.size());
            return tryRecoverFromOOM(batch); // 内存错误恢复
        } catch (Exception e) {
            XLog.e(TAG, "【错误】批处理失败 | 数量: " + batch.size() + " | 错误: " + e.getMessage());
            return result;
        } finally {
            dbLock.unlock();
        }
    }

    /**
     * 尝试从OOM（Out of Memory）错误中恢复处理
     * 当处理图片批次时发生内存不足异常，通过减少批次大小的方式进行恢复处理
     *
     * @param batch 需要处理的图片列表批次
     * @return ProcessResult 处理结果，包含插入和更新的记录数
     */
    private ProcessResult tryRecoverFromOOM(List<Photo> batch) {
        XLog.w(TAG, "【恢复】内存恢复机制启动 | 原批次: " + batch.size());
        ProcessResult result = new ProcessResult();

        // 尝试减少批次大小重试
        int newBatchSize = batch.size() / 2;
        if (newBatchSize < 10) newBatchSize = 10;

        XLog.i(TAG, "【恢复】分批重试 | 新批次: " + newBatchSize);

        // 分割批次重试
        List<Photo> subBatch = new ArrayList<>(newBatchSize);
        for (Photo photo : batch) {
            subBatch.add(photo);
            if (subBatch.size() >= newBatchSize) {
                ProcessResult subResult = processBatch(subBatch);
                result.inserted += subResult.inserted;
                result.updated += subResult.updated;
                subBatch.clear();
            }
        }

        // 处理剩余部分
        if (!subBatch.isEmpty()) {
            ProcessResult subResult = processBatch(subBatch);
            result.inserted += subResult.inserted;
            result.updated += subResult.updated;
        }

        return result;
    }


}
