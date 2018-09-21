package com.jscheng.scamera.render;

import com.jscheng.scamera.record.VideoEncoder;
import com.jscheng.scamera.util.StorageUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created By Chengjunsen on 2018/9/21
 */
public class RecordRenderDrawer extends BaseRenderDrawer implements Runnable{
    // 绘制的纹理 ID
    private int mTextureId;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private boolean isRecording;

    public RecordRenderDrawer() {
        this.mVideoEncoder = null;
        this.isRecording = false;
    }

    @Override
    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mTextureId;
    }

    @Override
    protected String getVertexSource() {
        return null;
    }

    @Override
    protected String getFragmentSource() {
        return null;
    }

    @Override
    public void create() {

    }

    @Override
    protected void onCreated() {

    }

    @Override
    public void surfaceChangedSize(int width, int height) {
        try {
            mVideoPath = StorageUtil.getVedioPath(true) + "glvideo.mp4";
            mVideoEncoder = new VideoEncoder(width, height, 1000000, new File(mVideoPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onChanged(int width, int height) {

    }

    @Override
    public void draw() {
        if (isRecording) {

        }
    }

    @Override
    protected void onDraw() {

    }

    @Override
    public void run() {

    }
}
