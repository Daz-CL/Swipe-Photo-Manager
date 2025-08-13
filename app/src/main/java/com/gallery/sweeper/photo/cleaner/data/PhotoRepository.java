package com.gallery.sweeper.photo.cleaner.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.app.App;
import com.gallery.sweeper.photo.cleaner.app.SPConstants;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoDao;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoGroupDao;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;
import com.gallery.sweeper.photo.cleaner.data.events.GroupEvent;
import com.gallery.sweeper.photo.cleaner.data.events.PhotoStatusChangedEvent;
import com.gallery.sweeper.photo.cleaner.data.events.ReloadGroupEvent;
import com.gallery.sweeper.photo.cleaner.permission.PermissionManager;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;
import com.gallery.sweeper.photo.cleaner.utis.SPUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 照片仓库管理
 * 负责媒体库扫描和照片删除操作
 * 优化：添加操作前权限检查，规范化日志输出
 */
public class PhotoRepository {

    private static final String TAG = "PhotoRepository";

    // 单例实例 - volatile确保多线程可见性
    private static volatile PhotoRepository instance;

    private static final long CACHE_EXPIRATION = 5 * 60 * 1000; // 5分钟缓存过期时间
    private static final int MAX_CACHE_SIZE = 10; // 最大缓存条目数

    // DAO接口
    private final PhotoDao photoDao;
    private final PhotoGroupDao photoGroupDao;

    // 线程资源
    private final ExecutorService executor;
    private final ReentrantLock dbLock = new ReentrantLock(); // 数据库操作锁
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false); // 关闭状态标志

    // 缓存系统
    private final Map<String, CacheEntry> groupCache = new ConcurrentHashMap<>();
    private final Map<String, PhotoGroup> singleGroupCache = new ConcurrentHashMap<>(); // 单个分组缓存
    private final Context context;

    // 单例初始化控制
    private static final Object initLock = new Object();

    // 清理任务参数
    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24小时
    private static final int BATCH_DELETE_SIZE = 100; // 每次删除的最大记录数
    private static final int BATCH_PROCESS_SIZE = 200; // 批处理大小

    private final Handler cleanupHandler = new Handler(Looper.getMainLooper());
    private final Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {
            //cleanupNonExistingPhotos();
            // 安排下一次清理
            cleanupHandler.postDelayed(this, CLEANUP_INTERVAL);
        }
    };


    private PhotoRepository(Context context) {
        this.context = context.getApplicationContext();
        PhotoDatabase database = PhotoDatabase.getInstance(this.context);

        // 初始化DAO
        this.photoDao = database.photoDao();
        this.photoGroupDao = database.photoGroupDao();

        // 初始化事件总线
        //initEventBus();

        // 创建单线程执行器（确保操作顺序性）
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PhotoRepository-Worker");
            thread.setPriority(Thread.NORM_PRIORITY - 1); // 降低优先级避免阻塞UI
            return thread;
        });
        XLog.i(TAG, "【系统】PhotoRepository初始化完成");

        // 启动定期清理
        //startPeriodicCleanup();
        XLog.i(TAG, "【系统】PhotoRepository初始化完成");
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
     * 扫描设备媒体库中的图片
     * 优化：添加前置权限检查
     *
     * @param context 上下文对象
     */

    public void scanMediaStore(Context context) {
        XLog.d(TAG, "【相册扫描】开始媒体库扫描流程");

        // 权限检查
        if (!PermissionManager.hasPermission(PermissionManager.PermissionType.SCAN)) {
            XLog.e(TAG, "【权限处理】无相册扫描权限，终止操作");
            EventBus.getDefault().post(new PermissionRequiredEvent(
                    PermissionRequiredEvent.OPERATION_SCAN));
            return;
        }

        // 检查关闭状态
        if (isShuttingDown.get()) {
            XLog.e(TAG, "【系统】扫描操作被拒绝: 系统正在关闭");
            return;
        }

        executor.execute(() -> {
            XLog.w(TAG, "【相册扫描】===== 开始扫描媒体库 =====");
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
                    XLog.e(TAG, "【相册扫描】媒体库查询失败: 返回的Cursor为null");
                    return;
                }

                int cursorCount = cursor.getCount();
                XLog.i(TAG, "【相册扫描】媒体库查询成功 | 图片总数: " + cursorCount);

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
                initializeGroups();
                //XLog.i(TAG, "【分组】照片分组初始化完成");
            } catch (SecurityException e) {
                XLog.e(TAG, "【权限处理】媒体库访问被拒绝: " + e.getMessage());
                //postPermissionRequired(PermissionRequiredEvent.OPERATION_SCAN); // 通知UI层
            } catch (Exception e) {
                XLog.e(TAG, "【错误】扫描未预期错误: " + e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                XLog.w(TAG, "【相册扫描】===== 媒体库扫描结束 ===== | 耗时: " + duration + "ms");
            }
        });
    }

    /**
     * 动态计算批次大小
     */
    private int calculateOptimalBatchSize(int totalItems) {
        if (totalItems <= 500) return 100;
        if (totalItems <= 2000) return 200;
        return 300; // 最大批次大小
    }

    /**
     * 处理批数据操作
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

            // 1. 获取数据库中已存在的照片
            List<Long> existingIds = new ArrayList<>();
            for (Photo photo : batch) {
                existingIds.add(photo.mediaStoreId);
            }
            List<Photo> existingPhotos = photoDao.getPhotosByIdsSync(existingIds);
            Map<Long, Photo> existingMap = new HashMap<>();
            for (Photo photo : existingPhotos) {
                existingMap.put(photo.mediaStoreId, photo);
            }

            // 2. 分离新照片和需要更新的照片
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

            // 3. 执行数据库操作
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

            // 4. 记录结果
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
     * 内存错误恢复机制
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

    /**
     * 获取分组照片并合并待恢复照片
     */
    public LiveData<List<Photo>> getPhotos(PhotoGroup group, List<Photo> pendingPhotos) {
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>();
        executor.execute(() -> {
            List<Photo> photos = new ArrayList<>();
            boolean ascending = isAscending();

            if (group.groupType.equals(GroupType.MONTH.toString())) {
                // 月份分组查询所有照片（包括不同状态）
                photos = photoDao.getAllPhotosByMonthSync(group.yearGroup, group.monthGroup);
            } else if (group.groupType.equals(GroupType.YEAR.toString())) {
                // 年份分组查询所有照片（包括不同状态）
                photos = photoDao.getAllPhotosByYearSync(group.yearGroup);
            }

            // 根据排序方向对结果进行排序
            if (!ascending) {
                photos.sort((p1, p2) -> Long.compare(p2.dateTaken, p1.dateTaken));
            } else {
                photos.sort((p1, p2) -> Long.compare(p1.dateTaken, p2.dateTaken));
            }

            XLog.d(TAG, "【分组】获取分组照片 | 类型: " + group.groupType +
                    " | 年份: " + group.yearGroup +
                    " | 月份: " + group.monthGroup +
                    " | 数量: " + photos.size());

            // 合并待恢复照片
            if (pendingPhotos != null && !pendingPhotos.isEmpty()) {
                List<Photo> mergedPhotos = new ArrayList<>(photos);
                for (Photo pendingPhoto : pendingPhotos) {
                    // 避免重复添加
                    boolean exists = false;
                    for (Photo photo : mergedPhotos) {
                        if (photo.mediaStoreId == pendingPhoto.mediaStoreId) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        mergedPhotos.add(0, pendingPhoto);
                    }
                }
                XLog.d(TAG, "【恢复】合并待恢复照片 | 原数量: " + photos.size() +
                        " | 待恢复: " + pendingPhotos.size() +
                        " | 合并后: " + mergedPhotos.size());
                photos = mergedPhotos;
            }
            liveData.postValue(photos);
        });
        return liveData;
    }

    /**
     * 获取照片状态为指定状态的图片
     */
    public LiveData<List<Photo>> getPhotosByStatus(Photo.Status status) {
        XLog.d(TAG, "【数据库】获取照片状态为 | " + status + " | 的图片");

        LiveData<List<Photo>> liveData = photoDao.getPhotosByStatus(status);

        return Transformations.map(liveData, photos -> {
            if (photos == null) {
                XLog.w(TAG, "【数据库】查询返回空结果 | 状态: " + status);
                return new ArrayList<>();
            }

            XLog.i(TAG, "【数据库】获取照片状态为指定状态的图片 | 查询结果 | 状态: " + status + " | 数量: " + photos.size());
            return photos;
        });
    }

    /**
     * 状态更新方法（完整优化版）
     */
    public void updatePhotoStatus(long mediaId, Photo.Status newStatus) {
        // 检查关闭状态
        if (isShuttingDown.get()) {
            XLog.e(TAG, "【系统】更新操作被拒绝: 系统正在关闭");
            return;
        }

        executor.execute(() -> {
            XLog.i(TAG, "【数据库】开始更新照片状态 | ID: " + mediaId + " | 新状态: " + newStatus);
            long startTime = System.currentTimeMillis();

            try {
                // 获取数据库锁
                dbLock.lock();

                // 1. 获取当前照片状态
                Photo existingPhoto = photoDao.getPhotoByIdSync(mediaId);
                if (existingPhoto == null) {
                    XLog.e(TAG, "【数据库】照片不存在 | ID: " + mediaId);
                    return;
                }

                Photo.Status oldStatus = existingPhoto.status;
                XLog.d(TAG, "【数据库】当前状态 | ID: " + mediaId + " | 旧状态: " + oldStatus);

                // 2. 更新数据库状态
                photoDao.updateStatus(mediaId, newStatus);

                // 3. 获取照片对象用于后续处理
                Photo photo = photoDao.getPhotoByIdSync(mediaId);
                if (photo == null) {
                    XLog.e(TAG, "【数据库】照片更新后不存在 | ID: " + mediaId);
                    return;
                }

                XLog.d(TAG, "【数据库】状态更新完成 | ID: " + mediaId + " | 新状态: " + photo.getStatus());

                // 4. 检查文件存在性
                File file = new File(photo.path);
                if (!file.exists()) {
                    XLog.w(TAG, "【文件】照片文件不存在 | ID: " + mediaId + " | 路径: " + photo.path);

                    // 直接删除记录（所有状态）
                    photoDao.deletePhoto(photo);
                    XLog.i(TAG, "【数据库】已删除不存在文件的照片记录 | ID: " + mediaId);
                    return;
                }

                // 5. 根据状态执行不同操作
                if (newStatus == Photo.Status.TRASHED) {
                    handlePhotoTrashed(photo);
                } else if (newStatus == Photo.Status.KEEP) {
                    handlePhotoKept(photo);
                } else if (newStatus == Photo.Status.NORMAL) {
                    handlePhotoNormal(photo);
                }

                // 6. 发送状态更新事件
                PhotoStatusChangedEvent statusEvent = new PhotoStatusChangedEvent(mediaId, oldStatus, newStatus, photo);
                EventBus.getDefault().post(statusEvent);
                XLog.d(TAG, "【事件】状态更新事件已发送 | ID: " + mediaId);

                // 7. 状态变更时更新分组缓存
                clearGroupCacheForGroup(photo.yearGroup, photo.monthGroup);
                XLog.d(TAG, "【缓存】分组缓存已清除: " + photo.yearGroup + "-" + photo.monthGroup);

                // 合并日志输出
                XLog.i(TAG, "【状态】照片状态更新完成 | ID: " + mediaId +
                        " | 耗时: " + (System.currentTimeMillis() - startTime) + "ms" +
                        " | 旧状态: " + oldStatus + " -> 新状态: " + newStatus);
            } catch (Exception e) {
                XLog.e(TAG, "【错误】状态更新失败 | ID: " + mediaId + " | 错误: " + e.getMessage());
            } finally {
                dbLock.unlock();
            }
        });
    }

    /**
     * 处理照片设为保护状态逻辑
     */
    private void handlePhotoKept(Photo photo) {
        XLog.d(TAG, "【操作】处理照片设为保护状态 | ID: " + photo.mediaStoreId);

        // 1. 更新分组信息
        updateGroupsAfterStatusChange(photo);

        // 2. 清除相关缓存
        clearGroupCacheForGroup(photo.yearGroup, photo.monthGroup);
        XLog.i(TAG, "【操作】照片已设为保护状态 | ID: " + photo.mediaStoreId);
    }

    /**
     * 处理照片恢复为正常状态逻辑
     */
    private void handlePhotoNormal(Photo photo) {
        XLog.d(TAG, "【操作】处理照片恢复为正常状态 | ID: " + photo.mediaStoreId);

        // 1. 更新分组信息
        updateGroupsAfterStatusChange(photo);

        // 2. 清除相关缓存
        clearGroupCacheForGroup(photo.yearGroup, photo.monthGroup);
        XLog.i(TAG, "【操作】照片已恢复为正常状态 | ID: " + photo.mediaStoreId);
    }

    /**
     * 处理照片放入垃圾桶逻辑
     */
    private void handlePhotoTrashed(Photo photo) {
        XLog.d(TAG, "【操作】处理照片放入垃圾桶 | ID: " + photo.mediaStoreId);

        // 1. 更新分组信息
        updateGroupsAfterStatusChange(photo);

        // 2. 清除相关缓存
        clearGroupCacheForGroup(photo.yearGroup, photo.monthGroup);
        XLog.i(TAG, "【操作】照片已放入垃圾桶 | ID: " + photo.mediaStoreId);
    }

    /**
     * 初始化分组数据
     */
    public void initializeGroups() {
        // 检查关闭状态
        if (isShuttingDown.get()) {
            XLog.e(TAG, "【分组】初始化拒绝 | 系统正在关闭");
            return;
        }

        executor.execute(() -> {
            XLog.w(TAG, "【分组】===== 开始分组初始化 =====");
            long startTime = System.currentTimeMillis();

            dbLock.lock();
            try {
                // 1. 获取当前分组类型
                int storedGroupType = (int) SPUtils.get(SPConstants.GROUP_TYPE, GroupTypeConverters.groupTypeToInt(GroupType.MONTH));
                GroupType groupType = GroupTypeConverters.groupTypeFromInt(storedGroupType);
                XLog.i(TAG, "【分组】当前分组类型: " + groupType);

                // 2. 清除旧分组数据
                int deletedCount = photoGroupDao.deleteAllGroups();
                XLog.i(TAG, "【数据库】旧分组删除完成 | 数量: " + deletedCount);

                // 3. 生成新分组
                List<PhotoGroup> allGroups = new ArrayList<>();

                // 年份分组
                List<PhotoGroup> yearGroups = photoDao.aggregateYearGroups();
                if (yearGroups != null && !yearGroups.isEmpty()) {
                    yearGroups.forEach(group -> {
                        group.groupType = GroupType.YEAR.toString();
                        group.displayName = group.yearGroup;
                    });
                    allGroups.addAll(yearGroups);
                    XLog.i(TAG, "【分组】年份分组聚合完成 | 数量: " + yearGroups.size());
                }

                // 月份分组
                List<PhotoGroup> monthGroups = photoDao.aggregateMonthGroups();
                if (monthGroups != null && !monthGroups.isEmpty()) {
                    monthGroups.forEach(group -> {
                        group.groupType = GroupType.MONTH.toString();
                        group.displayName = group.yearGroup + " " + group.monthGroup;
                    });
                    allGroups.addAll(monthGroups);
                    XLog.i(TAG, "【分组】月份分组聚合完成 | 数量: " + monthGroups.size());
                }

                // 4. 保存到数据库
                if (!allGroups.isEmpty()) {
                    photoGroupDao.insertGroups(allGroups);
                    XLog.i(TAG, "【数据库】分组保存完成 | 总数: " + allGroups.size());

                    // 发送分组加载完成事件
                    EventBus.getDefault().post(new GroupEvent.LoadedEvent(getCurrentGroupType() == GroupType.YEAR ? yearGroups : monthGroups));
                }

                // 5. 验证数据
                int dbYearCount = photoGroupDao.countByGroupType(GroupType.YEAR.toString());
                int dbMonthCount = photoGroupDao.countByGroupType(GroupType.MONTH.toString());

                boolean isValid = ((yearGroups == null || dbYearCount == yearGroups.size()) &&
                        (monthGroups == null || dbMonthCount == monthGroups.size()));

                if (isValid) {
                    XLog.i(TAG, "【分组】数据验证成功");
                } else {
                    XLog.e(TAG, "【错误】数据不一致 | 年份: " + dbYearCount + "/" + (yearGroups != null ? yearGroups.size() : 0) + " | 月份: " + dbMonthCount + "/" + (monthGroups != null ? monthGroups.size() : 0));
                }
            } catch (Exception e) {
                XLog.e(TAG, "【错误】分组初始化失败: " + e.getMessage());
            } finally {
                dbLock.unlock();
                clearGroupCache();

                long totalTime = System.currentTimeMillis() - startTime;
                XLog.w(TAG, "【分组】初始化完成 | 耗时: " + totalTime + "ms");
                XLog.w(TAG, "【分组】===== 分组初始化结束 =====");
            }
        });
    }

    /**
     * 清除分组缓存
     */
    public void clearGroupCache() {
        int count = groupCache.size();
        groupCache.clear();
        XLog.d(TAG, "【缓存】分组缓存已清空 | 清理项: " + count);
    }

    /**
     * 获取分组数据（带缓存）
     */
    public LiveData<List<PhotoGroup>> getGroups(GroupType type) {
        boolean ascending = isAscending();
        XLog.i(TAG, "【分组】请求分组数据 | 类型: " + type + " | 排序: " + (ascending ? "升序" : "降序"));

        // 创建缓存键
        String cacheKey = type.name() + "_" + (ascending ? "ASC" : "DESC");

        // 检查缓存
        synchronized (groupCache) {
            if (groupCache.containsKey(cacheKey)) {
                CacheEntry entry = groupCache.get(cacheKey);
                if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRATION) {
                    XLog.d(TAG, "【缓存】命中 | 键: " + cacheKey);
                    return new MutableLiveData<>(entry.groups);
                }
            }
        }

        XLog.d(TAG, "【缓存】未命中 | 从数据库加载: " + type);

        // 获取数据库数据
        LiveData<List<PhotoGroup>> dbLiveData = ascending ?
                photoGroupDao.getGroupsAsc(type.toString()) :
                photoGroupDao.getGroupsDesc(type.toString());

        // 转换结果并更新缓存
        return Transformations.map(dbLiveData, groups -> {
            if (groups == null || groups.isEmpty()) {
                XLog.w(TAG, "【数据库】返回空分组 | 类型: " + type);
                return groups;
            }

            // 更新缓存
            synchronized (groupCache) {
                groupCache.put(cacheKey, new CacheEntry(groups, System.currentTimeMillis()));
                cleanExpiredCache();
            }

            XLog.i(TAG, "【分组】数据加载完成 | 类型: " + type + " | 数量: " + groups.size());
            return groups;
        });
    }

    /**
     * 获取分组对象
     */
    public LiveData<PhotoGroup> getGroup(@NonNull String groupType, @NonNull String groupKey) {
        XLog.w(TAG, "【分组】获取分组数据 | 分组类型: " + groupType + " | 分组键: " + groupKey);
        String typeSuffix = isAscending() ? "_ASC" : "_DESC";
        groupType = groupType + typeSuffix;
        XLog.d(TAG, "【分组】分组类型: " + groupType);

        // 1. 首先检查缓存
        PhotoGroup cachedGroup = getGroupFromCache(groupKey);
        if (cachedGroup != null) {
            XLog.d(TAG, "【缓存】分组缓存命中 | 键: " + groupKey);
            return new MutableLiveData<>(cachedGroup);
        }

        XLog.d(TAG, "【缓存】分组缓存未命中 | 从数据库加载: " + groupKey);
        // 2. 使用 LiveData 从数据库加载分组
        return photoGroupDao.getGroupByKey(groupKey);
    }

    /**
     * 从缓存中获取分组
     */
    private PhotoGroup getGroupFromCache(String groupKey) {
        synchronized (groupCache) {
            for (CacheEntry entry : groupCache.values()) {
                for (PhotoGroup group : entry.groups) {
                    if (group.groupKey.equals(groupKey)) {
                        return group;
                    }
                }
            }
        }
        return null;
    }


    /**
     * 同步获取分组（带缓存）
     */
    public PhotoGroup getGroupByKeySync(String groupKey) {
        XLog.d(TAG, "【数据库】查询分组 | 键: " + groupKey);

        // 1. 检查缓存
        if (singleGroupCache.containsKey(groupKey)) {
            XLog.d(TAG, "【缓存】命中分组缓存 | 键: " + groupKey);
            return singleGroupCache.get(groupKey);
        }

        // 2. 从数据库加载
        PhotoGroup group = null;
        try {
            dbLock.lock();
            group = photoGroupDao.getGroupByKeySync(groupKey);
        } finally {
            dbLock.unlock();
        }

        // 3. 存入缓存
        if (group != null) {
            singleGroupCache.put(groupKey, group);
            XLog.d(TAG, "【缓存】缓存分组 | 键: " + groupKey);
        } else {
            XLog.w(TAG, "【数据库】未找到分组 | 键: " + groupKey);
        }

        return group;
    }


    /**
     * 状态变更后更新分组信息
     */
    private void updateGroupsAfterStatusChange(Photo photo) {
        String yearGroup = photo.yearGroup;
        String monthGroup = photo.monthGroup;

        XLog.d(TAG, "【分组】更新分组信息 | ID: " + photo.mediaStoreId + " | 年份: " + yearGroup + " | 月份: " + monthGroup);

        // 初始化计数器
        int updatedCount = 0;
        int deletedCount = 0;

        // 获取受影响的分组（包括月份和年份分组）
        List<PhotoGroup> affectedGroups = new ArrayList<>();

        // 添加月份分组
        PhotoGroup monthGroupObj = photoGroupDao.getGroupByKeySync(yearGroup + "-" + monthGroup);
        if (monthGroupObj != null) {
            affectedGroups.add(monthGroupObj);
        }
        // 添加年份分组
        PhotoGroup yearGroupObj = photoGroupDao.getGroupByKeySync(yearGroup);
        if (yearGroupObj != null) {
            affectedGroups.add(yearGroupObj);
        }
        if (affectedGroups.isEmpty()) {
            XLog.w(TAG, "【分组】未找到受影响分组 | 年份: " + yearGroup + " | 月份: " + monthGroup);
            return;
        }
        XLog.d(TAG, "【分组】受影响分组数量: " + affectedGroups.size());

        // 更新每组数据
        for (int i = 0; i < affectedGroups.size(); i++) {
            PhotoGroup group = affectedGroups.get(i);
            // 重新计算分组照片数量
            if (group.groupKey.contains("-")) {
                group.photoCount = photoDao.countPhotosInGroupByMonth(group.yearGroup, group.monthGroup);
            } else {
                group.photoCount = photoDao.countPhotosInGroupByYear(group.yearGroup);
            }

            // 更新状态计数
            if (group.groupKey.contains("-")) { // 月份分组
                group.trashCount = photoDao.countTrashPhotosInGroup(group.yearGroup, group.monthGroup);
                group.keepCount = photoDao.countKeepPhotosInGroup(group.yearGroup, group.monthGroup);
            } else { // 年份分组
                group.trashCount = photoDao.countTrashPhotosInYear(group.yearGroup);
                group.keepCount = photoDao.countKeepPhotosInYear(group.yearGroup);
            }

            // 更新分组封面
            updateGroupCover(i, group);

            // 更新分组数据
            if (group.photoCount > 0) {
                photoGroupDao.updateGroup(group);
                updatedCount++;
                XLog.i(TAG, "【数据库】分组更新完成 | 分组: " + group.groupKey);

                // 更新缓存中的分组数据
                updateGroupInCache(i, group);
            } else {
                // 分组中没有照片则删除
                photoGroupDao.deleteGroup(group);
                deletedCount++;
                XLog.w(TAG, "【数据库】分组已删除 | 无照片: " + group.groupKey);
            }
        }

        // 合并日志输出
        XLog.i(TAG, "【分组】受影响分组处理完成 | 总数: " + affectedGroups.size() + " | 更新: " + updatedCount + " | 删除: " + deletedCount);
    }

    /**
     * 更新缓存中的分组数据
     */
    public void updateGroupInCache(int position, PhotoGroup updatedGroup) {
        if (updatedGroup == null) return;

        XLog.d(TAG, "【缓存】更新缓存分组 | 分组键: " + updatedGroup.groupKey + " | 位置:" + position);

        synchronized (groupCache) {
            // 1. 更新所有包含该分组的缓存条目
            for (Map.Entry<String, CacheEntry> entry : groupCache.entrySet()) {
                CacheEntry cacheEntry = entry.getValue();
                boolean updated = false;

                for (int i = 0; i < cacheEntry.groups.size(); i++) {
                    PhotoGroup group = cacheEntry.groups.get(i);
                    if (group.groupKey.equals(updatedGroup.groupKey)) {
                        cacheEntry.groups.set(i, updatedGroup);
                        updated = true;
                        XLog.e(TAG, "【缓存】分组更新成功 | 缓存键: " + entry.getKey() + " | 分组: " + updatedGroup.groupKey);
                        EventBus.getDefault().post(new GroupEvent.UpdateEvent(updatedGroup));
                        break;
                    }
                }

                if (updated) {
                    cacheEntry.timestamp = System.currentTimeMillis();
                }
            }

            // 2. 特殊处理年份分组缓存
            if (updatedGroup.groupType.equals(GroupType.YEAR.toString())) {
                String yearCacheKey = GroupType.YEAR + "_" + (isAscending() ? "ASC" : "DESC");
                CacheEntry yearEntry = groupCache.get(yearCacheKey);

                if (yearEntry != null) {
                    for (int i = 0; i < yearEntry.groups.size(); i++) {
                        if (yearEntry.groups.get(i).groupKey.equals(updatedGroup.groupKey)) {
                            yearEntry.groups.set(i, updatedGroup);
                            yearEntry.timestamp = System.currentTimeMillis();
                            XLog.d(TAG, "【缓存】年份分组缓存更新 | 缓存: " + yearCacheKey);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新分组封面
     */
    private void updateGroupCover(int position, PhotoGroup group) {
        XLog.d(TAG, "【分组】更新分组封面 | 分组: " + group.groupKey);

        // 查找最新的非TRASHED照片作为封面
        Photo newCover;

        if (group.getGroupKey().contains("-")) {
            newCover = photoDao.findLatestPhotoInGroupByMonth(group.yearGroup, group.monthGroup);
        } else {
            newCover = photoDao.findLatestPhotoInGroupYear(group.yearGroup);
        }

        if (newCover != null) {
            group.groupCover = newCover.path;
            group.coverMediaId = newCover.mediaStoreId;
            XLog.i(TAG, "【分组】封面更新成功 | 分组: " + group.groupKey);
        } else {
            // 仅重置封面信息
            group.groupCover = null;
            group.coverMediaId = 0;
            XLog.w(TAG, "【分组】封面不可用 | 保留空封面分组: " + group.groupKey);
        }
    }

    private void updateAffectedGroups(Set<String> groupKeys) {
        try {
            dbLock.lock();
            XLog.d(TAG, "【分组更新】开始更新受影响分组 | 数量: " + groupKeys.size());

            for (String groupKey : groupKeys) {
                try {
                    PhotoGroup group = photoGroupDao.getGroupByKeySync(groupKey);
                    if (group == null) {
                        XLog.w(TAG, "【分组更新】分组不存在 | 分组键: " + groupKey);
                        continue;
                    }

                    // 根据分组类型执行不同的更新逻辑
                    if (group.groupType.equals(GroupType.YEAR.toString())) {
                        // 年份分组更新
                        group.photoCount = photoDao.countPhotosInGroupByYear(group.yearGroup);
                        group.trashCount = photoDao.countTrashPhotosInYear(group.yearGroup);
                        group.keepCount = photoDao.countKeepPhotosInYear(group.yearGroup);
                    } else {
                        // 月份分组更新
                        String[] parts = groupKey.split("-");
                        if (parts.length < 2) {
                            XLog.w(TAG, "【分组更新】无效的月份分组键: " + groupKey);
                            continue;
                        }
                        group.photoCount = photoDao.countPhotosInGroupByMonth(parts[0], parts[1]);
                        group.trashCount = photoDao.countTrashPhotosInGroup(parts[0], parts[1]);
                        group.keepCount = photoDao.countKeepPhotosInGroup(parts[0], parts[1]);
                    }

                    // 更新封面
                    updateGroupCover(0, group);

                    // 保存更新
                    if (group.photoCount > 0) {
                        photoGroupDao.updateGroup(group);
                        XLog.d(TAG, "【数据库】分组更新完成 | 分组: " + group.groupKey);
                    } else {
                        photoGroupDao.deleteGroup(group);
                        XLog.w(TAG, "【数据库】删除空分组 | 分组: " + group.groupKey);
                    }
                } catch (Exception e) {
                    XLog.e(TAG, "【错误】更新分组失败 | 分组键: " + groupKey + " | 错误: " + e.getMessage());
                }
            }
        } finally {
            dbLock.unlock();
        }
    }

    /**
     * 清除特定分组的缓存
     */
    public void clearGroupCacheForGroup(String yearGroup, String monthGroup) {
        XLog.d(TAG, "【缓存】清除分组缓存 | 年份: " + yearGroup + " | 月份: " + monthGroup);

        synchronized (groupCache) {
            List<String> keysToRemove = new ArrayList<>();

            for (Map.Entry<String, CacheEntry> entry : groupCache.entrySet()) {
                boolean shouldRemove = false;

                for (PhotoGroup group : entry.getValue().groups) {
                    // 检查年份是否匹配
                    if (yearGroup.equals(group.yearGroup)) {
                        // 如果月份参数为空，清除该年份的所有分组
                        if (monthGroup == null) {
                            shouldRemove = true;
                            break;
                        }
                        // 如果月份参数不为空，检查月份分组
                        else {
                            // 处理月份分组为空的情况
                            if (group.monthGroup == null) {
                                // 年份分组 - 月份参数不为空时不匹配
                            } else if (monthGroup.equals(group.monthGroup)) {
                                // 月份分组匹配
                                shouldRemove = true;
                                break;
                            }
                        }
                    }
                }

                if (shouldRemove) {
                    keysToRemove.add(entry.getKey());
                }
            }

            keysToRemove.forEach(groupCache::remove);
            XLog.d(TAG, "【缓存】清除完成 | 数量: " + keysToRemove.size());
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        synchronized (groupCache) {
            // 寻找过期项
            groupCache.forEach((key, entry) -> {
                if (now - entry.timestamp > CACHE_EXPIRATION) {
                    toRemove.add(key);
                }
            });

            // 移除过期项
            toRemove.forEach(key -> {
                groupCache.remove(key);
                XLog.d(TAG, "【缓存】过期移除 | 键: " + key);
            });

            // 检查缓存上限
            if (groupCache.size() > MAX_CACHE_SIZE) {
                groupCache.keySet().stream()
                        .skip(MAX_CACHE_SIZE)
                        .forEach(key -> {
                            groupCache.remove(key);
                            XLog.d(TAG, "【缓存】上限移除 | 键: " + key);
                        });
            }
        }
    }

    /**
     * 永久删除照片（带权限检查）
     */
    public void deletePhotosPermanently(List<Long> mediaIds) {
        // 检查存储权限
        if (mediaIds == null || mediaIds.isEmpty()) {
            XLog.w(TAG, "【照片删除】空照片列表，取消操作");
            return;
        }

        XLog.d(TAG, "【照片删除】准备删除" + mediaIds.size() + "张照片");

        // 权限检查
        if (!PermissionManager.hasPermission(PermissionManager.PermissionType.DELETE)) {
            XLog.e(TAG, "【权限处理】无照片删除权限，终止操作");
            EventBus.getDefault().post(new PermissionRequiredEvent(
                    PermissionRequiredEvent.OPERATION_DELETE));
            return;
        }

        if (isShuttingDown.get()) {
            XLog.e(TAG, "【删除操作】永久删除操作被拒绝: 系统正在关闭");
            return;
        }

        executor.execute(() -> {
            XLog.w(TAG, "【删除操作】===== 开始永久删除照片 =====");
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int failureCount = 0;
            Set<String> affectedGroups = new LinkedHashSet<>();

            // 获取照片详细信息
            List<Photo> photosToDelete;
            try {
                dbLock.lock();
                photosToDelete = photoDao.getPhotosByIdsSync(mediaIds);
            } finally {
                dbLock.unlock();
            }

            if (photosToDelete == null || photosToDelete.isEmpty()) {
                XLog.w(TAG, "【数据库】未找到要删除的照片 | ID数量: " + mediaIds.size());
                return;
            }

            XLog.i(TAG, "【数据库】获取待删除照片成功 | 数量: " + photosToDelete.size());

            // 先删除文件
            List<Long> successfullyDeletedFiles = new ArrayList<>();
            for (Photo photo : photosToDelete) {
                if (deletePhotoFile(photo)) {
                    successfullyDeletedFiles.add(photo.mediaStoreId);
                    successCount++;

                    // 收集受影响的分组
                    affectedGroups.add(photo.yearGroup);
                    affectedGroups.add(photo.yearGroup + "-" + photo.monthGroup);
                } else {
                    failureCount++;
                    XLog.w(TAG, "【文件】照片文件删除失败 | ID: " + photo.mediaStoreId);
                }
            }

            // 只删除文件删除成功的记录
            if (!successfullyDeletedFiles.isEmpty()) {
                try {
                    dbLock.lock();
                    int deletedRecords = photoDao.deletePhotosByIds(successfullyDeletedFiles);
                    XLog.i(TAG, "【数据库】删除照片记录成功 | 数量: " + deletedRecords);
                } finally {
                    dbLock.unlock();
                }
            }

            // 更新分组信息
            if (!affectedGroups.isEmpty()) {
                XLog.d(TAG, "【分组更新】开始更新受影响的分组 | 数量: " + affectedGroups.size());
                updateAffectedGroups(affectedGroups);

                // 发送事件通知UI刷新
                XLog.d(TAG, "【事件】发送分组刷新事件 | 受影响分组数量: " + affectedGroups.size());
                EventBus.getDefault().post(new ReloadGroupEvent(affectedGroups));
            }

            long duration = System.currentTimeMillis() - startTime;
            XLog.w(TAG, "【删除操作】永久删除完成 | 成功: " + successCount +
                    " | 失败: " + failureCount + " | 耗时: " + duration + "ms");
            XLog.w(TAG, "【删除操作】===== 永久删除结束 =====");
        });
    }

    /**
     * 删除单个照片文件
     */
    private boolean deletePhotoFile(Photo photo) {
        XLog.d(TAG, "【文件】开始删除照片文件 | ID: " + photo.mediaStoreId + " | 路径: " + photo.path);

        try {
            File file = new File(photo.path);
            if (!file.exists()) {
                XLog.w(TAG, "【文件】照片文件不存在 | ID: " + photo.mediaStoreId);
                return true; // 文件不存在视为删除成功
            }

            // 1. 尝试标准删除
            if (file.delete()) {
                XLog.d(TAG, "【文件】文件删除成功 | ID: " + photo.mediaStoreId);
                return true;
            }

            // 2. 标准删除失败时的备选方案
            XLog.w(TAG, "【文件】标准删除失败，尝试MediaStore删除 | ID: " + photo.mediaStoreId);
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    photo.mediaStoreId
            );

            int deleted = context.getContentResolver().delete(uri, null, null);
            if (deleted > 0) {
                XLog.d(TAG, "【文件】MediaStore删除成功 | ID: " + photo.mediaStoreId);
                return true;
            }

            XLog.e(TAG, "【文件】所有删除方法均失败 | 路径: " + photo.path);
            return false;
        } catch (SecurityException e) {
            XLog.e(TAG, "【权限】删除文件权限不足 | 路径: " + photo.path + " | 错误: " + e.getMessage());
            return false;
        } catch (Exception e) {
            XLog.e(TAG, "【错误】删除文件异常 | 路径: " + photo.path + " | 错误: " + e.getMessage());
            return false;
        }
    }

    public GroupType getCurrentGroupType() {
        return GroupTypeConverters.groupTypeFromInt((int) SPUtils.get(SPConstants.GROUP_TYPE, GroupTypeConverters.groupTypeToInt(GroupType.MONTH)));
    }

    public void setCurrentGroupType(GroupType currentGroupType) {
        SPUtils.save(SPConstants.GROUP_TYPE, currentGroupType.ordinal());
    }

    public boolean isAscending() {
        return (Boolean) SPUtils.get(SPConstants.IS_ASCENDING, false);
    }

    public void setAscending(boolean ascending) {
        SPUtils.save(SPConstants.IS_ASCENDING, ascending); // 默认降序排列（最新在前）
    }

    /**
     * 优雅关闭资源
     */
    public void shutdown() {
        // 设置关闭标志
        isShuttingDown.set(true);
        XLog.w(TAG, "【系统】===== 开始关闭资源 =====");

        // 关闭线程池
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                // 等待当前任务完成
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    XLog.w(TAG, "【线程】线程池强制关闭 | 等待超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                XLog.e(TAG, "【错误】线程池关闭中断");
            }
            XLog.i(TAG, "【线程】线程池关闭完成");
        }

        // 清理缓存
        clearGroupCache();
        instance = null;
        XLog.w(TAG, "【系统】===== 资源关闭完成 =====");
    }

    private static class CacheEntry {
        final List<PhotoGroup> groups;
        long timestamp;

        CacheEntry(List<PhotoGroup> groups, long timestamp) {
            this.groups = groups;
            this.timestamp = timestamp;
        }
    }
}