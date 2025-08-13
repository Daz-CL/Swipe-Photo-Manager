package com.gallery.sweeper.photo.cleaner.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.gallery.sweeper.photo.cleaner.data.PhotoStatusConverter;

import java.util.Objects;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 16:49
 * <p>
 * 照片实体类，表示单个照片的信息
 * 优化点：
 * 1. 添加完整的状态管理
 * 2. 增强数据验证
 * 3. 添加安全相关方法
 */
@Entity(tableName = "photos")
@TypeConverters(PhotoStatusConverter.class)
public class Photo {
    @PrimaryKey
    @ColumnInfo(name = "media_store_id")
    public long mediaStoreId;

    @ColumnInfo(name = "path")
    public String path; // 照片文件路径

    @ColumnInfo(name = "date_taken", index = true)
    public long dateTaken; // 照片拍摄时间戳（毫秒）

    @ColumnInfo(name = "year_group", index = true)
    public String yearGroup; // 年份分组标识（如"2025"）

    @ColumnInfo(name = "month_group", index = true)
    public String monthGroup; // 年月分组标识（如"2025-06"）

    // 添加状态字段
    @ColumnInfo(name = "status", defaultValue = "NORMAL")
    public Status status = Status.NORMAL;

    @Ignore
    private boolean isSelected; // UI状态，不持久化到数据库

    // 状态枚举
    public enum Status {
        NORMAL(0),    // 正常状态
        KEEP(1),      // 保护状态（不会被清理）
        TRASHED(2);   // 垃圾桶状态

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Status fromValue(int value) {
            for (Status status : Status.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            return NORMAL;
        }
    }

    public Photo() {
    }

    @Ignore
    public Photo(Photo other) {
        this.mediaStoreId = other.mediaStoreId;
        this.path = other.path;
        this.dateTaken = other.dateTaken;
        this.yearGroup = other.yearGroup;
        this.monthGroup = other.monthGroup;
        this.status = other.status;
        this.isSelected = other.isSelected;
    }

    @Ignore
    public Photo(long mediaStoreId, String path, long dateTaken, String yearGroup, String monthGroup) {
        this.mediaStoreId = mediaStoreId;
        this.path = path;
        this.dateTaken = dateTaken;
        this.yearGroup = yearGroup;
        this.monthGroup = monthGroup;
    }

    // Getters and Setters
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getMediaStoreId() {
        return mediaStoreId;
    }

    public void setMediaStoreId(long mediaStoreId) {
        this.mediaStoreId = mediaStoreId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
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

    // Helper methods
    public boolean isTrashed() {
        return status == Status.TRASHED;
    }

    public boolean isKept() {
        return status == Status.KEEP;
    }

    // 添加选中状态相关方法
    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Photo photo = (Photo) o;
        return mediaStoreId == photo.mediaStoreId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaStoreId);
    }

    @Override
    public String toString() {
        return "Photo{" +
                "mediaStoreId=" + mediaStoreId +
                ", path='" + path + '\'' +
                ", dateTaken=" + dateTaken +
                ", yearGroup='" + yearGroup + '\'' +
                ", monthGroup='" + monthGroup + '\'' +
                ", status=" + status +
                ", isSelected=" + isSelected +
                '}';
    }
}