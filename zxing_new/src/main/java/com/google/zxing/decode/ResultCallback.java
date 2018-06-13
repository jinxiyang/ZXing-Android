package com.google.zxing.decode;

import android.graphics.Bitmap;

import com.google.zxing.Result;

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
    void onResult(Result rawResult, Bitmap barcode, float scaleFactor);
}
