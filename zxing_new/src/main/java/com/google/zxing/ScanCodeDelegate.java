package com.google.zxing;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.SurfaceHolder;

import com.google.zxing.camera.CameraManager;

import java.util.Set;

/**
 * Author: 杨进玺
 * Time: 2018/5/21  14:22
 */
public class ScanCodeDelegate {

    IScanCodeView scanCodeView;
    ResultCallback resultCallback;
    Set<BarcodeFormat> barcodeFormats = DecodeFormatManager.FORMATS_FOR_MODE;
    String characterSet = "utf-8";
    boolean continuous;
    boolean decodeFullImage;

    private SurfaceHolder surfaceHolder;
    private boolean hasSurface;

    private CameraManager cameraManager;

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!hasSurface) {
                hasSurface = true;
                initCamera(holder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            hasSurface = false;
        }
    };

    public interface ResultCallback{
         void onHandleResult(Result rawResult, Bitmap barcode, float scaleFactor);
    }

    public void initCamera(Context context){
        cameraManager = new CameraManager(context);
        if (hasSurface) {
            _initCamera();
        } else {
            surfaceHolder.addCallback(surfaceCallback);
        }
    }

    private void _initCamera(){
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void closeCamera(){

    }

    public void startScan(){

    }

    public void stopScan(){

    }
}
