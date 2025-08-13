package com.daz.lib_base.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

/**
 * 作者 wx
 * 时间：
 * 描述：单行日志打印工具
 */
public class XLog {

    private static boolean isDebugEnabled;

    /**
     * 初始化日志系统
     *
     * @param enabled 是否开启调试模式
     */
    public static void init(boolean enabled) {
        isDebugEnabled = enabled;
        if (isDebugEnabled) {
            Log.i("XLog", "XLog初始化成功！");
        }
        Logger.addLogAdapter(new AndroidLogAdapter(/*formatStrategy*/) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return isDebugEnabled;
            }
        });
    }

    private static void initLogger() {
        /*FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .methodCount(30)
                .methodOffset(-9)
                //.logStrategy(new LogcatLogStrategy())
                .build();*/

    }

    // 日志级别封装方法（根据类型自动选择Log方法）
    private static void log(int priority, @NonNull String tag, @NonNull String message) {
        if (isDebugEnabled) {
            switch (priority) {
                case Log.VERBOSE:
                    Log.v(tag, message);
                    break;
                case Log.DEBUG:
                    Log.d(tag, message);
                    break;
                case Log.INFO:
                    Log.i(tag, message);
                    break;
                case Log.WARN:
                    Log.w(tag, message);
                    break;
                case Log.ERROR:
                    Log.e(tag, message);
                    break;
                case Log.ASSERT:
                    Log.wtf(tag, message);
                    break;
                default:
                    Log.i(tag, message);
                    break;
            }
        }
    }

    // 简化版日志接口（自动转换tag类型）
    public static void v(@NonNull String tag, @NonNull String message) {
        log(Log.VERBOSE, tag, message);
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        log(Log.DEBUG, tag, message);
    }

    public static void i(@NonNull String tag, @NonNull String message) {
        log(Log.INFO, tag, message);
    }

    public static void w(@NonNull String tag, @NonNull String message) {
        log(Log.WARN, tag, message);
    }

    public static void e(@NonNull String tag, @NonNull String message) {
        log(Log.ERROR, tag, message);
    }

    public static void wtf(@NonNull String tag, @NonNull String message) {
        log(Log.ASSERT, tag, message);
    }

    // 带格式的日志输出
    public static void format(@NonNull String format, @NonNull Object... args) {
        if (isDebugEnabled) {
            String content = String.format(format, args);
            log(Log.DEBUG, "XLog", content);
        }
    }

    public static void v(@NonNull String message) {
        Logger.v(message);
    }

    public static void d(@NonNull String message) {
        Logger.d(message);
    }

    public static void i(@NonNull String message) {
        Logger.i(message);
    }

    public static void w(@NonNull String message) {
        Logger.w( message);
    }

    public static void e(@NonNull String message) {
        Logger.e(message);
    }

    public static void wtf(@NonNull String message) {
        Logger.wtf(message);
    }
}
