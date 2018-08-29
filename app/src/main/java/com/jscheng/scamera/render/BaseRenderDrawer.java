package com.jscheng.scamera.render;

import android.opengl.GLES20;
import android.util.Log;

import com.jscheng.scamera.util.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.jscheng.scamera.util.LogUtil.TAG;

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

    protected FloatBuffer mFrameTextureBuffer;

    protected float vertexData[] = {
            -1f, -1f, 0.0f, // 左下角
            1f, -1f, 0.0f, // 右下角
            -1f, 1f, 0.0f, // 左上角
            1f, 1f, 0.0f  // 右上角
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
            1f, 0f // 右下角
    };

    protected float frameTextureData[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    protected final int CoordsPerVertexCount = 3;

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
        viewPort();
        onDraw();
    }

    protected void clear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

//    protected void bindTexture(int type){
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + type);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D , mTextureId);
//        GLES20.glUniform1i(mHTexture, type);
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

        this.mFrameTextureBuffer = ByteBuffer.allocateDirect(frameTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frameTextureData);
        this.mFrameTextureBuffer.position(0);
    }

    protected void useProgram(){
        GLES20.glUseProgram(mProgram);
    }

    protected void viewPort() {
        GLES20.glViewport(0, 0, width, height);
    }

//    protected void draw(){
//        GLES20.glEnableVertexAttribArray(mHPosition);
//        GLES20.glVertexAttribPointer(mHPosition,2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
//        GLES20.glEnableVertexAttribArray(mHCoord);
//        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,);
//        GLES20.glDisableVertexAttribArray(mHPosition);
//        GLES20.glDisableVertexAttribArray(mHCoord);
//    }



    public abstract void setInputTextureId(int textureId);

    public abstract int getOutputTextureId();

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();

    protected abstract void onCreated();

    protected abstract void onChanged(int width, int height);

    protected abstract void onDraw();

}
