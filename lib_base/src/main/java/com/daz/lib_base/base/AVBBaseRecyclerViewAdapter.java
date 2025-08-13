package com.daz.lib_base.base;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/6/13 16:54
 * 描述：
 */

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public abstract class AVBBaseRecyclerViewAdapter<T, VH extends RecyclerView.ViewHolder>
        extends ListAdapter<T, VH> {

    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;

    protected AVBBaseRecyclerViewAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        try {
            T item = getItem(position);
            bindItem(holder, item, position);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Bind error："+ e);
        }
    }

    protected abstract void bindItem(@NonNull VH holder, T item, int position);

    // 设置点击监听
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    // 在ViewHolder中设置点击事件
    protected void setupClick(VH holder) {
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(v, position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    return itemLongClickListener.onItemLongClick(v, position);
                }
            }
            return false;
        });
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }
}
