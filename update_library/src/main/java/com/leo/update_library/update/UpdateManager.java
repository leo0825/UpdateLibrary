package com.leo.update_library.update;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Function:下载调度管理器，调用request
 * Author:Levy
 * Create Time:2019/6/18 21:21:34
 **/
public class UpdateManager {
private ThreadPoolExecutor mThreadPoolExecutor = null;
private UpdateDownLoadRequest mDownLoadRequest = null;

    private UpdateManager(){
        mThreadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    }
    static class Holder{
        private static UpdateManager mUpdateManager = new UpdateManager();
    }

    public static UpdateManager getInstance(){
        return Holder.mUpdateManager;
    }

    public void startDownLoad(String downloadUrl, String localFilePath,
                              UpdateDownLoadListener listener){

        if (mDownLoadRequest != null){
            return;
        }

        checkLocalFilePath(localFilePath);
        mDownLoadRequest = new UpdateDownLoadRequest(downloadUrl,localFilePath,listener);
        Future<?> future = mThreadPoolExecutor.submit(mDownLoadRequest);
    }

    /**检查文件路径
     * @param localFilePath
     */
    private void checkLocalFilePath(String localFilePath) {
        File dir = new File(localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1));
        if (!dir.exists()){
            dir.mkdir();
        }

        File file = new File(localFilePath);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
