package com.yang.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.QRManager;
import com.google.zxing.Result;
import com.google.zxing.decode.ResultCallback;
import com.google.zxing.view.QRViewImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QRActivity extends AppCompatActivity {
    boolean hasSurface;
    private Button btnAction;
    private Button btnBitmap;
    private SurfaceView surfaceView;
    private QRManager qrManager;
    private QRViewImpl qrView;
    private ResultCallback resultCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        surfaceView = (SurfaceView) findViewById(R.id.surface);

        qrView = (QRViewImpl) findViewById(R.id.qr_view);
        btnAction = (Button)findViewById(R.id.btn_action);
        btnBitmap = (Button)findViewById(R.id.btn_bitmap);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action();
            }
        });

        btnBitmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRManager.DEBUG_RESULT = true;

//                qrManager.oneShotPreview(new Camera.PreviewCallback() {
//                    @Override
//                    public void onPreviewFrame(byte[] data, Camera camera) {
//                        Point previewSize = qrManager.getPreviewSize();
//                        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, previewSize.x, previewSize.y, null);
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.x, previewSize.y), 80, baos); //这里 80 是图片质量,取值范围 0-100,100为品质最高
//                        data = baos.toByteArray();//这时候 bmp 就不为 null 了
//                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//                        qrView.drawResultBitmap(bmp);
//                    }
//                });
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                hasSurface = true;
                openCamera();
                btnAction.setText("开启扫描");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                hasSurface = false;
            }
        });

        resultCallback = new ResultCallback() {

            @Override
            public void onResult(Result rawResult, Bitmap barcode, float scaleFactor) {
                Toast.makeText(QRActivity.this, rawResult.getText(), Toast.LENGTH_SHORT).show();
                qrView.drawResultBitmap(barcode);
                btnAction.setText("开启扫描");
            }
        };
    }

    private void action() {
        String text = btnAction.getText().toString();
        if ("开启相机".equals(text)){
            text = "开启扫描";
            openCamera();
        }else if ("开启扫描".equals(text)){
            text = "开启闪光灯";
            qrManager.setQrView(qrView);
            qrManager.setResultCallback(resultCallback);
            qrManager.startScan();
        }else if ("开启闪光灯".equals(text)){
            text = "关闭闪光灯";
            qrManager.flashlight(true);
        }else if ("关闭闪光灯".equals(text)){
            text = "关闭扫描";
            qrManager.flashlight(false);
        }else if ("关闭扫描".equals(text)){
            text = "关闭相机";
            qrManager.stopScan();
        }else if ("关闭相机".equals(text)){
            text = "开启相机";
            qrManager.closeCamera();
        }
        btnAction.setText(text);
    }

    private void openCamera() {
        if (hasSurface){
            qrManager = new QRManager();
            try {
                qrManager.openCamera(this, surfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
