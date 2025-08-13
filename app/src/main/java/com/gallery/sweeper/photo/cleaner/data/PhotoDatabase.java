package com.gallery.sweeper.photo.cleaner.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoDao;
import com.gallery.sweeper.photo.cleaner.data.dao.PhotoGroupDao;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;

import java.util.concurrent.Executors;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 17:31
 * 描述：照片数据库类，使用单例模式确保数据库实例唯一
 */
@Database(entities = {Photo.class, PhotoGroup.class},
        version = 1,
        exportSchema = false
)
@TypeConverters({GroupTypeConverters.class, PhotoStatusConverter.class})
public abstract class PhotoDatabase extends RoomDatabase {
    private static PhotoDatabase instance;

    public abstract PhotoDao photoDao();

    public abstract PhotoGroupDao photoGroupDao();

    public static synchronized PhotoDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), PhotoDatabase.class, "photo_database")
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // 创建索引
                            db.execSQL("CREATE INDEX idx_year_group ON photos(year_group)");
                            db.execSQL("CREATE INDEX idx_month_group ON photos(month_group)");
                            db.execSQL("CREATE INDEX idx_group_type ON photo_groups(group_type)");
                            XLog.i("PhotoDatabase", "数据库创建完成");
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            XLog.i("PhotoDatabase", "数据库已打开");
                        }
                    })
                    .setQueryCallback((sqlQuery, bindArgs) -> {
                        // 拦截并打印查询
                        SqlLogInterceptor.interceptQuery(new SimpleSQLiteQuery(sqlQuery), bindArgs);
                    }, Executors.newSingleThreadExecutor())
                    .build();
        }
        return instance;
    }
}