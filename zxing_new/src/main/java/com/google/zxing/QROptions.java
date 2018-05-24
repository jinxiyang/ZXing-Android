package com.google.zxing;

import android.view.SurfaceHolder;

import java.util.Collection;
import java.util.Map;

/**
 * Author: 杨进玺
 * Time: 2018/5/24  16:21
 */
public class QROptions {

    SurfaceHolder surfaceHolder;
    IScanCodeView scanCodeView;
    ResultCallback resultCallback;
    ResultPointCallback resultPointCallback;
    boolean continuous;
    boolean decodeFullImage;


    Collection<BarcodeFormat> decodeFormats;
    Map<DecodeHintType, ?> baseHints;
    String characterSet;
}
