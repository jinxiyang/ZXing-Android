package com.google.zxing;

import android.content.Context;
import android.os.Message;
import android.view.SurfaceHolder;

import com.google.zxing.camera.CameraManager;

import java.io.IOException;

/**
 * Author: 杨进玺
 * Time: 2018/5/24  16:17
 */
public class QRManager {


    public static void openCamera(SurfaceHolder surfaceHolder){

    }

    public static void startScan(Context context, QROptions qrOptions){
        CameraManager cameraManager = new CameraManager(context);
        try {
            cameraManager.openDriver(qrOptions.surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CaptureHandler captureHandler = new CaptureHandler(cameraManager, qrOptions);
        cameraManager.startPreview();
        Message.obtain(captureHandler, R.id.zxing_restart_preview).sendToTarget();
    }

    public static void stopScan(){

    }

    public static void quitCamera(){

    }
}
