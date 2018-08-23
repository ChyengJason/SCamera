package com.jscheng.scamera.util;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created By Chengjunsen on 2018/8/23
 */
public class CameraUtil {
    private static Camera mCamera = null;
    private static int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static int mOrientation = 0;

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }

    public static void openCamera(Activity activity) {
        mCamera = Camera.open();
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        mOrientation = getCameraPreviewOrientation(activity, mCameraID);
        mCamera.setDisplayOrientation(mOrientation);
    }

    public static Camera getCamera() {
        return mCamera;
    }

    public static void startPreview(SurfaceHolder surfaceHolder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview(SurfaceTexture surfaceTexture) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public static void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public static int getCameraPreviewOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        //前置
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }
        //后置
        else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
}
