package com.gallery.sweeper.photo.cleaner.permission;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.app.App;

import org.greenrobot.eventbus.EventBus;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:43
 * 描述：
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";

    /**
     * 检查应用是否具有存储权限
     *
     * @return boolean 是否具有存储权限
     * - true: 已获得存储权限
     * - false: 未获得存储权限
     */
    public static boolean hasStoragePermission() {
        // 使用Context检查权限
        Context context = App.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33) - 使用媒体权限
            XLog.w(TAG, "【权限】检查存储权限 | Android 13+ (API 33)");
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30) - 使用管理外部存储权限
            XLog.w(TAG, "【权限】检查存储权限 | Android 11+ (API 30)");
            return Environment.isExternalStorageManager();
        } else {
            // Android 10及以下 (API 29及以下) - 使用传统存储权限
            XLog.w(TAG, "【权限】检查存储权限 | Android 10- (API 29)");
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 获取需要请求的权限列表
     *
     * @return String[] 需要请求的权限数组
     */
    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要特殊处理，不能通过requestPermissions请求
            return new String[]{}; // MANAGE_EXTERNAL_STORAGE需要跳转设置页面
        } else {
            // Android 10及以下
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * 检查是否需要特殊权限设置（Android 11+的MANAGE_EXTERNAL_STORAGE）
     *
     * @return boolean 是否需要跳转到特殊权限设置页面
     */
    public static boolean needSpecialPermissionSetting() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager();
    }

    /**
     * 通知权限请求
     *
     * @param operationType 操作类型
     */
    public static void postPermissionRequired(int operationType) {
        XLog.w(TAG, "【权限】发送权限请求通知 | 操作类型: " + PermissionRequiredEvent.getOperationTypeName(operationType));
        EventBus.getDefault().post(new PermissionRequiredEvent(operationType));
    }
}
