package com.zzw.camear;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.zzw.camear1.CameraContainer;
import com.zzw.camear1.CameraView;
import com.zzw.camear1.CameraView.FlashMode;

/**
 * Des:
 * Created by zzw on 2017/11/6.
 */

public class CameraTestActivity extends AppCompatActivity implements View.OnClickListener, CameraContainer.TakePictureListener {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    boolean isCamera;
    private CameraContainer mCameraContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        findViewById(R.id.light).setOnClickListener(this);
        findViewById(R.id.take_picture).setOnClickListener(this);
        mCameraContainer = findViewById(R.id.surface_camera);
        mCameraContainer.setRootPath("test");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            isCamera = true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[]
            grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isCamera = true;
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



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture://拍照
                mCameraContainer.takePicture(this);
                break;

            case R.id.light://闪光灯切换
                if (mCameraContainer.getFlashMode() == FlashMode.ON) {
                    mCameraContainer.setFlashMode(FlashMode.OFF);
                } else if (mCameraContainer.getFlashMode() == FlashMode.OFF) {
                    mCameraContainer.setFlashMode(FlashMode.AUTO);
                } else if (mCameraContainer.getFlashMode() == FlashMode.AUTO) {
                    mCameraContainer.setFlashMode(FlashMode.TORCH);
                } else if (mCameraContainer.getFlashMode() == FlashMode.TORCH) {
                    mCameraContainer.setFlashMode(CameraView.FlashMode.ON);
                }
                break;

            default:
                break;
        }
    }
    @Override
    public void onTakePictureEnd(String filePath) {
        if(!TextUtils.isEmpty(filePath)){
            CropBitmapActivity.newInstance(this, filePath);
        }
    }
}
