package com.leo.update_library.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import android.util.Log;


import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.leo.update_library.BuildConfig;
import com.leo.update_library.R;

import java.io.File;

/**
 * Function:app下载后台服务
 * Author:Levy
 * Create Time:2019/6/18 21:21:35
 **/
public class UpdateService extends Service {
    private String apkUrl;
    private String filePath;
    private NotificationManager mNotificationManager;
    private Notification mNotification;

    public static final String KEY_APK_URL = "apk_url";
    public static final String KEY_NOTIFICATION_ICON_ID = "notification_icon";
    private int mNotificationIconId;
    private int notificationId = 0xf;


    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        filePath = Environment.getExternalStorageDirectory() + "/update.apk";

    }

    private void log(String tag, String log) {
        Log.e(tag, log);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {

            notifyUser("下载失败", 0);
            stopSelf();
        }
        apkUrl = intent.getStringExtra(KEY_APK_URL);
        mNotificationIconId = intent.getIntExtra(KEY_NOTIFICATION_ICON_ID, R.drawable.ic_launcher);
        startDownLoad();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startDownLoad() {
        UpdateManager.getInstance().startDownLoad(apkUrl, filePath, new UpdateDownLoadListener() {
            @Override
            public void onStart() {
                notifyUser("下载开始", 0);
            }

            @Override
            public void onProgressChange(float progress) {
                notifyUser("更新进度", progress);
            }

            @Override
            public void onFinish(float completeSize) {
                notifyUser("下载结束", 100);
                stopSelf();
                installApk();
                if (mNotificationManager != null) {
                    mNotificationManager.cancel(notificationId);
                }
            }

            @Override
            public void onFailure() {
                notifyUser("下载失败", 0);
                stopSelf();
            }
        });
    }

    private void notifyUser(String reason, float progress) {
        String channelId = null;
        String channelName = null;
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            channelId = "channel_id";
            channelName = "channel_name";
            NotificationChannel channel =
                    new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.canBypassDnd();
            channel.canShowBadge();
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            mNotificationManager.createNotificationChannel(channel);
        }


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(mNotificationIconId)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        mNotificationIconId));

        builder.setAutoCancel(true);

        builder.setContentTitle("版本更新")
//                .setContentText("content text ")
                .setWhen(System.currentTimeMillis())
                .setTicker("正在下载");

        log("tag","    progress " +progress);
        if (progress >= 0 && progress < 100) {
            builder.setProgress(100, (int) progress, false)
            .setContentText((int)progress + "%");
        } else {
            builder.setProgress(0, 0, false)
                    .setContentText(null);
            ;
        }

        builder.setContentIntent(progress >= 100 ? getContentIntent() :
                PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT));
        mNotification = builder.build();
        mNotificationManager.notify(notificationId, mNotification);

    }

    private PendingIntent getContentIntent() {
        File file = new File(filePath);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri realFilePath = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            realFilePath = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".fileProvider", file);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            realFilePath = Uri.fromFile(file);
        }

        intent.setDataAndType(realFilePath,
                "application/vnd.android.package-archive");
        PendingIntent pendingIntent = PendingIntent.
                getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }

    private void installApk() {
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri realFilePath = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            realFilePath = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".fileProvider", file);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            realFilePath = Uri.fromFile(file);
        }

        intent.setDataAndType(realFilePath,
                "application/vnd.android.package-archive");

        startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("tag", "bind");
        return null;
    }
}
