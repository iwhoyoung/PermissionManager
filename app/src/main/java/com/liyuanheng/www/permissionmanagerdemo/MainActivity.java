package com.liyuanheng.www.permissionmanagerdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.liyuanheng.www.permissionlibrary.PermissionManager;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionManager.OnPermitListener{

    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionManager.getInstance(this).oneKeyRequest("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"
                ,"android.permission.WRITE_CALENDAR","android.permission.ACCESS_FINE_LOCATION","android.permission.SYSTEM_ALERT_WINDOW","android.permission.WRITE_SETTINGS");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance(this).onPermissionResult(requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermissionManager.getInstance(this).onActivityResult(requestCode,resultCode,data);
    }

    @Override
    public void onPermissionGranted() {
        Log.i(TAG, "onPermissionGranted");
    }

    @Override
    public void onPermissionDenied(List<String> deniedPermission) {
        for (String s : deniedPermission)
            Log.i(TAG, "onPermissionDenied:" + s);
    }

    @Override
    public void onPermissionDeniedForever(List<String> strings) {
        for (String s : strings)
            Log.i(TAG, "onShowRationale:" + s);
    }
}
