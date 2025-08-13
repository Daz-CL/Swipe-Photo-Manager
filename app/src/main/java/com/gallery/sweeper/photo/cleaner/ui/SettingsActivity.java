package com.gallery.sweeper.photo.cleaner.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;

import com.daz.lib_base.base.SimpleActivity;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.databinding.ActivitySettingsBinding;


/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/11/1 10:53
 * 描述：
 */
public class SettingsActivity extends SimpleActivity<ActivitySettingsBinding> {

    @Override
    protected ActivitySettingsBinding initViewBinding() {
        return ActivitySettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEventAndData() {


        findViewById(R.id.tv_pp).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, WebActivity.class);
            intent.putExtra("title", "Privacy Policy");
            //intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vT71Y0zoBs4GMYzmKMhx16xV3QfinX_IbbGD1wUwxfrnQEbFTvhUXVDkwne96DNRuWGpobwzL67RBRu/pub");
            startActivity(intent);
        });

        findViewById(R.id.tv_service).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, WebActivity.class);
            intent.putExtra("title", "Terms of Service");
            //intent.putExtra("url", "https://docs.google.com/document/d/e/2PACX-1vQLE_nVV_R6WQmqAM0xxdH6fe7TWsEaOtp8DWlza4Za9e0eL2Txnn7yesMfL19QG3VmlKOBiSonT1Wj/pub");
            startActivity(intent);
        });


        binding.back.setOnClickListener(v -> finish());

        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            // 在这里可以对versionName进行使用，比如打印到日志或者显示在界面上
            //System.out.println("应用的versionName是: " + versionName);
            ((TextView) findViewById(R.id.version)).setText("Version " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
