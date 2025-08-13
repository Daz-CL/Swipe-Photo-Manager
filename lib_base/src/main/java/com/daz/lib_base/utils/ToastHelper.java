package com.daz.lib_base.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.daz.lib_base.R;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/5/19 15:31
 * 描述：ToastHelper类提供了一个静态方法来显示错误消息的Toast，防止在5秒内重复显示相同的消息。
 */
public class ToastHelper {
    private static final Object LAST_MESSAGE_LOCK = new Object();
    private static volatile String lastMessage = "";
    private static volatile long lastMessageTime = 0;

    /**
     * 显示错误消息的Toast。如果消息为空或与最近5秒内显示的消息相同，则不显示。确保在UI线程中调用。
     *
     * @param context 上下文对象，通常是一个Activity或Application对象。用于显示Toast消息。
     * @param msg     要显示的消息文本。Toast的内容。
     */
    public static void showErrorMessage(@NonNull Context context, @NonNull String msg) {
        // 使用弱引用避免潜在的内存泄露
        WeakReference<Context> weakContext = new WeakReference<>(context);

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            showErrorMessageInternal(weakContext.get(), msg);
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> showErrorMessageInternal(weakContext.get(), msg));
        }
    }

    /**
     * 内部方法，实际显示Toast消息。进行一些额外的检查，如确保context有效，且不是即将结束或已被销毁的Activity。
     *
     * @param context 上下文对象，通常是一个Activity或Application对象。用于显示Toast消息。
     * @param msg     要显示的消息文本。Toast的内容。
     */
    private static void showErrorMessageInternal(Context context, String msg) {
        if (context == null || TextUtils.isEmpty(msg)) {
            return;
        }

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        // 使用volatile变量LAST_MESSAGE_LOCK来避免锁的重入问题
        if (isSameMessageWithinDuration(msg, 5, TimeUnit.SECONDS)) {
            return;
        }

        lastMessage = msg;
        lastMessageTime = System.currentTimeMillis();

        try {
            Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0); // 设置Toast显示在屏幕中间
            toast.show();
        } catch (Exception e) {
            XLog.e("ToastUtils", "Failed to show error message: " + e.getMessage());
            // 可以在这里添加更多的异常处理逻辑，比如重新尝试显示或者记录错误详情等
        }
    }


    /**
     * 在屏幕中间显示自定义样式的Toast
     *
     * @param context 上下文
     * @param message 提示信息
     */
    public static void showCenterToast(Context context, String message) {

        // 使用弱引用避免潜在的内存泄露
        WeakReference<Context> weakContext = new WeakReference<>(context);

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            showErrorInternal(weakContext.get(), message);
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> showErrorInternal(weakContext.get(), message));
        }


    }

    private static void showErrorInternal(Context context, String message) {
        if (context == null || TextUtils.isEmpty(message)) {
            return;
        }

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        // 使用volatile变量LAST_MESSAGE_LOCK来避免锁的重入问题
        if (isSameMessageWithinDuration(message, 5, TimeUnit.SECONDS)) {
            return;
        }

        lastMessage = message;
        lastMessageTime = System.currentTimeMillis();

        try {
            // 加载自定义布局
            LayoutInflater inflater = LayoutInflater.from(context);
            View layout = inflater.inflate(R.layout.toast_custom_layout, null);

            // 设置提示信息
            TextView text = layout.findViewById(R.id.toast_message);
            text.setText(message);

            // 创建Toast并设置属性
            Toast toast = new Toast(context);
            toast.setGravity(Gravity.CENTER  , 0, 0); // 设置Toast显示在屏幕中间
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout); // 设置自定义布局
            toast.show();
        } catch (Exception e) {
            XLog.e("ToastUtils", "Failed to show error message: " + e.getMessage());
            // 可以在这里添加更多的异常处理逻辑，比如重新尝试显示或者记录错误详情等
        }
    }

    /**
     * 检查当前消息是否与过去指定时间内显示的消息相同。
     *
     * @param msg             当前要检查的消息。
     * @param durationSeconds 检查的时间范围，单位为秒。
     * @param timeUnit        时间单位。
     * @return 如果当前消息与最近指定时间内显示的消息相同，则返回true；否则返回false。
     */
    private static boolean isSameMessageWithinDuration(String msg, int durationSeconds, TimeUnit timeUnit) {
        long currentTime = System.currentTimeMillis();
        long durationMillis = timeUnit.toMillis(durationSeconds);

        synchronized (LAST_MESSAGE_LOCK) {
            // 检查消息是否相同且在指定时间内
            if (msg.equals(lastMessage) && currentTime - lastMessageTime <= durationMillis) {
                return true;
            }
        }
        return false;
    }

    public static void showSuccessMessage(Context context, String msg) {
        showErrorMessage(context,msg);
    }
}
