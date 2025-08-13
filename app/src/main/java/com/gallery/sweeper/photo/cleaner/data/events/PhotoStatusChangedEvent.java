package com.gallery.sweeper.photo.cleaner.data.events;

import com.gallery.sweeper.photo.cleaner.data.db.Photo;

public class PhotoStatusChangedEvent {
    public long mediaId;
    public Photo.Status oldStatus;
    public Photo.Status newStatus;

    public Photo photo;

    public PhotoStatusChangedEvent(long mediaId, Photo.Status oldStatus, Photo.Status newStatus, Photo photo) {
        this.mediaId = mediaId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.photo = photo;
    }
}
