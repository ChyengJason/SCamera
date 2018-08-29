package com.jscheng.scamera.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class CameraSurfaceRender implements GLSurfaceView.Renderer {

    private OriginalRenderDrawer mCameraDrawer;
    private DisplayRenderDrawer mTextureDrawer;
    private CameraSufaceRenderCallback mCallback;

    public CameraSurfaceRender() {
        this.mCameraDrawer = new OriginalRenderDrawer();
        this.mTextureDrawer = new DisplayRenderDrawer();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraDrawer.create();
        mTextureDrawer.create();
        if (getCameraSurfaceTexture() != null) {
            getCameraSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    if (mCallback != null) {
                        mCallback.onRequestRender();
                    }
                }
            });
        }
        if (mCallback != null) {
            mCallback.onCreate();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mCameraDrawer.surfaceChangedSize(width, height);
        mTextureDrawer.surfaceChangedSize(width, height);
        if (mCallback != null) {
            mCallback.onChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mCameraDrawer.draw();
        mTextureDrawer.setInputTextureId(mCameraDrawer.getOutputTextureId());
        mTextureDrawer.draw();

        if (mCallback != null) {
            mCallback.onDraw();
        }
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraDrawer.getCameraTexture();
    }

    public void setBackCamera(boolean isBackCamera) {
        mCameraDrawer.setBackCamera(isBackCamera);
    }

    public void setCallback(CameraSufaceRenderCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface CameraSufaceRenderCallback {
        void onRequestRender();
        void onCreate();
        void onChanged(int width, int height);
        void onDraw();
    }
}
