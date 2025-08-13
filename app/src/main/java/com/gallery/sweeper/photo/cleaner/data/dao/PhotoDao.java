package com.gallery.sweeper.photo.cleaner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface PhotoDao {
     String TAG = "PhotoDao";

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPhotos(List<Photo> photos);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updatePhotos(List<Photo> photos);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPhoto(Photo photo);

    @Update
    void updatePhoto(Photo photo);

    @Query("UPDATE photos SET status = :status WHERE media_store_id = :mediaId")
    void updateStatus(long mediaId, Photo.Status status);

    @Transaction
    @Query("UPDATE photos SET status = :status WHERE media_store_id IN (:mediaIds)")
    void batchUpdateStatus(List<Long> mediaIds, Photo.Status status);

    default void upsertPhotos(List<Photo> photos) {
        XLog.d(TAG, "【数据库】批量插入/更新照片 | 数量: " + photos.size());
        insertPhotos(photos); // 先尝试插入

        // 检查未插入成功的记录（冲突记录）
        List<Photo> conflicts = new ArrayList<>();
        for (Photo p : photos) {
            if (getPhotoByIdSync(p.mediaStoreId) != null) {
                conflicts.add(p);
            }
        }

        if (!conflicts.isEmpty()) {
            XLog.d(TAG, "【数据库】更新冲突照片 | 数量: " + conflicts.size());
            updatePhotos(conflicts); // 更新冲突记录
        }
    }

    @Delete
    int deletePhoto(Photo photo);

    @Query("SELECT * FROM photos WHERE media_store_id = :id")
    Photo getPhotoByIdSync(long id);

    @Query("SELECT * FROM photos WHERE media_store_id IN (:ids)")
    List<Photo> getPhotosByIdsSync(List<Long> ids);

    @Query("SELECT * FROM photos WHERE status = :status ORDER BY date_taken DESC")
    LiveData<List<Photo>> getPhotosByStatus(Photo.Status status);

    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "ORDER BY " +
            "  CASE status " +
            "    WHEN 0 THEN 1 " +  // NORMAL最高优先级
            "    WHEN 1 THEN 2 " +  // KEEP中等优先级
            "    WHEN 2 THEN 3 " +  // TRASHED最低优先级
            "    ELSE 4 " +
            "  END, " +            // 状态优先级排序
            "  date_taken DESC " + // 同状态按时间降序
            "LIMIT 1")             // 只取第一条
    Photo findLatestPhotoInGroupByMonth(String year, String month);

    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "ORDER BY " +
            "  CASE status " +
            "    WHEN 0 THEN 1 " +  // NORMAL最高优先级
            "    WHEN 1 THEN 2 " +  // KEEP中等优先级
            "    WHEN 2 THEN 3 " +  // TRASHED最低优先级
            "    ELSE 4 " +
            "  END, " +            // 状态优先级排序
            "  date_taken DESC " + // 同状态按时间降序
            "LIMIT 1")             // 只取第一条
    Photo findLatestPhotoInGroupYear(String year);

    /**
     * 按月份分组查询照片（升序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken ASC")
    LiveData<List<Photo>> getPhotosByMonthAsc(String year, String month);

    /**
     * 按月份分组查询照片（降序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken DESC")
    LiveData<List<Photo>> getPhotosByMonthDesc(String year, String month);

    /**
     * 按年份分组查询照片（升序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken ASC")
    LiveData<List<Photo>> getPhotosByYearAsc(String year);

    /**
     * 按年份分组查询照片（降序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken DESC")
    LiveData<List<Photo>> getPhotosByYearDesc(String year);

    /**
     * 同步获取月份分组照片（用于后台处理）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "ORDER BY date_taken ASC")
    List<Photo> getPhotosByMonthSync(String year, String month);

    /**
     * 同步获取年份分组照片（用于后台处理）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "ORDER BY date_taken ASC")
    List<Photo> getPhotosByYearSync(String year);

    /**
     * 按月份分组查询所有状态的照片（升序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "ORDER BY date_taken ASC")
    LiveData<List<Photo>> getAllPhotosByMonthAsc(String year, String month);

    /**
     * 按月份分组查询所有状态的照片（降序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "ORDER BY date_taken DESC")
    LiveData<List<Photo>> getAllPhotosByMonthDesc(String year, String month);

    /**
     * 按年份分组查询所有状态的照片（升序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "ORDER BY date_taken ASC")
    LiveData<List<Photo>> getAllPhotosByYearAsc(String year);

    /**
     * 按年份分组查询所有状态的照片（降序）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "ORDER BY date_taken DESC")
    LiveData<List<Photo>> getAllPhotosByYearDesc(String year);

    /**
     * 统计分组中状态为NORMAL的照片数量
     */
    @Query("SELECT COUNT(*) FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 0 ")
    int countNormalPhotosInGroup(String year, String month);

    /**
     * 聚合年份分组数据
     */
    @Query("SELECT \n" +
            "  year_group AS group_key, \n" +
            "  'YEAR' AS group_type, \n" +
            "  MAX(date_taken) AS latest_photo_timestamp, \n" +
            "  MIN(date_taken) AS earliest_photo_timestamp, \n" +
            "  COUNT(*) AS photo_count, \n" +
            "  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS trash_count, \n" +
            "  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS keep_count, \n" +
            "  (SELECT path FROM photos p2 WHERE p2.year_group = p.year_group " +
            "   AND status != 2 ORDER BY date_taken DESC LIMIT 1) AS group_cover, \n" +
            "  (SELECT media_store_id FROM photos p2 WHERE p2.year_group = p.year_group " +
            "   AND status != 2 ORDER BY date_taken DESC LIMIT 1) AS cover_media_id, \n" +
            "  year_group, \n" +
            "  NULL AS month_group \n" +
            "FROM photos p \n" +
            "GROUP BY year_group")
    List<PhotoGroup> aggregateYearGroups();

    /**
     * 聚合月份分组数据
     */
    @Query("SELECT \n" +
            "  year_group || '-' || month_group AS group_key, \n" +
            "  'MONTH' AS group_type, \n" +
            "  MAX(date_taken) AS latest_photo_timestamp, \n" +
            "  MIN(date_taken) AS earliest_photo_timestamp, \n" +
            "  COUNT(*) AS photo_count, \n" +
            "  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS trash_count, \n" +
            "  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS keep_count, \n" +
            "  (SELECT path FROM photos p2 WHERE p2.year_group = p.year_group AND p2.month_group = p.month_group " +
            "   AND status != 2 ORDER BY date_taken DESC LIMIT 1) AS group_cover, \n" +
            "  (SELECT media_store_id FROM photos p2 WHERE p2.year_group = p.year_group AND p2.month_group = p.month_group " +
            "   AND status != 2 ORDER BY date_taken DESC LIMIT 1) AS cover_media_id, \n" +
            "  year_group, \n" +
            "  month_group \n" +
            "FROM photos p \n" +
            "GROUP BY year_group, month_group")
    List<PhotoGroup> aggregateMonthGroups();

    @Query("SELECT COUNT(*) FROM photos " + "WHERE year_group = :year AND month_group = :month")
    int countPhotosInGroupByMonth(String year, String month);

    @Query("SELECT COUNT(*) FROM photos " + "WHERE year_group = :year")
    int countPhotosInGroupByYear(String year);

    @Query("SELECT * FROM photos")
    List<Photo> getAllPhotosSync();

    @Query("DELETE FROM photos WHERE media_store_id IN (:ids)")
    int deletePhotosByIds(List<Long> ids);

    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 0 " +
            "ORDER BY date_taken DESC LIMIT 1")
    Photo findLatestNormalPhotoInGroup(String year, String month);

    // ====================== 状态计数查询 ======================
    @Query("SELECT COUNT(*) FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 2") // TRASHED状态
    int countTrashPhotosInGroup(String year, String month);

    @Query("SELECT COUNT(*) FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 1") // KEEP状态
    int countKeepPhotosInGroup(String year, String month);

    @Query("SELECT COUNT(*) FROM photos " +
            "WHERE year_group = :year " +
            "AND status = 2") // TRASHED状态
    int countTrashPhotosInYear(String year);

    @Query("SELECT COUNT(*) FROM photos " +
            "WHERE year_group = :year " +
            "AND status = 1") // KEEP状态
    int countKeepPhotosInYear(String year);


    /**
     * 同步获取月份分组照片（所有状态）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year AND month_group = :month " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken ASC")
    List<Photo> getAllPhotosByMonthSync(String year, String month);

    /**
     * 同步获取年份分组照片（所有状态）
     */
    @Query("SELECT * FROM photos " +
            "WHERE year_group = :year " +
            "AND status = 0 " + // 使用整数值 0 (NORMAL)
            "ORDER BY date_taken ASC")
    List<Photo> getAllPhotosByYearSync(String year);
}