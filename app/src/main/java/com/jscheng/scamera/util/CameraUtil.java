package com.jscheng.scamera.util;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
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
import java.util.List;

/**
 * Created By Chengjunsen on 2018/8/23
 */
public class CameraUtil {
    private static final String TAG = CameraUtil.class.getSimpleName();
    private static Camera mCamera = null;
    private static int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static int mOrientation = 0;
    private static boolean isFocusing = false;

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }

    public static void openCamera() {
        mCamera = Camera.open();
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
    }

    public static Camera getCamera() {
        return mCamera;
    }

    public static void startPreview(Activity activity, SurfaceHolder surfaceHolder, int width, int height) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(surfaceHolder);
                startPreview(activity, width, height);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview(Activity activity, SurfaceTexture surfaceTexture, int width, int height) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewTexture(surfaceTexture);
                startPreview(activity, width, height);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview(Activity activity, int width, int height) {
        if (mCamera != null) {
            mOrientation = getCameraPreviewOrientation(activity, mCameraID);
            mCamera.setDisplayOrientation(mOrientation);
            Camera.Size bestSize = getPreviewOptimalSize(width, height);
            mCamera.getParameters().setPreviewSize(bestSize.width, bestSize.height);
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

    public static Camera.Size getPreviewOptimalSize(int width, int height) {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        return getOptimalSize(mCamera.getParameters().getSupportedPictureSizes(), width, height);
    }

    public static Camera.Size getOptimalSize(List<Camera.Size> supportSizes, int width, int height) {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        Camera.Size bestSize = supportSizes.get(0);
        int largestArea = bestSize.width * bestSize.height;
        for (Camera.Size s : supportSizes) {
            int area = s.width * s.height;
            if (area > largestArea) {
                bestSize = s;
                largestArea = area;
            }
        }
        return bestSize;
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

    public static boolean newCameraFocus(Point focusPoint, Size cameraSize, Camera.AutoFocusCallback callback) {
        if (mCamera == null) {
            throw new RuntimeException("mCamera is null");
        }
        Point cameraFoucusPoint = convertToCameraPoint(cameraSize, focusPoint);
        Rect cameraFoucusRect = convertToCameraRect(cameraFoucusPoint, 100);
        Log.e(TAG, "newCameraFocus: x: " + cameraFoucusRect.centerX() + ", y: " + cameraFoucusRect.centerY());
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
     * @param cameraViewSize
     * @param focusPoint
     * @return cameraPoint
     */
    private static Point convertToCameraPoint(Size cameraViewSize, Point focusPoint){
        int newX = focusPoint.y * 2000/cameraViewSize.getHeight() - 1000;
        int newY = -focusPoint.x * 2000/cameraViewSize.getWidth() + 1000;
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
        if (s > max) return max;
        if (s < min) return min;
        return s;
    }

    private static int getRotation(Activity activity) {
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
