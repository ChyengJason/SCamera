package com.jscheng.scamera.widget.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.jscheng.scamera.R;
import com.jscheng.scamera.widget.BaseRenderDrawer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OriginalImageRenderDrawer extends BaseRenderDrawer {
    private int mTextureId;
    private int avPosition;
    private int afPosition;
    private int sTexture;
    private Context mContext;

    public OriginalImageRenderDrawer(Context context) {
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
        mTextureId = loadTexture(R.mipmap.ic_launcher);
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

    protected int loadTexture(int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
        if (bitmap == null) {
            return -1;
        }
        int[] textureIds = new int[1];
        //创建纹理
        GLES20.glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            return -1;
        }
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        //根据以上指定的参数，生成一个2D纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureIds[0];
    }
}
