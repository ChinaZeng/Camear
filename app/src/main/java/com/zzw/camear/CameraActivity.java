package com.zzw.camear;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

/**
 * Des:
 *
 * @author zzw
 * @date 2017/11/1
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceView;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private Camera2Helper camera2Helper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2Helper = new Camera2Helper(this);
        } else {
            Toast.makeText(this, "仅支持5.0以上的手机！", Toast.LENGTH_SHORT).show();
            finish();
        }
        setContentView(R.layout.activity_main);
        findViewById(R.id.take_picture).setOnClickListener(this);
        mSurfaceView = findViewById(R.id.surface_camera);
        mSurfaceView.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            init();
        }
    }

    private void init() {
        camera2Helper.init(mSurfaceView);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_picture://拍照
                camera2Helper.takePicture();
                break;
            case R.id.surface_camera://对焦

                break;

            default:
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(this, "请开启摄像头权限!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            }

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

}
