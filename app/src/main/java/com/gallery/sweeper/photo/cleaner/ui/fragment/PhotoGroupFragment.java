package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.daz.lib_base.base.VBBaseFragment;
import com.daz.lib_base.dialog.MessageDialogFragmentDataCallback;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.SingleClickListener;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.GroupType;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;
import com.gallery.sweeper.photo.cleaner.data.events.GroupEvent;
import com.gallery.sweeper.photo.cleaner.data.events.ReloadGroupEvent;
import com.gallery.sweeper.photo.cleaner.databinding.FragmentPhotoGroupBinding;
import com.gallery.sweeper.photo.cleaner.permission.PermissionConstants;
import com.gallery.sweeper.photo.cleaner.permission.PermissionManager;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;
import com.gallery.sweeper.photo.cleaner.ui.SwipeTrashActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

/**
 * PhotoGroupFragment 是一个用于展示照片分组的 Fragment 页面。
 * 它继承自 VBBaseFragment，并实现了 EasyPermissions.PermissionCallbacks 接口，
 * 用于处理运行时权限请求，特别是相册扫描所需的存储权限。
 * <p>
 * 该 Fragment 负责初始化 UI、检查并请求权限、启动媒体扫描流程，并处理权限被拒或需要特殊权限的情况。
 */
public class PhotoGroupFragment extends VBBaseFragment<FragmentPhotoGroupBinding, PhotoGroupViewModel>
        implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "PhotoGroupFragment";
    private static final String DIALOG_TAG_PERM_DENIED = "permission_denied";
    private static final String DIALOG_TAG_MANAGE_STORAGE = "manage_storage";
    private PhotoGroupAdapter groupAdapter;

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
        XLog.d(TAG, "【UI初始化】照片分组页面初始化完成");
        initViews();
        initSortingControls();
        // 延迟启动权限检查流程
        new Handler().postDelayed(this::checkAndRequestScanPermission, 500);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getPhotoGroups().observe(getViewLifecycleOwner(), this::updateGroups);

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                binding.viewEmpty.setVisibility(View.VISIBLE);
                binding.viewMain.setVisibility(View.GONE);
                binding.tvEmpty.setText("Your group is empty");
                XLog.e(TAG, errorMessage);
            }
        });
    }

    private void initViews() {
        groupAdapter = new PhotoGroupAdapter(new ArrayList<>());
        binding.viewMain.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.viewMain.setAdapter(groupAdapter);

        groupAdapter.setOnItemClickListener((parent, view, position, id) -> {
            if (SingleClickListener.singleClick(500)) {
                PhotoGroup group = groupAdapter.getItem(position);
                if ((group.getKeepCount() + group.getTrashCount()) == group.getPhotoCount()) return;

                XLog.d(TAG, "点击分组: " + group);
                Intent intent = new Intent(getActivity(), SwipeTrashActivity.class);
                intent.putExtra("group_key", group.getGroupKey());
                intent.putExtra("group_type", group.getGroupType());
                startActivityForResult(intent, 1000);
            }
        });

        binding.viewEmpty.setOnClickListener(v -> {
            if (SingleClickListener.singleClick(500)) {
                checkAndRequestScanPermission();
            }
        });
    }

    private void initSortingControls() {
        // 分组类型切换统一处理
        View.OnClickListener groupTypeListener = v -> {
            if (!PermissionManager.hasPermission(PermissionManager.PermissionType.SCAN)) return;
            GroupType type = (v.getId() == R.id.btn_year_groups) ? GroupType.YEAR : GroupType.MONTH;
            viewModel.setGroupType(type);
            updateGroupsTypeButtonIcon(type);
        };

        binding.btnYearGroups.setOnClickListener(groupTypeListener);
        binding.btnMonthGroups.setOnClickListener(groupTypeListener);

        // 排序按钮
        binding.btnSort.setOnClickListener(v -> {
            if (SingleClickListener.singleClick(500)) {
                if (!PermissionManager.hasPermission(PermissionManager.PermissionType.SCAN)) return;
                PhotoRepository.getInstance().setAscending(!PhotoRepository.getInstance().isAscending());
                updateSortButtonIcon();
                viewModel.loadGroups();
            }
        });

        // 初始化按钮状态
        updateSortButtonIcon();
        updateGroupsTypeButtonIcon(PhotoRepository.getInstance().getCurrentGroupType());
    }

    private void updateSortButtonIcon() {
        binding.btnSort.setImageResource(PhotoRepository.getInstance().isAscending() ? R.mipmap.sorting_up : R.mipmap.sorting_down);
    }

    private void updateGroupsTypeButtonIcon(GroupType currentType) {
        binding.btnYearGroups.setSelected(currentType == GroupType.YEAR);
        binding.btnYearGroups.setTextColor(ContextCompat.getColor(requireContext(),
                currentType == GroupType.YEAR ? R.color.white : R.color.group_text));
        binding.btnYearGroups.setBackground(currentType == GroupType.YEAR ?
                ContextCompat.getDrawable(requireContext(), R.drawable.shape_group_select) : null);

        binding.btnMonthGroups.setSelected(currentType == GroupType.MONTH);
        binding.btnMonthGroups.setTextColor(ContextCompat.getColor(requireContext(),
                currentType == GroupType.MONTH ? R.color.white : R.color.group_text));
        binding.btnMonthGroups.setBackground(currentType == GroupType.MONTH ?
                ContextCompat.getDrawable(requireContext(), R.drawable.shape_group_select) : null);
    }

    private void updateGroups(List<PhotoGroup> groups) {
        groupAdapter.submitList(groups);

        // 根据是否有数据显示空视图或列表
        if (groups == null || groups.isEmpty()) {
            binding.viewEmpty.setVisibility(View.VISIBLE);
            binding.viewMain.setVisibility(View.GONE);
            binding.tvEmpty.setText("Your group is empty");
        } else {
            binding.viewEmpty.setVisibility(View.GONE);
            binding.viewMain.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 核心权限处理流程：检查并请求相册扫描权限。
     * <p>
     * 步骤如下：
     * 1. 检查是否已有权限；
     * 2. 若需特殊权限（如 MANAGE_EXTERNAL_STORAGE）则引导用户开启；
     * 3. 否则请求标准权限。
     */
    private void checkAndRequestScanPermission() {
        XLog.d(TAG, "【权限处理】开始相册扫描权限检查");

        // 步骤1：检查是否已有权限
        if (PermissionManager.hasPermission(PermissionManager.PermissionType.SCAN)) {
            XLog.i(TAG, "【权限处理】相册扫描权限已授权");
            startMediaScan();
            return;
        }

        // 步骤2：检查是否需要特殊权限
        if (PermissionManager.needSpecialPermissionSetting()) {
            XLog.w(TAG, "【权限处理】需要特殊权限(MANAGE_EXTERNAL_STORAGE)");
            showSpecialPermissionDialog();
            return;
        }

        // 步骤3：请求标准权限
        String[] requiredPermissions = PermissionManager.getRequiredPermissions(
                PermissionManager.PermissionType.SCAN);

        if (requiredPermissions.length == 0) {
            XLog.e(TAG, "【权限处理】无需要请求的权限，逻辑异常");
            return;
        }

        XLog.d(TAG, "【权限处理】请求标准权限: " + Arrays.toString(requiredPermissions));
        // 使用EasyPermissions请求权限
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this,
                        PermissionConstants.REQUEST_CODE_STORAGE_PERMISSION,
                        requiredPermissions)
                        .setRationale(R.string.permission_rationale_storage)
                        .setPositiveButtonText(R.string.permission_confirm)
                        .setNegativeButtonText(R.string.permission_cancel)
                        .build()
        );
    }

    /**
     * 启动媒体库扫描操作。
     * 延迟调用 PhotoRepository 的 scanMediaStore 方法进行实际扫描。
     */
    private void startMediaScan() {
        XLog.d(TAG, "【相册扫描】启动媒体库扫描");
        new Handler().postDelayed(() ->
                PhotoRepository.getInstance().scanMediaStore(requireActivity()), 500);
    }


    private void updateUIByPermissionState() {
        boolean hasPermission = PermissionManager.hasPermission(PermissionManager.PermissionType.SCAN);
        XLog.d(TAG, "【权限】更新UI状态: " + hasPermission);

        binding.btnSort.setEnabled(hasPermission);
        binding.btnYearGroups.setEnabled(hasPermission);
        binding.btnMonthGroups.setEnabled(hasPermission);

        if (hasPermission) {
            binding.viewEmpty.setVisibility(View.GONE);
            viewModel.loadGroups(); // 确保加载数据
        } else {
            showPermissionDeniedDialog();
        }
    }

    /**
     * 处理权限请求结果回调。
     * 将权限请求结果转发给 EasyPermissions 进行统一处理。
     *
     * @param requestCode  请求码
     * @param permissions  请求的权限数组
     * @param grantResults 权限授予结果数组
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将权限结果转发给EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 权限被授予时的回调方法。
     * 当请求码匹配时，启动媒体扫描流程。
     *
     * @param requestCode 请求码
     * @param perms       被授予的权限列表
     */
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PermissionConstants.REQUEST_CODE_STORAGE_PERMISSION) {
            XLog.i(TAG, "【权限处理】相册权限已授权: " + perms);
            startMediaScan();
        }
    }

    /**
     * 权限被拒绝时的回调方法。
     * 当请求码匹配时，显示权限被拒对话框。
     *
     * @param requestCode 请求码
     * @param perms       被拒绝的权限列表
     */
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PermissionConstants.REQUEST_CODE_STORAGE_PERMISSION) {
            XLog.w(TAG, "【权限处理】相册权限被拒绝: " + perms);
            showPermissionDeniedDialog();
        }
    }

    /**
     * 显示权限被拒对话框（解释+重试）。
     * 用户可以选择取消或重新尝试请求权限。
     */
    private void showPermissionDeniedDialog() {
        MessageDialogParams params = new MessageDialogParams(
                getString(R.string.permission_denied_title),
                getString(R.string.permission_denied_message_scan),
                1,
                MessageDialogParams.TYPE_WARNING,
                getString(R.string.cancel),
                getString(R.string.retry),
                new MessageDialogFragmentDataCallback() {
                    @Override
                    public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        XLog.d(TAG, "【UI交互】用户取消权限请求");
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        XLog.d(TAG, "【UI交互】用户选择重试权限请求");
                        checkAndRequestScanPermission();
                    }
                }
        );
        showMessageDialog(params, DIALOG_TAG_PERM_DENIED);
    }

    /**
     * 显示特殊权限引导对话框。
     * 引导用户前往系统设置页面开启 MANAGE_EXTERNAL_STORAGE 权限。
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
                        XLog.d(TAG, "【权限处理】用户取消特殊权限授权");
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        XLog.d(TAG, "【权限处理】用户选择前往设置");
                        openSpecialPermissionSetting();
                    }
                }
        );
        showMessageDialog(params, DIALOG_TAG_MANAGE_STORAGE);
    }

    /**
     * 打开特殊权限设置页面。
     * 根据 Android 版本决定跳转到 MANAGE_APP_ALL_FILES_ACCESS_PERMISSION 或 APPLICATION_DETAILS_SETTINGS 页面。
     */
    private void openSpecialPermissionSetting() {
        try {
            XLog.d(TAG, "【权限处理】打开特殊权限设置页面");
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            }
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, PermissionConstants.REQUEST_CODE_MANAGE_STORAGE);
        } catch (Exception e) {
            XLog.e(TAG, "【权限处理】打开设置页失败: " + e.getMessage());
        }
    }

    /**
     * 处理从设置页面返回的结果。
     * 当用户从特殊权限设置返回后，重新检查权限状态。
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回数据
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionConstants.REQUEST_CODE_MANAGE_STORAGE) {
            XLog.d(TAG, "【权限处理】用户从特殊权限设置返回");
            // 重新检查权限
            new Handler().postDelayed(this::checkAndRequestScanPermission, 300);
        }
    }

    /**
     * 接收权限相关事件。
     * 当收到 PermissionRequiredEvent 事件时，判断是否需要触发特殊权限引导。
     *
     * @param event 权限事件对象
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionEvent(PermissionRequiredEvent event) {
        XLog.d(TAG, "【事件处理】接收权限事件: " + PermissionRequiredEvent.getOperationTypeName(event.getOperationType()));
        if (event.getOperationType() == PermissionRequiredEvent.OPERATION_SPECIAL_PERMISSION) {
            XLog.d(TAG, "【权限处理】触发特殊权限引导");
            showSpecialPermissionDialog();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupEventLoaded(GroupEvent.LoadedEvent event) {
        XLog.d(TAG, "【事件】收到分组加载完成事件 | 分组数量: " + event.photoGroups.size());
        if (groupAdapter != null) {
            viewModel.loadGroups(); // 确保加载数据
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoGroupUpdateEvent(GroupEvent.UpdateEvent event) {
        XLog.e(TAG, "【事件】收到分组更新事件 | 分组: " + event.updatedGroup.getGroupKey());
        if (groupAdapter != null) {
            for (int i = 0; i < groupAdapter.getCurrentList().size(); i++) {
                if (groupAdapter.getItem(i).getGroupKey().equals(event.updatedGroup.getGroupKey())){
                    groupAdapter.setGroup(i, event.updatedGroup);
                    XLog.e(TAG, "【UI交互】更新UI | 分组: " + event.updatedGroup.getGroupKey());
                    break;
                }
            }
        }
    }
}
