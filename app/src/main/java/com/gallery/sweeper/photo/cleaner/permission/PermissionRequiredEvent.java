package com.gallery.sweeper.photo.cleaner.permission;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:44
 * 描述：
 * 权限请求事件类
 * 用于封装需要权限操作的事件信息
 */
public class PermissionRequiredEvent {
    /**
     * 扫描操作类型标识
     */
    public static final int OPERATION_SCAN = 1;
    /**
     * 删除操作类型标识
     */
    public static final int OPERATION_DELETE = 2;
    /**
     * 删除权限已获得类型标识
     */
    public static final int OPERATION_DELETE_GRANTED = 3; // 权限已获得
    /**
     * 特殊权限操作类型标识
     */
    public static final int OPERATION_SPECIAL_PERMISSION = 4;


    /**
     * 操作类型
     */
    private int operationType;

    /**
     * 构造函数
     *
     * @param operationType 操作类型，使用类中定义的OPERATION_*常量
     */
    public PermissionRequiredEvent(int operationType) {
        this.operationType = operationType;
    }

    /**
     * 获取操作类型
     *
     * @return 返回当前事件的操作类型
     */
    public int getOperationType() {
        return operationType;
    }

    public static String getOperationTypeName(int operationType) {
        switch (operationType) {
            case PermissionRequiredEvent.OPERATION_SCAN:
                return "扫描媒体文件";
            case PermissionRequiredEvent.OPERATION_DELETE:
                return "删除媒体文件";
            case PermissionRequiredEvent.OPERATION_DELETE_GRANTED:
                return "删除权限已授予";
            case PermissionRequiredEvent.OPERATION_SPECIAL_PERMISSION:
                return "特殊权限请求";
            default:
                return "未知操作类型";
        }
    }


}

