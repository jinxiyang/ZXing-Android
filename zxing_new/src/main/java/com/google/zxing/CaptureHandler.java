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

package com.google.zxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.camera.CameraManager;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureHandler extends Handler {

    private QRManager qrManager;
    private State state;
    private CameraManager cameraManager;
    private DecodeThread decodeThread;


    public CaptureHandler(CameraManager cameraManager, Rect framingRectInPreview) {
        this.cameraManager = cameraManager;
        qrManager = QRManager.getInstance();
        decodeThread = new DecodeThread(this,
                framingRectInPreview,
                qrManager.getHint());
        decodeThread.start();
        state = State.SUCCESS;
    }

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }


    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.zxing_restart_preview) {
            restartPreviewAndDecode();
        } else if (message.what == R.id.zxing_decode_succeeded) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();
            Bitmap barcode = null;
            float scaleFactor = 1.0f;
            if (bundle != null) {
                byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                if (compressedBitmap != null) {
                    barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                    // Mutable copy:
                    barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                }
                scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
            }
            handleDecode((Result) message.obj, barcode, scaleFactor);
        } else if (message.what == R.id.zxing_decode_failed) {
            // We're decoding as fast as possible, so when one decode fails, start another.
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);
        }
    }

    private void handleDecode(Result obj, Bitmap barcode, float scaleFactor) {
        ResultCallback resultCallback = qrManager.getResultCallback();
        if (resultCallback != null){
            resultCallback.onResult(obj, barcode, scaleFactor);
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.zxing_quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.zxing_decode_succeeded);
        removeMessages(R.id.zxing_decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);
        }
    }
}
