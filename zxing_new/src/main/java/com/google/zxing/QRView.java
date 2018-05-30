package com.google.zxing;

import android.graphics.Rect;

/**
 * Author: 杨进玺
 * Time: 2018/5/21  13:57
 */
public interface QRView {
    void startAnim();
    void stopAnim();
    void addPossibleResultPoint(ResultPoint point);
    Rect getScanCodeRect();
}
