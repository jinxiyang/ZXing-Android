package com.yang.demo;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = getClass().getSimpleName();

    boolean hasSurface;
    private SurfaceView svPreview;

    private boolean previewing = false;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenPreview = (Button) findViewById(R.id.btn_open_preview);
        btnOpenPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewing){
                    stopPreview();
                }else {
                    startPreview();
                }
            }
        });

        svPreview = (SurfaceView) findViewById(R.id.sv_preview);
        svPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                hasSurface = true;
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                hasSurface = false;
            }
        });
    }

    private void startPreview() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Log.i(TAG, "startPreview: " + numberOfCameras);

        Camera.CameraInfo backCamera = null;
        int index = 0;
        while (index < numberOfCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCamera = cameraInfo;
                break;
            }
            index++;
        }
        Log.i(TAG, "startPreview: " + index);


        if (index < numberOfCameras) {
            camera = Camera.open(index);
            try {
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

                if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0) {
                    for (Camera.Size size : supportedPreviewSizes) {
                        Log.i(TAG, "Camera.Size: [" + size.width + "," + size.height + "]");
                    }
                    Camera.Size size = supportedPreviewSizes.get(0);
//                    parameters.setPreviewSize(size.width, size.height);
                }

                int width = svPreview.getWidth();
                int height = svPreview.getHeight();
                Log.i(TAG, "svPreview: [" + width + "," + height + "]");

                Point bestPreviewSize = getBestPreviewSize(parameters, new Point(width, height));
                parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
                camera.setParameters(parameters);
                camera.setDisplayOrientation(getCameraDisplayOrientation(this, index));
                camera.setPreviewDisplay(svPreview.getHolder());
                previewing = true;
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
    }

    private void stopPreview() {
        if (previewing){
            camera.stopPreview();
            previewing = false;
            camera.release();
            camera = null;
        }
    }


    /**
     * 设置相机预览旋转方向
     * @param context
     * @param cameraId
     */
    public static int getCameraDisplayOrientation(Context context, int cameraId){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

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
                // Have seen this return incorrect values like -90
                if (rotation % 90 == 0) {
                    degrees = (360 + rotation) % 360;
                } else {
                    throw new IllegalArgumentException("Bad rotation: " + rotation);
                }
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }


    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.15;

    public static Point getBestPreviewSize(Camera.Parameters parameters, final Point screenResolution){
        List<Camera.Size> rawSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedPreviewSizes == null){
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null){
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        final float screenAspectRatio = screenResolution.x / (float)screenResolution.y;

        List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedPreviewSizes);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        while (iterator.hasNext()){
            Camera.Size size = iterator.next();
            int width = size.width;
            int height = size.height;

            //去除分辨率较小的
            if (width * height < MIN_PREVIEW_PIXELS){
                iterator.remove();
                continue;
            }

            //去除宽高比和预览框宽高比差别较大的
            boolean isCandidatePortrait = width < height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                iterator.remove();
                continue;
            }

            //正好和预览框宽高一样的，直接返回
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(width, height);
                return exactPoint;
            }
        }

        //按宽高比和预览宽高比最接近排序，降序
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                int width1 = o1.width;
                int height1 = o1.height;
                int width2 = o2.width;
                int height2 = o2.height;
                float ratio1 = Math.abs((float) height1 / width1 - screenAspectRatio);
                float ratio2 = Math.abs((float) height2 / width2 - screenAspectRatio);
                int result = Float.compare(ratio1, ratio2);
                if (result != 0) {
                    return result;
                } else {
                    int minGap1 = Math.abs(screenResolution.x - width1) + Math.abs(screenResolution.y - height1);
                    int minGap2 = Math.abs(screenResolution.x - width2) + Math.abs(screenResolution.y - height2);
                    return minGap1 - minGap2;
                }
            }
        });

        if (supportedPreviewSizes != null && !supportedPreviewSizes.isEmpty()){
            Camera.Size size = supportedPreviewSizes.get(0);
            return new Point(size.width, size.height);
        }

        Camera.Size defaultSize = parameters.getPreviewSize();
        if (defaultSize == null) {
            throw new IllegalStateException("Parameters contained no preview size!");
        }
        return new Point(defaultSize.width, defaultSize.height);
    }

}
