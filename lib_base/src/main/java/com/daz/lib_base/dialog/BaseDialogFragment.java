package com.daz.lib_base.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewbinding.ViewBinding;

import com.daz.lib_base.R;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/11 17:06
 * 描述：
 * 对话框Fragment基类（支持ViewBinding）
 *
 * @param <VB> 具体的ViewBinding类型
 *             </>
 *             使用说明：
 *             1. 继承本类并指定具体的ViewBinding类型
 *             2. 必须实现 initViewBinding() 方法
 *             3. 可选重写 setupViews() 和 observeData() 方法
 *             4. 通过 showSafely() 方法显示对话框
 */
public abstract class BaseDialogFragment<VB extends ViewBinding> extends AppCompatDialogFragment {

    // region 成员变量

    /**
     * ViewBinding实例（仅在onCreateView到onDestroyView之间有效）
     */
    protected VB binding;

    /**
     * 是否全屏显示（默认false）
     */
    private boolean fullScreen = false;

    /**
     * 点击对话框外部是否关闭（默认true）
     */
    private boolean cancelOutside = true;

    /**
     * 背景遮罩透明度（范围0-1，默认0.5）
     */
    private float dimAmount = 0.5f;

    protected final String TAG;

    public BaseDialogFragment() {
        TAG = getClass().getSimpleName();
    }

    // endregion

    // region 生命周期方法

    private Dialog mDialog;

    /**
     * 创建视图时初始化Binding
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 调用抽象方法初始化ViewBinding
        binding = initViewBinding(inflater, container);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后初始化配置
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureWindow(getDialog().getWindow());   // 配置窗口属性
        initViews();        // 初始化视图组件
        initData();      // 初始化数据观察
    }

    /**
     * 销毁视图时释放Binding引用
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 防止内存泄漏
    }

    // endregion

    // region 抽象方法

    /**
     * 初始化ViewBinding（必须实现）
     *
     * @param inflater  布局填充器
     * @param container 父容器
     * @return 具体的ViewBinding实例
     */
    @NonNull
    protected abstract VB initViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    // endregion

    // region 可选重写方法

    /**
     * 初始化视图组件（可选重写）
     */
    protected void initViews() {
    }

    /**
     * 初始化数据观察（可选重写）
     */
    protected void initData() {
    }

    // endregion

    // region 公共方法

    /**
     * 获取当前有效的Binding实例
     *
     * @throws IllegalStateException 当在Binding无效时访问抛出
     */
    @NonNull
    public VB getBinding() {
        if (binding == null) {
            throw new IllegalStateException("Binding 仅在 onCreateView 和 onDestroyView 之间有效");
        }
        return binding;
    }

    /**
     * 安全显示对话框（自动处理异常状态）
     *
     * @param manager Fragment管理器
     */
    public void showSafely(@NonNull FragmentManager manager) {
        // 避免重复添加
        if (isAdded() || isVisible()) return;

        try {
            // 常规显示方式
            show(manager, getClass().getSimpleName());
        } catch (IllegalStateException e) {
            // 处理状态丢失的情况
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, getClass().getSimpleName());
            ft.commitAllowingStateLoss();
        }
    }

    // endregion

    // region 样式配置方法

    /**
     * 配置窗口显示属性
     */
    protected void configureWindow( Window window) {
        if (window == null) return;
        // 其他配置
        mDialog = new Dialog(window.getContext(), R.style.BattleDialog);
        mDialog.setCanceledOnTouchOutside(cancelOutside);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (!cancelOutside) {
            mDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        }

        // 基础配置
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setDimAmount(dimAmount);

        // 设置窗口尺寸
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = fullScreen ?
                WindowManager.LayoutParams.MATCH_PARENT :
                WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);


    }

    /**
     * 设置是否全屏显示
     *
     * @param fullScreen true=全屏，false=自适应内容高度
     */
    public void setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
        updateWindowSize();
    }

    /**
     * 设置点击外部是否关闭
     *
     * @param cancelOutside true=可关闭，false=不可关闭
     */
    public void setCancelOutside(boolean cancelOutside) {
        this.cancelOutside = cancelOutside;
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(cancelOutside);
        }
        if (!cancelOutside) {
            mDialog.setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        }
    }

    public boolean isCancelOutside() {
        return cancelOutside;
    }

    /**
     * 设置背景遮罩透明度
     *
     * @param dimAmount 透明度（0-1）
     */
    public void setDimAmount(float dimAmount) {
        this.dimAmount = Math.max(0, Math.min(1, dimAmount));
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setDimAmount(this.dimAmount);
        }
    }

    /**
     * 更新窗口尺寸（当配置变更时调用）
     */
    private void updateWindowSize() {
        if (getDialog() == null || getDialog().getWindow() == null) return;

        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.height = fullScreen ?
                WindowManager.LayoutParams.MATCH_PARENT :
                WindowManager.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes(params);
    }

    // endregion
}
