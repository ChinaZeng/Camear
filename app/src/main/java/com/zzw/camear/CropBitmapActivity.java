// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.zzw.camear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.zzw.camear1.ImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Built-in activity for image cropping.<br>
 */
public class CropBitmapActivity extends AppCompatActivity
        implements CropImageView.OnCropImageCompleteListener, CropImageView.OnSetImageUriCompleteListener, View
        .OnClickListener {

    private static final String TAG = "CropBitmapActivity";
    /**
     * The crop image view library widget used in the activity
     */
    private CropImageView mCropImageView;

    /**
     * Persist URI image to crop URI if specific permissions are required
     */
    private String mCropImagePath;

    public static void newInstance(Context context, String filePath) {
        Intent intent = new Intent(context, CropBitmapActivity.class);
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_SOURCE, filePath);
        context.startActivity(intent);
    }

    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_bitmap_activity);

        mCropImageView = findViewById(com.theartofdev.edmodo.cropper.R.id.cropImageView);

        mCropImagePath = getIntent().getStringExtra(CropImage.CROP_IMAGE_EXTRA_SOURCE);
        if (TextUtils.isEmpty(mCropImagePath)) {
            finish();
        }

        initCropImageView();

        findViewById(R.id.rotate).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
    }

    private void initCropImageView() {
//    mCropImageView.setScaleType(options.scaleType);
//    mCropImageView.setCropShape(options.cropShape);
//    mCropImageView.setGuidelines(options.guidelines);
//    mCropImageView.setAspectRatio(options.aspectRatio.first, options.aspectRatio.second);
//    mCropImageView.setFixedAspectRatio(options.fixAspectRatio);
//    mCropImageView.setMultiTouchEnabled(options.multitouch);
//    mCropImageView.setShowCropOverlay(options.showCropOverlay);
//    mCropImageView.setShowProgressBar(options.showProgressBar);
//    mCropImageView.setAutoZoomEnabled(true);
//    mCropImageView.setMaxZoom(options.maxZoomLevel);
//    mCropImageView.setFlippedHorizontally(options.flipHorizontally);
//    mCropImageView.setFlippedVertically(options.flipVertically);
        mCropImageView.setImageUriAsync(Uri.fromFile(new File(mCropImagePath)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCropImageView.setOnCropImageCompleteListener(this);
        mCropImageView.setOnSetImageUriCompleteListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCropImageView.setOnCropImageCompleteListener(null);
        mCropImageView.setOnSetImageUriCompleteListener(null);

    }


    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
        if (result.getError() == null) {
//            FileOutputStream fos = null;
            try {
                Bitmap bm = result.getBitmap();
                final String fileName = System.currentTimeMillis() + ".jpeg";
                String filePath = Camera1Helper.getSavaFileAbspath(fileName);
                ImageUtil.compressBitmap(bm, 60, filePath);
//                fos = new FileOutputStream(filePath);
//                bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                fos.flush();

                //把文件插入到系统图库
                try {
                    MediaStore.Images.Media.insertImage(getContentResolver(),
                            filePath, fileName, null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 最后通知图库更新
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +
                        filePath)));

                //图片
//                final Bitmap originalBitmap = result.getOriginalBitmap();
//                final float originaLbyteCount = (float) originalBitmap.getByteCount();
//                Log.e(TAG, "原图片:  length " + originaLbyteCount + "b   " + originaLbyteCount / 1024 + "k " +
//                        "  " + originaLbyteCount / 1024 / 1024 + "m");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                if (fos != null) {
//                    try {
//                        fos.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rotate:
                mCropImageView.rotateImage(90);
                break;
            case R.id.delete:
                FileUtils.deleteFile(mCropImagePath);
                finish();
                break;
            case R.id.save:
                mCropImageView.getCroppedImageAsync();
                break;
            default:
                break;
        }
    }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        if (error != null) {
            Log.e("error", error.getMessage());
        }
    }
}
