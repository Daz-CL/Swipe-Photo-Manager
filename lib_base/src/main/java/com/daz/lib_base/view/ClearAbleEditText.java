package com.daz.lib_base.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.daz.lib_base.R;

/**
 * 创建者： wx

 * 创建时间：2017/3/29 17:09
 * 描述：此处添加类描述
 */

public class ClearAbleEditText extends AppCompatEditText
        implements View.OnFocusChangeListener, TextWatcher {

    /**
     * 删除按钮的引用
     */
    private Drawable mClearDrawable;

    /**
     * 控件是否有焦点
     */
    private boolean hasFocus;

    public ClearAbleEditText(Context context) {
        this(context, null);
    }

    public ClearAbleEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ClearAbleEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initEventAndData();
    }

    //文本改变监听
    private OnTextChangedListener onTextChangedListener;

    public interface OnTextChangedListener {
        void onBeforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int count, int after);

        void onAfterTextChanged(Editable s);

        void onShowClearDrawable();

        void onDismissClearDrawable();
    }

    public void setOnTextChangedListener(OnTextChangedListener onTextChangedListener) {
        this.onTextChangedListener = onTextChangedListener;
    }

    public void initEventAndData() {

        /*
          获取EditText的DrawableRight,假如没有设置我们就使用默认的图片
          getCompoundDrawables()获取Drawable的四个位置的数组
         */
        mClearDrawable = getCompoundDrawables()[2];

        if (mClearDrawable == null) {
            mClearDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_edit_clear);
        }

        //设置图标的位置以及大小,getIntrinsicWidth()获取显示出来的大小而不是原图片的带小
        mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(), mClearDrawable.getIntrinsicHeight());

        //默认设置隐藏图标
        setClearIconVisible(false);

        //设置焦点改变的监听
        setOnFocusChangeListener(this);

        //设置输入框里面内容发生改变的监听
        addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (onTextChangedListener != null) {
            onTextChangedListener.onBeforeTextChanged(s, start, count, after);
        }
    }

    /**
     * 当输入框里面内容发生变化的时候回调的方法
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (hasFocus) {
            boolean visible = getText().length() > 0;
            setClearIconVisible(visible);
            if (onTextChangedListener != null) {
                onTextChangedListener.onTextChanged(s, start, before, count);
                if (visible) {
                    onTextChangedListener.onShowClearDrawable();
                } else {
                    onTextChangedListener.onDismissClearDrawable();
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (onTextChangedListener != null) {
            onTextChangedListener.onAfterTextChanged(s);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            //getTotalPaddingRight()图标左边缘至控件右边缘的距离
            //getWidth() - getTotalPaddingRight()表示从最左边到图标左边缘的位置
            //getWidth() - getPaddingRight()表示最左边到图标右边缘的位置
            if (getCompoundDrawables()[2] != null) {
                boolean touchable = event.getX() > (getWidth() - getTotalPaddingEnd())
                        && (event.getX() < ((getWidth() - getPaddingEnd())));
                if (touchable) {
                    this.setText("");
                    if(onTextChangedListener != null){
                        onTextChangedListener.onDismissClearDrawable();
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        this.hasFocus = hasFocus;
        if (hasFocus) {
            setClearIconVisible(getText().length() > 0);
            //Logger.i("获取焦点");
        } else {
            setClearIconVisible(false);
            //Logger.i("失去焦点");
        }
    }

    /**
     * 设置清除图标的显示与隐藏，调用setCompoundDrawables为EditText绘制上去
     *
     * @param visible visible
     */
    protected void setClearIconVisible(boolean visible) {
        Drawable end = visible ? mClearDrawable : null;
        setCompoundDrawables(
                getCompoundDrawables()[0],
                getCompoundDrawables()[1],
                end,
                getCompoundDrawables()[3]);

        //Logger.i("clear drawable is visible: " + visible + "\nonTextChangedListener is null: " + (onTextChangedListener == null));

        /*if (onTextChangedListener != null) {
            if (visible) {
                onTextChangedListener.onShowClearDrawable();
            } else {
                onTextChangedListener.onDismissClearDrawable();
            }
        }*/
    }
}
