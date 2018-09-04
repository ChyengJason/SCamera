package com.jscheng.scamera.render;

import android.annotation.SuppressLint;
import android.opengl.GLES30;
import android.util.Log;

import com.jscheng.scamera.util.GlesUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/31
 * 统一管理所有的RenderDrawer 和 FBO
 */
public class RenderDrawerGroups {
    public List<BaseRenderDrawer> mRenderDrawers;
    private int mInputTexture;
    private int mFrameBuffer;
    private int[] mPixelBuffers;
    private int mPboIndex;
    private int mPboNewIndex;
    private ByteBuffer byteBuffer;

    public RenderDrawerGroups() {
        mRenderDrawers = new ArrayList<>();
        mPixelBuffers = new int[2];
        mFrameBuffer = 0;
        mInputTexture = 0;
        mPboIndex = 0;
        mPboNewIndex = 1;
        byteBuffer = null;
    }

    public void addRenderDrawer(BaseRenderDrawer drawer) {
        mRenderDrawers.add(drawer);
    }

    public void removeRenderDrawer(BaseRenderDrawer drawer) {
        mRenderDrawers.remove(drawer);
    }

    public boolean isEmpty() {
        return mRenderDrawers.isEmpty();
    }

    public void setInputTexture(int texture) {
        this.mInputTexture = texture;
    }

    @SuppressLint("NewApi")
    public void bindPixelBuffer(int width, int height) {
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPixelBuffers[mPboIndex]);
        // 读取到PBO中
        long readPixelsTime = System.currentTimeMillis();
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE,0);
        Log.e(TAG, "bindPixelBuffer glReadPixels: " + (System.currentTimeMillis() - readPixelsTime ) );
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPixelBuffers[mPboNewIndex]);
        // 映射到CPU内存中
        long mapBufferRangeTime = System.currentTimeMillis();
        byteBuffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, width * height * 4, GLES30.GL_MAP_READ_BIT);
        Log.e(TAG, "bindPixelBuffer glMapBufferRange: " + (System.currentTimeMillis() - mapBufferRangeTime) );
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        mPboIndex = (mPboIndex + 1) % mPixelBuffers.length;
        mPboNewIndex = (mPboNewIndex + 1) % mPixelBuffers.length;
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
        for (BaseRenderDrawer drawer : mRenderDrawers) {
            drawer.create();
        }
    }

    public void surfaceChangedSize(int width, int height) {
        mFrameBuffer = GlesUtil.createFrameBuffer();
        GlesUtil.createPixelsBuffers(mPixelBuffers, width, height);
        for (BaseRenderDrawer drawer : mRenderDrawers) {
            drawer.surfaceChangedSize(width, height);
        }
    }

    public int drawRender(BaseRenderDrawer drawer, boolean useFrameBuffer, int inputTexture, boolean readPixles) {
        drawer.setInputTextureId(inputTexture);
        if (useFrameBuffer) {
            bindFrameBuffer(drawer.getOutputTextureId());
        }
        drawer.draw();
        if (readPixles) {
            bindPixelBuffer(drawer.width, drawer.height);
        }
        if (useFrameBuffer) {
            unBindFrameBuffer();
        }
        return drawer.getOutputTextureId();
    }

    public void draw() {
        if (mInputTexture == 0 || mFrameBuffer == 0 || isEmpty()) {
            Log.e(TAG, "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        BaseRenderDrawer currentRender = null;
        int inputTexture = mInputTexture;
        int outputTexture = 0;
        for (int i = 0; i < mRenderDrawers.size(); i++) {
            currentRender = mRenderDrawers.get(i);
            if (i != mRenderDrawers.size() - 1) {
                outputTexture = drawRender(currentRender, true, inputTexture, false);
            } else {
                outputTexture = drawRender(currentRender, false, inputTexture, true);
            }
            inputTexture = outputTexture;
        }
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

}
