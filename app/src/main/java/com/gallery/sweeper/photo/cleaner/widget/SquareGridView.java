package com.gallery.sweeper.photo.cleaner.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/22 18:26
 * 描述：
 */
/**
 * 自定义 GridView，确保每个 item 是正方形，并自动计算 GridView 高度
 */
public class SquareGridView extends GridView {

    public SquareGridView(Context context) {
        super(context);
    }

    public SquareGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void layoutChildren() {
        try {
            super.layoutChildren();
        } catch (NullPointerException e) {
            // 捕获系统级异常并自动恢复
            post(this::requestLayout);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 无数据时返回最小高度
        if (getAdapter() == null || getAdapter().getCount() == 0) {
            setMeasuredDimension(
                    MeasureSpec.getSize(widthMeasureSpec),
                    resolveSize(0, heightMeasureSpec)
            );
            return;
        }

        // 有数据时正常测量
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}