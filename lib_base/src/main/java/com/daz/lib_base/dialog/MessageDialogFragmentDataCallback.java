package com.daz.lib_base.dialog;

import android.app.Dialog;

/**
 * 作者：wx
 * 时间：2019/6/20 9:19
 * 描述：消息Dialog回调
 */
public interface MessageDialogFragmentDataCallback {
    /**
     * 左按钮事件
     *  @param dialog         Dialog
     * @param messageType    消息类型
     * @param buttonText 左按钮文本
     */
    void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText);//左按钮


    /**
     * 右按钮事件
     *  @param dialog          Dialog
     * @param messageType     消息类型
     * @param buttonText 右按钮文本
     */
    void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText);//右按钮
}
