package com.jscheng.scamera.widget.image;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import com.jscheng.scamera.widget.BaseRenderDrawer;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageSurfaceRender implements GLSurfaceView.Renderer {
    private BaseRenderDrawer mOriginalRender;

    public ImageSurfaceRender(Context context) {
        mOriginalRender = new OriginalImageRenderDrawer(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mOriginalRender.create();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mOriginalRender.surfaceChangedSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mOriginalRender.draw();
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return new SurfaceTexture(mOriginalRender.getOutputTextureId());
    }
}
