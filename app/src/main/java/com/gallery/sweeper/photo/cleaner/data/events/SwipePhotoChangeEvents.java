package com.gallery.sweeper.photo.cleaner.data.events;

import com.gallery.sweeper.photo.cleaner.data.db.Photo;

import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/4 19:12
 * 描述：事件类增强
 * 修改：扩展事件类以支持撤销标记
 */
public  class SwipePhotoChangeEvents {
    public List<Photo> photos;
    public boolean isFromUndo = false;  // 是否来自撤销操作

    public SwipePhotoChangeEvents(List<Photo> photos) {
        this.photos = photos;
    }

    public void setFromUndo(boolean fromUndo) {
        isFromUndo = fromUndo;
    }
}
