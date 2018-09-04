package com.jscheng.scamera.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
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

    public CameraSurfaceRender(Context context) {
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
        if (mCallback != null) {
            mCallback.onDraw();
        }
        if (mCallback != null && mRenderGroups.getByteBuffer() != null) {
            mCallback.onFrame(width, height, mRenderGroups.getByteBuffer());
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

    public interface CameraSufaceRenderCallback {
        void onRequestRender();
        void onCreate();
        void onChanged(int width, int height);
        void onDraw();
        void onFrame(int width, int height, ByteBuffer buffer);
    }
}
