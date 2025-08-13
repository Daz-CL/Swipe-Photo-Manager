package com.daz.lib_base.base;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.daz.lib_base.dialog.MessageDialogFragment;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/6/13 16:51
 * 描述：
 */
public abstract class AVBSimpleActivity<VB extends ViewBinding, VM extends ViewModel> extends ABaseActivity {

    protected VB binding;
    protected VM viewModel;
    protected final String TAG;
    protected WeakReference<AVBSimpleActivity<VB, VM>> activityRef;
    protected Bundle savedInstanceState;

    public AVBSimpleActivity() {
        TAG = getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.savedInstanceState = savedInstanceState;
        activityRef = new WeakReference<>(this);

        try {
            binding = initViewBinding();
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "ViewBinding init failed："+ e);
            finish();
            return;
        }

        // 安全初始化ViewModel
        try {
            Class<VM> viewModelClass = getViewModelClass();
            if (viewModelClass != null) {
                viewModel = new ViewModelProvider(this).get(viewModelClass);
            }
        } catch (Exception e) {
            Log.e(TAG, "ViewModel creation failed："+ e);
        }

        initImmersionBar();
        onViewCreated();
        initEventAndData();
        observeViewModel();
    }
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
    protected void onDestroy() {
        super.onDestroy();
        safelyReleaseBinding();
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
     * 安全执行UI操作
     */
    protected void runOnUiThreadSafe(Runnable action) {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                try {
                    action.run();
                } catch (Exception e) {
                    Log.e(TAG, "UI action failed："+ e);
                }
            }
        });
    }

    /**
     * 扩展：安全执行Binding操作
     */
    protected void withBinding(SafeBindingAction<VB> action) {
        if (binding != null && !isFinishing() && !isDestroyed()) {
            try {
                action.run(binding);
            } catch (Exception e) {
                Log.e(TAG, "Binding action failed："+ e);
            }
        } else {
            Log.w(TAG, "Binding unavailable");
        }
    }

    /**
     * 接口定义
     */
    public interface SafeBindingAction<VB extends ViewBinding> {
        void run(VB binding) throws Exception;
    }

    /**
     * 抽象方法
     */
    protected abstract VB initViewBinding();

    protected abstract Class<VM> getViewModelClass();

    protected abstract void initEventAndData();

    /**
     * 可选择实现的空方法
     */
    protected void initImmersionBar() {
    }

    protected void onViewCreated() {
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
        fragment.show(getSupportFragmentManager(), "MessageDialogFragment");
        XLog.d(this.getClass().getSimpleName(), String.format("【对话框】显示消息对话框: %s", params.message));
    }
}
