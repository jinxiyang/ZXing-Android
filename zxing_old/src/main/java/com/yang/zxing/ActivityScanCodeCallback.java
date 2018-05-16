package com.yang.zxing;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.yang.zxing.view.ViewfinderView;


public interface ActivityScanCodeCallback {

    public Activity getScanCodeActivity();
    public ViewfinderView getViewfinderView();
    public Handler getHandler();

    //处理扫描结果
    public void handleDecode(Result result, Bitmap barcode);

}
