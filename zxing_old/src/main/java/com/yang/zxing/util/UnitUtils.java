package com.yang.zxing.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;


public class UnitUtils {
    private static DisplayMetrics displayMetrics = null;

    public UnitUtils() {
    }

    public static int dip2px(float value) {
        float scale = displayMetrics.density;
        return (int)(value * scale + 0.5F);
    }

    public static int px2dip(float value) {
        float scale = displayMetrics.density;
        return (int)(value / scale + 0.5F);
    }

    public static int sp2px(float value) {
        float fontScale = displayMetrics.scaledDensity;
        return (int)(value * fontScale + 0.5F);
    }

    public static int px2sp(float value) {
        float fontScale = displayMetrics.scaledDensity;
        return (int)(value / fontScale + 0.5F);
    }

    static {
        displayMetrics = Resources.getSystem().getDisplayMetrics();
    }
}
