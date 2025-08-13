package com.daz.lib_base.base;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;


/**
 * 作者：wx
 * 时间：2017/8/18 10:45
 * 描述：
 */
public abstract class VBSimpleActivity<VB extends ViewBinding, VM extends ViewModel> extends BaseActivity {

    protected VB binding;
    protected VM viewModel;
    protected Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.savedInstanceState = savedInstanceState;

        // 初始化 ViewBinding
        binding = initViewBinding();
        setContentView(binding.getRoot());

        // 初始化 ViewModel
        viewModel = createViewModel();

        initImmersionBar();
        onViewCreated();
        initEventAndData();
        observeViewModel(); // 添加观察方法
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
     */
    protected void initImmersionBar() {
        // 设置共同沉浸式样式
        // ImmersionBar.with(this).navigationBarColor(R.color.colorPrimary).init();
    }

    protected void onViewCreated() {
        // 可以在这里执行一些与View相关的初始化操作
    }

    /**
     * 初始化 ViewBinding
     */
    protected abstract VB initViewBinding();

    /**
     * 创建 ViewModel 实例
     */
    protected VM createViewModel() {
        return new ViewModelProvider(this).get(getViewModelClass());
    }

    /**
     * 获取 ViewModel 的 Class 类型
     */
    protected abstract Class<VM> getViewModelClass();

    /**
     * 初始化事件和数据
     */
    protected abstract void initEventAndData();

    /**
     * 观察 ViewModel 的数据变化
     */
    protected void observeViewModel() {
        // 默认空实现，子类可选择重写
    }
}
