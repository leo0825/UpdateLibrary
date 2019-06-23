package com.leo.updatedemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.leo.update_library.update.UpdateService;
import com.tbruyelle.rxpermissions2.RxPermissions;

import static com.leo.update_library.update.UpdateService.KEY_NOTIFICATION_ICON_ID;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RxPermissions rxPermissions = new RxPermissions(this);

        findViewById(R.id.btnDownLoad).setOnClickListener(v-> {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted) { // Always true pre-M
                            // I can control the camera now
                            downLoad();
                        } else {
                            // Oups permission denied
                        }
                    });
                    });



    }


    public void downLoad(){
        Log.e("tag", "downLoad: ");
        Intent intent = new Intent(this, UpdateService.class);
        intent.putExtra(KEY_NOTIFICATION_ICON_ID,R.drawable.icon);
        intent.putExtra("apk_url","http://47.107.246.94/android/new/install.apk");
        intent.putExtra(KEY_NOTIFICATION_ICON_ID,R.drawable.icon);
        startService(intent);
    }
}
