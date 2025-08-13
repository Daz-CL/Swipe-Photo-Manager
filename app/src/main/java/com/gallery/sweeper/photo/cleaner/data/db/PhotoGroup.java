package com.gallery.sweeper.photo.cleaner.data.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.daz.lib_base.utils.XLog;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 16:45
 * 描述：分组实体类，表示按年或按月分组的照片集合
 * 优化点：
 * 1. 添加封面照片更新逻辑
 * 2. 增强分组标识生成
 */
@Entity(tableName = "photo_groups",
        primaryKeys = {"group_key", "group_type"})
public class PhotoGroup {
    @NonNull
    @ColumnInfo(name = "group_key")
    public String groupKey; // 分组唯一标识

    @NonNull
    @ColumnInfo(name = "group_type")
    public String groupType; // 分组类型 (YEAR/MONTH)

    // 分组元数据
    @ColumnInfo(name = "year_group")
    public String yearGroup; // 所属年份

    @ColumnInfo(name = "month_group")
    public String monthGroup; // 月份分组(如"Jan")

    // 时间范围
    @ColumnInfo(name = "latest_photo_timestamp", index = true)
    public long latestPhotoTimestamp; // 组内最新照片时间戳

    @ColumnInfo(name = "earliest_photo_timestamp", index = true)
    public long earliestPhotoTimestamp; // 组内最早照片时间戳

    // 状态计数
    @ColumnInfo(name = "trash_count", defaultValue = "0")
    public int trashCount; // 回收站状态照片数量

    @ColumnInfo(name = "keep_count", defaultValue = "0")
    public int keepCount; // 保护状态照片数量

    // 照片统计
    @ColumnInfo(name = "photo_count")
    public int photoCount; // 照片数量

    // 封面信息
    @ColumnInfo(name = "group_cover")
    public String groupCover; // 封面照片路径

    @ColumnInfo(name = "cover_media_id")
    public long coverMediaId; // 封面照片的媒体库ID

    // 显示信息
    @ColumnInfo(name = "display_name")
    public String displayName; // 本地化显示名称

    // 构造方法
    public PhotoGroup() {
        // 无参构造方法用于Room
    }

    // 深拷贝构造函数
    public PhotoGroup(PhotoGroup other) {
        this.groupKey = other.groupKey;
        this.groupType = other.groupType;
        this.yearGroup = other.yearGroup;
        this.monthGroup = other.monthGroup;
        this.latestPhotoTimestamp = other.latestPhotoTimestamp;
        this.earliestPhotoTimestamp = other.earliestPhotoTimestamp;
        this.trashCount = other.trashCount;
        this.keepCount = other.keepCount;
        this.photoCount = other.photoCount;
        this.groupCover = other.groupCover;
        this.coverMediaId = other.coverMediaId;
        this.displayName = other.displayName;
    }

    @Ignore
    // 全参构造方法
    public PhotoGroup(@NonNull String groupKey, @NonNull String groupType,
                      String yearGroup, String monthGroup,
                      long latestPhotoTimestamp, long earliestPhotoTimestamp,
                      int trashCount, int keepCount, int photoCount,
                      String groupCover, long coverMediaId,
                      String displayName) {
        this.groupKey = groupKey;
        this.groupType = groupType;
        this.yearGroup = yearGroup;
        this.monthGroup = monthGroup;
        this.latestPhotoTimestamp = latestPhotoTimestamp;
        this.earliestPhotoTimestamp = earliestPhotoTimestamp;
        this.trashCount = trashCount;
        this.keepCount = keepCount;
        this.photoCount = photoCount;
        this.groupCover = groupCover;
        this.coverMediaId = coverMediaId;
        this.displayName = displayName;
    }

    @NonNull
    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(@NonNull String groupKey) {
        this.groupKey = groupKey;
    }

    @NonNull
    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(@NonNull String groupType) {
        this.groupType = groupType;
    }

    public String getYearGroup() {
        return yearGroup;
    }

    public void setYearGroup(String yearGroup) {
        this.yearGroup = yearGroup;
    }

    public String getMonthGroup() {
        return monthGroup;
    }

    public void setMonthGroup(String monthGroup) {
        this.monthGroup = monthGroup;
    }

    public long getLatestPhotoTimestamp() {
        return latestPhotoTimestamp;
    }

    public void setLatestPhotoTimestamp(long latestPhotoTimestamp) {
        this.latestPhotoTimestamp = latestPhotoTimestamp;
    }

    public long getEarliestPhotoTimestamp() {
        return earliestPhotoTimestamp;
    }

    public void setEarliestPhotoTimestamp(long earliestPhotoTimestamp) {
        this.earliestPhotoTimestamp = earliestPhotoTimestamp;
    }

    public int getTrashCount() {
        //XLog.d("【TAG】", "trashCount: " + trashCount);
        return trashCount;
    }

    public void setTrashCount(int trashCount) {
        this.trashCount = trashCount;
    }

    public int getKeepCount() {
        //XLog.d("【TAG】", "keepCount: " + keepCount);
        return keepCount;
    }

    public void setKeepCount(int keepCount) {
        this.keepCount = keepCount;
    }

    public int getPhotoCount() {
        //XLog.d("【TAG】", "photoCount: " + photoCount);
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }

    public String getGroupCover() {
        return groupCover;
    }

    public void setGroupCover(String groupCover) {
        this.groupCover = groupCover;
    }

    public long getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(long coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "PhotoGroup{" +
                "groupKey='" + groupKey + '\'' +
                ", groupType='" + groupType + '\'' +
                ", yearGroup='" + yearGroup + '\'' +
                ", monthGroup='" + monthGroup + '\'' +
                ", latestPhotoTimestamp=" + latestPhotoTimestamp +
                ", earliestPhotoTimestamp=" + earliestPhotoTimestamp +
                ", trashCount=" + trashCount +
                ", keepCount=" + keepCount +
                ", photoCount=" + photoCount +
                ", groupCover='" + groupCover + '\'' +
                ", coverMediaId=" + coverMediaId +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}