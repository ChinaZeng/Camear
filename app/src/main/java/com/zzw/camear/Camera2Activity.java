package com.zzw.camear;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
public class Camera2Activity extends AppCompatActivity implements View.OnClickListener{

    private AutoFitTextureView mSurfaceView;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private  Camera2Helper2 camera2Helper;

    @SuppressLint("WrongViewCast")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        findViewById(R.id.take_picture).setOnClickListener(this);
        mSurfaceView = findViewById(R.id.surface_camera);
        mSurfaceView.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2Helper = new Camera2Helper2(mSurfaceView,this);
        }else {
            Toast.makeText(this,"仅支持5.0以上的手机！",Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            init();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }


    private void init() {
        camera2Helper.init();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
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
            default:
                break;
        }
    }

}
