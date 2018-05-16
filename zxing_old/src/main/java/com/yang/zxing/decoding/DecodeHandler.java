package com.yang.zxing.decoding;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.yang.zxing.ActivityScanCodeCallback;
import com.yang.zxing.R;
import com.yang.zxing.camera.CameraManager;
import com.yang.zxing.camera.PlanarYUVLuminanceSource;

import java.util.Hashtable;


final class DecodeHandler extends Handler {

  private final ActivityScanCodeCallback callback;
  private final MultiFormatReader multiFormatReader;

  DecodeHandler(ActivityScanCodeCallback callback, Hashtable<DecodeHintType, Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.callback = callback;
  }

  @Override
  public void handleMessage(Message message) {
    if (message.what == R.id.decode) {
      decode((byte[]) message.obj, message.arg1, message.arg2);
    } else if (message.what == R.id.quit) {
      Looper.myLooper().quit();
    }
  }

  /**
   * 解码取景器矩形内的数据，并计算时间。效率，将相同的读取器对象重用到一个解码到另一个解码。
   *
   * @param data   YUV预览框。
   * @param width
   * @param height
   */
  private void decode(byte[] data, int width, int height) {
    Result rawResult = null;
    
    byte[] rotatedData = new byte[data.length];
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++)
            rotatedData[x * height + height - y - 1] = data[x + y * width];
    }
    int tmp = width;
    width = height;
    height = tmp;

    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(rotatedData, width, height);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    try {
      rawResult = multiFormatReader.decodeWithState(bitmap);
    } catch (ReaderException re) {
    } finally {
      multiFormatReader.reset();
    }

    if (rawResult != null) {
      Message message = Message.obtain(callback.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      message.sendToTarget();
    } else {
      Message message = Message.obtain(callback.getHandler(), R.id.decode_failed);
      message.sendToTarget();
    }
  }

}
