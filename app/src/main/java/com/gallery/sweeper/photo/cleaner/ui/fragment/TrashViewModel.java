package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.daz.lib_base.base.AVBAndroidViewModel;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.events.PhotoStatusChangedEvent;
import com.gallery.sweeper.photo.cleaner.data.events.ReloadGroupEvent;
import com.gallery.sweeper.photo.cleaner.data.events.TrashEvents;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:27
 * 描述：
 */
public class TrashViewModel extends AVBAndroidViewModel {
    private static final String TAG = "TrashViewModel";
    /**
     * 存储照片列表的LiveData对象
     * 用于观察和管理照片数据的变化
     */
    private final MutableLiveData<List<Photo>> photos = new MutableLiveData<>(new ArrayList<>());

    /**
     * 存储加载状态的LiveData对象
     * true表示正在加载数据，false表示加载完成
     */
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);

    /**
     * 存储错误信息的LiveData对象
     * 用于传递和显示错误消息
     */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");

    /**
     * 照片数据仓库对象
     * 用于访问和管理照片数据的业务逻辑
     */
    private final PhotoRepository photoRepository;


    public TrashViewModel(@NonNull Application application) {
        super(application);
        photoRepository = PhotoRepository.getInstance();
        EventBus.getDefault().register(this);
        XLog.d(TAG, "【ViewModel】垃圾桶ViewModel初始化完成");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        EventBus.getDefault().unregister(this);
        XLog.d(TAG, "【ViewModel】垃圾桶ViewModel已销毁");
    }

    /**
     * 加载垃圾桶中的照片数据
     * <p>
     * 该方法负责从照片仓库中获取状态为TRASHED的照片，并按拍摄时间倒序排列后更新到LiveData中。
     * 方法具有防重复加载机制，当加载正在进行时会直接返回。
     * </p>
     */
    public void loadPhotos() {
        // 检查是否正在加载中，避免重复加载
        if (Boolean.TRUE.equals(loadingState.getValue())) {
            XLog.w(TAG, "【数据加载】加载操作已在进行中，跳过");
            return;
        }

        loadingState.setValue(true);
        XLog.d(TAG, "【数据加载】开始加载垃圾桶照片");

        // 从照片仓库获取垃圾桶状态的照片数据
        LiveData<List<Photo>> photosLiveData = photoRepository.getPhotosByStatus(Photo.Status.TRASHED);
        photosLiveData.observeForever(new Observer<List<Photo>>() {
            @Override
            public void onChanged(List<Photo> trashPhotos) {
                photosLiveData.removeObserver(this);

                // 处理获取照片失败的情况
                if (trashPhotos == null) {
                    errorMessage.postValue("获取垃圾桶照片失败");
                    loadingState.postValue(false);
                    XLog.e(TAG, "【数据加载】获取垃圾桶照片失败");
                    return;
                }

                // 按时间倒序排序
                trashPhotos.sort((p1, p2) -> Long.compare(p2.getDateTaken(), p1.getDateTaken()));

                photos.postValue(trashPhotos);
                loadingState.postValue(false);
                XLog.d(TAG, "【数据加载】加载垃圾桶照片成功，数量: " + trashPhotos.size());
            }
        });
    }


    /**
     * 切换所有照片的选中状态
     * <p>
     * 该方法用于全选或取消全选所有照片。如果当前所有照片都已选中，则取消全选；
     * 如果当前有未选中的照片，则执行全选操作。
     */
    public void toggleSelectAll() {
        // 判断是否应该全选：当前不是全选状态时返回true，否则返回false
        boolean shouldSelectAll = !areAllPhotosSelected();
        // 根据shouldSelectAll的值设置所有照片的选中状态
        setAllPhotosSelected(shouldSelectAll);
    }


    /**
     * 设置所有照片的选中状态
     *
     * @param selected 要设置的选中状态，true表示选中，false表示取消选中
     */
    public void setAllPhotosSelected(boolean selected) {
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【全选操作】当前照片列表为空");
            return;
        }

        // 创建新的照片列表，更新每张照片的选中状态
        List<Photo> updated = new ArrayList<>();
        for (Photo photo : current) {
            Photo copy = new Photo(photo);
            copy.setSelected(selected);
            updated.add(copy);
        }

        photos.postValue(updated);
        XLog.d(TAG, "【全选操作】设置全部选中状态: " + selected);
    }

    /**
     * 处理照片状态变更事件
     * 当照片状态变为已删除(TRASHED)或恢复正常(NORMAL)时，更新UI显示
     *
     * @param event 照片状态变更事件对象，包含变更后的状态、媒体ID和照片信息
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoStatusChanged(PhotoStatusChangedEvent event) {
        if (event.newStatus == Photo.Status.TRASHED) {
            // 处理照片被放入垃圾桶的逻辑
            XLog.d(TAG, "【数据更新】收到照片放入垃圾桶事件 | ID: " + event.mediaId);
            addNewTrashPhoto(event.photo);
        } else if (event.newStatus == Photo.Status.NORMAL) {
            // 处理照片从垃圾桶恢复的逻辑
            XLog.d(TAG, "【数据更新】收到照片恢复事件 | ID: " + event.mediaId);
            removeUndoPhoto(event.mediaId);
        }
    }

    /**
     * 处理垃圾桶分组选择事件
     * 根据选择的分组类型和键值更新照片的选择状态
     *
     * @param event 垃圾桶分组选择事件对象，包含分组类型和分组键值
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventTrashSelectEvents(TrashEvents.selectTrashByGroup event) {
        // 记录分组选择事件日志并更新照片选择状态
        XLog.d(TAG, "【分组选择】收到选择分组事件 | 类型: " + event.groupType + " | key: " + event.groupKey);
        updatePhotoSelection(event.groupType, event.groupKey);
    }


    /**
     * 添加新的垃圾桶照片
     *
     * @param photo 要添加的照片对象
     */
    private void addNewTrashPhoto(Photo photo) {
        List<Photo> current = photos.getValue() != null ? new ArrayList<>(photos.getValue()) : new ArrayList<>();

        // 检查照片是否已存在，避免重复添加
        boolean exists = false;
        for (Photo p : current) {
            if (p.getMediaStoreId() == photo.getMediaStoreId()) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            current.add(photo);
            // 按拍摄时间降序排列
            current.sort((p1, p2) -> Long.compare(p2.getDateTaken(), p1.getDateTaken()));

            photos.postValue(current);
            XLog.d(TAG, "【数据更新】添加新垃圾桶照片成功 | ID: " + photo.getMediaStoreId());
        } else {
            XLog.w(TAG, "【数据更新】照片已在垃圾桶中 | ID: " + photo.getMediaStoreId());
        }
    }


    /**
     * 移除撤销照片
     * <p>
     * 该方法用于从当前照片列表中移除指定媒体ID的照片，并更新LiveData数据
     *
     * @param mediaId 要移除的照片的媒体存储ID
     */
    private void removeUndoPhoto(long mediaId) {
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【数据更新】当前照片列表为空，无法移除");
            return;
        }

        // 创建新的照片列表，过滤掉指定ID的照片
        List<Photo> updated = new ArrayList<>();
        for (Photo p : current) {
            if (p.getMediaStoreId() != mediaId) {
                updated.add(p);
            }
        }

        photos.postValue(updated);
        XLog.d(TAG, "【数据更新】移除撤销照片成功 | ID: " + mediaId);
    }


    /**
     * 根据分组类型和分组键更新照片选中状态
     *
     * @param groupType 分组类型，用于匹配照片的年份或月份分组
     * @param groupKey  分组键，可选值为"YEAR"或"MONTH"，表示按年份或月份进行分组匹配
     */
    private void updatePhotoSelection(String groupType, String groupKey) {
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【分组选择】当前照片列表为空");
            return;
        }

        int selectedCount = 0;
        List<Photo> updated = new ArrayList<>();

        // 遍历当前照片列表，根据分组条件更新每张照片的选中状态
        for (Photo photo : current) {
            Photo copy = new Photo(photo);

            boolean match = false;

            if (groupType != null) {
                switch (groupKey) {
                    case "YEAR":
                        match = groupType.contains(photo.getYearGroup());
                        break;
                    case "MONTH":
                        match = groupType.contains(photo.getMonthGroup());
                        break;
                }
                selectedCount += match ? 1 : 0;
            }
            copy.setSelected(match);
            updated.add(copy);
        }

        photos.postValue(updated);
        XLog.d(TAG, "【分组选择】更新分组选中状态完成 | 列表数量: " + updated.size() + " | 选中数量: " + selectedCount);
    }


    /**
     * 恢复选中的照片
     * <p>
     * 该方法会将当前选中的照片从回收站状态恢复为正常状态，
     * 并更新UI和数据库状态，同时通知相关分组进行刷新。
     * <p>
     * 执行流程：
     * 1. 检查当前照片列表是否为空
     * 2. 筛选出选中的照片和未选中的照片
     * 3. 将选中的照片从列表中移除并更新数据库状态
     * 4. 发送分组刷新事件通知UI更新
     */
    public void restoreSelectedPhotos() {
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【恢复操作】当前照片列表为空");
            return;
        }

        // 分离选中和未选中的照片
        List<Photo> toRestore = new ArrayList<>();
        List<Photo> remaining = new ArrayList<>();
        Set<String> affectedGroups = new HashSet<>(); // 受影响的分组集合

        for (Photo photo : current) {
            if (photo.isSelected()) {
                toRestore.add(photo);
                // 收集受影响的分组键
                addAffectedGroups(affectedGroups, photo);
            } else {
                remaining.add(photo);
            }
        }

        if (toRestore.isEmpty()) {
            XLog.w(TAG, "【恢复操作】没有选中的照片");
            return;
        }

        // 更新UI显示剩余的照片
        photos.postValue(remaining);

        // 更新数据库中选中照片的状态
        for (Photo photo : toRestore) {
            photoRepository.updatePhotoStatus(photo.getMediaStoreId(), Photo.Status.NORMAL);
            XLog.d(TAG, "【数据库】恢复照片状态 | ID: " + photo.getMediaStoreId());
        }

        // 发送携带受影响分组的事件
        EventBus.getDefault().post(new ReloadGroupEvent(affectedGroups));
        XLog.d(TAG, "【事件通知】发送分组刷新事件，受影响分组数量: " + affectedGroups.size());
    }


    /**
     * 删除选中的照片
     * <p>
     * 该方法会筛选出当前照片列表中被选中的照片，执行永久删除操作，并更新相关分组的显示状态。
     * 删除过程包括：更新UI显示、执行数据库删除、发送分组刷新事件。
     */
    public void deleteSelectedPhotos() {
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【删除操作】当前照片列表为空");
            return;
        }

        // 分离保留的照片和待删除的照片
        List<Photo> remaining = new ArrayList<>();
        List<Long> toDeleteIds = new ArrayList<>();
        Set<String> affectedGroups = new HashSet<>(); // 受影响的分组集合

        for (Photo photo : current) {
            if (!photo.isSelected()) {
                remaining.add(photo);
            } else {
                toDeleteIds.add(photo.getMediaStoreId());
                // 收集受影响的分组键
                addAffectedGroups(affectedGroups, photo);
            }
        }

        if (toDeleteIds.isEmpty()) {
            XLog.w(TAG, "【删除操作】没有选中的照片");
            return;
        }

        // 更新UI显示
        photos.postValue(remaining);

        // 执行数据库删除操作
        photoRepository.deletePhotosPermanently(toDeleteIds);
        XLog.w(TAG, "【数据库】永久删除照片 | 数量: " + toDeleteIds.size());

        // 发送携带受影响分组的事件
        EventBus.getDefault().post(new ReloadGroupEvent(affectedGroups));
        XLog.d(TAG, "【事件通知】发送分组刷新事件，受影响分组数量: " + affectedGroups.size());
    }


    /**
     * 添加照片对应的年份和月份分组到受影响分组集合中
     *
     * @param affectedGroups 受影响的分组集合，用于存储需要更新的分组键
     * @param photo          照片对象，包含年份和月份分组信息
     */
    private void addAffectedGroups(Set<String> affectedGroups, Photo photo) {
        // 年分组键 (格式: "2025")
        String yearGroupKey = photo.getYearGroup();
        // 月分组键 (格式: "2025-Feb")
        String monthGroupKey = photo.getYearGroup() + "-" + photo.getMonthGroup();

        // 检查年份和月份分组是否已存在，避免重复添加
        if (affectedGroups.contains(yearGroupKey) && affectedGroups.contains(monthGroupKey)) {
            XLog.w(TAG, "【分组更新】分组已存在，跳过添加");
            return;
        }
        affectedGroups.add(yearGroupKey);
        affectedGroups.add(monthGroupKey);
        XLog.d(TAG, "【分组更新】添加受影响分组: " + yearGroupKey + " 和 " + monthGroupKey);
    }


    /**
     * 获取当前选中的照片数量
     *
     * @return 返回当前选中的照片数量，如果没有照片数据则返回0
     */
    public int getSelectedCount() {
        List<Photo> current = photos.getValue();
        if (current == null) return 0;

        // 遍历所有照片，统计选中的数量
        int count = 0;
        for (Photo photo : current) {
            if (photo.isSelected()) count++;
        }
        return count;
    }


    /**
     * 检查所有照片是否都被选中
     *
     * @return 如果所有照片都被选中返回true，否则返回false
     */
    public boolean areAllPhotosSelected() {
        // 获取当前照片列表
        List<Photo> current = photos.getValue();
        if (current == null || current.isEmpty()) return false;

        // 遍历所有照片，检查是否都被选中
        for (Photo photo : current) {
            if (!photo.isSelected()) return false;
        }
        return true;
    }


    /**
     * 切换指定位置照片的选中状态
     *
     * @param position 照片在列表中的位置索引
     */
    public void toggleSelect(int position) {
        // 获取当前照片列表数据
        List<Photo> current = photos.getValue();
        if (current == null) {
            XLog.w(TAG, "【数据更新】当前照片列表为空");
            return;
        }

        if (current.isEmpty()) {
            XLog.w(TAG, "【数据更新】当前照片列表为空");
            return;
        }

        // 检查位置索引是否越界
        if (position < 0 || position >= current.size()) {
            XLog.w(TAG, "【数据更新】位置越界 | 位置: " + position);
            return;
        }

        // 切换指定位置照片的选中状态并更新数据
        Photo photo = current.get(position);
        photo.setSelected(!photo.isSelected());
        photos.postValue(current);
        XLog.d(TAG, "【数据更新】切换照片选中状态 | ID: " + photo.getMediaStoreId() + " | 选中: " + photo.isSelected());
    }


    public LiveData<List<Photo>> getPhotos() {
        return photos;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}