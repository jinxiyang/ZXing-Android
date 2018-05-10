package com.google.zxing.client.android;

import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * Author: 杨进玺
 * Time: 2018/5/10  11:03
 */
public interface IActivityProxy {

    ViewfinderView getViewfinderView();

    Handler getHandler();

    CameraManager getCameraManager();

    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor);
}
