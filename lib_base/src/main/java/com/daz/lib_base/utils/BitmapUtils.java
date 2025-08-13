package com.daz.lib_base.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/11/4 13:54
 * 描述：
 */
public class BitmapUtils {


    /**
     * 从视图创建位图
     *
     * @param v 要从中创建位图的视图，可为空
     * @return 创建的位图，如果视图为空则返回空
     */
    public static Bitmap createBitmapFromView(View v) {
        if (v == null) {
            return null;
        }

        Bitmap screenshot = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(screenshot);
        c.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(c);

        return screenshot;
    }

    public static String saveImageToGallery(Context context, Bitmap bmp) {
        long dataTake = System.currentTimeMillis();
        String jpegName = "IMG_" + dataTake + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, jpegName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        Uri external;
        Uri insertUri;
        String result = "";

        // 判断是否有SD卡，优先使用SD卡存储，当没有SD卡时使用手机存储
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            external = MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        }

        insertUri = context.getContentResolver().insert(external, values);
        if (insertUri == null) {
            return result;
        }

        OutputStream os = null;
        try {
            os = context.getContentResolver().openOutputStream(insertUri);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
            if (os != null) {
                os.flush();
                os.close();
            }
            result = insertUri.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static int[] getImageWidthAndHeight(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            int width = options.outWidth;
            int height = options.outHeight;
            inputStream.close();
            return new int[]{width, height};
        } catch (IOException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    public static String getRealPathFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            String[] projection = {MediaStore.Images.Media.DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    result = cursor.getString(columnIndex);
                }
            }
        } else if (uri.getScheme().equals("file")) {// 如果 URI 是文件 URI，直接返回路径
            result = uri.getPath();
        }
        return result;
    }

    public static int getImageOrientation(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0; // 图片未旋转
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0; // 读取失败时返回默认值
        }
    }
}
