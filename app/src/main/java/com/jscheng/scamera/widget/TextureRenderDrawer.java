package com.jscheng.scamera.widget;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class TextureRenderDrawer extends BaseRenderDrawer {
//    private int av_Position;
//    private int af_Position;
//    private int s_Texture;
//    private int mFrameBuffer;
//    private int mFrameTexture;
    private OriginalRenderDrawer mOrignialRenderDrawer;

    public TextureRenderDrawer() {
        mOrignialRenderDrawer = new OriginalRenderDrawer();
    }
    @Override
    public void create() {
        mOrignialRenderDrawer.create();
    }

    @Override
    protected void onCreated() {
        mOrignialRenderDrawer.onCreated();
//        mOrignialRenderDrawer.setBackCamera(true);
//        mFrameBuffer = createOutputFrameBuffer();
//        mFrameTexture = createOutputTexture();
//
//        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
//        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
//        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onChanged(int width, int height) {
        mOrignialRenderDrawer.onChanged(width, height);
    }

    @Override
    public void draw() {
        mOrignialRenderDrawer.clear();
        mOrignialRenderDrawer.useProgram();
        mOrignialRenderDrawer.draw();
    }

    @Override
    protected void onDraw() {
//        boolean a=GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
//        if(a){
//            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        }
////        bindFrameTexture(mFrameBuffer, mFrameTexture);
//        mOrignialRenderDrawer.clear();
//        mOrignialRenderDrawer.useProgram();
//        mOrignialRenderDrawer.draw();
//        if(a){
//            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//        }

//        unBindFrameBuffer();

//        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);
//        if (isBackCamera) {
//            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mBackTextureBuffer);
//        } else {
//            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mFrontTextureBuffer);
//        }
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameTexture);
//        GLES20.glUniform1i(s_Texture, 0);
//        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
//        GLES20.glDisableVertexAttribArray(av_Position);
//        GLES20.glDisableVertexAttribArray(af_Position);
//
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

//    private void bind2DTexture(int textureId, int textureType) {
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureType);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
//        GLES20.glUniform1i(s_Texture, textureType);
//    }
//
//    private void unBind2DTexure() {
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//    }
//
//    private int createOutputFrameBuffer() {
//        int[] buffers = new int[1];
//        GLES20.glGenFramebuffers(1, buffers, 0);
//        return buffers[0];
//    }
//
//    private int createOutputTexture() {
//        int[] textures = new int[1];
//        GLES20.glGenTextures(1, textures, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//        return textures[0];
//    }
//
//    protected void bindFrameTexture(int frameBufferId, int textureId){
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//    }
//
//    protected void unBindFrameBuffer(){
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
//    }

    @Override
    public void setTextureId(int textureId) {
        super.setTextureId(textureId);
        mOrignialRenderDrawer.setTextureId(textureId);
    }

    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
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
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_texture;\n" +
                "void main() {\n" +
                "   gl_FragColor = texture2D(s_texture, v_texPo);\n" +
                "}";
        return source;
    }
}
