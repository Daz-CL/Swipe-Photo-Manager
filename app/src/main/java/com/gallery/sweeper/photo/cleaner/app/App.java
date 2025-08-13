package com.gallery.sweeper.photo.cleaner.app;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/5/10 10:39
 * 描述：
 */
public class App extends Application {

    public static final String CLOSE_LOADING_DIALOG = "com.photoeditor.menhair.manstyle.piclab.ACTION_CLOSE_LOADING";
    public static final String SELECT_BACKGROUND = "com.photoeditor.menhair.manstyle.piclab.SELECT_BACKGROUND";
    public static final String SELECT_STICKER = "com.photoeditor.menhair.manstyle.piclab.SELECT_STICKER";

    private static final int JOB_ID = 1001;
    private static final long SCAN_INTERVAL = 12 * 60 * 60 * 1000; // 12小时

    private static volatile App instance;

    public static App getInstance() {
        if (instance == null) {
            synchronized (App.class) {
                if (instance == null) {
                    instance = new App();
                }
            }
        }
        return instance;
    }



    @Override
    public void onCreate() {
        super.onCreate();

        //AppsFlyerLib.getInstance().init("GXVAvB7K6PczJyUsjPyppV", null, this);
        //AppsFlyerLib.getInstance().start(this);

        instance = this;

        /*BackgroundSettings.init();



        TradPlusSdk.setTradPlusInitListener(new TradPlusSdk.TradPlusInitListener() {
            @Override
            public void onInitSuccess() {
                // 初始化成功，建议在该回调后 发起广告请求
                XLog.d("广告", "TradPlusSdk初始化成功");
            }
        });
        TradPlusSdk.initSdk(instance, TestAdUnitId.APPID);*/

        //PhotoRepository.getInstance().cleanupNonExistingPhotos();

        // 注册媒体库变化观察者
        /*ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                new MediaStoreObserver()
        );*/


        //scheduleMediaScanJob(this);
    }
    /**
     * 安排媒体扫描任务
     * @param context 上下文
     */
    public static void scheduleMediaScanJob(Context context) {
        /*JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // 创建任务组件
        ComponentName componentName = new ComponentName(context, MediaScanJobService.class);

        // 创建任务配置
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE) // 不需要网络
                .setPersisted(true) // 设备重启后保持
                .setPeriodic(SCAN_INTERVAL) // 间隔时间
                .build();

        // 安排任务
        int result = jobScheduler.schedule(jobInfo);

        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d("MediaScan", "Job scheduled successfully");
        } else {
            Log.e("MediaScan", "Job scheduling failed");
        }*/
    }

    /**
     * 立即触发媒体扫描任务（用于调试）
     * @param context 上下文
     */
    public static void triggerMediaScanJobNow(Context context) {
        /*JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // 创建任务组件
        ComponentName componentName = new ComponentName(context, MediaScanJobService.class);

        // 创建立即执行的任务
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID + 1, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setOverrideDeadline(0) // 立即执行
                .build();

        jobScheduler.schedule(jobInfo);*/
    }
    /**
     * 媒体库变化观察者
     */
    /*private class MediaStoreObserver extends ContentObserver {
        public MediaStoreObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // 媒体库变化时触发扫描
            PhotoRepository.getInstance().scanMediaStore(App.this);
        }
    }*/
}
