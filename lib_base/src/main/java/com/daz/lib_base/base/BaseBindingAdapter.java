package com.daz.lib_base.base;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/13 23:39
 * 描述：
 */
// 基类适配器
public abstract class BaseBindingAdapter<D, VB extends ViewBinding>
        extends RecyclerView.Adapter<BaseBindingAdapter.BaseBindingViewHolder<VB>> {

    protected final List<D> mData = new ArrayList<>();
    protected OnItemClickListener<D> mItemClickListener;
    protected OnItemLongClickListener<D> mItemLongClickListener;

    // 创建ViewBinding的抽象方法
    protected abstract VB onCreateViewBinding(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);

    // 数据绑定抽象方法
    protected abstract void onBindViewHolder(@NonNull VB binding, int position, @Nullable D item);

    @NonNull
    @Override
    public BaseBindingViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        VB binding = onCreateViewBinding(inflater, parent);
        return new BaseBindingViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder<VB> holder, int position) {
        final D item = getItem(position);
        onBindViewHolder(holder.binding, position, item);

        // 点击事件处理
        holder.itemView.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(item, position);
            }
        });

        // 长按事件处理
        holder.itemView.setOnLongClickListener(v -> {
            if (mItemLongClickListener != null) {
                return mItemLongClickListener.onItemLongClick(item, position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public D getItem(int position) {
        if (position >= 0 && position < mData.size()) {
            return mData.get(position);
        }
        return null;
    }

    // 设置数据（带差异更新）
    public void setData(List<D> newData) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mData.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                D oldItem = mData.get(oldItemPosition);
                D newItem = newData.get(newItemPosition);
                return Objects.equals(oldItem, newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                D oldItem = mData.get(oldItemPosition);
                D newItem = newData.get(newItemPosition);
                return oldItem.equals(newItem);
            }
        });

        mData.clear();
        mData.addAll(newData);
        result.dispatchUpdatesTo(this);
    }

    // ViewHolder基类
    static class BaseBindingViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        final VB binding;

        BaseBindingViewHolder(VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // 点击监听接口
    public interface OnItemClickListener<D> {
        void onItemClick(D item, int position);
    }

    public interface OnItemLongClickListener<D> {
        boolean onItemLongClick(D item, int position);
    }

    // 设置监听器方法
    public void setOnItemClickListener(OnItemClickListener<D> listener) {
        this.mItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<D> listener) {
        this.mItemLongClickListener = listener;
    }
}
