package com.daz.lib_base.dialog;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/9 23:11
 * 描述：
 */
// 新增参数封装类
public class MessageDialogParams {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_WARNING = 1;
    public static final int TYPE_ERROR = 2;

    public static final int TYPE_RESTORE = 3;
    public static final int TYPE_ORANGE = 4;

    public String message;
    public String subMessage;
    public int isTouchable;
    public int type;
    public String leftButtonText;
    public String rightButtonText;
    public MessageDialogFragmentDataCallback callback;

    public MessageDialogParams() {
    }

    public MessageDialogParams(
            String message,
            String subMessage,
            int isTouchable,
            int type,
            String leftButtonText,
            String rightButtonText,
            MessageDialogFragmentDataCallback callback) {
        this.message = message;
        this.subMessage = subMessage;
        this.isTouchable = isTouchable;
        this.type = type;
        this.leftButtonText = leftButtonText;
        this.rightButtonText = rightButtonText;
        this.callback = callback;
    }
}
