package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.daz.lib_base.base.VBBaseFragment;
import com.daz.lib_base.dialog.MessageDialogFragmentDataCallback;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.databinding.FragmentPhotoGroupBinding;
import com.gallery.sweeper.photo.cleaner.permission.PermissionManager;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * 照片分组展示页面
 * 负责请求存储权限并扫描媒体库
 */
public class PhotoGroupFragment extends VBBaseFragment<FragmentPhotoGroupBinding, PhotoGroupViewModel> implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 2;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 3;
    // 对话框标识
    private static final String DIALOG_TAG_PERM_RATIONALE = "permission_rationale";
    private static final String DIALOG_TAG_MANAGE_STORAGE = "manage_storage";

    @Override
    protected FragmentPhotoGroupBinding initViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentPhotoGroupBinding.inflate(inflater, container, false);
    }

    @Override
    protected Class<PhotoGroupViewModel> getViewModelClass() {
        return PhotoGroupViewModel.class;
    }

    @Override
    protected void initEventAndData() {
        new Handler().postDelayed(() -> PhotoRepository.getInstance().scanMediaStore(getActivity()), 1000);
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();
    }

    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {
        XLog.d(TAG, "【权限】权限已授权:" + list);
        // 权限获取成功后开始扫描媒体库
        new Handler().postDelayed(() -> PhotoRepository.getInstance().scanMediaStore(getActivity()), 500);
    }

    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        XLog.d(TAG, "【权限】权限被拒绝:" + list);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionEvent(PermissionRequiredEvent event) {
        XLog.d(TAG, "【权限】接收权限请求事件 | 操作类型: " + PermissionRequiredEvent.getOperationTypeName(event.getOperationType()));
        switch (event.getOperationType()) {
            case PermissionRequiredEvent.OPERATION_SCAN:
                XLog.d(TAG, "【权限】开始请求权限");
                requestStoragePermission();
                break;
            case PermissionRequiredEvent.OPERATION_SPECIAL_PERMISSION:
                XLog.d(TAG, "【权限】需要特殊权限，引导用户前往设置页面");
                showSpecialPermissionDialog();
                break;
        }
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        if (PermissionManager.needSpecialPermissionSetting()) {
            // Android 11+ 需要特殊权限
            XLog.d(TAG, "【权限】需要特殊权限，引导用户前往设置页面");
            showSpecialPermissionDialog();
        } else {
            // 传统权限请求
            String[] permissions = PermissionManager.getRequiredPermissions();
            if (permissions.length > 0) {
                XLog.d(TAG, "【权限】开始请求权限 | 权限列表: " + Arrays.toString(permissions));
                // 检查是否已经拥有权限
                if (EasyPermissions.hasPermissions(requireContext(), permissions)) {
                    // 已经拥有权限，直接开始扫描
                    new Handler().postDelayed(() -> {
                        XLog.d(TAG, "【权限】开始扫描");
                        PhotoRepository.getInstance().scanMediaStore(getActivity());
                    }, 500);
                } else {
                    // 请求权限
                    XLog.d(TAG, "【权限】开始请求权限");
                    EasyPermissions.requestPermissions(this,
                            getString(R.string.permission_rationale_storage),
                            REQUEST_CODE_STORAGE_PERMISSION, permissions);
                }
            }else {
                XLog.d(TAG, "【权限】不需要权限");
            }
        }
    }

    /**
     * 显示特殊权限引导对话框
     */
    private void showSpecialPermissionDialog() {
        MessageDialogParams params = new MessageDialogParams(
                getString(R.string.permission_required_title),
                getString(R.string.permission_rationale_manage_storage),
                1,
                MessageDialogParams.TYPE_NORMAL,
                getString(R.string.permission_cancel),
                getString(R.string.permission_open_settings),
                new MessageDialogFragmentDataCallback() {
                    @Override
                    public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        // 用户取消授权特殊权限
                        XLog.d(TAG, "【权限】用户取消特殊权限授权");
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        openSpecialPermissionSetting();
                    }
                }
        );
        showMessageDialog(params, DIALOG_TAG_MANAGE_STORAGE);
    }

    /**
     * 打开特殊权限设置页面
     */
    private void openSpecialPermissionSetting() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } catch (Exception e) {
                XLog.e(TAG, "【权限】打开特殊权限设置页面失败: " + e.getMessage());
                // 如果上面的方法不起作用，尝试打开应用详情页
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            // 用户从特殊权限设置页面返回，重新检查权限
            if (PermissionManager.hasStoragePermission()) {
                XLog.d(TAG, "【权限】特殊权限已获得，触发媒体访问权限对话框");
                // 权限已获得，触发媒体访问权限对话框并开始扫描
                triggerMediaAccessDialog();
            } else {
                XLog.d(TAG, "【权限】特殊权限未获得");
            }
        }
    }

    /**
     * 触发媒体访问权限对话框
     * 在获得MANAGE_EXTERNAL_STORAGE权限后，通过访问媒体内容触发系统权限对话框
     */
    private void triggerMediaAccessDialog() {
        new Thread(() -> {
            try {
                // 执行一个简单的媒体查询来触发系统权限对话框
                requireContext().getContentResolver().query(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{android.provider.MediaStore.Images.Media._ID},
                        null, null, null);
            } catch (Exception e) {
                XLog.e(TAG, "【权限】触发媒体权限对话框失败: " + e.getMessage());
            } finally {
                // 延迟执行实际的扫描操作
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        PhotoRepository.getInstance().scanMediaStore(getActivity()), 1000);
            }
        }).start();
    }
}
