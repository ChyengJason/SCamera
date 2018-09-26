package com.jscheng.scamera.render;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.jscheng.scamera.util.GlesUtil;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/31
 * 统一管理所有的RenderDrawer 和 FBO
 */
public class RenderDrawerGroups {
    private int mInputTexture;
    private int mFrameBuffer;
    private OriginalRenderDrawer mOriginalDrawer;
    private WaterMarkRenderDrawer mWaterMarkDrawer;
    private DisplayRenderDrawer mDisplayDrawer;
    private RecordRenderDrawer mRecordDrawer;

    public RenderDrawerGroups(Context context) {
        this.mOriginalDrawer = new OriginalRenderDrawer();
        this.mWaterMarkDrawer = new WaterMarkRenderDrawer(context);
        this.mDisplayDrawer = new DisplayRenderDrawer();
        this.mRecordDrawer = new RecordRenderDrawer();
        this.mFrameBuffer = 0;
        this.mInputTexture = 0;
    }

    public void setInputTexture(int texture) {
        this.mInputTexture = texture;
    }

    public void bindFrameBuffer(int textureId) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBuffer);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0);
    }

    public void unBindFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    public void deleteFrameBuffer() {
        GLES30.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
        GLES30.glDeleteTextures(1, new int[]{mInputTexture}, 0);
    }

    public void create() {
        this.mOriginalDrawer.create();
        this.mWaterMarkDrawer.create();
        this.mDisplayDrawer.create();
        this.mRecordDrawer.create();
    }

    public void surfaceChangedSize(int width, int height) {
        mFrameBuffer = GlesUtil.createFrameBuffer();
        this.mOriginalDrawer.surfaceChangedSize(width, height);
        this.mWaterMarkDrawer.surfaceChangedSize(width, height);
        this.mDisplayDrawer.surfaceChangedSize(width, height);
        this.mRecordDrawer.surfaceChangedSize(width, height);
    }

    public int drawRender(BaseRenderDrawer drawer, boolean useFrameBuffer, int inputTexture, long timestamp, float[] transformMatrix) {
        drawer.setInputTextureId(inputTexture);
        if (useFrameBuffer) {
            bindFrameBuffer(drawer.getOutputTextureId());
        }
        drawer.draw(timestamp, transformMatrix);
        if (useFrameBuffer) {
            unBindFrameBuffer();
        }
        return drawer.getOutputTextureId();
    }

    public void draw(long timestamp, float[] transformMatrix) {
        if (mInputTexture == 0 || mFrameBuffer == 0) {
            Log.e(TAG, "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        int inputTexture = drawRender(mOriginalDrawer, true, mInputTexture, timestamp, transformMatrix);
        inputTexture = drawRender(mWaterMarkDrawer, true, inputTexture, timestamp, transformMatrix);
        drawRender(mDisplayDrawer, false, inputTexture, timestamp, transformMatrix);
        drawRender(mRecordDrawer, false, inputTexture, timestamp, transformMatrix);
    }

    public void startRecord() {
        mRecordDrawer.startRecord();
    }

    public void stopRecord() {
        mRecordDrawer.stopRecord();
    }
}
