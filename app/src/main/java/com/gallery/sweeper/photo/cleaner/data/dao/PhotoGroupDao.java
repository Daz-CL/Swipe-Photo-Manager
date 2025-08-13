package com.gallery.sweeper.photo.cleaner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;

import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 18:38
 * 描述：照片分组数据访问对象
 * 优化点：
 * 1. 添加分组更新方法
 * 2. 优化查询性能
 */
@Dao
public interface PhotoGroupDao {
    @Query("DELETE FROM photo_groups WHERE group_type = :groupType")
    int deleteByGroupType(String groupType);

    @Query("SELECT COUNT(*) FROM photo_groups WHERE group_type = :groupType")
    int countByGroupType(String groupType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroups(List<PhotoGroup> groups);

    /**
     * 查询所有分组数据
     *
     * @return 所有分组数据的LiveData
     */
    @Query("SELECT * FROM photo_groups")
    LiveData<List<PhotoGroup>> getAllGroups();

    /**
     * 查询所有分组数据（非LiveData版本）
     *
     * @return 所有分组数据列表
     */
    @Query("SELECT * FROM photo_groups")
    List<PhotoGroup> getAllGroupsSync();

    @Query("SELECT * FROM photo_groups WHERE group_type = :groupType ORDER BY latest_photo_timestamp ASC")
    LiveData<List<PhotoGroup>> getGroupsAsc(String groupType);

    @Query("SELECT * FROM photo_groups WHERE group_type = :groupType ORDER BY latest_photo_timestamp DESC")
    LiveData<List<PhotoGroup>> getGroupsDesc(String groupType);

    @Query("SELECT * FROM photo_groups WHERE year_group = :year AND month_group = :month")
    List<PhotoGroup> findGroupsContainingPhoto(String year, String month);

    @Update
    void updateGroup(PhotoGroup group);

    @Delete
    void deleteGroup(PhotoGroup group);

    // 新增：删除所有分组
    @Query("DELETE FROM photo_groups")
    int deleteAllGroups();

    /**
     * 根据分组键同步获取分组（仅用于后台线程）
     */
    @Query("SELECT * FROM photo_groups WHERE group_key = :groupKey")
    PhotoGroup getGroupByKeySync(String groupKey);

    /**
     * 根据分组键异步获取分组（支持主线程观察）
     */
    @Query("SELECT * FROM photo_groups WHERE group_key = :groupKey")
    LiveData<PhotoGroup> getGroupByKey(String groupKey);
}
