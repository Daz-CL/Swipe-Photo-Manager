package com.daz.lib_base.view.scroll;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/2/17 16:58
 * 描述：
 */
public class CustomHorizontalScrollView extends HorizontalScrollView {

    public CustomHorizontalScrollView(Context context) {
        super(context);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 判断是否需要拦截事件
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 获取子控件的位置
            View child = getChildAt(0);
            if (child != null) {
                Rect rect = new Rect();
                child.getHitRect(rect);
                if (rect.contains((int) ev.getX(), (int) ev.getY())) {
                    // 如果点击位置在子控件内，不拦截事件
                    return false;
                }
            }
        }
        awakenScrollBars();
        return super.onInterceptTouchEvent(ev);
    }
}
