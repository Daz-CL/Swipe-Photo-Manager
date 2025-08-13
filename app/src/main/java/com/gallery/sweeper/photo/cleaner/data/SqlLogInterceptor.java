package com.gallery.sweeper.photo.cleaner.data;


import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.daz.lib_base.utils.XLog;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/27 22:03
 * 描述：
 * SQL 查询日志拦截器
 * 用于打印所有执行的 SQL 查询语句
 */
public class SqlLogInterceptor extends RoomDatabase.Callback {
    private static final String TAG = "SqlLogInterceptor";

    @Override
    public void onCreate(@NotNull SupportSQLiteDatabase db) {
        super.onCreate(db);
        XLog.i(TAG, "数据库创建完成");
    }

    @Override
    public void onOpen(@NotNull SupportSQLiteDatabase db) {
        super.onOpen(db);
        XLog.i(TAG, "数据库已打开");
    }

    @Override
    public void onDestructiveMigration(@NotNull SupportSQLiteDatabase db) {
        super.onDestructiveMigration(db);
        XLog.w(TAG, "数据库破坏性迁移");
    }

    /**
     * 查询拦截方法
     */
    public static void interceptQuery(SupportSQLiteQuery query, List<?> bindArgs) {
        StringBuilder sqlBuilder = new StringBuilder(query.getSql());

        // 处理参数
        if (bindArgs != null && !bindArgs.isEmpty()) {
            for (Object arg : bindArgs) {
                int index = sqlBuilder.indexOf("?");
                if (index != -1) {
                    String value = arg instanceof String ? "'" + arg + "'" : String.valueOf(arg);
                    sqlBuilder.replace(index, index + 1, value);
                }
            }
        }

        // 打印完整的 SQL 语句
        String fullSql = sqlBuilder.toString();
        //XLog.d(TAG, "执行 SQL 查询: " + fullSql);

        // 记录到文件（可选）
        logToFile(fullSql);
    }

    /**
     * 记录到文件（可选）
     */
    private static void logToFile(String sql) {
        // 实际应用中可以将 SQL 记录到文件
        // 这里只是示例
    }
}
