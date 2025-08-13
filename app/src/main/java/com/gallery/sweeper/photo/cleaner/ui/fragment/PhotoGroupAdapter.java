package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.daz.lib_base.base.BaseRecyclerViewAdapter;
import com.daz.lib_base.view.bitmap.GlideProxy;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;
import com.gallery.sweeper.photo.cleaner.widget.RoundImageView2;

import java.util.List;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/23 18:10
 * 描述：
 */
public class PhotoGroupAdapter extends BaseRecyclerViewAdapter<PhotoGroupAdapter.ViewHolder> {
    private List<PhotoGroup> groupList;

    public PhotoGroupAdapter(List<PhotoGroup> groupList) {
        this.groupList = groupList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        PhotoGroup group = groupList.get(position);
        // 加载分组封面图片
        GlideProxy.circleCorners(holder.ivCover.getContext(), group.getGroupCover(), holder.ivCover, 25);
        // 设置最新照片时间
        holder.tvMonth.setText(String.format("%s.", group.getMonthGroup()));
        // 设置分组名称
        holder.tvYear.setText(group.yearGroup);

        switch (PhotoRepository.getInstance().getCurrentGroupType()) {
            case YEAR:
                // 设置最新照片时间
                holder.tvMonth.setText(group.yearGroup);
                // 设置分组名称
                holder.tvYear.setText("");
                break;
            case MONTH:
                // 设置最新照片时间
                holder.tvMonth.setText(String.format("%s.", group.getMonthGroup()));
                // 设置分组名称
                holder.tvYear.setText(group.yearGroup);
                break;
        }
        // 设置照片数量
        holder.tvPhotoCount.setText((group.getKeepCount()+group.getTrashCount())+"/"+group.getPhotoCount());

        holder.itemView.setAlpha((group.getKeepCount()+group.getTrashCount())==group.getPhotoCount()?0.5f:1.0f);
    }

    @Override
    public int getItemCount() {
        return groupList == null ? 0 : groupList.size();
    }

    // 更新数据
    public void submitList(List<PhotoGroup> newList) {
        groupList = newList;
        notifyDataSetChanged();
    }

    // 获取指定位置的分组
    public PhotoGroup getItem(int position) {
        return groupList.get(position);
    }

    public List<PhotoGroup> getCurrentList() {
        return groupList;
    }

    public void setGroup(int position, PhotoGroup updatedGroup) {
        groupList.set(position, updatedGroup);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RoundImageView2 ivCover;
        TextView tvYear;
        TextView tvPhotoCount;
        TextView tvMonth;

        public ViewHolder(View itemView, final PhotoGroupAdapter adapter) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvMonth = itemView.findViewById(R.id.tv_month);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvPhotoCount = itemView.findViewById(R.id.tv_photo_count);

            itemView.setOnClickListener(v -> adapter.onItemHolderClick(ViewHolder.this));
        }
    }
}