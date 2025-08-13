package com.daz.lib_base.base;

import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.viewbinding.ViewBinding;


/**
 * 作者：wx
 * 时间：2017/8/18 10:45
 * 描述：
 */
public abstract class SimpleActivity<VB extends ViewBinding> extends BaseActivity {

    protected VB binding;
    protected final String TAG;
    protected Bundle savedInstanceState;
    public SimpleActivity() {
        TAG = getClass().getSimpleName();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        this.savedInstanceState = savedInstanceState;

        // 初始化ViewBinding
        binding = initViewBinding();
        setContentView(binding.getRoot());

        initImmersionBar();

        onViewCreated();

        initEventAndData();
    }

    @Override
    protected void onDestroy() {
        // 先释放子类资源
        if (binding != null) {
            binding = null;
        }
        // 再释放基类资源
        super.onDestroy();
    }

    /**
     * 初始化沉浸式
     * Init immersion bar.
     */
    protected void initImmersionBar() {
        //设置共同沉浸式样式
        //ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary).init();
    }

    protected void onViewCreated() {
        // 可以在这里执行一些与View相关的初始化操作
    }

    protected abstract VB initViewBinding(); // 新增方法，用于初始化具体的ViewBinding

    protected abstract void initEventAndData();

}

