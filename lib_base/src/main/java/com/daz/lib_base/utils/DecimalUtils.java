package com.daz.lib_base.utils;

import java.text.DecimalFormat;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/12 17:38
 * 描述：
 */
public class DecimalUtils {
    private final static DecimalFormat decimalFormat = new DecimalFormat("#######0.00");//构造方法的字符格式这里如果小数不足2位,会以0补足.
    public static double formatDouble(double d) {
        return Double.parseDouble(decimalFormat.format(d).replace(",", "."));
    }
}
