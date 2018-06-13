/*
 * Copyright (C) 2010 ZXing authors
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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.QRManager;
import com.google.zxing.R;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private CaptureHandler captureHandler;
    private MultiFormatReader multiFormatReader;
    private Rect framingRectInPreview;
    private boolean resultContainBitmap;

    private boolean running = true;

    DecodeHandler(CaptureHandler captureHandler,
                  Map<DecodeHintType, Object> hints,
                  Rect framingRectInPreview,
                  boolean resultContainBitmap) {
        this.captureHandler = captureHandler;
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.framingRectInPreview = framingRectInPreview;
        this.resultContainBitmap = resultContainBitmap;
    }

    @Override
    public void handleMessage(Message message) {
        if (message == null || !running) {
            return;
        }
        if (message.what == R.id.zxing_decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.zxing_quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();

        //一维码横向只能横向解析,预览方向和手机方向不一致时，需旋转图片解析
//        boolean isCameraPortrait = width < height;
//        boolean isScreenPortrait = framingRectInPreview.width() < framingRectInPreview.height();
//        if (isCameraPortrait != isScreenPortrait) {
//            byte[] rotatedData = new byte[data.length];
//            for (int y = 0; y < height; y++) {
//                for (int x = 0; x < width; x++)
//                    rotatedData[x * height + height - y - 1] = data[x + y * width];
//            }
//            int tmp = width;
//            width = height;
//            height = tmp;
//            data = rotatedData;
//        }

        Result rawResult = null;
        PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

        if (QRManager.DEBUG_RESULT){
            rawResult = new Result("debug 扫到了", null, null, null);
            QRManager.DEBUG_RESULT = false;
        }


        if (captureHandler != null){
            if (rawResult != null){
                Message message = Message.obtain(captureHandler, R.id.zxing_decode_succeeded, rawResult);
                if (resultContainBitmap){
                    Bundle bundle = new Bundle();
                    bundleThumbnail(source, bundle);
                    message.setData(bundle);
                }
                message.sendToTarget();
            }else {
                Message message = Message.obtain(captureHandler, R.id.zxing_decode_failed);
                message.sendToTarget();
            }
        }
        // Don't log the barcode contents for security.
        long end = System.currentTimeMillis();
        Log.d(TAG, "Found barcode in " + (end - start) + " ms");
    }

    private void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }


    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        // Go ahead and assume it's YUV rather than die.
        PlanarYUVLuminanceSource source = null;
        try {
            source = new PlanarYUVLuminanceSource(data, width, height, framingRectInPreview.left, framingRectInPreview.top, framingRectInPreview.width(), framingRectInPreview.height(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return source;
    }

}
