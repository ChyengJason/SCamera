package com.jscheng.scamera.widget.image;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import com.jscheng.scamera.widget.BaseRenderDrawer;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageSurfaceRender implements GLSurfaceView.Renderer {
    private OriginalImageRenderDrawer mOriginalRender;
    private FliterImageRenderDrawer mFliterRender;

    public ImageSurfaceRender(Context context) {
        mFliterRender = new FliterImageRenderDrawer(context);
        mOriginalRender = new OriginalImageRenderDrawer(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mFliterRender.create();
        mOriginalRender.create();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mOriginalRender.surfaceChangedSize(width, height);
        mFliterRender.surfaceChangedSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
//        mFliterRender.bindFrameBuffer();
        mOriginalRender.draw();
//        mFliterRender.unBindFrameBuffer();
//        mFliterRender.draw();
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return new SurfaceTexture(mOriginalRender.getOutputTextureId());
    }
}
