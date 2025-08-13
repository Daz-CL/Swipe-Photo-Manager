package com.daz.lib_base.base;

import android.widget.AdapterView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2024/11/1 17:17
 * 描述：
 */
public abstract class BaseRecyclerViewAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    protected AdapterView.OnItemClickListener onItemClickListener;
    protected AdapterView.OnItemLongClickListener onItemLongClickListener;

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void onItemHolderClick(RecyclerView.ViewHolder itemHolder) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(
                    null,
                    itemHolder.itemView,
                    itemHolder.getAdapterPosition(),
                    itemHolder.getItemId());
        } else {
           // Logger.i("onItemHolderClick error: Please call setOnItemClickListener method set the click event listeners.");
            throw new IllegalArgumentException("Please call setOnItemClickListener method set the click event listeners.");
        }
    }


    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemClickListener) {
        this.onItemLongClickListener = onItemClickListener;
    }

    public void onItemLongHolderClick(RecyclerView.ViewHolder itemHolder) {
        if (onItemLongClickListener != null) {
            onItemLongClickListener.onItemLongClick(
                    null,
                    itemHolder.itemView,
                    itemHolder.getAdapterPosition(),
                    itemHolder.getItemId());
        } else {
            //XLogger.i("onItemHolderClick error: Please call setOnItemClickListener method set the click event listeners.");
            throw new IllegalArgumentException("Please call setOnItemClickListener method set the click event listeners.");
        }
    }


}
