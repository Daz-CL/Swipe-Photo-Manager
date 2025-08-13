package com.daz.lib_base.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Admin on 2015/10/21.
 */
public class ScreenUtil {

    public static int dip2px(Context context, double dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5);
    }

    /**
     * @return 获取屏幕的高 单位：px
     */
    public static int getScreenHeightPx(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        if (windowManager != null) {
//            windowManager.getDefaultDisplay().getMetrics(dm);
            windowManager.getDefaultDisplay().getRealMetrics(dm);
            return dm.heightPixels;
        }
        return 0;

    }

    public static int getScreenWidth(Context context){
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context){
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    //获取状态栏高度方法1
    public static int getStatusBarHeight(Context context){
        int result=0;
        int resourceId=context.getResources().getIdentifier("status_bar_height","dimen","android");
        if(resourceId>0){
            result=context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    //获取状态栏高度方法2 反射
    public static int getStatusHeight(Context context) {

        int statusHeight = -1;
        try {

            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 代码截屏
     * @param activity
     * @return
     */
    public static Bitmap takeScreenShot(Activity activity){

//View是你需要截图的View

        View view = activity.getWindow().getDecorView();

        view.setDrawingCacheEnabled(true);

        view.buildDrawingCache();

        Bitmap b1 = view.getDrawingCache();

//获取状态栏高度

        Rect frame = new Rect();

        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);

        int statusBarHeight = frame.top;

        System.out.println(statusBarHeight);//获取屏幕长和高

        int width = activity.getWindowManager().getDefaultDisplay().getWidth();

        int height = activity.getWindowManager().getDefaultDisplay().getHeight();//去掉标题栏

          //Bitmap b = Bitmap.createBitmap(b1, 0, 25, 320, 455);

        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height - statusBarHeight);

        view.destroyDrawingCache();

        return b;

    }

    public static int getNavigationBarHeight(Context var0) {
        boolean var1 = ViewConfiguration.get(var0).hasPermanentMenuKey();
        int var2;
        return (var2 = var0.getResources().getIdentifier("navigation_bar_height", "dimen", "android")) > 0 && !var1?var0.getResources().getDimensionPixelSize(var2):0;
    }

    /**
     * 屏幕截图
     * @param activity
     * @return
     */
    public static Bitmap screenShot(AppCompatActivity activity) {
        if (activity == null){
            return null;
        }
        View view = activity.getWindow().getDecorView();
        //允许当前窗口保存缓存信息
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        int navigationBarHeight = getNavigationBarHeight(view.getContext());


        //获取屏幕宽和高
        int width = getScreenWidth(view.getContext());
        int height = getScreenHeight(view.getContext());

        // 全屏不用考虑状态栏，有导航栏需要加上导航栏高度
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
                    height + navigationBarHeight);
        } catch (Exception e) {
            // 这里主要是为了兼容异形屏做的处理，我这里的处理比较仓促，直接靠捕获异常处理
            // 其实vivo oppo等这些异形屏手机官网都有判断方法
            // 正确的做法应该是判断当前手机是否是异形屏，如果是就用下面的代码创建bitmap


            String msg = e.getMessage();
            // 部分手机导航栏高度不占窗口高度，不用添加，比如OppoR15这种异形屏
            if (msg.contains("<= bitmap.height()")){
                try {
                    bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
                            height);
                } catch (Exception e1) {
                    msg = e1.getMessage();
                    // 适配Vivo X21异形屏，状态栏和导航栏都没有填充
                    if (msg.contains("<= bitmap.height()")) {
                        try {
                            bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
                                    height - getStatusHeight(view.getContext()));
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }else {
                        e1.printStackTrace();
                    }
                }
            }else {
                e.printStackTrace();
            }
        }

        //销毁缓存信息
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    /**
     * view截图
     * @return
     */
    /*public static void viewShot(@NonNull final View v, @Nullable final String filePath,
                                @Nullable final ShotCallback shotCallback){
        if (null == v) {
            return;
        }
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                // 核心代码start
                Bitmap bitmap = Bitmap.createBitmap(v.getWidth() , v.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bitmap);
                v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
                v.draw(c);
                // end
                String savePath = filePath;
                if (TextUtils.isEmpty(savePath)){
                   // savePath = createImagePath();
                }
               *//* try {
                    //compressAndGenImage(bitmap,savePath);
                    com.orhanobut.logger.Logger.i("截图保存地址","--->截图保存地址：" + savePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }*//*
                if (null != shotCallback){
                    shotCallback.onShotComplete(bitmap,savePath);
                }
            }
        });
    }*/


}
