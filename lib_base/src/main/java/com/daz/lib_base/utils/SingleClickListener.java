package com.daz.lib_base.utils;

import java.util.Calendar;


/**
 * 创建者： wx

 * 创建时间：2017/3/30 11:50
 * 描述：此处添加类描述
 */

public class SingleClickListener {

    // 记录最后一次点击的时间，用于判断是否为快速连续点击
    private static long lastClickTime;

    /**
     * 判断是否为单击操作
     * 通过比较当前点击时间和上次点击时间的间隔，判断是否为快速连续点击
     *
     * @param delay 延迟时间，用于判断两次点击是否过快
     * @return 如果是单击操作则返回true，否则返回false
     */
    public static boolean singleClick(int delay) {
        // 获取当前时间毫秒数
        long currentTime = Calendar.getInstance().getTimeInMillis();
        // 如果是第一次点击，或者当前点击与上次点击的时间间隔大于设定的延迟时间
        if (lastClickTime == 0 || currentTime - lastClickTime > delay) {
            // 更新最后一次点击时间
            lastClickTime = currentTime;
            // 表示这是一次有效的单击操作
            return true;
        }
        // 如果当前点击与上次点击的时间间隔小于设定的延迟时间，表示这不是一次单击操作
        return false;
    }


}
