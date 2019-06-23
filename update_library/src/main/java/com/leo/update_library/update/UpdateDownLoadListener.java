package com.leo.update_library.update;

/**
 * Function:
 * Author:Levy
 * Create Time:2019/6/18 21:21:29
 **/
public interface UpdateDownLoadListener {
    void onStart();

    void onProgressChange(float progress);

    void onFinish(float completeSize);

    void onFailure();
}
