package com.jscheng.scamera.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.jscheng.scamera.render.image.WaterMarkRenderDrawer;
import com.jscheng.scamera.util.GlesUtil;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class CameraSurfaceRender implements GLSurfaceView.Renderer {

    private CameraSufaceRenderCallback mCallback;
    private RenderDrawerGroups mRenderGroups;
    private int width, height;
    private int mCameraTextureId;
    private SurfaceTexture mCameraTexture;
    private boolean takeFrameBack;
    private ByteBuffer mFrameDataBuffer;

    public CameraSurfaceRender(Context context) {
        this.takeFrameBack = false;
        this.mRenderGroups = new RenderDrawerGroups();
        mRenderGroups.addRenderDrawer(new OriginalRenderDrawer());
        mRenderGroups.addRenderDrawer(new WaterMarkRenderDrawer(context));
        mRenderGroups.addRenderDrawer(new DisplayRenderDrawer());
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraTextureId = GlesUtil.createCameraTexture();
        mRenderGroups.setInputTexture(mCameraTextureId);
        mRenderGroups.create();
        initCameraTexture();
        if (mCallback != null) {
            mCallback.onCreate();
        }
    }

    public void initCameraTexture() {
        mCameraTexture = new SurfaceTexture(mCameraTextureId);
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
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        this.width = width;
        this.height = height;
        mFrameDataBuffer = ByteBuffer.allocate(width * height * 4);
        mRenderGroups.surfaceChangedSize(width, height);
        if (mCallback != null) {
            mCallback.onChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (mCameraTexture != null) {
            mCameraTexture.updateTexImage();
        }
        mRenderGroups.draw();
        if (takeFrameBack) {
            mFrameDataBuffer.clear();
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mFrameDataBuffer);
            if (mCallback != null) {
                mCallback.onFrameDataBack(width, height, mFrameDataBuffer);
            }
        }
        if (mCallback != null) {
            mCallback.onDraw();
        }
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraTexture;
    }

    public void setCallback(CameraSufaceRenderCallback mCallback) {
        this.mCallback = mCallback;
    }

    public void releaseSurfaceTexture() {
        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
    }

    public void resumeSurfaceTexture() {
        initCameraTexture();
    }

    public void setFrameDataBack(boolean takeFrameBack) {
        this.takeFrameBack = takeFrameBack;
    }

    public interface CameraSufaceRenderCallback {
        void onRequestRender();
        void onCreate();
        void onChanged(int width, int height);
        void onDraw();
        void onFrameDataBack(int width, int height, ByteBuffer mBuffer); // 不要在这里做耗时操作
    }
}
