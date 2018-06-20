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

package com.google.zxing.decode;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import com.google.zxing.DecodeHintType;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

    private final Map<DecodeHintType, Object> hints;
    private final CountDownLatch handlerInitLatch;
    private Handler handler;
    private CaptureHandler captureHandler;
    private Rect framingRectInPreview;
    private boolean resultContainBitmap;
    private int cameraDisplayOrientation;

    public DecodeThread(CaptureHandler captureHandler,
                        Rect framingRectInPreview,
                        Map<DecodeHintType, Object> hints,
                        boolean resultContainBitmap,
                        int cameraDisplayOrientation) {
        this.captureHandler = captureHandler;
        this.framingRectInPreview = framingRectInPreview;
        handlerInitLatch = new CountDownLatch(1);
        this.hints = hints;
        this.resultContainBitmap = resultContainBitmap;
        this.cameraDisplayOrientation = cameraDisplayOrientation;
    }

    public Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(captureHandler, hints, framingRectInPreview, resultContainBitmap, cameraDisplayOrientation);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
