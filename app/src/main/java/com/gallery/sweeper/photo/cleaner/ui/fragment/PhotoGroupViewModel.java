package com.gallery.sweeper.photo.cleaner.ui.fragment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.daz.lib_base.base.AVBAndroidViewModel;
import com.daz.lib_base.utils.XLog;
import com.gallery.sweeper.photo.cleaner.data.GroupType;
import com.gallery.sweeper.photo.cleaner.data.PhotoRepository;
import com.gallery.sweeper.photo.cleaner.data.db.PhotoGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PhotoGroupViewModel extends AVBAndroidViewModel {
    private static final String TAG = "PhotoGroupViewModel";
    private final MutableLiveData<List<PhotoGroup>> photoGroups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");

    public PhotoGroupViewModel(@NonNull Application application) {
        super(application);
    }

    public void loadGroups() {
        if (Boolean.TRUE.equals(loadingState.getValue())) {
            XLog.w(TAG, "【数据加载】加载操作已在进行中，跳过");
            return;
        }

        loadingState.setValue(true);
        errorMessage.setValue("");
        XLog.d(TAG, "【数据加载】开始加载分组数据");

        LiveData<List<PhotoGroup>> groupsLiveData = PhotoRepository.getInstance().getGroups(PhotoRepository.getInstance().getCurrentGroupType());
        groupsLiveData.observeForever(new Observer<List<PhotoGroup>>() {
            @Override
            public void onChanged(List<PhotoGroup> groups) {
                groupsLiveData.removeObserver(this);

                if (groups == null) {
                    errorMessage.postValue("分组数据为空");
                    loadingState.postValue(false);
                    XLog.e(TAG, "【数据加载】获取分组数据失败");
                    return;
                }

                if (groups.isEmpty()) {
                    errorMessage.postValue("分组数据为空");
                    loadingState.postValue(false);
                    return;
                }

                XLog.d(TAG, "加载分组成功，数量：" + groups.size());
                photoGroups.postValue(groups);
                loadingState.postValue(false);
            }
        });
    }

    public void setGroupType(GroupType type) {
        if (PhotoRepository.getInstance().getCurrentGroupType() != type) {
            PhotoRepository.getInstance().setCurrentGroupType(type);
            loadGroups(); // 切换分组类型后重新加载数据
        }
    }

    public LiveData<List<PhotoGroup>> getPhotoGroups() {
        return photoGroups;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}