package com.zzw.camear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.zzw.camear1.ImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Des:
 * Created by zzw on 2017/11/2.
 */

public class Camera1Helper implements SurfaceHolder.Callback {

    private static final String TAG = "Camera1Helper";

    private volatile boolean saving = false;

    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1920;
    private static final float DEFAULT_RATIO = ((float) DEFAULT_HEIGHT) / (float) DEFAULT_WIDTH;  //因为旋转了90度 所以宽高变了


    private Camera mCamera;
    private SurfaceHolder sHolder;
    private Activity mActivity;


    private boolean isFouce;

    public static String PIC_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
            "fachan";

    public Camera1Helper(Activity activity) {
        this.mActivity = activity;
    }

    public void init(SurfaceView surfaceView) {
        initCamera(surfaceView);
    }



    /**
     * 初始化Camera相关
     */
    private void initCamera(final SurfaceView surfaceView) {
        mCamera = getCamera();
        sHolder = surfaceView.getHolder();
        sHolder.addCallback(this);
//        startPreview();    //由于APP在第一次安装时，onResume不会执行，所以重新获得cemera权限以后重新start
    }


    /**
     * 获得一个Camera
     */
    private Camera getCamera() {
        Camera camera = Camera.open();
        return camera;
    }


    /**
     * 开启Camera预览
     */
    public void startPreview() {
        try {
            if (mCamera == null || sHolder == null) {
                return;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);//自动对焦
            List<Camera.Size> size2 = parameters.getSupportedPreviewSizes();     //得到手机支持的预览分辨率
            Point point = findBestPreviewSizeValue(size2);
            if (point == null) {
                parameters.setPreviewSize(size2.get(0).width, size2.get(0).height);
            } else {
                parameters.setPreviewSize(point.x, point.y);
            }
            mCamera.setPreviewDisplay(sHolder);//绑定holder
            setCameraDisplayOrientation(findFrontFacingCameraID(), mCamera);//将系统Camera角度进行调整

            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
            //设置图片样式
            parameters.setPictureFormat(ImageFormat.JPEG);
            Point point1 = findBestPictureSizeValue(supportedPictureSizes);
            if (point1 == null) {
                parameters.setPictureSize(supportedPictureSizes.get(0).width, supportedPictureSizes.get(0).height);
            } else {
                parameters.setPictureSize(point1.x, point1.y);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();//开启预览
            Log.e("zzz", "预览x: " + parameters.getPreviewSize().width + "  y: " + parameters.getPreviewSize().height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击拍照
     */
    public void capture() {
//        Camera.Parameters parameters = mCamera.getParameters();
//        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
//        parameters.setPictureFormat(ImageFormat.JPEG);//设置图片样式
//        Point point = findBestPictureSizeValue(supportedPictureSizes, new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT));
//        if (point == null) {
//            parameters.setPictureSize(supportedPictureSizes.get(0).width, supportedPictureSizes.get(0).height);
//        } else {
//            parameters.setPictureSize(point.x, point.y);
//        }
//        mCamera.setParameters(parameters);
//        Log.e("zzz", "照相 x: " + parameters.getPictureSize().width + " y: " + parameters.getPictureSize().height);
//        Log.e("zzz", "照相预览 x: " + parameters.getPreviewSize().width + " y: " + parameters.getPreviewSize().height);


        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success && !saving) {
                    saving = true;
                    mCamera.takePicture(null, null, pictureCallback);
                }
            }
        });
    }


    /**
     * 自动对焦
     */
    public void autoFocus() {
        if (mCamera == null && isFouce) {
            return;
        }
        isFouce = true;
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                isFouce = false;
            }
        });
    }

    /**
     * 打开闪光灯
     */
    public void turnOnFlash() {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭闪光灯
     */
    public void turnOffFlash() {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        //byte[]是拍照后完整的数据
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            String fileName = System.currentTimeMillis() + ".jpeg";
            new ImageSaverThread(data, fileName).start();
        }
    };


    private void onPicture(final boolean isOk, final String filePath, final String fileName, final Bitmap bitmap) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOnCaptureListener != null) {
                    mOnCaptureListener.onCapture(isOk, filePath, fileName, bitmap);
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releasePreview();
    }

    /**
     * 释放Camera
     */
    public void releasePreview() {
        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();//停止预览
        mCamera.release();
        mCamera = null;
    }

    public void resumePreview() {
        if (mCamera == null) {
            mCamera = getCamera();
            startPreview();
        }
    }


    /**
     * 如果文件不存在，就创建文件
     *
     * @param fileName 文件name
     * @return
     */
    public static String getSavaFileAbspath(String fileName) {
        File file = new File(PIC_DIR + File.separator + fileName);
        try {
            File parentDir = new File(PIC_DIR);
            if (!parentDir.getParentFile().mkdirs()) {
                parentDir.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    /**
     * 预览最大分辨率
     *
     * @param sizeList
     * @return
     */
    private static Point findBestPreviewSizeValue(List<Camera.Size> sizeList) {
        int bestX = 0;
        int bestY = 0;
        int size = 0;
        for (int i = 0; i < sizeList.size(); i++) {
            // 如果有符合的分辨率，则直接返回
            if (sizeList.get(i).width == DEFAULT_WIDTH && sizeList.get(i).height == DEFAULT_HEIGHT) {
                Log.d(TAG, "get default preview size!!!");
                return new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }

            int newX = sizeList.get(i).width;
            int newY = sizeList.get(i).height;
            int newSize = Math.abs(newX * newX) + Math.abs(newY * newY);
            float ratio = (float) newX / (float) newY;
            Log.d(TAG, newX + ":" + newY + ":" + ratio);
            if (newSize >= size && ratio == DEFAULT_RATIO) {  // 确保图片是16：9的
                bestX = newX;
                bestY = newY;
                size = newSize;
            }
        }

        if (bestX > 0 && bestY > 0) {
            Log.e(TAG, "预览best" + bestX + "  " + bestY);
            return new Point(bestX, bestY);
        }
        return null;
    }

    private static Point findBestPictureSizeValue(List<Camera.Size> sizeList) {
        int bestX = 0;
        int bestY = 0;
        int size = 0;
        for (int i = 0; i < sizeList.size(); i++) {
            // 如果有符合的分辨率，则直接返回
//            if (sizeList.get(i).width == DEFAULT_WIDTH && sizeList.get(i).height == DEFAULT_HEIGHT) {
//                Log.d(TAG, "get default preview size!!!");
//                return new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//            }
            int newX = sizeList.get(i).width;
            int newY = sizeList.get(i).height;
            int newSize = Math.abs(newX * newX) + Math.abs(newY * newY);
            float ratio = (float) newX / (float) newY;
            Log.d(TAG, newX + ":" + newY + ":" + ratio);
            if (newSize >= size && ratio == DEFAULT_RATIO) {  // 确保图片是16：9的
                bestX = newX;
                bestY = newY;
                size = newSize;
            }
        }

        if (bestX > 0 && bestY > 0) {
            Log.e(TAG, "照相best:" + bestX + "  " + bestY);
            return new Point(bestX, bestY);
        }
        return null;
    }


    private OnCaptureListener mOnCaptureListener;

    public interface OnCaptureListener {
        void onCapture(boolean isOk, String filePath, String fileName, Bitmap bitmap);
    }

    public void setOnCaptureListener(OnCaptureListener onCaptureListener) {
        this.mOnCaptureListener = onCaptureListener;
    }


    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int findFrontFacingCameraID() {
        int cameraId = -1;
        // Search for the back facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }


    /**
     * 将JPG保存到指定的文件中。
     */
    private class ImageSaverThread extends Thread {
        private byte[] data;
        private String fileName;

        public ImageSaverThread(byte[] data, String fileName) {
            this.data = data;
            this.fileName = fileName;
        }

        @Override
        public void run() {
//            FileOutputStream fos = null;
            try {
                Bitmap bm0 = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.e(TAG, "bm0 " + bm0.getByteCount());
                Matrix m = new Matrix();
                m.setRotate(90, (float) bm0.getWidth() / 2, (float) bm0.getHeight() / 2);
                Bitmap bm = Bitmap.createBitmap(bm0, 0, 0, bm0.getWidth(), bm0.getHeight(), m, true);
                Log.e(TAG, "bm " + bm.getByteCount());

                String filePath = getSavaFileAbspath(fileName);
                int status = ImageUtil.compressBitmap(bm, 50, filePath);
                if (status == 1) {
                    onPicture(true, filePath, fileName, null);
                } else {
                    onPicture(false, null, null, null);
                }
//                    String filePath = getSavaFileAbspath(fileName);
//                    //保存图片
//                    Log.e(TAG, "保存图片:" + filePath + "  length " + data.length + "b   " + (float) data.length / 1024
// + "k " +
//                            "  " + (float) data
//                            .length / 1024 / 1024 + "m");
//                    fos = new FileOutputStream(filePath);
//                    bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                    fos.flush();
//                    bm.recycle();
//                    onPicture(true,filePath,fileName,null);
            } catch (Exception e) {
                e.printStackTrace();
                onPicture(false, null, null, null);
            } finally {
//                if (fos != null) {
//                    try {
//                        fos.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
                saving = false;
            }
        }

    }


//    private static Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
//        float focusAreaSize = 300;
//        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
//        int centerX = (int) (x / width * 2000 - 1000);
//        int centerY = (int) (y / height * 2000 - 1000);
//
//        int halfAreaSize = areaSize / 2;
//        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000)
//                , clamp(centerY - halfAreaSize, -1000, 1000)
//                , clamp(centerX + halfAreaSize, -1000, 1000)
//                , clamp(centerY + halfAreaSize, -1000, 1000));
//
//        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF
//                .bottom));
//    }
//
//
//    private static int clamp(int x, int min, int max) {
//        if (x > max) {
//            return max;
//        }
//        if (x < min) {
//            return min;
//        }
//        return x;
//    }
//
//
//    private static float getFingerSpacing(MotionEvent event) {
//        float x = event.getX(0) - event.getX(1);
//        float y = event.getY(0) - event.getY(1);
//        return (float) Math.sqrt(x * x + y * y);
//    }
//
//
//    private void handleZoom(boolean isZoomIn, Camera camera) {
//        Camera.Parameters params = camera.getParameters();
//        if (params.isZoomSupported()) {
//            int maxZoom = params.getMaxZoom();
//            int zoom = params.getZoom();
//            if (isZoomIn && zoom < maxZoom) {
//                zoom++;
//            } else if (zoom > 0) {
//                zoom--;
//            }
//            params.setZoom(zoom);
//            camera.setParameters(params);
//        } else {
//            Log.i(TAG, "zoom not supported");
//        }
//    }
}
