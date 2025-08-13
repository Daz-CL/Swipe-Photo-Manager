package com.gallery.sweeper.photo.cleaner.data;

import androidx.room.TypeConverter;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 17:22
 * 描述：类型转换器，用于Room数据库中的枚举类型转换
 */
public class GroupTypeConverters {
    /**
     * 将整数值转换为GroupType枚举
     */
    @TypeConverter
    public static GroupType groupTypeFromInt(int value) {
        return value == 0 ? GroupType.YEAR : GroupType.MONTH;
    }

    /**
     * 将GroupType枚举转换为整数值
     */
    @TypeConverter
    public static int groupTypeToInt(GroupType type) {
        return type == GroupType.YEAR ? 0 : 1;
    }
}