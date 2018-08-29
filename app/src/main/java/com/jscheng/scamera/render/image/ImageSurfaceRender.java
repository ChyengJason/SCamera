package com.jscheng.scamera.render.image;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import com.jscheng.scamera.render.DisplayRenderDrawer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageSurfaceRender implements GLSurfaceView.Renderer {
    private OriginalImageRenderDrawer mOriginalRender;
    private DisplayRenderDrawer mDisplayRender;

    public ImageSurfaceRender(Context context) {
        mDisplayRender = new DisplayRenderDrawer();
        mOriginalRender = new OriginalImageRenderDrawer(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mDisplayRender.create();
        mOriginalRender.create();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mOriginalRender.surfaceChangedSize(width, height);
        mDisplayRender.surfaceChangedSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mOriginalRender.draw();
        mDisplayRender.setInputTextureId(mOriginalRender.getOutputTextureId());
        mDisplayRender.draw();
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return new SurfaceTexture(mOriginalRender.getOutputTextureId());
    }
}
