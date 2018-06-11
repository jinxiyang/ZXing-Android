package com.yang.demo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.QRManager;
import com.google.zxing.QRView;
import com.google.zxing.ResultCallback;

import java.io.IOException;

public class QRActivity extends AppCompatActivity {
    boolean hasSurface;
    private Button btnAction;
    private SurfaceView surfaceView;
    private QRManager qrManager;
    private QRView qrView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        surfaceView = (SurfaceView) findViewById(R.id.surface);

        btnAction = (Button) findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action();
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

        qrView = new QRView() {
            Rect rect;

            @Override
            public void startAnim() {
                Toast.makeText(QRActivity.this, "startAnim", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void stopAnim() {
                Toast.makeText(QRActivity.this, "stopAnim", Toast.LENGTH_SHORT).show();
            }

            @Override
            public Rect getScanCodeRect() {
                if (rect == null){
                    rect = new Rect(0, 0, 300, 400);
                }
                return rect;
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
            qrManager = QRManager.getInstance();
            try {
                qrManager.openCamera(this, surfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
