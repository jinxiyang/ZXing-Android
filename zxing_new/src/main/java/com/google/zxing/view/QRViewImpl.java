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

package com.google.zxing.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.List;


public final class QRViewImpl extends View implements QRView{
    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private final Paint paint;
    private final int maskColor;
    private final int resultColor;
    private Bitmap resultBitmap;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    private Rect framingRect;

    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private int requestedFramingRectMatginTop;

    // This constructor is used when the class is built from an XML resource.
    public QRViewImpl(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskColor = Color.parseColor("#60000000");
        resultColor = Color.parseColor("#b0000000");
        laserColor = Color.parseColor("#ffcc0000");
        resultPointColor = Color.parseColor("#c0ffbd21");
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    private void initFramingRect(int width, int height) {
        if (width == 0 || height == 0){
            Point displaySize = new Point();
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getSize(displaySize);
            width = displaySize.x;
            height = displaySize.y;
        }

        int framingWidth;
        int framingHeight;

        if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0){
            framingWidth = requestedFramingRectWidth;
            framingHeight = requestedFramingRectHeight;
        }else {
            framingWidth = findDesiredDimensionInRange(width, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            framingHeight = findDesiredDimensionInRange(height, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
        }
        int leftOffset = (width - framingWidth) / 2;
        int topOffset = requestedFramingRectMatginTop > 0 ? requestedFramingRectMatginTop : (height - framingHeight) / 2;
        framingRect = new Rect(leftOffset, topOffset, leftOffset + framingWidth, topOffset + framingHeight);
        requestedFramingRectWidth = 0;
        requestedFramingRectHeight = 0;
        requestedFramingRectMatginTop = 0;
    }

    private int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (framingRect == null || (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) || requestedFramingRectMatginTop > 0){
            initFramingRect(0, 0);
        }

        Rect frame = framingRect;

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            Rect rect = new Rect(0, 0, resultBitmap.getWidth(), resultBitmap.getHeight());
            canvas.drawBitmap(resultBitmap, null, rect, paint);
        } else {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    public void setFramingRect(int width, int height) {
        requestedFramingRectWidth = width;
        requestedFramingRectHeight = height;
        postInvalidate();
    }

    public void setFramingRect(int width, int height, int marginTop) {
        requestedFramingRectWidth = width;
        requestedFramingRectHeight = height;
        requestedFramingRectMatginTop = marginTop;
        postInvalidate();
    }

    public void setFramingRectMatginTop(int marginTop) {
        requestedFramingRectMatginTop = marginTop;
        postInvalidate();
    }

    @Override
    public void startAnim() {
        Toast.makeText(getContext(), "startAnim", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void stopAnim() {
        Toast.makeText(getContext(), "stopAnim", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Rect getScanCodeRect() {
        if (framingRect == null){
            initFramingRect(0, 0);
        }
        return framingRect;
    }
}
