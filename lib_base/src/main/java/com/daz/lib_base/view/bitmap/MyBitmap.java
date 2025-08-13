package com.daz.lib_base.view.bitmap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyBitmap {

	public static String bitmaptoString(Bitmap bitmap) {

		// 将Bitmap转换成字符串

		String string = null;

		ByteArrayOutputStream bStream = new ByteArrayOutputStream();

		bitmap.compress(CompressFormat.PNG, 100, bStream);

		byte[] bytes = bStream.toByteArray();

		string = Base64.encodeToString(bytes, Base64.DEFAULT);

		return string;


	}

	public static  Bitmap drawableToBitamp(Drawable drawable)
	     {
		         int w = drawable.getIntrinsicWidth();
		         int h = drawable.getIntrinsicHeight();
		         System.out.println("Drawable转Bitmap");
		         Config config =
				                 drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888
		                         : Config.RGB_565;
		         Bitmap bitmap = Bitmap.createBitmap(w,h,config);
		         //注意，下面三行代码要用到，否在在View或者surfaceview里的canvas.drawBitmap会看不到图
		         Canvas canvas = new Canvas(bitmap);
		         drawable.setBounds(0, 0, w, h);
		         drawable.draw(canvas);
			 return bitmap;
		}

	public static Bitmap stringtoBitmap(String string) {

		// 将字符串转换成Bitmap类型

		Bitmap bitmap = null;

		try {

			byte[] bitmapArray;

			bitmapArray = Base64.decode(string, Base64.DEFAULT);

			bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0,

					bitmapArray.length);

		} catch (Exception e) {

			e.printStackTrace();

		}



		return bitmap;

	}

	public static byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	public static Bitmap Bytes2Bimap(byte[] b) {
		if (b==null||b.length == 0) {
			return null;
		}
		return BitmapFactory.decodeByteArray(b, 0, b.length);
	}

	public static String Bitmap2StrByBase64(Bitmap bit,int bilv) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bit.compress(CompressFormat.JPEG, bilv, bos);// ����100��ʾ��ѹ��
		byte[] bytes = bos.toByteArray();
		return Base64.encodeToString(bytes, Base64.DEFAULT);
	}


	public static Bitmap toRoundBitmap(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float roundPx;
		float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
		if (width <= height) {
			roundPx = width / 2;
			top = 0;
			bottom = width;
			left = 0;
			right = width;
			height = width;
			dst_left = 0;
			dst_top = 0;
			dst_right = width;
			dst_bottom = width;
		} else {
			roundPx = height / 2;
			float clip = (width - height) / 2;
			left = clip;
			right = width - clip;
			top = 0;
			bottom = height;
			width = height;
			dst_left = 0;
			dst_top = 0;
			dst_right = height;
			dst_bottom = height;
		}
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect src = new Rect((int) left, (int) top, (int) right,
				(int) bottom);
		final Rect dst = new Rect((int) dst_left, (int) dst_top,
				(int) dst_right, (int) dst_bottom);
		final RectF rectF = new RectF(dst);
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, src, dst, paint);
		return output;
	}

	// ����С����
	public static Bitmap zoomBitmap(Bitmap bitmap, float w, float h) {
		if (bitmap == null) {
			return null;
		}
		//MyLog.log("zoomBitmap::"+w+"XX"+h);
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Matrix matrix = new Matrix();
		float scaleWidht = ((float) w / width);
		float scaleHeight = ((float) h / height);
		matrix.postScale(scaleWidht, scaleHeight);
		//MyLog.log("postScale::"+scaleWidht+"XX"+scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
		if (bitmap.isRecycled()) {
			bitmap.recycle();
		}
		if (newbmp.isRecycled()) {
			newbmp.recycle();
		}
		return newbmp;
	}

	// ����С����
	//使图片适应屏幕宽高
	public static Bitmap zoomBitmap2(Bitmap bitmap, float screenWidth) {
		if (bitmap == null) {
			return null;
		}
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float radio=(float)height/(float)width;
		float screenHeight=radio*screenWidth;


		Matrix matrix = new Matrix();
		float scaleWidht = ((float) screenWidth / width);
		float scaleHeight = ((float) screenHeight / height);
		matrix.postScale(scaleWidht, scaleHeight);
		Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
				matrix, true);
		if (bitmap.isRecycled()) {
			bitmap.recycle();
		}
		if (newbmp.isRecycled()) {
			newbmp.recycle();
		}
		return newbmp;
	}




	public static Bitmap getBitmap2SDCard(String path) {
		try {
			if (path == null) {
				return null;
			}
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, opts);

			opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
			opts.inJustDecodeBounds = false;

			try {
				Bitmap bmp = BitmapFactory.decodeFile(path, opts);
				return bmp;
			} catch (OutOfMemoryError err) {
			}
		} catch (Exception e) {
			return null;
		}
		return null;

	}

	public static Bitmap getBitmap2SDCard2(String path, int width, int height) {
		try {
			if (path == null) {
				return null;
			}
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, opts);

			opts.inSampleSize = computeSampleSize(opts, -1, width * height);
			opts.inJustDecodeBounds = false;

			try {
				Bitmap bmp = BitmapFactory.decodeFile(path, opts);
				return bmp;
			} catch (OutOfMemoryError err) {
			}
		} catch (Exception e) {
			return null;
		}
		return null;

	}



	private static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	//��ȡbitmap�Ĵ�С
	public static int getBitmapSize(Bitmap bitmap){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){    //API 19
			return bitmap.getAllocationByteCount();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1){//API 12
			return bitmap.getByteCount();
		}
		return bitmap.getRowBytes() * bitmap.getHeight();                //earlier version
	}

	private static void savePic(Bitmap b,String strFileName){

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(strFileName);
			if (null != fos)
			{
				b.compress(CompressFormat.PNG, 90, fos);
				fos.flush();
				fos.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
}
