package com.google.zxing;

import android.content.Context;
import android.graphics.Rect;
import android.os.Message;
import android.view.SurfaceHolder;

import com.google.zxing.camera.CameraManager;
import com.google.zxing.decode.CaptureHandler;
import com.google.zxing.decode.DecodeFormatManager;
import com.google.zxing.decode.ResultCallback;
import com.google.zxing.view.QRView;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;


public class QRManager {
    public static boolean DEBUG_RESULT = false;


    private CameraManager cameraManager;
    private CaptureHandler captureHandler;
    private ResultCallback resultCallback;
    private QRView qrView;

    private Map<DecodeHintType, Object> hints;


    public void openCamera(Context context, SurfaceHolder surfaceHolder) throws IOException {
        cameraManager = new CameraManager(context);
        cameraManager.openDriver(surfaceHolder);
        cameraManager.startPreview();
    }

    public void startScan(){
        Rect rect = cameraManager.framingRectInPreview(qrView.getScanCodeRect());
        captureHandler = new CaptureHandler(cameraManager, rect, getHint(), resultCallback);
        Message.obtain(captureHandler, R.id.zxing_restart_preview).sendToTarget();
        qrView.startAnim();
    }


    public void stopScan(){
        captureHandler.quitSynchronously();
        captureHandler = null;
        qrView.stopAnim();
    }

    public void closeCamera(){
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        cameraManager = null;
    }

    public void flashlight(boolean open){
        cameraManager.setTorch(open);
    }

    public void setResultCallback(ResultCallback resultCallback) {
        this.resultCallback = resultCallback;
    }

    public void setQrView(QRView qrView) {
        this.qrView = qrView;
    }

    public Map<DecodeHintType, Object> getHint(){
        if (hints == null || hints.isEmpty()){
            hints = new EnumMap<>(DecodeHintType.class);
            Collection<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
//        hints.put(DecodeHintType.CHARACTER_SET, characterSet);
//        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
        }
        return hints;
    }

    public void setHints(Map<DecodeHintType, Object> hints) {
        this.hints = hints;
    }
}
