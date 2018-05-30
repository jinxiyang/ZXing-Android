package com.google.zxing;

import android.content.Context;
import android.os.Message;
import android.view.SurfaceHolder;

import com.google.zxing.camera.CameraManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Author: 杨进玺
 * Time: 2018/5/24  16:17
 */
public class QRManager {
    private Context context;
    private CameraManager cameraManager;
    private SurfaceHolder surfaceHolder;
    private CaptureHandler captureHandler;
    private ResultCallback resultCallback;
    private ResultPointCallback resultPointCallback;
    private QRView qrView;

    private Map<DecodeHintType, ?> baseHints;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;

    private boolean hasSurface = false;
    private boolean scanning = false;

    private SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            hasSurface = true;
            if (scanning){
                startScan();
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

    public void openCamera(){
        if (cameraManager != null){
            return;
        }
        cameraManager = new CameraManager(context);
        if (hasSurface){
            try {
                cameraManager.openDriver(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cameraManager.startPreview();
        }else {
            surfaceHolder.addCallback(shCallback);
        }
    }


    public void startScan(){
        openCamera();
        if (scanning || !hasSurface){
            return;
        }
        captureHandler = new CaptureHandler(cameraManager,
                qrView.getScanCodeRect(),
                resultCallback,
                baseHints,
                decodeFormats,
                characterSet,
                resultPointCallback);
        cameraManager.startPreview();
        Message.obtain(captureHandler, R.id.zxing_restart_preview).sendToTarget();
        if (qrView != null) qrView.startAnim();
        scanning = true;
    }


    public void stopScan(){
        if (!scanning){
            return;
        }
        captureHandler.quitSynchronously();
        captureHandler = null;
        if (qrView != null) qrView.stopAnim();
        scanning = false;
    }

    public void closeCamera(){
        stopScan();
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        cameraManager = null;
    }
}
