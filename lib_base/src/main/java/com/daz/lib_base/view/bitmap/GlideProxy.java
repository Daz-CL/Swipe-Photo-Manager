package com.daz.lib_base.view.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.daz.lib_base.R;
import com.daz.lib_base.utils.ScreenUtil;

/**
 * 作者：wx
 * 时间：2018/5/15 11:39
 * 描述：Glide 代理
 */

public class GlideProxy {

    public static void normalNotBg(Context context, String imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions()
                .dontAnimate()
                .centerCrop();
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }


    public static void normal(Context context, Bitmap imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions()
                .dontAnimate()
                .centerCrop()
                //.placeholder(R.drawable.shape_blue)
                /*.error(R.drawable.shape_blue)*/;
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void normal(Context context, String imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions()
                .dontAnimate()
                .centerCrop()
                //.placeholder(R.drawable.shape_blue)
                /*.error(R.drawable.shape_blue)*/;
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void normal(Context context, @DrawableRes int imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions()
                .dontAnimate()
                .centerCrop();
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(ContextCompat.getDrawable(context, imgUrl))
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void normal(Context context, String imgUrl, ImageView imageView, @NonNull RequestOptions requestOptions) {
        if (context == null) return;
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }


    public static void normal(Context context, String imgUrl, ImageView imageView, int width, int height) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.shape_blue)
                .error(R.drawable.shape_blue)
                .override(width, height);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        try {
            Glide.with(context)
                    .load(imgUrl)
                    .transition(transitionOptions)
                    .apply(requestOptions)
                    .into(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void circle(Context context, String imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.shape_blue)
                .placeholder(R.drawable.shape_blue)
                .transform(new GlideCircleTransforms(context));
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        try {
            Glide.with(context)
                    .load(imgUrl)
                    .transition(transitionOptions)
                    .apply(requestOptions)
                    .into(imageView);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void circle(Context mContext, String imgUrl, ImageView imageView, int radio,
                              boolean leftTop, boolean rightTop, boolean leftBottom, boolean rightBottom) {
        if (mContext == null) return;
        CornerTransform transformation = new CornerTransform(mContext, ScreenUtil.dip2px(mContext, radio));
        //只是绘制左上角和右上角圆角
        transformation.setExceptCorner(leftTop, rightTop, leftBottom, rightBottom);

        Glide.with(mContext)
                .asBitmap() //加载with后面，load前面
                .skipMemoryCache(true) //必须添加此属性
                .load(imgUrl)
                .thumbnail(0.2f)
                //  .apply(RequestOptions.bitmapTransform(transformation))
                .transform(transformation)
                .into(imageView);

    }


    public static void circle(Context context, String imgUrl, ImageView imageView, int radio, int width, int height) {
        if (context == null) return;
        //设置图片圆角角度
        RoundedCorners roundedCorners = new RoundedCorners(radio);

        //通过RequestOptions扩展功能,override:采样率,因为ImageView就这么大,可以压缩图片,降低内存消耗
        RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners).override(300, 300);

        Glide.with(context).load(imgUrl).apply(requestOptions).into(imageView);

    }

    public static void circle(Context context, String imgUrl, ImageView imageView, int radio) {
        if (context == null) return;
        //设置图片圆角角度
        RoundedCorners roundedCorners = new RoundedCorners(radio);

        //通过RequestOptions扩展功能,override:采样率,因为ImageView就这么大,可以压缩图片,降低内存消耗
        RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners).override(300, 300);

        Glide.with(context).load(imgUrl).apply(requestOptions).into(imageView);

    }

    public static void circleCorners(Context context, String imgUrl, ImageView imageView, int radio) {
        if (context == null) return;
        //设置图片圆角角度
        GlideRoundTransform roundedCorners = new GlideRoundTransform(context,radio);

        //通过RequestOptions扩展功能,override:采样率,因为ImageView就这么大,可以压缩图片,降低内存消耗
        RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners)
                .error(R.drawable.btn_background_save);

        Glide.with(context)
                .load(imgUrl)
                .apply(requestOptions)
                .into(imageView);
    }

   /* public static void circleCorners(Context context, String res , ImageView imageView, int radio) {
        if (context == null) return;
        //设置图片圆角角度
        GlideRoundTransform roundedCorners = new GlideRoundTransform(context, radio);

        //通过RequestOptions扩展功能,override:采样率,因为ImageView就这么大,可以压缩图片,降低内存消耗
        RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners)
                .error(R.drawable.btn_background_save);

        Glide.with(context)
                .load(res)
                .apply(requestOptions)
                .into(imageView);
    }*/

    public static void circleCorners(Context context, int resId , ImageView imageView, int radio) {
        if (context == null) return;
        //设置图片圆角角度
        GlideRoundTransform roundedCorners = new GlideRoundTransform(context, radio);

        //通过RequestOptions扩展功能,override:采样率,因为ImageView就这么大,可以压缩图片,降低内存消耗
        RequestOptions requestOptions = RequestOptions.bitmapTransform(roundedCorners)
                .error(R.drawable.shape_blue);

        Glide.with(context)
                .load(resId)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circle(Context context, @DrawableRes int imgUrl, ImageView imageView) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new GlideCircleTransforms(context));
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .dontTransition()
                .crossFade();
        Glide.with(context)
                .load(ContextCompat.getDrawable(context, imgUrl))
                .transition(transitionOptions)
                .apply(requestOptions)
                .dontAnimate()
                .skipMemoryCache(true)
                .into(imageView);
    }

    public static void circle(Context context, String imgUrl, ImageView imageView, int width, int height) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .placeholder(R.drawable.shape_blue)
                .override(width, height)
                .transform(new GlideCircleTransforms(context));
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .skipMemoryCache(true)
                .into(imageView);
    }

    public static void circle(Context context, @DrawableRes int imgUrl, ImageView imageView, int width, int height) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .placeholder(R.drawable.shape_blue)
                .override(width, height)
                .transform(new GlideCircleTransforms(context));
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(ContextCompat.getDrawable(context, imgUrl))
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circle(Context context, @DrawableRes int imgUrl, ImageView imageView, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .placeholder(R.drawable.shape_blue)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(ContextCompat.getDrawable(context, imgUrl))
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circle(Context context, String imgUrl, ImageView imageView, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circleNoBG(Context context, @DrawableRes int imgUrl, ImageView imageView, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(ContextCompat.getDrawable(context, imgUrl))
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circleNoBG(Context context, String imgUrl, ImageView imageView, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(imgUrl)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circleError(Context context, ImageView imageView) {
        if (context == null) return;
       /* RequestOptions requestOptions = new RequestOptions() .dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue);*/
        Glide.with(context)
                .load(R.drawable.shape_blue)
                .into(imageView);

    }

    public static void normalError(Context context, ImageView imageView) {
        if (context == null) return;
       /* RequestOptions requestOptions = new RequestOptions() .dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue);*/
        Glide.with(context)
                .load(R.drawable.shape_blue)
                .into(imageView);

    }

    public static void circleLogo(Context context, ImageView imageView, int logoId, int defaultId, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(defaultId)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(logoId)
                .transition(transitionOptions).skipMemoryCache(true)
                .apply(requestOptions)
                .into(imageView);
    }
    public static void circleLogo(Context context, ImageView imageView, Bitmap logoUrl, int defaultId, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(defaultId)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(logoUrl)
                .transition(transitionOptions).skipMemoryCache(true)
                .apply(requestOptions)
                .into(imageView);
    }
    public static void circleLogo(Context context, ImageView imageView, String logoUrl, int defaultId, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(defaultId)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(logoUrl)
                .transition(transitionOptions).skipMemoryCache(true)
                .apply(requestOptions)
                .into(imageView);
    }

     public static void circleLogo(Context context, ImageView imageView, Uri uri, int defaultId, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(defaultId)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(uri)
                .transition(transitionOptions).skipMemoryCache(true)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void circleLogo(Context context, ImageView imageView, GlideCircleTransform glideCircleTransform) {
        if (context == null) return;
        RequestOptions requestOptions = new RequestOptions().dontAnimate()
                .centerCrop()
                .error(R.drawable.shape_blue)
                .transform(glideCircleTransform);
        DrawableTransitionOptions transitionOptions = new DrawableTransitionOptions()
                .crossFade();
        Glide.with(context)
                .load(R.drawable.shape_blue)
                .transition(transitionOptions)
                .apply(requestOptions)
                .into(imageView);
    }

    public static void loadImage(Context context, String url, ImageView imageView) {
        Glide.with(context)
                .asBitmap()//强制Glide返回一个Bitmap对象
                .load(url)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable @org.jetbrains.annotations.Nullable Transition<? super Bitmap> transition) {
                        if (imageView == null) return;

                        int width = resource.getWidth();
                        int height = resource.getHeight();

                        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                            /*Logger.d("标签："
                                    + "\n" + "图片宽高：" + width + " × " + height
                                    + "\n" + "控件宽高：" + layoutParams.width + " × " + layoutParams.height);*/

                        layoutParams.width = width;
                        layoutParams.height = height;

                        imageView.setLayoutParams(layoutParams);
                        //ViewGroup.LayoutParams layoutParams1 = imageView.getLayoutParams();
                            /*Logger.w("标签："
                                    + "\n" + "图片宽高：" + width + " × " + height
                                    + "\n" + "控件宽高：" + layoutParams1.width + " × " + layoutParams1.height);*/
                        GlideProxy.normal(context, resource, imageView);
                    }
                });
    }
}
