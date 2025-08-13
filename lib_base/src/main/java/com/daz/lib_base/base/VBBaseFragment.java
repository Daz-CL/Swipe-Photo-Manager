package com.daz.lib_base.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.daz.lib_base.dialog.MessageDialogFragment;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;

import org.greenrobot.eventbus.EventBus;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/6/13 14:04
 * 描述：
 */
public abstract class VBBaseFragment<VB extends ViewBinding, VM extends AndroidViewModel> extends Fragment {
    protected VB binding;
    protected VM viewModel;

    protected Activity mActivity;

    protected Bundle savedInstanceState;
    protected String TAG;


    @Override
    public void onStart() {
        super.onStart();
        try {
            // 注册EventBus（新增）
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            // 注销EventBus（新增）
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
                //XLog.d("EventBus已注销");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = this.getClass().getSimpleName();
        XLog.w( TAG,"创建(Fragment): "+TAG );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        binding = initViewBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化 ViewModel
        viewModel = createViewModel();

        onViewInitialized();
        initEventAndData();
        observeViewModel();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseBinding();
        XLog.i( TAG,"销毁(Fragment): "+TAG );
    }

    /**
     * 初始化 ViewBinding
     */
    protected abstract VB initViewBinding(LayoutInflater inflater, ViewGroup container);

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

    /**
     * 视图初始化完成回调
     */
    protected void onViewInitialized() {
        // 可以在这里执行一些与View相关的初始化操作
    }

    private void releaseBinding() {
        binding = null;
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

    protected void showMessageDialog(MessageDialogParams params, String tag) {
        // 使用自定义的showMessageDialog方法，但添加标签支持
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
        fragment.show(getChildFragmentManager(), tag); // 使用传入的标签
        XLog.d(TAG, "【对话框】显示消息对话框: " + tag);
    }
}
