package com.gallery.sweeper.photo.cleaner.ui;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.daz.lib_base.base.SimpleActivity;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.events.TrashEvents;
import com.gallery.sweeper.photo.cleaner.databinding.ActivityMainBinding;
import com.gallery.sweeper.photo.cleaner.ui.fragment.MainFragmentController;
import com.gallery.sweeper.photo.cleaner.ui.fragment.PhotoGroupFragment;
import com.gallery.sweeper.photo.cleaner.ui.fragment.SettingsFragment;
import com.gallery.sweeper.photo.cleaner.ui.fragment.TrashFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends SimpleActivity<ActivityMainBinding> {
    private MainFragmentController mainFragmentController;
    private final ArrayList<Fragment> fragments = new ArrayList<>();
    private final List<View> tabViewList = new ArrayList<>();
    private int tabOldIndex = 0;
    private TrashFragment trashFragment;
    private PhotoGroupFragment photoGroupFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected ActivityMainBinding initViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEventAndData() {
        initFragments();
    }

    private void initFragments() {
        FragmentManager fm = getSupportFragmentManager();

        // 复用或创建Fragment
        trashFragment = (TrashFragment) fm.findFragmentByTag("trash");
        if (trashFragment == null) {
            trashFragment = new TrashFragment();
        }
        fragments.add(trashFragment);
        tabViewList.add(binding.tabItemTrash);

        photoGroupFragment = (PhotoGroupFragment) fm.findFragmentByTag("photo_group");
        if (photoGroupFragment == null) {
            photoGroupFragment = new PhotoGroupFragment();
        }
        fragments.add(photoGroupFragment);
        tabViewList.add(binding.tabItemSwipeClean);

        settingsFragment = (SettingsFragment) fm.findFragmentByTag("settings");
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
        }
        fragments.add(settingsFragment);
        tabViewList.add(binding.tabItemSettings);

        // 统一设置点击监听
        binding.tabItemTrash.setOnClickListener(v -> switchTab(0));
        binding.tabItemSwipeClean.setOnClickListener(v -> switchTab(1));
        binding.tabItemSettings.setOnClickListener(v -> switchTab(2));

        binding.tabItemTrash.setSelected(true);
        mainFragmentController = MainFragmentController.getInstance(this, R.id.main_layout_frame_container, fragments);
        switchTab(0);
        mainFragmentController.showFragment(0);
    }

    private void switchTab(int index) {
        try {
            if (index != tabOldIndex) {
                tabViewList.get(tabOldIndex).setSelected(false);
                tabViewList.get(index).setSelected(true);
                tabOldIndex = index;
                mainFragmentController.showFragment(index);
            }
        } catch (Exception e) {
            XLog.e(TAG, "【UI】切换Fragment失败: " + e.getMessage());
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        try {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
                XLog.d(TAG, "【权限】EventBus已注册");
            }
        } catch (Exception e) {
            XLog.e(TAG, "【权限】EventBus注册失败: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PhotoRepository.getInstance().shutdown();
        try {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
                XLog.d(TAG, "【权限】EventBus已注销");
            }
        } catch (Exception e) {
            XLog.e(TAG, "【权限】EventBus注销失败: " + e.getMessage());
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventTrashChangeEvents(TrashEvents.TrashChangeEvent event) {
        int size = event.size;
        binding.tvCountTrash.setVisibility(size > 0 ? View.VISIBLE : View.INVISIBLE);
        binding.tvCountTrash.setText(String.format("%d", size));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventTrashSelectEvents(TrashEvents.selectTrashByGroup event) {
        switchTab(0);
    }
}