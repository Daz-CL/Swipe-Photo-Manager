package com.daz.lib_base.base;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/6/13 16:52
 * 描述：
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ABaseActivity extends AppCompatActivity {

    protected Activity mActivity;
    private ProgressDialog loadingDialog;
    protected boolean isResumed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
    }

    protected void showLoading(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(message) || isFinishing()) return;

                dismissLoading();
                try {
                    loadingDialog = new ProgressDialog(ABaseActivity.this);
                    loadingDialog.setMessage(message);
                    loadingDialog.setCancelable(false);
                    loadingDialog.show();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Show loading failed", e);
                }
            }
        });
    }

    protected void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Dismiss loading failed", e);
            }
            loadingDialog = null;
        }
    }

    protected boolean isActivityVisible() {
        return isResumed && hasWindowFocus();
    }

    protected void safeRunOnUiThread(Runnable action) {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                try {
                    action.run();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "UI action failed", e);
                }
            }
        });
    }
}
