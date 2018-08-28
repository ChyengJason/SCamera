package com.jscheng.scamera.widget;

import android.opengl.GLES20;
import android.util.Log;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class TextureRenderDrawer extends BaseRenderDrawer {
    private int av_Position;
    private int af_Position;
    private int s_Texture;
    private int mFrameBuffer;
    private int mFrameTexture;
//    private int mFrameRender;
    private OriginalRenderDrawer mOrignialRenderDrawer;
//    private ByteBuffer mBuffer;

    public TextureRenderDrawer() {
        mOrignialRenderDrawer = new OriginalRenderDrawer();
    }

    @Override
    protected void onCreated() {
        mOrignialRenderDrawer.create();
        mOrignialRenderDrawer.setBackCamera(true);
        isBackCamera = true;

        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onChanged(int width, int height) {
        mOrignialRenderDrawer.surfaceChangedSize(width, height);
        mFrameBuffer = createOutputFrameBuffer();
        mFrameTexture = createOutputTexture();
//        mFrameRender = createOutputRender();
        bindFrameTexture(mFrameBuffer, mFrameTexture);
//        bindFrameRender(mFrameBuffer, mFrameRender);

//        mBuffer = ByteBuffer.allocate(width * height * 4);
        checkFrameBuffer();
    }

    @Override
    public void draw() {
        boolean isDepthEnable = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        if(isDepthEnable ){
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameTexture, 0);
//        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mFrameRender);

        mOrignialRenderDrawer.draw();

//        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameTexture, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        if(isDepthEnable) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
//        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBuffer);
//        Log.e(TAG, "draw buffer: " + mBuffer.array().length );
        onDraw();
    }

    @Override
    protected void onDraw() {
        clear();
        useProgram();
        viewPort();
        GLES20.glViewport(0, 0, width, height);
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mFrameTextureBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameTexture);
        GLES20.glUniform1i(s_Texture, 0);
        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void bind2DTexture(int textureId, int textureType) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureType);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(s_Texture, textureType);
    }

    private void unBind2DTexure() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private int createOutputFrameBuffer() {
        int[] buffers = new int[1];
        GLES20.glGenFramebuffers(1, buffers, 0);
        return buffers[0];
    }

    private int createOutputRender() {
        int[] render = new int[1];
        GLES20.glGenRenderbuffers(1, render, 0);
        checkError();
        return render[0];
    }

    private int createOutputTexture() {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "createOutputTexture: width or height is 0");
            return -1;
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        checkError();
        return textures[0];
    }

    protected void bindFrameTexture(int frameBufferId, int textureId){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        checkError();
    }

    protected void bindFrameRender(int frameBufferId, int renderId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderId);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void deleteFrameBuffer() {
//        GLES20.glDeleteRenderbuffers(1, new int[]{mFrameRender}, 0);
        GLES20.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
        GLES20.glDeleteTextures(1, new int[]{mFrameTexture}, 0);
    }

    @Override
    public void setInputTextureId(int textureId) {
        mOrignialRenderDrawer.setInputTextureId(textureId);
    }

    @Override
    public int getOutputTextureId() {
        return mFrameTexture;
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
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                //"   vec4 tc = texture2D(s_texture, v_texPo);\n" +
                //"   float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                "}";
        return source;
    }
}
