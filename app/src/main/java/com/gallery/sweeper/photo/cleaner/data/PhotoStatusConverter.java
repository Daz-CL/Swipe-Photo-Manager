package com.gallery.sweeper.photo.cleaner.data;

import androidx.room.TypeConverter;

import com.gallery.sweeper.photo.cleaner.data.db.Photo.Status;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/25 20:04
 * 描述：
 */
public class PhotoStatusConverter {
    @TypeConverter
    public static Status fromInteger(int value) {
        return Status.fromValue(value);
    }

    @TypeConverter
    public static int toInteger(Status status) {
        return status == null ? Status.NORMAL.getValue() : status.getValue();
    }
}
