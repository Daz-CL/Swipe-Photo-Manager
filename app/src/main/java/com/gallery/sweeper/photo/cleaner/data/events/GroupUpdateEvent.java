package com.gallery.sweeper.photo.cleaner.data.events;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/1 20:33
 * 描述：
 */
public class GroupUpdateEvent {
    public final String yearGroup;
    public final String monthGroup;

    public GroupUpdateEvent(String yearGroup, String monthGroup) {
        this.yearGroup = yearGroup;
        this.monthGroup = monthGroup;
    }
}
