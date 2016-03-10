package com.cobayasu.memorycapture;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by cobayasu on 2/19/16.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{

    private Camera mCam;

    public CameraPreview(Context context, Camera camera) {
        super(context);

        mCam = camera;

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCam.setPreviewDisplay(holder);
            mCam.startPreview();
        } catch (Exception e) {

        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
