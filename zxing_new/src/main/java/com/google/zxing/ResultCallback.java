package com.google.zxing;

import android.graphics.Bitmap;

/**
 * 扫码结果回调
 */
public interface ResultCallback {

    /**
     * 扫码解析结果回调
     * @param rawResult
     * @param barcode
     * @param scaleFactor
     */
    void onHandleResult(Result rawResult, Bitmap barcode, float scaleFactor);
}
