package com.jscheng.scamera.util;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created By Chengjunsen on 2018/8/23
 */
public class CameraUtil {
    private static final String TAG = CameraUtil.class.getSimpleName();
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

    public static void openCamera() {
        mCamera = Camera.open(mCameraID);
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
    }

    public static Camera getCamera() {
        return mCamera;
    }

    public static void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public static void switchCamera(Activity activity, boolean isBackCamera, SurfaceTexture texture) {
        if (mCamera != null) {
            releaseCamera();
            mCameraID = isBackCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
            openCamera();
            startPreview(activity, texture);
        }
    }

    public static void switchCamera(Activity activity, boolean isBackCamera, SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            releaseCamera();
            mCameraID = isBackCamera ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
            openCamera();
            startPreview(activity, surfaceHolder);
        }
    }

    public static boolean isBackCamera() {
        return mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public static void startPreview(Activity activity, SurfaceHolder surfaceHolder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(surfaceHolder);
                startPreview(activity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview(Activity activity, SurfaceTexture surfaceTexture) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(surfaceTexture);
                startPreview(activity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview(Activity activity) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            mOrientation = getCameraPreviewOrientation(activity, mCameraID);
            Size bestSize = getOptimalSize(parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(bestSize.getWidth(), bestSize.getHeight());
            //parameters.setPictureSize(bestSize.getWidth(), bestSize.getHeight());
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(mOrientation);
            mCamera.startPreview();
        }
    }

    private static Size getOptimalSize(List<Camera.Size> supportList) {
        int width = 0;
        int height = 0;
        Iterator<Camera.Size> itor = supportList.iterator();
        while (itor.hasNext()) {
            Camera.Size cur = itor.next();
            if (cur.width >= width && cur.height >= height) {
                width = cur.width;
                height = cur.height;
            }
        }
        return new Size(width, height);
    }

    public static int getCameraPreviewOrientation(Activity activity, int cameraId) {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        int degrees = getRotation(activity);
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

    public static boolean newCameraFocus(Point focusPoint, Size screenSize, Camera.AutoFocusCallback callback) {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        Point cameraFoucusPoint = convertToCameraPoint(screenSize, focusPoint);
        Rect cameraFoucusRect = convertToCameraRect(cameraFoucusPoint, 100);
        Camera.Parameters parameters = mCamera.getParameters();
        if (Build.VERSION.SDK_INT > 14) {
            if (parameters.getMaxNumFocusAreas() <= 0) {
                return focus(callback);
            }
            clearCameraFocus();
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(new Camera.Area(cameraFoucusRect, 100));
            parameters.setFocusAreas(focusAreas);
            // 设置感光区域
            parameters.setMeteringAreas(focusAreas);
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } 
        }
        return focus(callback);
    }

    private static boolean focus(Camera.AutoFocusCallback callback) {
        if (mCamera == null) {
            return false;
        }
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(callback);
        return true;
    }

    /**
     * 清除焦点
     */
    public static void clearCameraFocus() {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        mCamera.cancelAutoFocus();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusAreas(null);
        parameters.setMeteringAreas(null);
        try {
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将屏幕坐标转换成camera坐标
     * @param screenSize
     * @param focusPoint
     * @return cameraPoint
     */
    private static Point convertToCameraPoint(Size screenSize, Point focusPoint){
        int newX = focusPoint.y * 2000/screenSize.getHeight() - 1000;
        int newY = -focusPoint.x * 2000/screenSize.getWidth() + 1000;
        return new Point(newX, newY);
    }

    private static Rect convertToCameraRect(Point centerPoint, int radius) {
        int left = limit(centerPoint.x - radius, 1000, -1000);
        int right = limit(centerPoint.x + radius, 1000, -1000);
        int top = limit(centerPoint.y - radius, 1000, -1000);
        int bottom = limit(centerPoint.y + radius, 1000, -1000);
        return new Rect(left, top, right, bottom);
    }

    private static int limit(int s, int max, int min) {
        if (s > max) { return max; }
        if (s < min) { return min; }
        return s;
    }

    public static int getRotation(Activity activity) {
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
        return degrees;
    }
}
