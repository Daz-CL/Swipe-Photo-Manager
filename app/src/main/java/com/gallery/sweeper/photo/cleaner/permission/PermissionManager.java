package com.gallery.sweeper.photo.cleaner.permission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.app.App;

/**
 * 统一权限管理工具类
 * 职责：封装权限检查、请求和特殊权限处理逻辑
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";

    // 权限类型枚举
    public enum PermissionType {
        SCAN,      // 相册扫描权限
        DELETE     // 照片删除权限
    }

    /**
     * 检查指定操作类型的权限状态
     * @param type 权限类型（SCAN/DELETE）
     * @return 是否已获得权限
     */
    public static boolean hasPermission(PermissionType type) {
        Context context = App.getInstance();
        if (type == PermissionType.SCAN) {
            return checkScanPermission(context);
        } else {
            return checkDeletePermission(context);
        }
    }

    // 检查扫描权限（兼容Android版本）
    private static boolean checkScanPermission(Context context) {
        // Android 13+ (API 33) - 需要媒体权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            XLog.d(TAG, "【权限检查】扫描权限(Android 13+)");
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Android 11+ (API 30) - 需要管理存储权限
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XLog.d(TAG, "【权限检查】扫描权限(Android 11+)");
            return Environment.isExternalStorageManager();
        }
        // Android 10及以下 - 传统存储权限
        else {
            XLog.d(TAG, "【权限检查】扫描权限(Android 10-)");
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 检查删除权限（兼容Android版本）
    private static boolean checkDeletePermission(Context context) {
        // Android 11+ 需要管理存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XLog.d(TAG, "【权限检查】删除权限(Android 11+)");
            return Environment.isExternalStorageManager();
        }
        // Android 10及以下 - 写存储权限
        else {
            XLog.d(TAG, "【权限检查】删除权限(Android 10-)");
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 获取需要请求的权限列表
     * @param type 权限类型（SCAN/DELETE）
     * @return 需要请求的权限数组
     */
    public static String[] getRequiredPermissions(PermissionType type) {
        if (type == PermissionType.SCAN) {
            return getScanPermissions();
        } else {
            return getDeletePermissions();
        }
    }

    private static String[] getScanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[]{};
        } else {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    private static String[] getDeletePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[]{};
        } else {
            return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
    }

    /**
     * 检查是否需要特殊权限设置
     * @return 是否需要跳转到特殊权限设置页面
     */
    public static boolean needSpecialPermissionSetting() {
        // Android 11+ 且未获得管理权限
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager();
    }

    /**
     * 打开特殊权限设置页面
     * @param fragment 发起请求的Fragment
     */
    public static void openSpecialPermissionSetting(androidx.fragment.app.Fragment fragment) {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            }
            Uri uri = Uri.fromParts("package", fragment.requireContext().getPackageName(), null);
            intent.setData(uri);
            fragment.startActivityForResult(intent,
                    PermissionConstants.REQUEST_CODE_MANAGE_STORAGE);
        } catch (Exception e) {
            XLog.e(TAG, "【权限处理】打开设置页失败: " + e.getMessage());
        }
    }
}