package com.jscheng.scamera.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import com.jscheng.scamera.util.GlesUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class CameraSurfaceRender implements GLSurfaceView.Renderer {

    private OriginalRenderDrawer mCameraDrawer;
    private DisplayRenderDrawer mTextureDrawer;
    private CameraSufaceRenderCallback mCallback;
    private SurfaceTexture mCameraTexture;
    private int mCameraTextureId;

    public CameraSurfaceRender() {
        this.mCameraDrawer = new OriginalRenderDrawer();
        this.mTextureDrawer = new DisplayRenderDrawer();
        mCameraTextureId = GlesUtil.createCameraTexture();

        mCameraTexture = new SurfaceTexture(mCameraTextureId);
        mCameraDrawer.setInputTextureId(mCameraTextureId);
        mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mCallback != null) {
                    mCallback.onRequestRender();
                }
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraDrawer.create();
        mTextureDrawer.create();
        if (mCallback != null) {
            mCallback.onCreate(mCameraTexture);
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
        if (mCameraTexture != null) {
            mCameraTexture.updateTexImage();
//            mCameraTexture.getTransformMatrix();
        }

        mCameraDrawer.bindFrameBuffer();
        mCameraDrawer.draw();
        mCameraDrawer.unBindFrameBuffer();
        mTextureDrawer.setInputTextureId(mCameraDrawer.getOutputTextureId());
        mTextureDrawer.draw();

        if (mCallback != null) {
            mCallback.onDraw();
        }
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraTexture;
    }

    public void setBackCamera(boolean isBackCamera) {
        mCameraDrawer.setBackCamera(isBackCamera);
    }

    public void setCallback(CameraSufaceRenderCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface CameraSufaceRenderCallback {
        void onRequestRender();
        void onCreate(SurfaceTexture texture);
        void onChanged(int width, int height);
        void onDraw();
    }
}
