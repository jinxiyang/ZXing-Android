package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Manages beeps
 */
public class BeepManager {

    private static final float BEEP_VOLUME = 1.0f;

    public static void playBeep(Context context, int rawId) {
        playBeep(context, rawId, BEEP_VOLUME, BEEP_VOLUME);
    }

    public static void playBeep(Context context, int rawId, float leftVolume, float rightVolume) {
        if (context == null || !shouldBeep(context)) {
            return;
        }
        if (context instanceof Activity) {
            ((Activity) context).setVolumeControlStream(AudioManager.STREAM_SYSTEM);
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor file = context.getResources().openRawResourceFd(rawId);
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(leftVolume, rightVolume);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer.release();
        }
    }

    private static boolean shouldBeep(Context context){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }
}
