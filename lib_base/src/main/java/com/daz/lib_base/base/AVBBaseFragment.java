package com.daz.lib_base.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.daz.lib_base.dialog.MessageDialogFragment;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;

import java.lang.ref.WeakReference;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/6/13 16:45
 * 描述：
 */
public abstract class AVBBaseFragment<VB extends ViewBinding, VM extends ViewModel> extends Fragment {

    protected VB binding;
    protected VM viewModel;
    protected WeakReference<Activity> activityRef;
    protected Bundle savedInstanceState;
    protected String TAG;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            activityRef = new WeakReference<>((Activity) context);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        try {
            binding = initViewBinding(inflater, container);
            return binding.getRoot();
        } catch (Exception e) {
            Log.e("VBBaseFragment", "ViewBinding init failed："+ e);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TAG = this.getClass().getSimpleName();

        // 安全创建ViewModel
        try {
            Class<VM> viewModelClass = getViewModelClass();
            if (viewModelClass != null) {
                viewModel = new ViewModelProvider(this).get(viewModelClass);
            }
        } catch (Exception e) {
            Log.e(TAG, "ViewModel creation failed："+ e);
        }

        onViewInitialized();
        initEventAndData();
        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        safelyReleaseBinding();
    }

    /**
     * 安全释放Binding资源
     */
    protected void safelyReleaseBinding() {
        try {
            binding = null;
        } catch (Exception e) {
            Log.e(TAG, "Binding release failed："+ e);
        }
    }

    /**
     * 安全获取Activity引用
     */
    @Nullable
    protected Activity getSafeActivity() {
        return activityRef != null ? activityRef.get() : null;
    }

    /**
     * 安全显示对话框
     */
    protected void showSafeDialog(MessageDialogParams params) {
        Activity activity = getSafeActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            // 实现对话框显示逻辑
            showMessageDialog(params);
        } catch (Exception e) {
            Log.e(TAG, "Dialog show failed："+ e);
        }
    }

    /**
     * 抽象方法
     */
    protected abstract VB initViewBinding(LayoutInflater inflater, ViewGroup container);

    protected abstract Class<VM> getViewModelClass();

    protected abstract void initEventAndData();

    /**
     * 可选择实现的空方法
     */
    protected void onViewInitialized() {
    }

    protected void observeViewModel() {
    }

    protected void showMessageDialog(MessageDialogParams params) {
        MessageDialogFragment fragment = new MessageDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(MessageDialogFragment.INTENT_KEY_TOUCH, params.isTouchable);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE, params.message);
        bundle.putString(MessageDialogFragment.INTENT_KEY_MESSAGE_SUB, params.subMessage);
        bundle.putInt(MessageDialogFragment.INTENT_KEY_MESSAGE_TYPE, params.type);
        bundle.putString(MessageDialogFragment.INTENT_KEY_LEFT_TEXT, params.leftButtonText);
        bundle.putString(MessageDialogFragment.INTENT_KEY_RIGHT_TEXT, params.rightButtonText);

        fragment.setArguments(bundle);
        fragment.setDataCallback(params.callback);
        fragment.show(getChildFragmentManager(), "MessageDialogFragment");
        XLog.d(this.getClass().getSimpleName(), String.format("【对话框】显示消息对话框: %s", params.message));
    }
}