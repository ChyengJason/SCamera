package com.jscheng.scamera.render.image;

import android.content.Context;
import android.opengl.GLES20;

import com.jscheng.scamera.R;
import com.jscheng.scamera.util.GlesUtil;
import com.jscheng.scamera.render.BaseRenderDrawer;

public class OriginalImageRenderDrawer extends BaseRenderDrawer {
    private int mInputTextureId;
    private int mOutputTextureId;
    private int avPosition;
    private int afPosition;
    private int sTexture;
    private Context mContext;
    private int mFrameBuffer;

    public OriginalImageRenderDrawer(Context context) {
        this.mContext = context;
    }

    @Override
    public void setInputTextureId(int textureId) {
        mInputTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mOutputTextureId;
    }

    @Override
    protected void onCreated() {
    }

    @Override
    protected void onChanged(int width, int height) {
        mOutputTextureId = GlesUtil.createFrameTexture(width, height);
        mFrameBuffer = GlesUtil.createFrameBuffer();
        GlesUtil.bindFrameTexture(mFrameBuffer, mOutputTextureId);
        mInputTextureId = GlesUtil.loadBitmapTexture(mContext, R.mipmap.ic_launcher);
        avPosition = GLES20.glGetAttribLocation(mProgram, "av_Position");
        afPosition = GLES20.glGetAttribLocation(mProgram, "af_Position");
        sTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
    }

    @Override
    protected void onDraw() {
        bindFrameBuffer();

        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);
        //设置顶点位置值
        GLES20.glVertexAttribPointer(avPosition, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(afPosition, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mDisplayTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInputTextureId);
        GLES20.glUniform1i(sTexture, 0);
        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);

        unBindFrameBuffer();
    }

    public void bindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
    }

    public void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    protected String getVertexSource() {
        final String source =
                "attribute vec4 av_Position; " +
                        "attribute vec2 af_Position; " +
                        "varying vec2 v_texPo; " +
                        "void main() { " +
                        "    v_texPo = af_Position; " +
                        "    gl_Position = av_Position; " +
                        "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source =
                "precision mediump float; " +
                        "varying vec2 v_texPo; " +
                        "uniform sampler2D sTexture; " +
                        "void main() { " +
                        "   gl_FragColor = texture2D(sTexture, v_texPo); " +
                        "} ";
        return source;
    }
}
