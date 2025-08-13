package com.gallery.sweeper.photo.cleaner.ui.fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.daz.lib_base.utils.XLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing on 2016/9/25.
 * 主界面Fragment控制器
 * 使用本类必须在onDestroy中调用hideFragment()方法
 */

public class MainFragmentController {

    /**
     * 容器视图ID
     */
    private int containerId;

    /**
     * Fragment集合
     */
    private ArrayList<Fragment> fragments;

    /**
     * fragmentManager
     */
    private FragmentManager fragmentManager;
    private int currentFragmentPosition = 0;

    /**
     * @param activity    FragmentActivity
     * @param containerId 容器视图ID
     * @param fragments   Fragment集合
     */
    private MainFragmentController(FragmentActivity activity, int containerId, ArrayList<Fragment> fragments) {
        fragmentManager = activity.getSupportFragmentManager();
        this.containerId = containerId;
        this.fragments = fragments;
        initFragment(fragments);
    }

    /**
     * 创建 BaseFragmentController 实例
     *
     * @param activity    FragmentActivity
     * @param containerId 容器视图ID
     * @param fragments   Fragment集合
     * @return controller
     */
    public static MainFragmentController getInstance(FragmentActivity activity, int containerId, @NonNull ArrayList<Fragment> fragments) {
        return new MainFragmentController(activity, containerId, fragments);
    }

    /**
     * 初始化 Fragment
     */
    private void initFragment(List<Fragment> fragments) {
        if (fragments.size() == 0) {
            XLog.w("TAG","List<Fragment> size 不能为0");
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragments) {
            transaction.add(containerId, fragment);
        }
        transaction.commit();
    }

    /**
     * 显示 Fragment
     *
     * @param position fragments 数组 position
     */
    public void showFragment(int position) {
        if (fragments.size() == 0) {
            XLog.w("TAG","List<Fragment> size 不能为0");
            return;
        }
        hideFragment();
        currentFragmentPosition = position;
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.show(fragments.get(position));
        transaction.commitAllowingStateLoss(); // 允许状态丢失
    }

    /**
     * 隐藏 Fragment
     * 必须在Activity的onDestroy()方法super.onDestroy()之前执行
     * protected void onDestroy() {
     * controller.hideFragment();
     * super.onDestroy();
     * }
     */
    public void hideFragment() {
        if (fragments.size() == 0) {
            XLog.w("TAG","List<Fragment> size 不能为0");
            return;
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragments) {
            if (fragment != null) {
                transaction.hide(fragment);
            }
        }
        transaction.commitAllowingStateLoss();
    }


    public int getCurrentFragment(){
        return currentFragmentPosition;
    }
}
