package com.jscheng.scamera.player;

import android.view.Surface;

/**
 * Created By Chengjunsen on 2018/9/14
 */
public class MediaPlayer {
    private VideoPlayerThread mVideoThread;
    private AudioPlayerThread mAudioThread;
    public MediaPlayer(String path) {
        mVideoThread = new VideoPlayerThread(path);
        mAudioThread = new AudioPlayerThread(path);
    }

    public void setVideoSize(int mWidth, int mHeight) {
        mVideoThread.setVideoSize(mWidth, mHeight);
    }

    public void setSurface(Surface surface) {
        mVideoThread.setSurface(surface);
    }

    public void start() {
        mVideoThread.prapare();
        mAudioThread.prapare();
        mVideoThread.start();
        mAudioThread.start();
    }
}
