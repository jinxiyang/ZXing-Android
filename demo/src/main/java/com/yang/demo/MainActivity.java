package com.yang.demo;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = getClass().getSimpleName();

    boolean hasSurface;
    private SurfaceView svPreview;

    private boolean previewing = false;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenPreview = (Button) findViewById(R.id.btn_open_preview);
        btnOpenPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "" + hasSurface, Toast.LENGTH_SHORT).show();
                if (previewing){
                    stopPreview();
                }else {
                    startPreview();
                }
            }
        });

        svPreview = (SurfaceView) findViewById(R.id.sv_preview);
        svPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                hasSurface = true;
                printDate();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                hasSurface = false;
                printDate();
            }
        });
    }

    private void startPreview() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Log.i(TAG, "startPreview: " + numberOfCameras);

        Camera.CameraInfo backCamera = null;
        int index = 0;
        while (index < numberOfCameras){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                backCamera = cameraInfo;
                break;
            }
            index++;
        }
        Log.i(TAG, "startPreview: " + index);


        if (index < numberOfCameras){
            camera = Camera.open(index);
            try {
                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
                if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0){
                    Camera.Size size = supportedPreviewSizes.get(0);
                    parameters.setPreviewSize(size.width, size.height);
                }

                camera.setParameters(parameters);
                camera.setDisplayOrientation(backCamera.orientation);
                camera.setPreviewDisplay(svPreview.getHolder());
                previewing = true;
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String string = format.format(new Date());
        Log.i(TAG, "printDate: " + string + "   " + hasSurface);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        stopPreview();
    }

    private void stopPreview() {
        if (previewing){
            camera.stopPreview();
            previewing = false;
            camera.release();
            camera = null;
        }
    }
}
