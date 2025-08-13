package com.daz.lib_base.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

/**
 * 作者：wx
 * 时间：2017/8/18 14:27
 * 描述：
 */

public abstract class SimpleFragment<T extends ViewBinding> extends BaseFragment {

    protected T binding;
    protected Activity mActivity;
    //protected Context mContext;
    protected Bundle savedInstanceState;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (Activity) activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用泛型T来指定特定的Binding类
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        TAG = this.getClass().getSimpleName();
        initEventAndData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 释放binding引用
        releaseBinding();
    }

    protected abstract T inflateBinding(LayoutInflater inflater, ViewGroup parent);

    protected abstract void initEventAndData();

    private void releaseBinding() {
        if (binding != null) {
            binding = null;
        }
    }
}

