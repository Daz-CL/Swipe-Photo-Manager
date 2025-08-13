package com.daz.lib_base.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/12 18:20
 * 描述：
 */
public class InputMethodUtil {


    /**
     * 隐藏软键盘
     *
     * @param activity 当前Activity
     * @param view     当前获取焦点的View
     */
    public static void hideKeyboard(Activity activity, View view) {
        if (activity == null || view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 清除焦点并隐藏软键盘
     *
     * @param activity 当前Activity
     * @param view     当前获取焦点的View
     */
    public static void clearFocusAndHideKeyboard(Activity activity, View view) {
        if (view != null) {
            view.clearFocus(); // 清除焦点
            hideKeyboard(activity, view); // 隐藏软键盘
        }
    }

    public static void showSoftKeyBoard(EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
    }

 /*   public static void hide(@NonNull IBinder token) {
        InputMethodManager inputMethodManager = (InputMethodManager) App.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(token, 0);
    }*/

    public static void hide(@NonNull Context context, EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void show(@NonNull final Context context, final EditText editText) {
        //为 editLoginPassword 设置监听器，在 DialogFragment 绘制完后立即呼出软键盘，呼出成功后即注销
        editText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null && inputMethodManager.showSoftInput(editText, 0)) {
                    editText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }


    public static void showEdittext(EditText editText) {
        editText.setVisibility(View.VISIBLE);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        editText.setSelection(editText.getText().length());
        showSoftKeyBoard(editText);
    }

    public static void hideEdittext(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(true);
        //editText.clearFocus();
        hide(editText.getContext(), editText);
    }
}

