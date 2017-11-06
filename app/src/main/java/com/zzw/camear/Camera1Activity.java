package com.zzw.camear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.FileNotFoundException;

/**
 * Des:
 * Created by zzw on 2017/11/2.
 */

public class Camera1Activity extends AppCompatActivity implements View.OnClickListener, Camera1Helper
        .OnCaptureListener {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private Camera1Helper cameraHelper;

    private SurfaceView surfaceView;
    boolean isCamera;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        cameraHelper = new Camera1Helper(this);
        findViewById(R.id.light).setOnClickListener(this);
        findViewById(R.id.take_picture).setOnClickListener(this);
        surfaceView = findViewById(R.id.surface_camera);
        surfaceView.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            isCamera = true;
            initCamera();
        }
    }

    private void initCamera() {
        cameraHelper.init(surfaceView);
        cameraHelper.setOnCaptureListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isCamera = true;
                    initCamera();
                } else {
                    Toast.makeText(this, "权限申请失败！", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "请开文件目录权限!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                break;
        }
    }

    /**
     * 获得Camera，开启预览
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!isCamera) {
            return;
        }
        cameraHelper.resumePreview();
    }

    /**
     * 停止预览，销毁Camera
     */
    @Override
    protected void onPause() {
        super.onPause();
        cameraHelper.releasePreview();
    }


    private int light = 0;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture://拍照
                cameraHelper.capture();
                break;
            case R.id.surface_camera://对焦
                cameraHelper.autoFocus();
                break;
            case R.id.light://闪光灯切换
                if (light == 0) {//关闭状态 打开
                    light = 1;
                    cameraHelper.turnOnFlash();
                } else if (light == 1) {
                    light = 0;
                    cameraHelper.turnOffFlash();
                }
                break;

            default:
                break;
        }
    }




    @Override
    public void onCapture(boolean isOk, String filePath, String fileName, Bitmap bitmap) {
        if(isOk){
            CropBitmapActivity.newInstance(this,filePath);
        }


    }
}
