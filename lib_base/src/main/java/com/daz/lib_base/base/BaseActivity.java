package com.daz.lib_base.base;


import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daz.lib_base.dialog.MessageDialogFragment;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;
import com.daz.lib_loading.view.LoadingDialog;

/**
 * 作者：wx
 * 日期：2024/5/10 12:46
 * 描述：
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Activity mActivity;
    private LoadingDialog loadingDialog;

    private boolean isResumed = false;// 是否处于前台
    private boolean hasWindowFocus = false;// 是否具有焦点
    protected final String TAG;
    public BaseActivity() {
        TAG = getClass().getSimpleName();
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
        checkVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
        checkVisibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading(); // 确保销毁时关闭对话框
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hasWindowFocus = hasFocus;
        checkVisibility();
    }

    /**
     * 当前是否真正可见
     */
    protected boolean isActivityVisible() {
        return isResumed && hasWindowFocus;
    }

    private void checkVisibility() {
        //XLog.d(this.getClass().getSimpleName(), "当前可见状态: " + isActivityVisible());
    }

    protected void showLoading(String message) {
        if (TextUtils.isEmpty(message)) return;
        dismissLoading();
        loadingDialog = new LoadingDialog(this);
        loadingDialog.setLoadingText(message);
        loadingDialog.show();
    }

    protected void dismissLoading() {
        XLog.w(this.getClass().getSimpleName(), "关闭对话框");
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.close(); // 使用标准 close()
            loadingDialog = null;
        }
    }

    protected void showMessageDialog(MessageDialogParams params) {
        MessageDialogFragment fragment = new MessageDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(MessageDialogFragment.INTENT_KEY_TOUCH, params.isTouchable);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE, params.message);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE_SUB, params.subMessage);
        bundle.putInt(MessageDialogFragment.INTENT_KEY_MESSAGE_TYPE, params.type);
        bundle.putString(MessageDialogFragment.INTENT_KEY_LEFT_TEXT, params.leftButtonText);
        bundle.putString(MessageDialogFragment.INTENT_KEY_RIGHT_TEXT, params.rightButtonText);

        fragment.setArguments(bundle);
        fragment.setDataCallback(params.callback);
        fragment.show(getSupportFragmentManager(), "MessageDialogFragment");
        XLog.d(this.getClass().getSimpleName(), String.format("【对话框】显示消息对话框: %s", params.message));
    }

    protected void showMessageDialog(MessageDialogParams params, String tag) {
        // 使用自定义的showMessageDialog方法，但添加标签支持
        MessageDialogFragment fragment = new MessageDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(MessageDialogFragment.INTENT_KEY_TOUCH, params.isTouchable);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE, params.message);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE_SUB, params.subMessage);
        bundle.putInt(MessageDialogFragment.INTENT_KEY_MESSAGE_TYPE, params.type);
        bundle.putString(MessageDialogFragment.INTENT_KEY_LEFT_TEXT, params.leftButtonText);
        bundle.putString(MessageDialogFragment.INTENT_KEY_RIGHT_TEXT, params.rightButtonText);

        fragment.setArguments(bundle);
        fragment.setDataCallback(params.callback);
        fragment.show(getSupportFragmentManager(), tag); // 使用传入的标签
        XLog.d(TAG, "【对话框】显示消息对话框: " + tag);
    }
}