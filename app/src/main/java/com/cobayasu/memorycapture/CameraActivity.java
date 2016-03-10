package com.cobayasu.memorycapture;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.Image;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class CameraActivity extends Activity {

    //Layout parameter
    private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
    //Camera instance
    private Camera mCam = null;
    //Camera preview
    private CameraPreview mCameraPreview = null;
    //Guide image instance
    private Bitmap guideBmp;
    private ImageView guideImg;
    //Transparent ratio
    private final int TRANSPARENT_RATIO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Get camera instance
        try {
            int cameraId = 0;
            mCam = Camera.open(cameraId);


        } catch (Exception e) {
            this.finish();
        }

        //Set CameraPreview to FrameLayout
        mCameraPreview = new CameraPreview(this, mCam);
        RelativeLayout cameraPreview = (RelativeLayout)findViewById(R.id.cameraPreview);
        cameraPreview.addView(mCameraPreview);

        //Double guide image with tranceparency
        //TODO prevent OutOfMemoryError in using capture jpg for guide
        ImageView guideImage = new ImageView(this);
        guideImage.setImageResource(R.drawable.ic_launcher);
        guideImage.setAlpha(70);

        //TODO setting layout xml
        RelativeLayout overlay = (RelativeLayout)findViewById(R.id.guideOverlay);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WC, WC);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        overlay.addView(guideImage, params);


        //Add touch event
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mCam.takePicture(null, null, pictureListener);
                }
                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Release Camera instance after destroying
        if (mCam != null) {
            mCam.release();
            mCam = null;
        }
    }

    //Callback for pressing shutter
    private Camera.ShutterCallback shutterListener = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
        }
    };

    private Camera.PictureCallback pictureListener = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data != null) {

                String saveDir = Environment.getExternalStorageDirectory().getPath() + "/memory_capture_test";

                //Create folder on SD card
                File file = new File(saveDir);

                if (!file.exists()) {
                    if (!file.mkdir()) {
                        Log.e("MemoryCapture", "Make Dir Error");
                    }
                }

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String imgPath = saveDir + "/" + sf.format(cal.getTime()) +  ".jpg";

                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(imgPath, true);
                    fos.write(data);
                    fos.close();

                    registerAndroidDB(imgPath);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fos = null;
                }

                camera.startPreview();
            }
        }
    };

    //Register pictures into Android DB
    private void registerAndroidDB(String path) {
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = CameraActivity.this.getContentResolver();
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put("_data", path);
        contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    //Get guide image that overlays preview
    private Bitmap getGuideImg() {
        Bitmap srcBmp = BitmapFactory.decodeResource(getResources(), R.drawable.sample);
        if (!srcBmp.isMutable()) {
            srcBmp = srcBmp.copy(Bitmap.Config.ALPHA_8, true);
        }

        int width = srcBmp.getWidth();
        int height = srcBmp.getHeight();

        int[] pixels = new int[width * height];
        srcBmp.getPixels(pixels, 0 ,width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //Get transparent ratio from pixel info
                int alpha = pixels[x + y * width];
                alpha = alpha >>> 24;

                if (alpha > TRANSPARENT_RATIO) {
                    alpha -= 100;
                } else {
                    alpha = 0;
                }
                alpha = alpha << 24;

                //Delete transparent ratio from pixel info

                int color = pixels[x + y * width];
                color = color << 8;
                color = color >>> 8;

                //Merge transparent ratio with color into pixel info
                pixels[x + y * width] = alpha ^ color;
            }
        }
        srcBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return srcBmp;
    }
}
