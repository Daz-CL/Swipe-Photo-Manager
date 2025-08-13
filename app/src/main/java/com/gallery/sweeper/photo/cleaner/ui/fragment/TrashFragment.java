package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.daz.lib_base.base.VBBaseFragment;
import com.gallery.sweeper.photo.cleaner.databinding.FragmentTrashBinding;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:24
 * 描述：
 */
public class TrashFragment extends VBBaseFragment<FragmentTrashBinding, TrashViewModel> {
    @Override
    protected FragmentTrashBinding initViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentTrashBinding.inflate(inflater, container, false);
    }

    @Override
    protected Class<TrashViewModel> getViewModelClass() {
        return TrashViewModel.class;
    }

    @Override
    protected void initEventAndData() {

    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionEvent(PermissionRequiredEvent event) {
        switch (event.getOperationType()) {
            case PermissionRequiredEvent.OPERATION_DELETE://删除文件
                break;
            case PermissionRequiredEvent.OPERATION_DELETE_GRANTED://删除权限已授予
                break;
        }
    }
}
