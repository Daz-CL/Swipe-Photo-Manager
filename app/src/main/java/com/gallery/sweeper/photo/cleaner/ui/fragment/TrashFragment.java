package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daz.lib_base.base.VBBaseFragment;
import com.daz.lib_base.dialog.MessageDialogFragmentDataCallback;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.events.TrashEvents;
import com.gallery.sweeper.photo.cleaner.databinding.FragmentTrashBinding;
import com.gallery.sweeper.photo.cleaner.permission.PermissionRequiredEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/8/11 22:24
 * 描述：
 */
public class TrashFragment extends VBBaseFragment<FragmentTrashBinding, TrashViewModel> {

    private TrashAdapter trashAdapter;

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
        initAdapter();
        initButtonListeners();
        loadData();
    }

    private void initAdapter() {
        trashAdapter = new TrashAdapter(requireContext(), new ArrayList<>());
        binding.viewMain.setAdapter(trashAdapter);
        binding.viewMain.setOnItemClickListener((parent, view, position, id) ->
                viewModel.toggleSelect(position)
        );
    }

    private void initButtonListeners() {
        binding.btnSelect.setOnClickListener(v -> viewModel.toggleSelectAll());
        binding.btnRestore.setOnClickListener(v -> showRestoreDialog());
        binding.btnDelete.setOnClickListener(v -> showDeleteDialog());
    }

    private void loadData() {
        viewModel.loadPhotos();
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();

        viewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            trashAdapter.updateData(photos);
            EventBus.getDefault().post(new TrashEvents.TrashChangeEvent(photos.size()));
            updateUIState(photos);
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showErrorMessage(error);
            }
        });
    }

    private void updateUIState(List<Photo> photos) {
        int selectedCount = viewModel.getSelectedCount();
        boolean allSelected = viewModel.areAllPhotosSelected();
        boolean hasPhotos = !photos.isEmpty();

        binding.viewEmpty.setVisibility(hasPhotos ? View.GONE : View.VISIBLE);
        binding.viewMain.setVisibility(hasPhotos ? View.VISIBLE : View.GONE);

        if (allSelected && selectedCount > 0) {
            binding.btnSelect.setText("Deselect All");
            binding.btnSelect.setTextColor(Color.parseColor("#888888"));
        } else {
            binding.btnSelect.setText("Select All");
            binding.btnSelect.setTextColor(selectedCount > 0 ?
                    Color.parseColor("#111111") : Color.parseColor("#888888"));
        }

        //binding.btnRestore.setEnabled(selectedCount > 0);
        binding.btnRestore.setBackgroundResource(selectedCount > 0 ?
                R.drawable.shape_bg_undo_enable : 0);
        binding.btnRestore.setImageResource(selectedCount > 0 ?
                R.mipmap.revoke_02_g : R.mipmap.revoke_03_g);

        //binding.btnDelete.setEnabled(selectedCount > 0);
        binding.btnDelete.setBackgroundResource(selectedCount > 0 ?
                R.drawable.shape_bg_delete_enable : 0);
    }

    private void showRestoreDialog() {
        int count = viewModel.getSelectedCount();
        if (count == 0) {
            showMessageDialog(new MessageDialogParams(
                    "Please select photos to restore.",
                    "",
                    1,
                    MessageDialogParams.TYPE_ORANGE,
                    "",
                    "OK",
                    new MessageDialogFragmentDataCallback() {
                        @Override
                        public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        }

                        @Override
                        public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        }
                    }
            ));
            return;
        }

        showMessageDialog(new MessageDialogParams(
                "Restore " + count + " photos from Trash?",
                "",
                1,
                MessageDialogParams.TYPE_ORANGE,
                "Cancel",
                "Restore",
                new MessageDialogFragmentDataCallback() {
                    @Override
                    public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        dialog.dismiss();
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        viewModel.restoreSelectedPhotos();
                    }
                }
        ));
    }

    private void showDeleteDialog() {
        int count = viewModel.getSelectedCount();
        if (count == 0) {
            showMessageDialog(new MessageDialogParams(
                    "Please select photos to delete.",
                    "",
                    1,
                    MessageDialogParams.TYPE_ORANGE,
                    "",
                    "OK",
                    new MessageDialogFragmentDataCallback() {
                        @Override
                        public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        }

                        @Override
                        public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        }
                    }
            ));
            return;
        }

        showMessageDialog(new MessageDialogParams(
                "Delete " + count + " photos from Trash?",
                "",
                1,
                MessageDialogParams.TYPE_ORANGE,
                "Cancel",
                "Delete",
                new MessageDialogFragmentDataCallback() {
                    @Override
                    public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        dialog.dismiss();
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        viewModel.deleteSelectedPhotos();
                    }
                }
        ));
    }

    private void showErrorMessage(String error) {
        showMessageDialog(new MessageDialogParams(
                "",
                error,
                1,
                MessageDialogParams.TYPE_ERROR,
                "OK",
                "",
                new MessageDialogFragmentDataCallback() {
                    @Override
                    public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                        dialog.dismiss();
                    }

                    @Override
                    public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                        dialog.dismiss();
                    }
                }
        ));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionEvent(PermissionRequiredEvent event) {
        if (event.getOperationType() == PermissionRequiredEvent.OPERATION_DELETE_GRANTED) {
            XLog.i(TAG, "【权限】删除权限已获得，重新尝试删除");
            if (viewModel.getSelectedCount() > 0) {
                viewModel.deleteSelectedPhotos();
            }
        }
    }
}