package com.gallery.sweeper.photo.cleaner.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.daz.lib_base.base.AVBSimpleActivity;
import com.daz.lib_base.dialog.MessageDialogFragmentDataCallback;
import com.daz.lib_base.dialog.MessageDialogParams;
import com.daz.lib_base.utils.XLog;
import com.daz.lib_base.view.bitmap.GlideProxy;
import com.daz.tantan.CardConfig;
import com.daz.tantan.CardItemTouchHelperCallback;
import com.daz.tantan.CardLayoutManager;
import com.daz.tantan.OnSwipeListener;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;
import com.gallery.sweeper.photo.cleaner.data.events.SwipePhotoChangeEvents;
import com.gallery.sweeper.photo.cleaner.data.events.TrashEvents;
import com.gallery.sweeper.photo.cleaner.databinding.ActivitySwipeTrashBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SwipeTrashActivity extends AVBSimpleActivity<ActivitySwipeTrashBinding, SwipeTrashViewModel> {
    private PhotoAdapter photoAdapter;
    private ItemTouchHelper touchHelper;
    private String groupKey;
    private String groupType;
    private boolean isFirst = true;

    @Override
    protected ActivitySwipeTrashBinding initViewBinding() {
        return ActivitySwipeTrashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected Class<SwipeTrashViewModel> getViewModelClass() {
        return SwipeTrashViewModel.class;
    }

    @Override
    protected void initEventAndData() {
        XLog.d(TAG, "【初始化】活动初始化开始");
        groupKey = getIntent().getStringExtra("group_key");
        groupType = getIntent().getStringExtra("group_type");
        XLog.i(TAG, "【数据】接收分组参数: key=" + groupKey + ", type=" + groupType);

        initViews();
        initWithCachedData();
        XLog.d(TAG, "【初始化】初始化完成");
    }

    private void initViews() {
        XLog.d(TAG, "【UI】初始化UI组件");
        binding.back.setOnClickListener(v -> finish());

        binding.btnUndo.setEnabled(false);
        binding.btnUndo.setImageResource(R.mipmap.revoke_0);

        binding.btnUndo.setOnClickListener(v -> {
            XLog.d(TAG, "【操作】撤销按钮点击");
            if (viewModel.undoLastAction()) {
                XLog.i(TAG, "【操作】撤销操作成功");
            } else {
                XLog.w(TAG, "【操作】没有可撤销的操作");
            }
        });

        binding.btnTrash.setOnClickListener(v -> {
            showMessageDialog(new MessageDialogParams(
                    "Review Trash?",
                    "Clear storage by emptying the trash",
                    1,
                    MessageDialogParams.TYPE_NORMAL,
                    "Later",
                    "Review Trash",
                    new MessageDialogFragmentDataCallback() {
                        @Override
                        public void messageDialogClickLeftButtonListener(Dialog dialog, int messageType, String buttonText) {
                            dialog.dismiss();
                        }

                        @Override
                        public void messageDialogClickRightButtonListener(Dialog dialog, int messageType, String buttonText) {
                            EventBus.getDefault().post(new TrashEvents.selectTrashByGroup(groupKey, groupType));
                            // 延迟100ms再finish()，确保MainActivity处理事件
                            binding.getRoot().postDelayed(() -> finish(), 100);
                        }
                    }
            ));
        });

        photoAdapter = new PhotoAdapter();
        binding.viewMain.setItemAnimator(new DefaultItemAnimator());
        binding.viewMain.setAdapter(photoAdapter);
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();

        if (isFinishing()) return;

        viewModel.getTitleStatus().observe(this, title -> {
            withBinding(binding -> {
                XLog.d(TAG, "【UI更新】标题更新: " + title);
                binding.tvTitle.setText(title);
            });
        });

        viewModel.getGroupChange().observe(this, group -> {
            withBinding(binding -> {
                if (group == null) {
                    XLog.e(TAG, "【错误】分组数据为空");
                    return;
                }
                XLog.d(TAG, "【数据】分组数据变更: " + group.getGroupKey());

                binding.tvCountTrash.setText(String.valueOf(group.getTrashCount()));

                int makeCount = group.getTrashCount() + group.getKeepCount();
                binding.tvTotal.setText(makeCount + "/" + group.getPhotoCount());

                double percentage = 0.0;
                if (group.getPhotoCount() > 0) {
                    percentage = (double) makeCount / group.getPhotoCount() * 100;
                    percentage = Math.max(0.0, Math.min(100.0, percentage));
                } else {
                    XLog.w(TAG, "【警告】无效的分组数据: photoCount=" + group.getPhotoCount());
                }

                String formattedPercentage = String.format("%.1f", percentage);
                binding.tvProgress.setText("Swipe：" + formattedPercentage + "%");
                binding.progressBar.setProgress((int) Math.round(percentage));

                XLog.d(TAG, "【进度】进度更新: " + makeCount + "/" + group.getPhotoCount() + " = " + formattedPercentage + "%");
            });
        });

        viewModel.getUndoAvailable().observe(this, available -> {
            XLog.d(TAG, "【状态】撤销按钮状态变更: " + available);
            withBinding(binding -> {
                binding.btnUndo.setEnabled(available);
                binding.btnUndo.setImageResource(available ? R.mipmap.revoke_02_g : R.mipmap.revoke_0);
            });
        });
    }

    private void initSwipeCardSystem(List<Photo> photos) {
        XLog.d(TAG, "【卡片】初始化滑动卡片系统");
        if (photos == null) photos = new ArrayList<>();

        resetCardSystem();

        CardItemTouchHelperCallback cardCallback = new CardItemTouchHelperCallback(photoAdapter, photos);
        cardCallback.setOnSwipedListener(new CardSwipeListener());

        touchHelper = new ItemTouchHelper(cardCallback);
        CardLayoutManager cardLayoutManager = new CardLayoutManager(binding.viewMain, touchHelper);
        binding.viewMain.setLayoutManager(cardLayoutManager);
        touchHelper.attachToRecyclerView(binding.viewMain);

        photoAdapter.submitList(photos);
        XLog.i(TAG, "【卡片】卡片系统初始化完成，照片数量: " + photos.size());
    }

    private void resetCardSystem() {
        XLog.d(TAG, "【卡片】重置卡片系统");
        if (touchHelper != null) {
            touchHelper.attachToRecyclerView(null);
            touchHelper = null;
        }
        binding.viewMain.setLayoutManager(null);
        binding.viewMain.setAdapter(null);
        photoAdapter = new PhotoAdapter();
        binding.viewMain.setAdapter(photoAdapter);
    }

    private class CardSwipeListener implements OnSwipeListener<Photo> {
        @Override
        public void onSwiping(RecyclerView.ViewHolder viewHolder, float ratio, int direction) {
            PhotoViewHolder holder = (PhotoViewHolder) viewHolder;
            holder.itemView.setAlpha(1 - Math.abs(ratio) * 0.2f);
            if (direction == CardConfig.SWIPING_LEFT) {
                holder.dislikeImageView.setAlpha(Math.abs(ratio));
                holder.likeImageView.setAlpha(0f);
            } else if (direction == CardConfig.SWIPING_RIGHT) {
                holder.likeImageView.setAlpha(Math.abs(ratio));
                holder.dislikeImageView.setAlpha(0f);
            } else {
                holder.dislikeImageView.setAlpha(0f);
                holder.likeImageView.setAlpha(0f);
            }
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, Photo photo, int direction) {
            XLog.d(TAG, "【操作】卡片滑动 - 方向: " +
                    (direction == 1 ? "回收" : direction == 4 ? "保留" : "未知") + " | ID: " + photo.mediaStoreId);
            isFirst = false;
            PhotoViewHolder holder = (PhotoViewHolder) viewHolder;
            holder.itemView.setAlpha(1f);
            holder.dislikeImageView.setAlpha(0f);
            holder.likeImageView.setAlpha(0f);

            updateProgressUI();

            if (direction == CardConfig.SWIPED_LEFT) {
                viewModel.trashPhoto(photo);
            } else if (direction == CardConfig.SWIPED_RIGHT) {
                viewModel.keepPhoto(photo);
            }
        }

        @Override
        public void onSwipedClear() {
            XLog.i(TAG, "【状态】所有照片已处理完毕");
        }
    }

    private void updateProgressUI() {
        withBinding(binding -> {
            if (isFirst && viewModel.getCurrentGroup() != null && (viewModel.getCurrentGroup().getTrashCount() + viewModel.getCurrentGroup().getKeepCount()) == 0) {
                binding.viewTips.setVisibility(View.VISIBLE);
                binding.viewBottom.setVisibility(View.INVISIBLE);
                binding.tvCountTrash.setVisibility(View.INVISIBLE);
                binding.btnUndo.setVisibility(View.INVISIBLE);
            } else {
                binding.viewTips.setVisibility(View.INVISIBLE);
                binding.viewBottom.setVisibility(View.VISIBLE);
                binding.tvCountTrash.setVisibility(View.VISIBLE);
                binding.btnUndo.setVisibility(View.VISIBLE);

                PhotoGroup currentGroup = viewModel.getCurrentGroup();
                if (currentGroup != null) {
                    binding.tvCountTrash.setText(String.valueOf(currentGroup.getTrashCount()));
                } else {
                    binding.tvCountTrash.setText("0");
                    XLog.w(TAG, "【错误】当前分组数据为空");
                }
            }
        });
    }

    private static class PhotoAdapter extends ListAdapter<Photo, PhotoViewHolder> {
        private static final DiffUtil.ItemCallback<Photo> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<Photo>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                        return oldItem.mediaStoreId == newItem.mediaStoreId;
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                        return oldItem.equals(newItem);
                    }
                };

        PhotoAdapter() {
            super(DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_swipe_card, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }

    private static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final ImageView likeImageView;
        private final ImageView dislikeImageView;

        PhotoViewHolder(View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_avatar);
            likeImageView = itemView.findViewById(R.id.iv_like);
            dislikeImageView = itemView.findViewById(R.id.iv_dislike);
        }

        void bind(Photo photo) {
            // 空值和路径验证
            String TAG = "PhotoViewHolder";
            if (photo == null || photo.path == null || photo.path.isEmpty()) {
                XLog.w(TAG, "【警告】无效照片对象或路径为空");
                avatarImageView.setImageResource(R.drawable.ic_photo_placeholder);
                return;
            }

            try {
                GlideProxy.normal(itemView.getContext(), photo.path, avatarImageView);
            } catch (Exception e) {
                XLog.e(TAG, "【错误】图片加载失败 | 路径: " + photo.path + " | 错误: " + e.getMessage());
                avatarImageView.setImageResource(R.drawable.ic_photo_placeholder);
            }
            likeImageView.setAlpha(0f);
            dislikeImageView.setAlpha(0f);
        }
    }

    private void initWithCachedData() {
        List<Photo> cachedPhotos = viewModel.getCachedPhotos();
        PhotoGroup cachedGroup = viewModel.getCachedGroup();
        if (cachedPhotos != null && !cachedPhotos.isEmpty() && cachedGroup != null && groupKey.equals(cachedGroup.getGroupKey()) && groupType.equals(cachedGroup.getGroupType())) {
            updateUIWithCachedGroup(cachedGroup);
            initSwipeCardSystem(cachedPhotos);
            XLog.d(TAG, "【缓存】使用缓存数据初始化UI");
        } else {
            viewModel.setGroupKey(groupType, groupKey);
            updateProgressUI();
            XLog.d(TAG, "【数据】加载新数据");
        }
    }

    private void updateUIWithCachedGroup(PhotoGroup group) {
        withBinding(binding -> {
            String title = "";
            switch (PhotoRepository.getInstance().getCurrentGroupType()) {
                case YEAR:
                    title = group.yearGroup;
                    break;
                case MONTH:
                    title = group.monthGroup + "." + group.yearGroup;
                    break;
            }
            binding.tvTitle.setText(title);

            binding.tvCountTrash.setText(String.valueOf(group.getTrashCount()));

            int makeCount = group.getTrashCount() + group.getKeepCount();
            binding.tvTotal.setText(makeCount + "/" + group.getPhotoCount());

            double percentage = 0.0;
            if (group.getPhotoCount() > 0) {
                percentage = (double) makeCount / group.getPhotoCount() * 100;
                percentage = Math.max(0.0, Math.min(100.0, percentage));
            }
            String formattedPercentage = String.format("%.1f", percentage);
            binding.tvProgress.setText("Swipe：" + formattedPercentage + "%");
            binding.progressBar.setProgress((int) Math.round(percentage));

            binding.btnUndo.setEnabled(viewModel.getUndoAvailable().getValue() != null &&
                    viewModel.getUndoAvailable().getValue());
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoChangeChanged(SwipePhotoChangeEvents event) {
        int size = (event.photos != null) ? event.photos.size() : 0;
        XLog.d(TAG, "【事件】照片数据变更, 数量: " + size);

        withBinding(binding -> {
            List<Photo> photosToShow = viewModel.getCachedPhotos();
            PhotoGroup groupToShow = viewModel.getCachedGroup();

            if (groupToShow != null) {
                updateUIWithCachedGroup(groupToShow);
            }

            // 确保照片列表不为空
            if (photosToShow == null) {
                photosToShow = Collections.emptyList();
                XLog.w(TAG, "【警告】照片列表为空，使用空列表初始化");
            }

            initSwipeCardSystem(photosToShow);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 2. 通知 ViewModel 停止后台任务
        if (viewModel != null) {
            viewModel.cancelPendingOperations();
        }
    }

    @Override
    public void finish() {
        XLog.i(TAG, "【生命周期】活动结束 - 分组类型: " + groupType + ", 键: " + groupKey);
        Intent intent = new Intent();
        intent.putExtra("groupType", groupType);
        intent.putExtra("groupKey", groupKey);
        setResult(Activity.RESULT_OK, intent);
        super.finish();
    }
}