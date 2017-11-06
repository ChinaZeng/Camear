package com.zzw.camear1;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author LinJ
 * @ClassName: CameraView
 * @Description: 和相机绑定的SurfaceView 封装了拍照方法
 * @date 2014-12-31 上午9:44:56
 */
public class CameraView extends SurfaceView implements CameraOperation {

    public final static String TAG = "CameraView";
    /**
     * 和该View绑定的Camera对象
     */
    private Camera mCamera;

    /**
     * 当前闪光灯类型，默认为关闭
     */
    private FlashMode mFlashMode = FlashMode.OFF;

    /**
     * 当前缩放级别  默认为0
     */
    private int mZoom = 0;

    /**
     * 当前屏幕旋转角度
     */
    private int mOrientation = 0;
    /**
     * 是否打开前置相机,true为前置,false为后置
     */
    private boolean mIsFrontCamera;

    public CameraView(Context context) {
        super(context);
        //初始化容器
        getHolder().addCallback(callback);
        openCamera();
        mIsFrontCamera = false;
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化容器
        getHolder().addCallback(callback);
        openCamera();
        mIsFrontCamera = false;
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (mCamera == null) {
                    openCamera();
                }
                setCameraParameters();
                mCamera.setPreviewDisplay(getHolder());
            } catch (Exception e) {
                Toast.makeText(getContext(), "打开相机失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, e.getMessage());
            }
            mCamera.startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            updateCameraOrientation();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

        }
    };


    /**
     * 转换前置和后置照相机
     */
    @Override
    public void switchCamera() {
        mIsFrontCamera = !mIsFrontCamera;
        openCamera();
        if (mCamera != null) {
            setCameraParameters();
            updateCameraOrientation();
            try {
                mCamera.setPreviewDisplay(getHolder());
                mCamera.startPreview();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据当前照相机状态(前置或后置)，打开对应相机
     */
    private boolean openCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mIsFrontCamera) {
            CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        mCamera = Camera.open(i);
                    } catch (Exception e) {
                        mCamera = null;
                        return false;
                    }

                }
            }
        } else {
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                mCamera = null;
                return false;
            }

        }
        return true;
    }

    /**
     * 获取当前闪光灯类型
     *
     * @return
     */
    @Override
    public FlashMode getFlashMode() {
        return mFlashMode;
    }

    /**
     * 设置闪光灯类型
     *
     * @param flashMode
     */
    @Override
    public void setFlashMode(FlashMode flashMode) {
        if (mCamera == null) {
            return;
        }
        mFlashMode = flashMode;
        Parameters parameters = mCamera.getParameters();
        switch (flashMode) {
            case ON:
                parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                break;
            case AUTO:
                parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
                break;
            case TORCH:
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                break;
            default:
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                break;
        }
        mCamera.setParameters(parameters);
    }

    @Override
    public void takePicture(PictureCallback callback, CameraContainer.TakePictureListener listener) {
        mCamera.takePicture(null, null, callback);
    }

    /**
     * 手动聚焦
     *
     * @param point 触屏坐标
     */
    protected void onFocus(Point point, AutoFocusCallback callback) {
        Parameters parameters = mCamera.getParameters();
        //不支持设置自定义聚焦，则使用自动聚焦，返回
        if (parameters.getMaxNumFocusAreas() <= 0) {
            mCamera.autoFocus(callback);
            return;
        }
        List<Area> areas = new ArrayList<Area>();
        int left = point.x - 300;
        int top = point.y - 300;
        int right = point.x + 300;
        int bottom = point.y + 300;
        left = left < -1000 ? -1000 : left;
        top = top < -1000 ? -1000 : top;
        right = right > 1000 ? 1000 : right;
        bottom = bottom > 1000 ? 1000 : bottom;
        areas.add(new Area(new Rect(left, top, right, bottom), 100));
        parameters.setFocusAreas(areas);
        try {
            //本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
            //目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        mCamera.autoFocus(callback);
    }

    /**
     * 获取最大缩放级别，最大为40
     *
     * @return
     */
    @Override
    public int getMaxZoom() {
        if (mCamera == null) {
            return -1;
        }
        Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported()) {
            return -1;
        }
        return parameters.getMaxZoom() > 40 ? 40 : parameters.getMaxZoom();
    }

    /**
     * 设置相机缩放级别
     *
     * @param zoom
     */
    @Override
    public void setZoom(int zoom) {
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported()) {
            return;
        }
        parameters.setZoom(zoom);
        mCamera.setParameters(parameters);
        mZoom = zoom;
    }

    @Override
    public int getZoom() {
        return mZoom;
    }


    /**
     * 设置照相机参数
     */
    private void setCameraParameters() {
        Parameters parameters = mCamera.getParameters();
        // 选择合适的预览尺寸
        List<Size> sizeList = parameters.getSupportedPreviewSizes();
//        if (sizeList.size() > 0) {
//            Size cameraSize = sizeList.get(0);
//            //预览图片大小
//            parameters.setPreviewSize(cameraSize.width, cameraSize.height);
//        }
        Point point = findBestPreviewSizeValue(sizeList);
        parameters.setPreviewSize(point.x, point.y);

        //设置生成的图片大小
        sizeList = parameters.getSupportedPictureSizes();
//        if (sizeList.size() > 0) {
//            Size cameraSize = sizeList.get(0);
//            for (Size size : sizeList) {
//                //小于100W像素
//                if (size.width * size.height < 100 * 10000) {
//                    cameraSize = size;
//                    break;
//                }
//            }
//            parameters.setPictureSize(cameraSize.width, cameraSize.height);
//        }
        Point point1 = findBestPictureSizeValue(sizeList);
        parameters.setPictureSize(point1.x, point1.y);
        //设置图片格式
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setJpegQuality(100);
        parameters.setJpegThumbnailQuality(100);
        //自动聚焦模式
        parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(parameters);
        //设置闪光灯模式。此处主要是用于在相机摧毁后又重建，保持之前的状态
        setFlashMode(mFlashMode);
        //设置缩放级别
        setZoom(mZoom);
        //开启屏幕朝向监听
        startOrientationChangeListener();
    }

    /**
     * 启动屏幕朝向改变监听函数 用于在屏幕横竖屏切换时改变保存的图片的方向
     */
    private void startOrientationChangeListener() {
        OrientationEventListener mOrEventListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int rotation) {

                if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
                    rotation = 0;
                } else if ((rotation > 45) && (rotation <= 135)) {
                    rotation = 90;
                } else if ((rotation > 135) && (rotation <= 225)) {
                    rotation = 180;
                } else if ((rotation > 225) && (rotation <= 315)) {
                    rotation = 270;
                } else {
                    rotation = 0;
                }
                if (rotation == mOrientation) {
                    return;
                }
                mOrientation = rotation;
                updateCameraOrientation();
            }
        };
        mOrEventListener.enable();
    }

    /**
     * 根据当前朝向修改保存图片的旋转角度
     */
    private void updateCameraOrientation() {
        if (mCamera != null) {
            Parameters parameters = mCamera.getParameters();
            //rotation参数为 0、90、180、270。水平方向为0。
            int rotation = 90 + mOrientation == 360 ? 0 : 90 + mOrientation;
            //前置摄像头需要对垂直方向做变换，否则照片是颠倒的
            if (mIsFrontCamera) {
                if (rotation == 90) {
                    rotation = 270;
                } else if (rotation == 270) {
                    rotation = 90;
                }
            }
            parameters.setRotation(rotation);//生成的图片转90°
            //预览图片旋转90°
            mCamera.setDisplayOrientation(90);//预览转90°
            mCamera.setParameters(parameters);
        }
    }

    /**
     * @Description: 闪光灯类型枚举 默认为关闭
     */
    public enum FlashMode {
        /**
         * ON:拍照时打开闪光灯
         */
        ON,
        /**
         * OFF：不打开闪光灯
         */
        OFF,
        /**
         * AUTO：系统决定是否打开闪光灯
         */
        AUTO,
        /**
         * TORCH：一直打开闪光灯
         */
        TORCH
    }

    private static final int MIN_PICTURE_WIDTH = 1024;
    private static final int MIN_PICTURE_HEIGHT = 1024;

    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1920;
    private static final float DEFAULT_RATIO = ((float) DEFAULT_HEIGHT) / (float) DEFAULT_WIDTH;  //因为旋转了90度 所以宽高变了

    /**
     * 预览最大分辨率
     *
     * @param sizeList
     * @return
     */
    private static Point findBestPreviewSizeValue(List<Size> sizeList) {
        int bestX = 0;
        int bestY = 0;
        int size = 0;
        for (int i = 0; i < sizeList.size(); i++) {
            // 如果有符合的分辨率，则直接返回
            if (sizeList.get(i).width == DEFAULT_WIDTH && sizeList.get(i).height == DEFAULT_HEIGHT) {
                Log.d(TAG, "使用默认的预览尺寸" + "   w：" + DEFAULT_WIDTH + "  h:" + DEFAULT_HEIGHT);
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
            Log.e(TAG, "预览最佳尺寸  " + bestX + "  " + bestY);
            return new Point(bestX, bestY);
        }
        return new Point(sizeList.get(0).width, sizeList.get(0).height);
    }

    /**
     * 照相最佳的分辨率
     *
     * @param sizeList
     * @return
     */
    private static Point findBestPictureSizeValue(List<Size> sizeList) {
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

            if (newX < MIN_PICTURE_WIDTH || newY < MIN_PICTURE_HEIGHT) {
                continue;
            }

            int newSize = Math.abs(newX * newX) + Math.abs(newY * newY);
            float ratio = (float) newX / (float) newY;
            Log.d(TAG, "照相:" + newX + "  " + newY + "  " + ratio);
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
        return new Point(sizeList.get(0).width, sizeList.get(0).height);
    }
}