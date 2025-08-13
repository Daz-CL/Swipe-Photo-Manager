package com.gallery.sweeper.photo.cleaner.data.events;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/5 21:08
 * 描述：
 */
public class TrashSelectEvents {
    public static class selectTrashByGroup {
        public String groupType;
        public String groupKey;

        public selectTrashByGroup(String groupType, String groupKey) {
            this.groupType = groupType;
            this.groupKey = groupKey;
        }
    }
}
