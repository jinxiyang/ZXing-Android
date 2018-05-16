package com.yang.zxing.decoding;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.yang.zxing.ActivityScanCodeCallback;
import com.yang.zxing.R;
import com.yang.zxing.camera.CameraManager;
import com.yang.zxing.view.ViewfinderResultPointCallback;

import java.util.Vector;


/**
 *这个类处理所有的消息，包括为捕获的状态机。
 */
public final class CaptureActivityHandler extends Handler {

  private final ActivityScanCodeCallback callback;
  private final DecodeThread decodeThread;
  private State state;

  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  public CaptureActivityHandler(ActivityScanCodeCallback callback, Vector<BarcodeFormat> decodeFormats,
                                String characterSet) {
    this.callback = callback;
    decodeThread = new DecodeThread(callback, decodeFormats, characterSet,
            new ViewfinderResultPointCallback(callback.getViewfinderView()));
    decodeThread.start();
    state = State.SUCCESS;
    // 开始捕捉预览和解码。
    CameraManager.get().startPreview();
    restartPreviewAndDecode();
  }

  @Override
  public void handleMessage(Message message) {
    if (message.what == R.id.auto_focus) {
      // 当一个自动对焦结束时，开始另一个。这是最接近的事情。连续AF。它看起来有点像打猎，但我不知道还能做什么。
      if (state == State.PREVIEW) {
        CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
      }

    } else if (message.what == R.id.restart_preview) {
      restartPreviewAndDecode();

    } else if (message.what == R.id.decode_succeeded) {
      state = State.SUCCESS;
      Bundle bundle = message.getData();

      Bitmap barcode = bundle == null ? null :
              (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);

      callback.handleDecode((Result) message.obj, barcode);

    } else if (message.what == R.id.decode_failed) {
      state = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);

    } else if (message.what == R.id.return_scan_result) {
      callback.getScanCodeActivity().setResult(Activity.RESULT_OK, (Intent) message.obj);
      callback.getScanCodeActivity().finish();

    } else if (message.what == R.id.launch_product_query) {
      String url = (String) message.obj;
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      callback.getScanCodeActivity().startActivity(intent);
    }
  }

  public void quitSynchronously() {
    state = State.DONE;
    CameraManager.get().stopPreview();
    Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
    quit.sendToTarget();
    try {
      decodeThread.join();
    } catch (InterruptedException e) {
    }

    // 绝对肯定我们不会发送任何排队的消息。
    removeMessages(R.id.decode_succeeded);
    removeMessages(R.id.decode_failed);
  }

  public void restartPreviewAndDecode() {
    if (state == State.SUCCESS) {
      state = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
      CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
    }
  }

}
