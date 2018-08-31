package com.jscheng.scamera.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.jscheng.scamera.util.GlesUtil;

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

    public RenderDrawerGroups() {
        mRenderDrawers = new ArrayList<>();
        mFrameBuffer = 0;
        mInputTexture = 0;
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

    public void bindFrameBuffer(int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    public void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void deleteFrameBuffer() {
//        GLES20.glDeleteRenderbuffers(1, new int[]{mFrameRender}, 0);
        GLES20.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
        GLES20.glDeleteTextures(1, new int[]{mInputTexture}, 0);
    }

    public void create() {
        mFrameBuffer = GlesUtil.createFrameBuffer();
        for (BaseRenderDrawer drawer : mRenderDrawers) {
            drawer.create();
        }
    }

    public void surfaceChangedSize(int width, int height) {
        for (BaseRenderDrawer drawer : mRenderDrawers) {
            drawer.surfaceChangedSize(width, height);
        }
    }

    public void draw() {
        if (mInputTexture == 0 || mFrameBuffer == 0 || isEmpty()) {
            Log.e(TAG, "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        BaseRenderDrawer currentRender = null;
        BaseRenderDrawer lastRender;
        for (int i = 0; i < mRenderDrawers.size(); i++) {
            lastRender = currentRender;
            currentRender = mRenderDrawers.get(i);
            if (lastRender == null) {
                currentRender.setInputTextureId(mInputTexture);
            } else {
                currentRender.setInputTextureId(lastRender.getOutputTextureId());
            }
            if (i != mRenderDrawers.size()-1) {
                bindFrameBuffer(currentRender.getOutputTextureId());
                currentRender.draw();
                unBindFrameBuffer();
            } else {
                currentRender.draw();
            }
        }
    }
}
