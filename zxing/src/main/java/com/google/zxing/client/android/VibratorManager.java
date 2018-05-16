package com.google.zxing.client.android;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Manages Vibrator
 *
 * need permission : android.Manifest.permission.VIBRATE
 */
public class VibratorManager {

    private static final long VIBRATE_DURATION = 200L;

    public static void vibrate(Context context, long duration){
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect vibrationEffect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(vibrationEffect);
        } else {
            vibrator.vibrate(duration);
        }
    }

    public static void vibrate(Context context){
        vibrate(context, VIBRATE_DURATION);
    }
}
