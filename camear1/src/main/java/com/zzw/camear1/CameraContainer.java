package com.zzw.camear1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * @author LinJ
 * @ClassName: CameraContainer
 * @Description: 相机界面的容器 包含相机绑定的surfaceview、拍照后的临时图片View和聚焦View
 * @date 2014-12-31 上午9:38:52
 */
public class CameraContainer extends RelativeLayout implements CameraOperation {

    // b   5M
    private final static long COMPRESS_MAX_SIZE = 5 * 1024 * 1024;

    public final static String TAG = "CameraContainer";

    /**
     * 是否正在保存照片
     */
    private volatile boolean saving = false;


    /**
     * 相机绑定的SurfaceView
     */
    private CameraView mCameraView;

    /**
     * 触摸屏幕时显示的聚焦图案
     */
    private FocusImageView mFocusImageView;


    /**
     * 存放照片的根目录
     */
    private String mSavePath;

    /**
     * 照片字节流处理类
     */
    private DataHandler mDataHandler;

    /**
     * 拍照监听接口，用以在拍照开始和结束后执行相应操作
     */
    private TakePictureListener mListener;

    public CameraContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        setOnTouchListener(new TouchListener());
    }

    /**
     * 初始化子控件
     *
     * @param context
     */
    private void initView(Context context) {
        inflate(context, R.layout.cameracontainer, this);
        mCameraView = (CameraView) findViewById(R.id.cameraView);
        mFocusImageView = (FocusImageView) findViewById(R.id.focusImageView);
    }


    /**
     * 改变相机模式 在拍照模式和录像模式间切换 两个模式的初始缩放级别不同
     *
     * @param zoom 缩放级别
     */
    public void switchMode(int zoom) {
        mCameraView.setZoom(zoom);
        //自动对焦
        mCameraView.onFocus(new Point(getWidth() / 2, getHeight() / 2), autoFocusCallback);
    }


    /**
     * 前置、后置摄像头转换
     */
    @Override
    public void switchCamera() {
        mCameraView.switchCamera();
    }

    /**
     * 获取当前闪光灯类型
     *
     * @return
     */
    @Override
    public CameraView.FlashMode getFlashMode() {
        return mCameraView.getFlashMode();
    }

    /**
     * 设置闪光灯类型
     *
     * @param flashMode
     */
    @Override
    public void setFlashMode(CameraView.FlashMode flashMode) {
        mCameraView.setFlashMode(flashMode);
    }

    /**
     * 设置文件保存路径
     *
     * @param rootPath
     */
    public void setRootPath(String rootPath) {
        this.mSavePath = rootPath;
    }


    /**
     * @param @param listener 拍照监听接口
     * @return void
     * @throws
     * @Description: 拍照方法
     */
    public void takePicture(TakePictureListener listener) {
        this.mListener = listener;
        takePicture(pictureCallback, mListener);
    }


    @Override
    public void takePicture(PictureCallback callback,
                            TakePictureListener listener) {
        mCameraView.takePicture(callback, listener);
    }

    @Override
    public int getMaxZoom() {
        return mCameraView.getMaxZoom();
    }

    @Override
    public void setZoom(int zoom) {
        mCameraView.setZoom(zoom);
    }

    @Override
    public int getZoom() {
        return mCameraView.getZoom();
    }


    private final AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            //聚焦之后根据结果修改图片
            if (success) {
                mFocusImageView.onFocusSuccess();
            } else {
                //聚焦失败显示的图片，由于未找到合适的资源，这里仍显示同一张图片
                mFocusImageView.onFocusFailed();

            }
        }
    };

    private final PictureCallback pictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
//            if (mSavePath == null) throw new RuntimeException("mSavePath is null");
            if (mDataHandler == null) {
                mDataHandler = new DataHandler();
            }
            mDataHandler.save(data);
            //重新打开预览图，进行下一次的拍照准备
            camera.startPreview();
        }
    };

    private final class TouchListener implements OnTouchListener {

        /**
         * 记录是拖拉照片模式还是放大缩小照片模式
         */

        private static final int MODE_INIT = 0;
        /**
         * 放大缩小照片模式
         */
        private static final int MODE_ZOOM = 1;
        private int mode = MODE_INIT;// 初始状态

        /**
         * 用于记录拖拉图片移动的坐标位置
         */

        private float startDis;


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            /** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                // 手指压下屏幕
                case MotionEvent.ACTION_DOWN:
                    mode = MODE_INIT;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = MODE_ZOOM;
                    /** 计算两个手指间的距离 */
                    startDis = distance(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == MODE_ZOOM) {
                        //只有同时触屏两个点的时候才执行
                        if (event.getPointerCount() < 2) {
                            return true;
                        }
                        float endDis = distance(event);// 结束距离
                        //每变化10f zoom变1
                        int scale = (int) ((endDis - startDis) / 10f);
                        if (scale >= 1 || scale <= -1) {
                            int zoom = mCameraView.getZoom() + scale;
                            //zoom不能超出范围
                            if (zoom > mCameraView.getMaxZoom()) {
                                zoom = mCameraView.getMaxZoom();
                            }
                            if (zoom < 0) {
                                zoom = 0;
                            }
                            mCameraView.setZoom(zoom);
                            //将最后一次的距离设为当前距离
                            startDis = endDis;
                        }
                    }
                    break;
                // 手指离开屏幕
                case MotionEvent.ACTION_UP:
                    if (mode != MODE_ZOOM) {
                        //设置聚焦
                        Point point = new Point((int) event.getX(), (int) event.getY());
                        mCameraView.onFocus(point, autoFocusCallback);
                        mFocusImageView.startFocus(point);
                    }
                    break;

                default:
                    break;
            }

            return true;
        }

        /**
         * 计算两个手指间的距离
         */
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            /** 使用勾股定理返回两点之间的距离 */
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

    }

    /**
     * 拍照返回的byte数据处理类
     *
     * @author linj
     */
    private final class DataHandler {

        /**
         * 大图存放路径
         */
        private String mImageFolder;


        public DataHandler() {
            mImageFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + mSavePath;
        }


        /**
         * 保存图片
         *
         * @param
         * @return 解析流生成的缩略图
         */
        public void save(byte[] data) {
            if (saving) {
                return;
            }
            saving = true;

            File folder = new File(mImageFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            //产生新的文件名
            String fileName = createFileNmae(".jpeg");
            String filePath = mImageFolder + File.separator + fileName;
            new ImageSaverThread(data, filePath).start();
        }
    }

    /**
     * @param extension 后缀名 如".jpg"
     * @return
     */
    public static String createFileNmae(String extension) {
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        // 转换为字符串
        String formatDate = format.format(new Date());
        //查看是否带"."
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return formatDate + extension;
    }

    /**
     * 将JPG保存到指定的文件中。
     */
    private class ImageSaverThread extends Thread {
        private byte[] data;
        private String filePath;


        public ImageSaverThread(byte[] data, String filePath) {
            this.data = data;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            ByteArrayOutputStream bao = null;
            try {
                Bitmap bm0 = BitmapFactory.decodeByteArray(data, 0, data.length);
//                Log.e(TAG, "bm0 " + bm0.getByteCount());
//                Matrix m = new Matrix();
//                m.setRotate(90, (float) bm0.getWidth() / 2, (float) bm0.getHeight() / 2);
//                Bitmap bm = Bitmap.createBitmap(bm0, 0, 0, bm0.getWidth(), bm0.getHeight(), m, true);
//                Log.e(TAG, "bm " + bm.getByteCount());

                //先采用jpeg质量压缩  如果压缩失败  再采用采样率压缩 压缩到5m一下
                int status = ImageUtil.compressBitmap(bm0, 50, filePath);
                if (status == 1) {
                    //如果大于5M在用采样压缩
                    if (new File(filePath).length() > COMPRESS_MAX_SIZE) {
                        Bitmap bm1 = BitmapFactory.decodeFile(filePath);
                        fos = new FileOutputStream(filePath);
                        bao = compress(bm1, COMPRESS_MAX_SIZE);
                        fos.write(bao.toByteArray());
                        fos.flush();
                    }
                    onPicture(filePath);
                } else {
                    fos = new FileOutputStream(filePath);
                    bao = compress(bm0, COMPRESS_MAX_SIZE);
                    fos.write(bao.toByteArray());
                    fos.flush();
                    onPicture(filePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onPicture(null);
            } finally {
                close(fos, bao);
                saving = false;
            }
        }

        /**
         * 图片压缩方法
         *
         * @param bitmap  图片文件
         * @param maxSize 文件大小最大值
         * @return 压缩后的字节流
         * @throws Exception
         */
        private ByteArrayOutputStream compress(Bitmap bitmap, long maxSize) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            int options = 99;

            // 循环判断如果压缩后图片是否大于maxSize,大于继续压缩
            while (baos.toByteArray().length > maxSize) {
                options -= 3;// 每次都减少10
                //压缩比小于0，不再压缩
                if (options < 0) {
                    break;
                }
                Log.d(TAG, baos.toByteArray().length / 1024 + "k");
                baos.reset();// 重置baos即清空baos
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            }
            return baos;
        }

    }


    private void onPicture(final String imgPath) {
        post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onTakePictureEnd(imgPath);
                }
            }
        });
    }

    /**
     * @author LinJ
     * @ClassName: TakePictureListener
     * @Description: 拍照监听接口，用以在拍照开始和结束后执行相应操作
     * @date 2014-12-31 上午9:50:33
     */
    public interface TakePictureListener {
        /**
         * 拍照结束执行的动作，该方法会在onPictureTaken函数执行后触发
         *
         * @param filePath 拍照生成的图片文件路径
         */
        public void onTakePictureEnd(String filePath);
    }

    /**
     * 关闭流
     *
     * @param closeable
     */
    public static void close(Closeable... closeable) {
        if (closeable != null) {
            try {
                for (Closeable closeable1 : closeable) {
                    if (closeable1 != null) {
                        closeable1.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}