/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.zxing.camera.open.OpenCamera;
import com.google.zxing.camera.open.OpenCameraInterface;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private final Context context;
    private OpenCamera camera;
    private AutoFocusManager autoFocusManager;
    private boolean previewing;

    private Point screenPoint;
    private Point previewSize;

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private PreviewCallback previewCallback;

    public CameraManager(Context context) {
        this.context = context;
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(-1);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);
        previewCallback = new PreviewCallback(previewSize);
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            camera.getCamera().release();
            camera = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * Convenience method for
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        OpenCamera theCamera = camera;
        if (theCamera != null && newSetting != getTorchState(theCamera.getCamera())) {
            boolean wasAutoFocusManager = autoFocusManager != null;
            if (wasAutoFocusManager) {
                autoFocusManager.stop();
                autoFocusManager = null;
            }
            setTorch(theCamera.getCamera(), newSetting);
            if (wasAutoFocusManager) {
                autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
                autoFocusManager.start();
            }
        }
    }

    private boolean getTorchState(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = parameters.getFlashMode();
                return flashMode != null && (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }
        }
        return false;
    }

    private void setTorch(Camera camera, boolean newSetting) {
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setTorch(parameters, newSetting);
//        CameraConfigurationUtils.setBestExposure(parameters, newSetting); //曝光
        camera.setParameters(parameters);
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
    }


    private void setDesiredCameraParameters(OpenCamera openCamera, boolean safeMode) {
        Camera theCamera = openCamera.getCamera();
        Camera.Parameters parameters = theCamera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        int previewFormat = parameters.getPreviewFormat();
        Log.i(TAG, "previewFormat: " + previewFormat);

        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        if (supportedPreviewFormats != null && supportedPreviewFormats.size() > 0){
            for (Integer integer : supportedPreviewFormats){
                Log.i(TAG, "supportedPreviewFormats: " + integer);
            }
        }


        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        CameraConfigurationUtils.setFocus(parameters, true, true, safeMode);//自动对焦

        if (!safeMode) {
            //反色，扫描黑色背景上的白色条码时
//            CameraConfigurationUtils.setInvertColor(parameters);

            //使用条形码场景匹配
            CameraConfigurationUtils.setBarcodeSceneMode(parameters);

            //以下三个设置：使用距离测量
//            CameraConfigurationUtils.setVideoStabilization(parameters);
//            CameraConfigurationUtils.setFocusArea(parameters);
//            CameraConfigurationUtils.setMetering(parameters);

            //SetRecordingHint to true also a workaround for low framerate on Nexus 4
            //https://stackoverflow.com/questions/14131900/extreme-camera-lag-on-nexus-4
            parameters.setRecordingHint(true);

        }

        screenPoint = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(screenPoint);
        previewSize = getBestPreviewSize(parameters, screenPoint);
        parameters.setPreviewSize(previewSize.x, previewSize.y);

        theCamera.setParameters(parameters);

        int cameraDisplayOrientation = getCameraDisplayOrientation(context, openCamera.getId());
        theCamera.setDisplayOrientation(cameraDisplayOrientation);

        Camera.Parameters afterParameters = theCamera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (previewSize.x != afterSize.width || previewSize.y != afterSize.height)) {
            previewSize.x = afterSize.width;
            previewSize.y = afterSize.height;
        }
    }


    public synchronized Rect framingRectInPreview(Rect framingRect){
        Rect framingRectInPreview = new Rect(0, 0, previewSize.x, previewSize.y);
        try {
            if (framingRect != null) {
                boolean isCameraPortrait = previewSize.x < previewSize.y;
                boolean isScreenPortrait = screenPoint.x < screenPoint.y;
                if (isCameraPortrait == isScreenPortrait) {
                    framingRectInPreview.left = framingRect.left * previewSize.x / screenPoint.x;
                    framingRectInPreview.right = framingRect.right * previewSize.x / screenPoint.x;
                    framingRectInPreview.top = framingRect.top * previewSize.y / screenPoint.y;
                    framingRectInPreview.bottom = framingRect.bottom * previewSize.y / screenPoint.y;
                } else {
                    framingRectInPreview.left = framingRect.top * previewSize.y / screenPoint.x;
                    framingRectInPreview.right = framingRect.bottom * previewSize.y / screenPoint.x;
                    framingRectInPreview.top = framingRect.left * previewSize.x / screenPoint.y;
                    framingRectInPreview.bottom = framingRect.right * previewSize.x / screenPoint.y;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return framingRectInPreview;
    }


    /**
     * 设置相机预览旋转方向
     * @param context
     * @param cameraId
     */
    private int getCameraDisplayOrientation(Context context, int cameraId){
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


    private Point getBestPreviewSize(Camera.Parameters parameters, Point screenResolution){
        int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
        double MAX_ASPECT_DISTORTION = 0.15;

        List<Camera.Size> rawSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedPreviewSizes == null){
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null){
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            return new Point(defaultSize.width, defaultSize.height);
        }

        Point screenSize = screenResolution;

        Camera.Size defaultSize = rawSupportedPreviewSizes.get(0);
        boolean isCameraPortrait = defaultSize.width < defaultSize.height;
        boolean isScreenPortrait = screenResolution.x < screenResolution.y;
        if (isCameraPortrait != isScreenPortrait){
            screenSize = new Point(screenResolution.y, screenResolution.x);
        }

        double screenAspectRatio = screenSize.x / (double)screenSize.y;

        List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedPreviewSizes);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        while (iterator.hasNext()){
            Camera.Size size = iterator.next();
            int width = size.width;
            int height = size.height;

            //正好和预览框宽高一样的，直接返回
            if (width == screenSize.x && height == screenSize.y){
                return new Point(width, height);
            }

            //去除分辨率较小的
            if (width * height < MIN_PREVIEW_PIXELS){
                iterator.remove();
                continue;
            }

            //去除宽高比和预览框宽高比差别较大的
            double aspectRatio = width / (double) height;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                iterator.remove();
                continue;
            }
        }

        //相机支持的预览尺寸按照由大到小的顺序排列
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        if (!supportedPreviewSizes.isEmpty()){
            Camera.Size largestPreview = supportedPreviewSizes.get(0);
            return new Point(largestPreview.width, largestPreview.height);
        }
        return new Point(defaultSize.width, defaultSize.height);
    }

    public synchronized void oneShotPreview(Camera.PreviewCallback previewCallback){
        if (camera != null){
            camera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
    }


    public Point getPreviewSize() {
        return previewSize;
    }
}
