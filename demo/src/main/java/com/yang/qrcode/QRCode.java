package com.yang.qrcode;

import android.graphics.Bitmap;
import android.graphics.Camera;

/**
 * Author: 杨进玺
 * Time: 2018/5/10  16:18
 */
public class QRCode {
    private Camera camera;

    public void startScan(){

    }

    public void stopScan(){

    }

    public void openFlashlight() {

    }

    public void closeFlashlight() {

    }

    public void scanBitmap(Bitmap bitmap){

    }

    public void scan(String uri){

    }


    public static class Builder{

        private Params params;

        public Builder setParams(Params params) {
            this.params = params;
            return this;
        }

        public QRCode build(){
            return new QRCode();
        }
    }


    public static class Params {
        private boolean autoFocus;
        private long autoFocusIntervalMs;




    }
}
