package com.gallery.sweeper.photo.cleaner.data.events;

import java.util.Set;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/5 22:44
 * 描述：
 */
public class ReloadGroupEvent {
    private final Set<String> groupKeys; // 需要刷新的分组键集合

    public ReloadGroupEvent(Set<String> groupKeys) {
        this.groupKeys = groupKeys;
    }

    public Set<String> getGroupKeys() {
        return groupKeys;
    }
}
