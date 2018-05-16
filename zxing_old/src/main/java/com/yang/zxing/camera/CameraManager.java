package com.yang.zxing.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * 此对象包装相机服务对象，并期望是唯一与它对话的对象。这个
 *实现封装了预览大小图像所需的步骤，这些步骤用于预览和解码。
 */
public final class CameraManager {

  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 720;
  private static final int MAX_FRAME_HEIGHT = 720;

  private static CameraManager cameraManager;

  static final int SDK_INT;
  static {
    int sdkInt;
    try {
      sdkInt = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException nfe) {
      sdkInt = 10000;
    }
    SDK_INT = sdkInt;
  }

  private final Context context;
  private final CameraConfigurationManager configManager;
  private Camera camera;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private final boolean useOneShotPreviewCallback;
  /**
   * 预览帧在这里传递，我们将其传递给已注册的处理程序。确保
   *清除处理程序，这样它只接收一条消息。
   */
  private final PreviewCallback previewCallback;
  /** 自动对焦回调到这里，并派出所要求的处理. */
  private final AutoFocusCallback autoFocusCallback;

  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  public static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {
    this.context = context;
    this.configManager = new CameraConfigurationManager(context);
    useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3;

    previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
    autoFocusCallback = new AutoFocusCallback();
  }

  /**
   * 打开相机驱动程序并初始化硬件参数。
   */
  public void openDriver(SurfaceHolder holder) throws IOException {
    if (camera == null) {
      camera = Camera.open();

      if (camera == null) {
        throw new IOException();
      }
      camera.setPreviewDisplay(holder);

      if (!initialized) {
        initialized = true;
        configManager.initFromCameraParameters(camera);
      }
      configManager.setDesiredCameraParameters(camera);
    }
  }

  /**
   * 关闭相机驱动器，如果仍在使用。
   */
  public void closeDriver() {
    if (camera != null) {
      camera.release();
      camera = null;
    }
  }

  /**
   *请求相机硬件开始将预览帧绘制到屏幕上。
   */
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  /**
   * 告诉照相机停止绘制预览帧。
   */
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      autoFocusCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  /**
   * 单个预览框架将返回给提供的处理程序。数据将以字节[ [] ]到达。
   *在message.obj场，宽度和高度编码为message.arg1和message.arg2，分别。
   */
  public void requestPreviewFrame(Handler handler, int message) {
    if (camera != null && previewing) {
      previewCallback.setHandler(handler, message);
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  /**
   * 要求相机硬件执行自动对焦。
   */
  public void requestAutoFocus(Handler handler, int message) {
    try{
      if (camera != null && previewing) {
        autoFocusCallback.setHandler(handler, message);
        camera.autoFocus(autoFocusCallback);
      }
    }catch (RuntimeException e){}
  }

  /**
   * 计算框架矩形的界面应显示用户的位置
   *条码。这个目标有助于对齐，也有助于用户按住设备。
   *足够远，以确保图像将成为焦点。
   */
  public Rect getFramingRect(int paddingTop) {
    Point screenResolution = configManager.getScreenResolution();
    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
      int width = screenResolution.x * 3 / 4;
      if (width < MIN_FRAME_WIDTH) {
        width = MIN_FRAME_WIDTH;
      } else if (width > MAX_FRAME_WIDTH) {
        width = MAX_FRAME_WIDTH;
      }
      int height = width*177/302;
      int leftOffset = (screenResolution.x - width) / 2;
      int topOffset = (screenResolution.y - height) / 2;
      if (paddingTop>0){
        framingRect = new Rect(leftOffset, paddingTop, leftOffset + width, paddingTop + height);
      }else {
        framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      }
    }
    return framingRect;
  }

  public Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      int paddingTop=300;
      if (context!=null){
        paddingTop=context.getResources().getDisplayMetrics().heightPixels/5;
      }
      Rect rect = new Rect(getFramingRect(paddingTop));
      Point cameraResolution = configManager.getCameraResolution();
      Point screenResolution = configManager.getScreenResolution();
      rect.left = rect.left * cameraResolution.y / screenResolution.x;
      rect.right = rect.right * cameraResolution.y / screenResolution.x;
      rect.top = rect.top * cameraResolution.x / screenResolution.y;
      rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
      framingRectInPreview = rect;
    }
    return framingRectInPreview;
  }

  /**
   * 一个构建基于格式适当的luminancesource对象工厂方法,预览缓存，被描述为camera.parameters。
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    int previewFormat = configManager.getPreviewFormat();
    String previewFormatString = configManager.getPreviewFormatString();
    switch (previewFormat) {
      // 这是所有设备都需要支持的标准Android格式。理论上，这是我们唯一应该关心的事情。
      case PixelFormat.YCbCr_420_SP:
      // 这种格式从未在野外看到过，但我们只关心它的兼容性。关于y通道，允许它。
      case PixelFormat.YCbCr_422_SP:
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
      default:
        // 三星时刻错误地使用了这个变体而不是“SP”版本。幸运的是，它的Y数据也在前面，所以我们可以阅读它。
        if ("yuv420p".equals(previewFormatString)) {
          return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
        }
    }
    throw new IllegalArgumentException("不支持的图片格式: " + previewFormat + '/' + previewFormatString);
  }

  public Context getContext() {
		return context;
	}

  public void openLight() {
    if (camera != null) {
      Camera.Parameters parameter = camera.getParameters();
      parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      camera.setParameters(parameter);
    }
  }

  public void offLight() {
    if (camera != null) {
      Camera.Parameters parameter = camera.getParameters();
      parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
      camera.setParameters(parameter);
    }
  }

}
