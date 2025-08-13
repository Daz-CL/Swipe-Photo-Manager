package com.daz.lib_base.base;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/3/13 23:51
 * 描述：
 */

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.daz.lib_base.databinding.LayoutFootBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用RecyclerView基类适配器（View Binding版）
 *
 * @param <T>  数据类型
 * @param <VB> ViewBinding类型
 */
public abstract class BaseBindingRecyclerAdapter<T, VB extends ViewBinding> extends RecyclerView.Adapter<BaseBindingRecyclerAdapter.BindingViewHolder<VB>> {

    // 数据源
    protected final List<T> mData = new ArrayList<>();

    // 事件监听
    private OnItemClickListener<T> mItemClickListener;
    private OnItemLongClickListener<T> mItemLongClickListener;
    private OnLoadMoreListener mLoadMoreListener;

    // 布局类型相关
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FOOTER = 1;
    private boolean mShowFooter = false;

    @NonNull
    @Override
    public BindingViewHolder<VB> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            // 加载更多布局（示例）
            LayoutFootBinding binding = LayoutFootBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return (BindingViewHolder<VB>) new FootViewHolder(binding);
        }

        VB binding = onCreateViewBinding(LayoutInflater.from(parent.getContext()), parent);
        return new BindingViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BindingViewHolder<VB> holder, int position) {
        if (holder instanceof FootViewHolder) {
            ((FootViewHolder) holder).bind(mShowFooter);
            return;
        }

        final T item = getItem(position);
        if (item != null) {
            onBindView(holder.binding, item, position);
            setupItemClickListener(holder, item, position);
        }

        // 触发加载更多
        if (position == getItemCount() - 1 && mLoadMoreListener != null) {
            mLoadMoreListener.onLoadMore();
        }
    }

    @Override
    public int getItemCount() {
        return mData.size() + (mShowFooter ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowFooter && position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_NORMAL;
    }

    /**
     * 创建ViewBinding（抽象方法）
     */
    protected abstract VB onCreateViewBinding(LayoutInflater inflater, ViewGroup parent);

    /**
     * 绑定数据（抽象方法）
     */
    protected abstract void onBindView(VB binding, T item, int position);

    /**
     * 设置点击事件监听
     */
    private void setupItemClickListener(BindingViewHolder<VB> holder, T item, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(item, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mItemLongClickListener != null) {
                return mItemLongClickListener.onItemLongClick(item, position);
            }
            return false;
        });
    }

    /**
     * 更新数据（带DiffUtil）
     */
    public void updateData(List<T> newData) {
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
            public boolean areItemsTheSame(int oldPos, int newPos) {
                T oldItem = mData.get(oldPos);
                T newItem = newData.get(newPos);
                return BaseBindingRecyclerAdapter.this.areItemsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                T oldItem = mData.get(oldPos);
                T newItem = newData.get(newPos);
                return BaseBindingRecyclerAdapter.this.areContentsTheSame(oldItem, newItem);
            }
        });

        mData.clear();
        mData.addAll(newData);
        result.dispatchUpdatesTo(this);
    }

    /**
     * 判断是否为同一个Item（可重写）
     */
    protected boolean areItemsTheSame(T oldItem, T newItem) {
        return oldItem.equals(newItem);
    }

    /**
     * 判断内容是否相同（可重写）
     */
    protected boolean areContentsTheSame(T oldItem, T newItem) {
        return oldItem.equals(newItem);
    }

    /**
     * 获取指定位置数据
     */
    public T getItem(int position) {
        if (position >= 0 && position < mData.size()) {
            return mData.get(position);
        }
        return null;
    }

    /**
     * 显示/隐藏加载更多
     */
    public void showFooter(boolean show) {
        if (mShowFooter != show) {
            mShowFooter = show;
            notifyItemChanged(getItemCount());
        }
    }

    // region 事件监听接口
    public interface OnItemClickListener<T> {
        void onItemClick(T item, int position);
    }

    public interface OnItemLongClickListener<T> {
        boolean onItemLongClick(T item, int position);
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        mItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        mItemLongClickListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mLoadMoreListener = listener;
    }
    // endregion

    // region ViewHolder定义
    static class BindingViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
        final VB binding;

        BindingViewHolder(VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * 加载更多FooterViewHolder（示例）
     */
    static class FootViewHolder extends BindingViewHolder<LayoutFootBinding> {
        FootViewHolder(LayoutFootBinding binding) {
            super(binding);
        }

        void bind(boolean show) {
            //binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    // endregion
}
