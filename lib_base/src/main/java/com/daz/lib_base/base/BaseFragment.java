package com.daz.lib_base.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.daz.lib_base.utils.XLog;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/5/11 9:37
 * 描述：
 */
public class BaseFragment extends Fragment {

    protected Activity mActivity;
    //protected Context mContext;
    protected boolean isDestroy = false;
    protected View mView;
    public String TAG = "onStop";
    protected boolean isAttached = false;

    public BaseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        //mContext = getContext();
        TAG = this.getClass().getSimpleName();
        //判断是否需要注册
        /*if (this.getClass().isAnnotationPresent(BindEventBus.class)) {
            EventBus.getDefault().register(this);
        }*/
        XLog.i( "BaseFragment","创建(Fragment): "+TAG );
    }

    private boolean isPrepared;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mView = getView();
        initPrepare();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached = true;
    }

    private Activity a;

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
    }

    /**
     * 第一次onResume中的调用onUserVisible避免操作与onFirstUserVisible操作重复
     */
    private boolean isFirstResume = true;

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstResume) {
            isFirstResume = false;
            return;
        }
        if (getUserVisibleHint()) {
            onUserVisible();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getUserVisibleHint()) {
            onUserInvisible();
        }
    }

    private boolean isFirstVisible = true;
    private boolean isFirstInvisible = true;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (isFirstVisible) {
                isFirstVisible = false;
                initPrepare();
            } else {
                onUserVisible();
            }
        } else {
            if (isFirstInvisible) {
                isFirstInvisible = false;
                onFirstUserInvisible();
            } else {
                onUserInvisible();
            }
        }
    }

    public synchronized void initPrepare() {
        if (isPrepared) {
            onFirstUserVisible();
        } else {
            isPrepared = true;
        }
    }

    /**
     * 第一次fragment可见（进行初始化工作）
     */
    public void onFirstUserVisible() {
        XLog.i(TAG, "onFirstUserVisible");

    }


    /**
     * fragment可见（切换回来或者onResume）
     */
    public void onUserVisible() {
        XLog.i(TAG, "onUserVisible");

    }

    /**
     * 第一次fragment不可见（不建议在此处理事件）
     */
    public void onFirstUserInvisible() {

    }

    /**
     * fragment不可见（切换掉或者onPause）
     */
    public void onUserInvisible() {

    }

    @Override
    public void onDestroy() {
        XLog.w( "BaseFragment","销毁(Fragment): "+TAG );
        isDestroy = true;
        mActivity = null;
        //mContext = null;
        mView = null;
        super.onDestroy();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}

