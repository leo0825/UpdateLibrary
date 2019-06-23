package com.leo.update_library.update;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

/**
 * Function:文件下载
 * Author:Levy
 * Create Time:2019/6/18 21:21:33
 **/
public class UpdateDownLoadRequest implements Runnable {
    private String downLoadUrl;
    private String localFilePath;
    private UpdateDownLoadListener mDownLoadListener;
    private boolean isDownloading = false;
    private long fileLength;
    private DownLoadResponseHandler mHandler;


    public UpdateDownLoadRequest(String downLoadUrl, String localFilePath,
                                 UpdateDownLoadListener downLoadListener) {
        this.downLoadUrl = downLoadUrl;
        this.localFilePath = localFilePath;
        mDownLoadListener = downLoadListener;

        isDownloading = true;
        mHandler = new DownLoadResponseHandler();
    }

    //真正建立连接
    private void makeRequest() throws IOException, InterruptedException {

        if (!Thread.currentThread().isInterrupted()){
            URL url = new URL(downLoadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setRequestProperty("Connection","Keep-Alive");
            connection.connect();//阻塞当前线程
            fileLength = connection.getContentLength();
            if (!Thread.currentThread().isInterrupted()){

                //真正的完成文件下载
                mHandler.sendResponseMsg(connection.getInputStream());
            }

        }
    }

    @Override
    public void run() {
        try {
            makeRequest();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private String getTwoPointFloatStr(float value){
        DecimalFormat format = new DecimalFormat("0.00");
        return format.format(value);
    }

    public enum FailureCode{
        UnknowHost,Socket,ConnectTimeOut,IO,HttpResponse,JSON,Interrupted
    }

    /**
     * 用于真正的下载文件，发送消息和回调
     */
    public class DownLoadResponseHandler{
        protected static final int MSG_SUCCESS = 0;
        protected static final int MSG_FAILURE = 1;
        protected static final int MSG_START = 2;
        protected static final int MSG_FINISH = 3;
        protected static final int MSG_NETWORK_OFF = 4;
        protected static final int MSG_PROGRESS_CHANGE = 5;

        private float mCompleteSize = 0;
        private int progress = 0;
        private Handler mHandler;

        public DownLoadResponseHandler() {
           mHandler = new Handler(Looper.getMainLooper()){
               @Override
               public void handleMessage(Message msg) {
                  handleSelfMessage(msg);
               }
           };
        }

        protected void sendFinishMsg(){
         sendMsg(obtainMsg(MSG_FINISH,null));
        }

        protected void sendFailureMsg(FailureCode failureCode){
            sendMsg(obtainMsg(MSG_FAILURE,new Object[]{failureCode}));
        }

        protected void sendProgressChangeMsg(int progress){
            sendMsg(obtainMsg(MSG_PROGRESS_CHANGE,new Object[]{progress}));
        }

        private void handleSelfMessage(Message msg) {
            Object[] response;
            switch (msg.what){
                case MSG_FAILURE:
                    response = (Object[]) msg.obj;
                    handleFailureMsg((FailureCode) response[0]);
                    break;
                case MSG_FINISH:
                    handleFinishMsg();
                    break;
                case MSG_PROGRESS_CHANGE:

                    response = (Object[]) msg.obj;
                    handleProgressChangeProgress(((Integer) response[0]).intValue());
                    break;

            }
        }



        protected void handleProgressChangeProgress(int progress){
            mDownLoadListener.onProgressChange(progress);
        }

        protected void handleFailureMsg(FailureCode failureCode){
            mDownLoadListener.onFailure();
        }


        private void handleFinishMsg() {
            mDownLoadListener.onFinish(mCompleteSize);
        }

        //文件下载方法，会发送各种类型的事件
        void sendResponseMsg(InputStream is){
            RandomAccessFile randomAccessFile = null;
            mCompleteSize = 0;

            byte[] buffer = new byte[1024 * 10];
            int length = -1;
            int limit = 0;
            try {
                randomAccessFile = new RandomAccessFile(localFilePath, "rwd");
                while ((length = is.read(buffer)) != -1){
                    if (isDownloading)
                    {
                        randomAccessFile.write(buffer,0,length);
                        mCompleteSize += length;

                        //计算下载进度
                        if (mCompleteSize < fileLength){
                            progress = (int) Float.parseFloat(
                                    getTwoPointFloatStr((mCompleteSize / fileLength)*100));
                            //限制notifiecation刷新频率

                            if (limit % 20 == 0 && progress <= 100){
                                sendProgressChangeMsg(progress);

                            }
                            limit++;
                        }
                    }
                }
                sendFinishMsg();
            }  catch (IOException e) {
                e.printStackTrace();
                sendFailureMsg(FailureCode.IO);
            }
            finally {
                try {
                    if (is != null){
                        is.close();
                    }
                    if (randomAccessFile != null){
                        randomAccessFile.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendFailureMsg(FailureCode.IO);
                }
            }
        }


        protected void sendMsg(Message msg){
            if (mHandler != null){
                mHandler.sendMessage(msg);
            }
            else
            {
                handleSelfMessage(msg);
            }
        }

        protected Message obtainMsg(int responseMsg, Object response){
            Message msg = null;
            if (mHandler != null)
            {
                msg = mHandler.obtainMessage(responseMsg, response);
            }
            else {
                msg = Message.obtain();
                msg.what = responseMsg;
                msg.obj = response;
            }
            return msg;
        }
    }
}
