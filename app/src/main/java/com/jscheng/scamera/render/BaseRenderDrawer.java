package com.jscheng.scamera.render;

import android.opengl.GLES30;

import com.jscheng.scamera.util.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public abstract class BaseRenderDrawer {
    protected int width;

    protected int height;

    protected int mProgram;

    //顶点坐标 Buffer
    protected FloatBuffer mVertexBuffer;

    //纹理坐标 Buffer
    protected FloatBuffer mFrontTextureBuffer;

    //纹理坐标 Buffer
    protected FloatBuffer mBackTextureBuffer;

    protected FloatBuffer mDisplayTextureBuffer;

    protected FloatBuffer mFrameTextureBuffer;

    protected float vertexData[] = {
            -1f, -1f,// 左下角
            1f, -1f, // 右下角
            -1f, 1f, // 左上角
            1f, 1f,  // 右上角
    };

    protected float frontTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };

    protected float backTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f  // 右上角
    };

    protected float displayTextureData[] = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
    };

    protected float frameBufferData[] = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };

    protected final int CoordsPerVertexCount = 2;

    protected final int VertexCount = vertexData.length / CoordsPerVertexCount;

    protected final int VertexStride = CoordsPerVertexCount * 4;

    protected final int CoordsPerTextureCount = 2;

    protected final int TextureStride = CoordsPerTextureCount * 4;

    public BaseRenderDrawer() {
        initBuffer();
    }

    public void create() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        onCreated();
    }

    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
        onChanged(width, height);
    }

    public void draw(){
        clear();
        useProgram();
        viewPort(0, 0, width, height);
        onDraw();
    }

    protected void clear(){
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

//    protected void bindTexture(int type){
//        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + type);
//        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D , mTextureId);
//        GLES30.glUniform1i(mHTexture, type);
//    }

    protected void initBuffer() {
        this.mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        this.mVertexBuffer.position(0);

        this.mBackTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        this.mBackTextureBuffer.position(0);

        this.mFrontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        this.mFrontTextureBuffer.position(0);

        this.mDisplayTextureBuffer = ByteBuffer.allocateDirect(displayTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(displayTextureData);
        this.mDisplayTextureBuffer.position(0);

        this.mFrameTextureBuffer = ByteBuffer.allocateDirect(frameBufferData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frameBufferData);
        this.mFrameTextureBuffer.position(0);
    }

    protected void useProgram(){
        GLES30.glUseProgram(mProgram);
    }

    protected void viewPort(int x, int y, int width, int height) {
        GLES30.glViewport(x, y, width,  height);
    }

//    protected void draw(){
//        GLES30.glEnableVertexAttribArray(mHPosition);
//        GLES30.glVertexAttribPointer(mHPosition,2, GLES30.GL_FLOAT, false, 0, mVerBuffer);
//        GLES30.glEnableVertexAttribArray(mHCoord);
//        GLES30.glVertexAttribPointer(mHCoord, 2, GLES30.GL_FLOAT, false, 0, mTexBuffer);
//        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,);
//        GLES30.glDisableVertexAttribArray(mHPosition);
//        GLES30.glDisableVertexAttribArray(mHCoord);
//    }



    public abstract void setInputTextureId(int textureId);

    public abstract int getOutputTextureId();

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();

    protected abstract void onCreated();

    protected abstract void onChanged(int width, int height);

    protected abstract void onDraw();

}
