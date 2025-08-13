package com.gallery.sweeper.photo.cleaner.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.daz.lib_base.base.SimpleActivity;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.app.SPConstants;
import com.gallery.sweeper.photo.cleaner.databinding.ActivitySplashBinding;
import com.gallery.sweeper.photo.cleaner.utis.SPUtils;


/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/5/11 10:34
 * 描述：开屏广告
 * 开屏广告是打开app的时候展示一个3-5s的全屏的广告
 * 开屏广告分冷启动和热启动，冷启动时要尽可能提前开始加载广告，这样才能确保在进入app之前加载到并展示广告
 * 热启动是app切换到后台，并没有真正的退出，这种情况下要能检测到并提前加载广告
 * <p>
 * 开屏广告一般要配合app的启动页来使用，在加载的时间先给用户看启动页，等广告加载成功后展示广告，广告结束进入app内部
 */
public class SplashActivity extends SimpleActivity<ActivitySplashBinding> {

    // 协议接受状态
    private boolean isAccept = false;

    // 主线程Handler用于UI操作
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    // 广告容器视图
    private FrameLayout adContainer;

    @Override
    protected ActivitySplashBinding initViewBinding() {
        return ActivitySplashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEventAndData() {
        initViews();                // 🗝️ 初始化视图组件

        scheduleControlsDisplay();  // 延迟显示协议控件
        initDataAndListeners();     // 初始化数据和点击监听

        // 开屏广告一般要配合app的启动页来使用，在加载的时间先给用户看启动页，等广告加载成功后展示广告，广告结束进入app内部
        // 启动超时定时器
        startTimeoutTimer();
    }

    private void startTimeoutTimer() {
        // 这里要做一个超时判断，如果超过xx秒以后没有广告返回，那么需要自动跳转到app内部，不影响app的使用

    }

    private void initViews() {
        // 初始隐藏协议相关控件
        binding.text.setVisibility(View.INVISIBLE);
        binding.accept.setVisibility(View.INVISIBLE);
        binding.accept.setEnabled(false);

        // 创建广告容器并添加到根布局
        adContainer = new FrameLayout(this);
        ((ViewGroup) binding.getRoot()).addView(adContainer);
    }

    private void scheduleControlsDisplay() {
        // 🗝️ 延迟1秒显示协议条款（合规性要求）
        isAccept = (Boolean) SPUtils.get(SPConstants.IS_ACCEPT, false);
        if (!isAccept) {
            uiHandler.postDelayed(() -> {
                if (!isFinishing()) {
                    binding.text.setVisibility(View.VISIBLE);
                    binding.accept.setVisibility(View.VISIBLE);
                    refreshAccept();
                    XLog.i(TAG, "用户协议条款已显示");
                }
            }, 1000);
        } else {
            uiHandler.postDelayed(() -> {
                if (!isFinishing()) {
                    navigateToMain();
                    startActivity(new Intent(mActivity, MainActivity.class));
                    XLog.i(TAG, "用户已同意协议，跳转主界面");
                }
            }, 1000);
        }
    }

    private void initDataAndListeners() {
        //DataLoader.init();  // 初始化数据加载器
        setupClickListeners(); // 设置点击事件
        refreshText();      // 刷新协议文本样式
    }

    //private boolean isClick = false;

    private void setupClickListeners() {
        /*binding.downtime.setOnClickListener(v -> {
            isClick = true;
        });*/
        // 协议勾选框点击事件
        binding.text.setOnClickListener(v -> {
            isAccept = !isAccept;
            XLog.d(TAG, "用户点击协议勾选框，当前状态：" + isAccept);
            refreshAccept();
        });

        isAccept = (Boolean) SPUtils.get(SPConstants.IS_ACCEPT, false);
        if (isAccept) {
            binding.accept.setVisibility(View.INVISIBLE);
            binding.text.setVisibility(View.INVISIBLE);
            /*binding.downtime.setVisibility(View.VISIBLE);
            new CountDownTimer(5000, 1000) {
                public void onTick(long millisUntilFinished) {
                    // 剩余的时间
                    XLog.w(TAG, "倒计时：" + (millisUntilFinished / 1000) + " 秒");
                    binding.downtime.setText((millisUntilFinished / 1000 + 1) + " s");
                    if (isClick && mActivity != null) {
                        XLog.e(TAG, "是手动点击!");
                        navigateToMain();
                    }
                }

                public void onFinish() {
                    if (!isFinishing()) {
                        navigateToMain();
                    }
                }
            }.start();*/
        }

        // 同意按钮点击事件
        binding.accept.setOnClickListener(v -> {
            if (isAccept && !isFinishing()) {
                SPUtils.save(SPConstants.IS_ACCEPT, isAccept);
                // 如果广告准备好，则展示插页广告
                /*if (videoUtils != null && videoUtils.isReadyInterstitial()) {
                    XLog.d(TAG, "广告准备好了,展示");
                    videoUtils.showInterstitial(mActivity);
                } else {
                    XLog.w(TAG, "广告未准备好，直接跳转");
                }*/
                XLog.i(TAG, "用户已同意协议，跳转主界面");
                navigateToMain();
            } else {
                XLog.w(TAG, "未同意协议时点击按钮被拦截");
            }
        });
    }

    private void cleanAdResources() {
        if (adContainer != null) {
            XLog.d(TAG, "清理广告容器视图");
            adContainer.removeAllViews();
        }
    }

    private void navigateToMain() {
        if (isAccept && !isFinishing()) {
            // 创建跳转意图时添加标志位
            Intent intent = new Intent(mActivity, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            // 如果MainActivity已存在则直接唤醒
            if (isTaskRoot()) {
                startActivity(intent);
            } else {
                // 直接唤醒现有实例
                mActivity.startActivity(intent);
            }

            // 优化销毁逻辑
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) finish();
            }, 50); // 适当延迟确保跳转完成
        }
    }

    private void refreshText() {
        TextView textView = binding.text;
        String text = "I Agree to the Terms of Service and acknowledge that I have read the Privacy Policy";

        SpannableString spannableString = new SpannableString(text);

        // 设置 "Terms of Service" 的样式和点击事件
        ClickableSpan termsOfServiceSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 处理点击事件，比如跳转到 Terms of Service 页面
                Intent intent = new Intent(mActivity, WebActivity.class);
                intent.putExtra("title", "Terms of Service");
                intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vSjMmN356jz0M1lzSoS6vL4GStEMZajpo8Kf677rVv0RZpTtB4PEvIOcct5UWMbSKL5T0Z9X_2hQNIf/pub");
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(Color.parseColor("#063DE2")); // 设置颜色为 #063DE2
                ds.setUnderlineText(false); // 不显示下划线
            }
        };

        // 设置 "Privacy Policy" 的样式和点击事件
        ClickableSpan privacyPolicySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 处理点击事件，比如跳转到 Privacy Policy 页面
                Intent intent = new Intent(mActivity, WebActivity.class);
                intent.putExtra("title", "Privacy Policy");
                intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vT9yzGn0X9tD3vk7QBUSBFFEht2Kk6nc-o8teKWitvaJHml4V5jZYXkhZehn8fGB4pePpvdKgIsBFfr/pub");
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(Color.parseColor("#063DE2")); // 设置颜色为 #063DE2
                ds.setUnderlineText(false); // 不显示下划线
            }
        };

        // 找到 "Terms of Service" 和 "Privacy Policy" 的位置
        int termsStart = text.indexOf("Terms of Service");
        int termsEnd = termsStart + "Terms of Service".length();
        int privacyStart = text.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        // 应用 Span
        spannableString.setSpan(termsOfServiceSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyPolicySpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance()); // 必须设置，否则点击事件无效
        textView.setHighlightColor(Color.TRANSPARENT); // 去除点击后的背景色
    }

    private void refreshAccept() {
        binding.text.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(mActivity, isAccept ? R.mipmap.check_act_p : R.mipmap.check_act_n), null, null, null);
        binding.accept.setEnabled(isAccept);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        uiHandler.removeCallbacksAndMessages(null);

        cleanAdResources();

    }

    //开屏页一定要禁止用户对返回按钮的控制，否则将可能导致用户手动退出了App而广告无法正常曝光和计费
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode || KeyEvent.KEYCODE_HOME == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

