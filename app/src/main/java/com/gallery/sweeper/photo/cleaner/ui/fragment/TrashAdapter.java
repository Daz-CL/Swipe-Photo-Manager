package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.daz.lib_base.view.bitmap.GlideProxy;
import com.gallery.sweeper.photo.cleaner.R;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.Photo;

import java.util.ArrayList;
import java.util.List;


/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/22 18:29
 * 描述：
 * 垃圾桶适配器
 * 优化点：
 * 1. 添加图片加载优化
 * 2. 添加恢复功能
 */
public class TrashAdapter extends BaseAdapter {
    private List<Photo> lists;
    private final LayoutInflater inflater;
    private final Context context;

    public TrashAdapter(Context context, List<Photo> data) {
        this.context = context;
        this.lists = data != null ? data : new ArrayList<>(); // 防御null数据
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<Photo> newData) {
        this.lists = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int position) {
        return position < lists.size() ? lists.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyDialogHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_trash, parent, false);
            holder = new MyDialogHolder();
            holder.imgView = convertView.findViewById(R.id.iv_trash);
            holder.select = convertView.findViewById(R.id.btn_select);
            convertView.setTag(holder);
        } else {
            holder = (MyDialogHolder) convertView.getTag();
        }

        // 防御性编程
        if (position < lists.size()) {
            Photo item = lists.get(position);


            // 优化图片加载 - 使用合适尺寸
            GlideProxy.normal(holder.imgView.getContext(), item.getPath(), holder.imgView);

            // 显示选中状态
            holder.select.setImageDrawable(ContextCompat.getDrawable(holder.select.getContext(),item.isSelected()? R.mipmap.okey_act_p: R.mipmap.okey_act_n));

            // 添加长按恢复功能
            /*convertView.setOnLongClickListener(v -> {
                showRestoreDialog(item);
                return true;
            });*/


        }

        return convertView;
    }

    // 显示恢复对话框
    private void showRestoreDialog(Photo photo) {
        new AlertDialog.Builder(context)
                .setTitle("恢复照片")
                .setMessage("确定要将这张照片恢复到相册吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 更新照片状态为正常
                    PhotoRepository.getInstance().updatePhotoStatus(
                            photo.mediaStoreId, Photo.Status.NORMAL);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void toggleSelect(int position) {
        Photo item = lists.get(position);
        item.setSelected(!item.isSelected());
        notifyDataSetChanged();
    }

    static class MyDialogHolder {
        ImageView imgView;
        ImageView select;
    }
}