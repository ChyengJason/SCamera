package com.jscheng.scamera.widget.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.jscheng.scamera.R;
import com.jscheng.scamera.widget.BaseRenderDrawer;

import static com.jscheng.scamera.util.LogUtil.TAG;

public class FliterImageRenderDrawer extends BaseRenderDrawer {
    private int mTextureId;
    private int avPosition;
    private int afPosition;
    private int sTexture;
    private Context mContext;

    public FliterImageRenderDrawer(Context context) {
        this.mContext = context;
    }

    @Override
    public void setInputTextureId(int textureId) {
        mTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mTextureId;

    }

    @Override
    protected void onCreated() {
        avPosition = GLES20.glGetAttribLocation(mProgram, "av_Position");
        afPosition = GLES20.glGetAttribLocation(mProgram, "af_Position");
        sTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
    }

    @Override
    protected void onChanged(int width, int height) {

    }

    @Override
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);
        //设置顶点位置值
        GLES20.glVertexAttribPointer(avPosition, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(afPosition, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mFrameTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(sTexture, 0);
        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);
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
