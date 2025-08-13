package com.gallery.sweeper.photo.cleaner.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.daz.lib_base.base.AVBAndroidViewModel;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;
import com.gallery.sweeper.photo.cleaner.data.events.SwipePhotoChangeEvents;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SwipeTrashViewModel extends AVBAndroidViewModel {
    private static final String TAG = "SwipeTrashViewModel";
    // 状态数据
    private MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private MutableLiveData<String> titleStatus = new MutableLiveData<>();
    private MutableLiveData<PhotoGroup> groupChange = new MutableLiveData<>();
    private MutableLiveData<Boolean> undoAvailable = new MutableLiveData<>(false);

    // 操作栈
    private Stack<UndoAction> undoStack = new Stack<>();
    private PhotoGroup currentGroup;

    // 缓存系统
    private List<Photo> cachedPhotos = new ArrayList<>();
    private PhotoGroup cachedGroup;
    private final Queue<Runnable> operationQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService queueExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);
    private final ReentrantLock queueLock = new ReentrantLock();

    public SwipeTrashViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * 当ViewModel被清除时调用此方法，用于执行清理操作
     * 主要功能包括：处理操作队列、关闭队列执行器、记录日志等
     * 该方法确保在ViewModel销毁前正确释放资源
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        XLog.d(TAG, "【系统】ViewModel销毁开始");

        // 处理剩余的操作队列任务
        processOperationQueue();

        // 关闭队列执行器并等待任务完成
        queueExecutor.shutdown();
        try {
            // 等待最多5秒让队列中的任务执行完毕
            if (!queueExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                // 超时则强制关闭队列执行器
                queueExecutor.shutdownNow();
                XLog.d(TAG, "【队列】任务队列强制关闭");
            }
            XLog.d(TAG, "【队列】任务队列已关闭");
        } catch (InterruptedException e) {
            // 被中断时强制关闭队列执行器并恢复中断状态
            queueExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            XLog.e(TAG, "【错误】队列关闭时被中断: " + e.getMessage());
        }
        XLog.i(TAG, "【系统】ViewModel已销毁");
    }


    /**
     * 设置分组键值，根据指定的分组类型和键值获取分组信息并进行相关处理
     *
     * @param groupType 分组类型，用于标识分组的分类方式
     * @param groupKey  分组键值，用于唯一标识一个分组
     */
    public void setGroupKey(String groupType, String groupKey) {
        XLog.d(TAG, "【分组】设置分组键值 - 类型: " + groupType + ", 键值: " + groupKey);

        // 监听分组数据变化，获取指定分组类型和键值对应的分组信息
        PhotoRepository.getInstance().getGroup(groupType, groupKey).observeForever(group -> {
            if (group != null) {
                // 缓存当前分组数据
                currentGroup = group;
                cachedGroup = new PhotoGroup(currentGroup);
                groupChange.postValue(currentGroup);
                XLog.d(TAG, "【缓存】分组数据缓存成功: " + currentGroup.getGroupKey());

                // 根据分组类型设置标题显示内容
                String title = "";
                switch (PhotoRepository.getInstance().getCurrentGroupType()) {
                    case YEAR:
                        title = currentGroup.yearGroup;
                        break;
                    case MONTH:
                        title = currentGroup.monthGroup + "." + currentGroup.yearGroup;
                        break;
                }
                titleStatus.postValue(title);
                XLog.d(TAG, "【UI】标题设置: " + title);

                // 加载当前分组的图片数据
                loadPhotos(currentGroup);
            } else {
                XLog.e(TAG, "【错误】获取分组数据失败");
                errorMessage.postValue("无法加载分组数据");
            }
        });
    }


    /**
     * 加载指定分组的照片数据
     *
     * @param currentGroup 当前要加载照片的分组对象，包含分组标识等信息
     */
    public void loadPhotos(PhotoGroup currentGroup) {
        XLog.d(TAG, "【数据】开始加载照片 - 分组: " + currentGroup.getGroupKey());
        // 设置加载状态为正在加载
        loadingState.setValue(true);
        errorMessage.setValue("");

        // 从照片仓库获取指定分组的照片数据
        PhotoRepository.getInstance().getPhotos(currentGroup, new ArrayList<>()).observeForever(photos -> {
            // 检查照片数据是否为空
            if (photos == null || photos.isEmpty()) {
                errorMessage.postValue("【" + currentGroup.getGroupKey() + "】分组图片数据为空");
                loadingState.postValue(false);
                XLog.e(TAG, "【错误】照片数据加载失败");
                return;
            }

            // 照片数据加载成功，更新缓存并通知UI
            XLog.i(TAG, "【数据】照片加载成功 - 数量: " + photos.size());
            cachedPhotos = new ArrayList<>(photos);
            EventBus.getDefault().post(new SwipePhotoChangeEvents(cachedPhotos));
            loadingState.postValue(false);
            XLog.d(TAG, "【缓存】照片已缓存并通知UI更新");
        });
    }

    /**
     * 将指定的照片移至回收站。该操作包括更新照片状态、更新UI计数、记录数据库操作以及支持撤销功能。
     *
     * @param photo 需要被移至回收站的照片对象，不能为null
     */
    public void trashPhoto(Photo photo) {
        XLog.d(TAG, "【操作】照片移至回收站 - ID: " + photo.mediaStoreId);

        // 创建原始照片的深拷贝，用于后续撤销操作
        Photo originalPhoto = new Photo(photo);
        Photo.Status originalStatus = originalPhoto.status;
        XLog.d(TAG, "【状态】原始状态: " + originalStatus);

        // 更新缓存中的照片状态为已回收
        updateCachedPhotoStatus(photo.mediaStoreId, Photo.Status.TRASHED);

        // 立即更新UI中当前分组的回收站计数
        if (currentGroup != null) {
            currentGroup.trashCount = Math.max(0, currentGroup.trashCount + 1);
            if (cachedGroup != null) {
                cachedGroup.trashCount = currentGroup.trashCount;
            }
            groupChange.postValue(currentGroup);
            XLog.d(TAG, "【UI】回收站计数更新: " + currentGroup.trashCount);
        } else {
            XLog.e(TAG, "【错误】当前分组为空");
        }

        // 添加数据库更新操作到队列中异步执行
        addToOperationQueue(() -> {
            XLog.i(TAG, "【数据库】更新照片状态 | ID: " + photo.mediaStoreId +
                    " | 旧状态: " + originalStatus + " | 新状态: TRASHED");
            PhotoRepository.getInstance().updatePhotoStatus(photo.mediaStoreId, Photo.Status.TRASHED);
        });

        // 创建并保存撤销操作记录，以便用户可以撤销本次操作
        UndoAction action = new UndoAction(originalPhoto, Photo.Status.TRASHED);
        undoStack.push(action);
        XLog.i(TAG, "【撤销】添加到撤销栈 - 照片ID: " + photo.mediaStoreId +
                " | 原始状态: " + originalStatus +
                ", 操作类型: TRASHED");

        // 更新撤销按钮的可用性状态
        updateUndoAvailability();

        XLog.i(TAG, "【操作】照片移至回收站完成");
    }

    /**
     * 标记指定照片为保留状态，并更新相关UI和数据状态。
     * <p>
     * 该方法会执行以下操作：
     * 1. 记录原始照片状态并创建深拷贝；
     * 2. 更新缓存中的照片状态为 KEEP；
     * 3. 立即更新当前分组的保留计数并通知UI刷新；
     * 4. 将数据库状态更新操作加入异步队列；
     * 5. 创建撤销操作记录并压入撤销栈；
     * 6. 更新撤销按钮的可用性。
     *
     * @param photo 需要被标记为保留的照片对象，不能为 null
     */
    public void keepPhoto(Photo photo) {
        XLog.d(TAG, "【操作】标记照片为保留 - ID: " + photo.mediaStoreId);

        // 创建原始照片的深拷贝，用于后续撤销操作
        Photo originalPhoto = new Photo(photo);
        Photo.Status originalStatus = originalPhoto.status;
        XLog.d(TAG, "【状态】原始状态: " + originalStatus);

        // 更新缓存中该照片的状态为 KEEP
        updateCachedPhotoStatus(photo.mediaStoreId, Photo.Status.KEEP);

        // 立即更新UI计数并通知观察者刷新界面
        if (currentGroup != null) {
            currentGroup.keepCount = Math.max(0, currentGroup.keepCount + 1);
            if (cachedGroup != null) {
                cachedGroup.keepCount = currentGroup.keepCount;
            }
            groupChange.postValue(currentGroup);
            XLog.d(TAG, "【UI】保留计数更新: " + currentGroup.keepCount);
        } else {
            XLog.e(TAG, "【错误】当前分组为空");
        }

        // 添加数据库更新操作到异步任务队列中执行
        addToOperationQueue(() -> {
            XLog.i(TAG, "【数据库】更新照片状态 | ID: " + photo.mediaStoreId +
                    " | 旧状态: " + originalStatus + " | 新状态: KEEP");
            PhotoRepository.getInstance().updatePhotoStatus(photo.mediaStoreId, Photo.Status.KEEP);
        });

        // 创建撤销操作记录（包含完整照片对象）
        UndoAction action = new UndoAction(originalPhoto, Photo.Status.KEEP);
        undoStack.push(action);
        XLog.i(TAG, "【撤销】添加到撤销栈 - 照片ID: " + photo.mediaStoreId +
                " | 原始状态: " + originalStatus +
                ", 操作类型: KEEP");

        // 更新撤销按钮状态
        updateUndoAvailability();

        XLog.i(TAG, "【操作】照片标记保留完成");
    }

    /**
     * 撤销上一次对照片执行的操作。
     * <p>
     * 该方法会从撤销栈中取出最近的一次操作，恢复照片的原始状态，并更新缓存、数据库以及相关UI状态。
     * 若撤销栈为空，则无法执行撤销并返回 false。
     * </p>
     *
     * @return 撤销操作是否成功执行。若撤销栈为空则返回 false，否则返回 true。
     */
    public boolean undoLastAction() {
        XLog.d(TAG, "【操作】开始执行撤销");

        if (undoStack.isEmpty()) {
            XLog.w(TAG, "【撤销】操作失败: 撤销栈为空");
            return false;
        }

        // 弹出最近的操作（包含完整照片对象）
        UndoAction action = undoStack.pop();
        Photo recoveredPhoto = new Photo(action.originalPhoto); // 创建恢复照片的深拷贝

        XLog.i(TAG, "【恢复】原始照片数据:\n" + formatPhotoLog(recoveredPhoto));

        // 1. 确保恢复照片加入缓存列表
        boolean foundInCache = false;
        List<Photo> newCache = new ArrayList<>();
        for (Photo photo : cachedPhotos) {
            if (photo.mediaStoreId == recoveredPhoto.mediaStoreId) {
                // 替换为恢复的照片对象
                newCache.add(recoveredPhoto);
                foundInCache = true;
                XLog.d(TAG, "【缓存】替换缓存照片 | ID: " + recoveredPhoto.mediaStoreId);
            } else {
                newCache.add(photo);
            }
        }

        if (!foundInCache) {
            newCache.add(0, recoveredPhoto);
            XLog.i(TAG, "【缓存】添加恢复照片到缓存 | ID: " + recoveredPhoto.mediaStoreId);
        }
        cachedPhotos = newCache;

        // 2. 更新分组计数
        updateGroupCountAfterUndo(action);

        // 3. 添加到操作队列
        addToOperationQueue(() -> {
            XLog.i(TAG, "【数据库】撤销操作 | ID: " + recoveredPhoto.mediaStoreId);
            PhotoRepository.getInstance().updatePhotoStatus(
                    recoveredPhoto.mediaStoreId,
                    recoveredPhoto.status
            );

            // 刷新分组数据
            refreshGroupDataSync();
        });

        // 4. 立即发送照片更新事件
        EventBus.getDefault().post(new SwipePhotoChangeEvents(cachedPhotos));
        XLog.d(TAG, "【事件】发送照片更新事件 | 数量: " + cachedPhotos.size());

        // 5. 更新撤销按钮状态
        updateUndoAvailability();

        XLog.i(TAG, "【操作】撤销完成");
        return true;
    }

    /**
     * 格式化照片对象为日志字符串
     *
     * @param photo 照片对象，可能为null
     * @return 格式化后的照片信息字符串，如果photo为null则返回"Photo{null}"
     */
    private String formatPhotoLog(Photo photo) {
        // 处理空对象情况
        if (photo == null) {
            return "Photo{null}";
        }

        // 构建格式化的字符串表示
        return "Photo {\n" +
                "  mediaStoreId = " + photo.mediaStoreId + "\n" +
                "  path = " + (photo.path != null ? photo.path : "null") + "\n" +
                "  status = " + photo.status + "\n" +
                "  dateTaken = " + photo.dateTaken + "\n" +
                "  yearGroup = " + (photo.yearGroup != null ? photo.yearGroup : "null") + "\n" +
                "  monthGroup = " + (photo.monthGroup != null ? photo.monthGroup : "null") + "\n" +
                "}";
    }

    /**
     * 同步刷新分组数据
     * <p>
     * 该方法用于从数据仓库同步获取最新的分组数据，并更新当前分组状态。
     * 在刷新过程中会保留用户在UI操作中手动更新的计数信息。
     * </p>
     */
    private void refreshGroupDataSync() {
        XLog.d(TAG, "【数据】同步刷新分组数据");

        // 检查当前分组是否为空
        if (currentGroup == null) {
            XLog.e(TAG, "【错误】当前分组为空，无法刷新");
            return;
        }

        // 从数据仓库同步获取分组数据
        PhotoGroup group = PhotoRepository.getInstance().getGroupByKeySync(
                currentGroup.getGroupKey()
        );

        if (group != null) {
            // 保留手动更新的计数
            int trashCount = currentGroup.getTrashCount();
            int keepCount = currentGroup.getKeepCount();

            currentGroup = group;

            // 保留UI操作后的计数
            currentGroup.trashCount = trashCount;
            currentGroup.keepCount = keepCount;

            // 更新缓存
            cachedGroup = new PhotoGroup(currentGroup);

            groupChange.postValue(currentGroup);
            XLog.i(TAG, "【数据】分组刷新成功");
        } else {
            XLog.e(TAG, "【错误】分组刷新失败");
        }
    }

    /**
     * 在执行撤销操作后更新分组计数
     *
     * @param action 撤销操作对象，包含操作类型等信息
     */
    private void updateGroupCountAfterUndo(UndoAction action) {
        if (currentGroup != null) {
            // 根据撤销的操作类型减少对应的计数器
            if (action.actionType == Photo.Status.TRASHED) {
                currentGroup.trashCount = Math.max(0, currentGroup.trashCount - 1);
                XLog.d(TAG, "【计数】回收站计数减少: " + currentGroup.trashCount);
            } else if (action.actionType == Photo.Status.KEEP) {
                currentGroup.keepCount = Math.max(0, currentGroup.keepCount - 1);
                XLog.d(TAG, "【计数】保留计数减少: " + currentGroup.keepCount);
            }

            // 更新缓存分组的计数，保持与当前分组同步
            if (cachedGroup != null) {
                cachedGroup.trashCount = currentGroup.trashCount;
                cachedGroup.keepCount = currentGroup.keepCount;
                XLog.d(TAG, "【缓存】更新分组计数: trash=" + cachedGroup.trashCount +
                        ", keep=" + cachedGroup.keepCount);
            }

            // 直接发送更新，不触发数据重载
            groupChange.postValue(currentGroup);
            XLog.d(TAG, "【UI】本地计数已更新 | 分组: " + currentGroup.getGroupKey());
        }
    }


    /**
     * 更新缓存中指定照片的状态
     *
     * @param mediaId   照片的媒体存储ID
     * @param newStatus 照片的新状态
     */
    private void updateCachedPhotoStatus(long mediaId, Photo.Status newStatus) {
        XLog.d(TAG, "【缓存】更新照片状态 | ID: " + mediaId + " | 新状态: " + newStatus);
        boolean updated = false;

        // 遍历缓存照片列表，查找并更新指定ID的照片状态
        for (Photo photo : cachedPhotos) {
            if (photo.mediaStoreId == mediaId) {
                photo.status = newStatus;
                updated = true;
                break;
            }
        }

        if (updated) {
            // 通知UI更新
            EventBus.getDefault().post(new SwipePhotoChangeEvents(cachedPhotos));
            XLog.d(TAG, "【事件】缓存照片更新通知已发送");
        } else {
            XLog.w(TAG, "【缓存】未找到照片 | ID: " + mediaId);
        }
    }


    /**
     * 将任务添加到操作队列中
     *
     * @param task 需要添加到队列中的可执行任务
     */
    private void addToOperationQueue(Runnable task) {
        queueLock.lock();
        try {
            // 将任务添加到队列中
            operationQueue.offer(task);
            XLog.d(TAG, "【队列】任务已添加 | 队列大小: " + operationQueue.size());

            // 如果当前没有在处理队列，则开始处理队列中的任务
            if (!isProcessingQueue.get()) {
                processOperationQueue();
            }
        } finally {
            queueLock.unlock();
        }
    }


    /**
     * 处理操作队列中的任务
     * 该方法会在线程池中执行队列处理逻辑，依次执行队列中的所有任务，
     * 并在执行完毕后检查是否有新任务加入，如有则递归重新处理队列
     */
    private void processOperationQueue() {
        queueExecutor.execute(() -> {
            XLog.d(TAG, "【队列】开始处理任务队列");
            isProcessingQueue.set(true);
            try {
                Runnable task;
                // 循环处理队列中的所有任务
                while ((task = operationQueue.poll()) != null) {
                    try {
                        long startTime = System.currentTimeMillis();
                        task.run();
                        XLog.d(TAG, "【队列】任务执行成功 | 耗时: " +
                                (System.currentTimeMillis() - startTime) + "ms");
                    } catch (Exception e) {
                        XLog.e(TAG, "【错误】队列任务执行失败: " + e.getMessage());
                    }
                }
                XLog.i(TAG, "【队列】任务队列已清空");
            } finally {
                isProcessingQueue.set(false);
                // 检查队列是否还有任务，如有则重新处理
                if (!operationQueue.isEmpty()) {
                    XLog.d(TAG, "【队列】检测到新任务，重新处理队列");
                    processOperationQueue();
                }
            }
        });
    }


    // ==================== 公共方法 ====================
    public List<Photo> getCachedPhotos() {
        // 添加空检查
        int size = (cachedPhotos != null) ? cachedPhotos.size() : 0;
        XLog.d(TAG, "【缓存】获取缓存照片 | 数量: " + size);
        return (cachedPhotos != null) ? cachedPhotos : Collections.emptyList();
    }

    public PhotoGroup getCachedGroup() {
        XLog.d(TAG, "【缓存】获取缓存分组");
        return cachedGroup;
    }

    public PhotoGroup getCurrentGroup() {
        return currentGroup;
    }

    private void updateUndoAvailability() {
        boolean available = !undoStack.isEmpty();
        undoAvailable.postValue(available);
        XLog.d(TAG, "【状态】撤销栈状态: " + (available ? "有操作可撤销" : "空"));
    }

    // ==================== Getter 方法 ====================

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getTitleStatus() {
        return titleStatus;
    }

    public MutableLiveData<PhotoGroup> getGroupChange() {
        return groupChange;
    }

    public LiveData<Boolean> getUndoAvailable() {
        return undoAvailable;
    }

    public void cancelPendingOperations() {
        queueExecutor.shutdownNow();
    }


    // ==================== 内部类 ====================

    /**
     * 撤销操作类，用于存储照片编辑操作前的状态信息，支持撤销功能
     *
     * @param originalPhoto 操作前的完整照片对象，用于恢复照片到操作前的状态
     * @param actionType    照片状态类型，标识执行的具体操作类型
     */
    private static class UndoAction {
        final Photo originalPhoto;  // 存储操作前的完整照片对象
        final Photo.Status actionType;

        public UndoAction(Photo originalPhoto, Photo.Status actionType) {
            this.originalPhoto = originalPhoto;
            this.actionType = actionType;
        }
    }

}