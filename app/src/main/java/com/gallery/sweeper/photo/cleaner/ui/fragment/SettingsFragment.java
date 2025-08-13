package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.daz.lib_base.base.SimpleFragment;
import com.gallery.sweeper.photo.cleaner.databinding.FragmentSettingsBinding;
import com.gallery.sweeper.photo.cleaner.ui.WebActivity;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/22 19:12
 * 描述：
 */
public class SettingsFragment extends SimpleFragment<FragmentSettingsBinding> {
    @Override
    protected FragmentSettingsBinding inflateBinding(LayoutInflater inflater, ViewGroup parent) {
        return FragmentSettingsBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void initEventAndData() {
        binding.tvPp.setOnClickListener(v -> {
            Intent intent = new Intent(mActivity, WebActivity.class);
            intent.putExtra("title", "Privacy Policy");
            //intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vT71Y0zoBs4GMYzmKMhx16xV3QfinX_IbbGD1wUwxfrnQEbFTvhUXVDkwne96DNRuWGpobwzL67RBRu/pub");
            startActivity(intent);
        });

        binding.tvService.setOnClickListener(v -> {
            Intent intent = new Intent(mActivity, WebActivity.class);
            intent.putExtra("title", "Terms of Service");
            //intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vQLE_nVV_R6WQmqAM0xxdH6fe7TWsEaOtp8DWlza4Za9e0eL2Txnn7yesMfL19QG3VmlKOBiSonT1Wj/pub");
            startActivity(intent);
        });


        try {
            PackageManager packageManager = mActivity.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(mActivity.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            // 在这里可以对versionName进行使用，比如打印到日志或者显示在界面上
            //System.out.println("应用的versionName是: " + versionName);
            binding.version.setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
