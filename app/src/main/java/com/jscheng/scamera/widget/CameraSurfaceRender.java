package com.jscheng.scamera.widget;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class CameraSurfaceRender implements GLSurfaceView.Renderer {

    private BaseRenderDrawer mTextureDrawer;
    private CameraSufaceRenderCallback mCallback;
    private SurfaceTexture mCameraTexture;
    private int mCameraTextureId;

    public CameraSurfaceRender(BaseRenderDrawer mTextureDrawer) {
        this.mTextureDrawer = mTextureDrawer;
        mCameraTextureId = createCameraTexture();
        mCameraTexture = new SurfaceTexture(mCameraTextureId);
        mTextureDrawer.setInputTextureId(mCameraTextureId);
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
        mTextureDrawer.create();
        if (mCallback != null) {
            mCallback.onCreate(mCameraTexture);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
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
        mTextureDrawer.draw();
        if (mCallback != null) {
            mCallback.onDraw();
        }
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraTexture;
    }

    private int createCameraTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return texture[0];
    }

    public void setBackCamera(boolean isBackCamera) {
        mTextureDrawer.setBackCamera(isBackCamera);
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
