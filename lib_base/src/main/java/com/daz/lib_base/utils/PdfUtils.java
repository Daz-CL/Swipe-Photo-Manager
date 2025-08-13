package com.daz.lib_base.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/12 1:38
 * 描述：
 */
public class PdfUtils {

    private static final String TAG = "PdfUtils";

    /**
     * 将 Bitmap 保存为 PDF 文件
     *
     * @param bitmap   要保存的图片
     * @param fileName 输出文件名（无需扩展名）
     * @return 保存后的文件路径，失败返回 null
     */
    public static String saveImageToPdf(Bitmap bitmap, String fileName) {
        // 1. 创建 PDF 文档
        PdfDocument document = new PdfDocument();

        // 2. 调整图片尺寸以适应 A4 纸（可选）
        Bitmap scaledBitmap = scaleBitmapToA4(bitmap);

        // 3. 创建页面信息
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                scaledBitmap.getWidth(),
                scaledBitmap.getHeight(),
                1 // 页码
        ).create();

        // 4. 开始页面
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // 5. 绘制图片到 PDF
        canvas.drawBitmap(scaledBitmap, 0, 0, null);

        // 6. 完成页面
        document.finishPage(page);

        // 7. 保存文件
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                fileName + ".pdf"
        );

        try {
            document.writeTo(new FileOutputStream(file));
            Log.d(TAG, "PDF 保存成功: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "保存 PDF 失败: " + e.getMessage());
            return null;
        } finally {
            // 8. 关闭文档
            document.close();
            scaledBitmap.recycle();
        }
    }

    /**
     * 将 Bitmap 缩放到 A4 尺寸（210mm x 297mm）
     */
    private static Bitmap scaleBitmapToA4(Bitmap original) {
        // A4 尺寸像素（300 DPI）
        int a4Width = 2480;  // 210mm * 300 DPI / 25.4
        int a4Height = 3508; // 297mm * 300 DPI / 25.4

        float scale = Math.min(
                (float) a4Width / original.getWidth(),
                (float) a4Height / original.getHeight()
        );

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
                original,
                0, 0,
                original.getWidth(),
                original.getHeight(),
                matrix,
                true
        );
    }

    public static String saveImagesToPdf(List<Bitmap> bitmaps, String fileName) {
        PdfDocument document = new PdfDocument();

        try {
            for (int i = 0; i < bitmaps.size(); i++) {
                Bitmap scaled = scaleBitmapToA4(bitmaps.get(i));
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        scaled.getWidth(),
                        scaled.getHeight(),
                        i + 1
                ).create();

                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawBitmap(scaled, 0, 0, null);
                document.finishPage(page);
                scaled.recycle();
            }

            // 保存文件（同上）

        } finally {
            document.close();
        }
        return fileName;
    }
}
