package com.gallery.sweeper.photo.cleaner.ui;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.daz.lib_base.base.SimpleActivity;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.databinding.ActivityWebBinding;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/11/19 22:59
 * 描述：
 */
public class WebActivity extends SimpleActivity<ActivityWebBinding> {

    private WebView webView;

    @Override
    protected ActivityWebBinding initViewBinding() {
        return ActivityWebBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEventAndData() {
        binding.back.setOnClickListener(v -> finish());
        webView = binding.webView;
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        String title = getIntent().getStringExtra("title");

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(title);

        // 获取传递过来的 URL
        String url = getIntent().getStringExtra("url");


        // 加载 URL
        webView.loadUrl(url);

        // 设置 WebViewClient，防止点击链接时跳转到浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
