package com.jscheng.scamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created By Chengjunsen on 2018/8/25
 */
public class CameraGLSurfaceView extends GLSurfaceView implements CameraSurfaceRender.CameraSufaceRenderCallback{

    private CameraSurfaceRender mRender;
    private CameraGLSurfaceViewCallback mCallback;

    public CameraGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mRender = new CameraSurfaceRender(new TextureRenderDrawer());
        mRender.setCallback(this);
        mRender.setBackCamera(true);
        setRenderer(mRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getCameraSurfaceTexture();
    }

    @Override
    public void onRequestRender() {
        requestRender();
    }

    @Override
    public void onCreate(SurfaceTexture texture) {
        if (mCallback != null) {
            mCallback.onSurfaceViewCreate(texture);
        }
    }

    @Override
    public void onChanged(int width, int height) {
        if (mCallback != null) {
            mCallback.onSurfaceViewChange(width, height);
        }
    }

    @Override
    public void onDraw() {

    }

    public void setCallback(CameraGLSurfaceViewCallback mCallback) {
        this.mCallback = mCallback;
    }

    public void setBackCamera(boolean isBackCamera) {
        this.mRender.setBackCamera(isBackCamera);
    }

    public interface CameraGLSurfaceViewCallback {
        void onSurfaceViewCreate(SurfaceTexture texture);
        void onSurfaceViewChange(int width, int height);
    }
}