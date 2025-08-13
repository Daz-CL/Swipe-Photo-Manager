package com.gallery.sweeper.photo.cleaner.utis;


import android.content.Context;
import android.content.SharedPreferences;

import com.gallery.sweeper.photo.cleaner.app.App;
import com.gallery.sweeper.photo.cleaner.app.SPConstants;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/11/17 17:49
 * 描述：
 */
/**
 * SharedPreferences 工具类
 * 用于统一管理应用中对 SharedPreferences 的操作，包括保存、读取、删除、清除等。
 * <p>
 * 该类为静态工具类，所有方法均为静态方法。
 * 使用前需要确保 App 类已正确初始化，否则可能导致 NullPointerException。
 * </p>
 */
public class SPUtils {

    /**
     * SharedPreferences 文件名，用于保存数据的文件名称。
     * 默认为 "photo_cleaner_sp"。
     */
    public static final String FILE_NAME = SPConstants.FILE_NAME;

    /**
     * 保存数据到 SharedPreferences 中。
     * 支持基本类型和对象（对象会转为字符串保存）。
     *
     * @param key    保存数据的键
     * @param object 要保存的数据，支持 String、Integer、Boolean、Float、Long 等类型
     */
    public static void save(String key, Object object) {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // 获取 Editor 实例
        SharedPreferences.Editor editor = sp.edit();

        // 根据不同类型，调用对应的 put 方法
        if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else {
            // 其他类型转为字符串保存
            editor.putString(key, object.toString());
        }

        // 使用兼容方式提交数据（Android 2.3 以下使用 commit）
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 从 SharedPreferences 中读取数据。
     * 根据默认值类型返回对应的数据。
     *
     * @param key           要读取的数据键
     * @param defaultObject 默认值，用于判断返回类型和默认值
     * @return 返回读取到的数据，如果不存在则返回默认值
     */
    public static Object get(String key, Object defaultObject) {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

        // 根据默认值类型，调用对应的 get 方法
        if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        }

        // 不支持的类型返回 null
        return null;
    }

    /**
     * 从 SharedPreferences 中移除某个键值对。
     *
     * @param key 要移除的键
     */
    public static void remove(String key) {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // 获取 Editor 实例并移除指定键
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        // 提交修改
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 清空 SharedPreferences 中的所有数据。
     */
    public static void clear() {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // 获取 Editor 实例并清空
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        // 提交修改
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 判断某个键是否存在于 SharedPreferences 中。
     *
     * @param key 要判断的键
     * @return 如果存在返回 true，否则返回 false
     */
    public static boolean contains(String key) {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // 判断是否包含该键
        return sp.contains(key);
    }

    /**
     * 获取 SharedPreferences 中所有的键值对。
     *
     * @return 返回一个 Map，包含所有键值对
     */
    public static Map<String, ?> getAll() {
        // 获取 SharedPreferences 实例
        SharedPreferences sp = App.getInstance().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // 返回所有键值对
        return sp.getAll();
    }

    /**
     * SharedPreferences 兼容工具类
     * 解决 Android 2.3 以下版本中 Editor.apply() 方法不存在的问题。
     */
    public static class SharedPreferencesCompat {

        /**
         * 反射获取 Editor.apply() 方法
         */
        private static final Method sApplyMethod = findApplyMethod();

        /**
         * 反射查找 apply 方法
         *
         * @return 如果找到返回 Method 对象，否则返回 null
         */
        private static Method findApplyMethod() {
            try {
                // 查找 SharedPreferences.Editor 类中的 apply 方法
                Class<?> clz = SharedPreferences.Editor.class;
                return clz.getMethod("apply");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 如果设备支持 apply 方法，则调用 apply，否则使用 commit
         *
         * @param editor SharedPreferences.Editor 实例
         */
        public static void apply(SharedPreferences.Editor editor) {
            try {
                if (sApplyMethod != null) {
                    // 调用 apply 方法
                    sApplyMethod.invoke(editor);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 回退到 commit 方法
            editor.commit();
        }
    }
}