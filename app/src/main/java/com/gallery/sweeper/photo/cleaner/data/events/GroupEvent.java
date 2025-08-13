package com.gallery.sweeper.photo.cleaner.data.events;

import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;

import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/13 21:02
 * 描述：
 */
public class GroupEvent {
    public static class LoadedEvent {
        public final List<PhotoGroup> photoGroups;

        public LoadedEvent(List<PhotoGroup> photoGroups) {
            this.photoGroups = photoGroups;
        }
    }

    public static class UpdateEvent {
        public final PhotoGroup updatedGroup;
        public UpdateEvent(PhotoGroup updatedGroup) {
            this.updatedGroup = updatedGroup;
        }
    }
}
