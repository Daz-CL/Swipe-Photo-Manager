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
public class CustomScrollView extends HorizontalScrollView {
        private float mLastX;
        private float mLastY;

        public CustomScrollView(Context context) {
            super(context);
        }

        public CustomScrollView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                // 获取当前触摸的坐标
                mLastX = ev.getX();
                mLastY = ev.getY();
            }

            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float deltaX = ev.getX() - mLastX;
                    float deltaY = ev.getY() - mLastY;

                    // 如果水平滑动的距离大于垂直滑动的距离，则交给水平滑动处理
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        return super.onInterceptTouchEvent(ev);
                    } else {
                        // 如果垂直滑动的距离大于水平滑动的距离，则交给垂直滑动处理
                        return false;
                    }
                default:
                    return super.onInterceptTouchEvent(ev);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            // 处理触摸事件，确保在上下滑动时也能滚动
            return super.onTouchEvent(ev);
        }
    }
