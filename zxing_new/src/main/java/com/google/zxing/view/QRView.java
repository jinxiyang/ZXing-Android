package com.google.zxing.view;

import android.graphics.Rect;


public interface QRView {
    void startAnim();
    void stopAnim();
    Rect getScanCodeRect();
}
