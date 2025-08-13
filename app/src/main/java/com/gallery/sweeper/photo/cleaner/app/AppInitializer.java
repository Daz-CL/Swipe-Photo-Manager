package com.gallery.sweeper.photo.cleaner.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/5/18 16:25
 * 描述：AppInitializer 类实现了 Initializer 接口，用于初始化 AnalyticsService。
 */
public class AppInitializer implements Initializer<AnalyticsService> {

    /**
     * 创建并初始化 AnalyticsService。
     *
     * @param context 上下文环境，用于 AnalyticsService 的初始化。
     * @return 返回已初始化的 AnalyticsService 实例。
     */
    @NonNull
    @Override
    public AnalyticsService create(@NonNull Context context) {
        // 创建 AnalyticsService 实例并进行初始化
        AnalyticsService analyticsService = new AnalyticsService();
        analyticsService.initialize(context);
        return analyticsService;
    }

    /**
     * 获取初始化 AnalyticsService 所需的依赖项列表。
     *
     * @return 返回一个空列表，表示此初始化器不依赖于其他 Initializer。
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        // 返回空列表，表示无依赖项
        return Collections.emptyList();
    }
}
