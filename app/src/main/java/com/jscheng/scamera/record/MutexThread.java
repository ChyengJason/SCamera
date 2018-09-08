package com.jscheng.scamera.record;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.jscheng.scamera.util.LogUtil.TAG;


/**
 * Created By Chengjunsen on 2018/9/8
 */
public class MutexThread implements Runnable{
    private Queue<MutexBean> mVideoQueue;
    private Queue<MutexBean> mAudioQueue;
    private boolean isRecording;
    private AudioRecordThread mAudioThread;
    private VideoRecordThread mVideoThread;
    private String path;
    private MediaMuxer mMediaMuxer;
    private int mAudioTrack;
    private int mVideoTrack;
    private boolean isMediaMuxerStart;

    public MutexThread(String path) {
        this.isRecording = false;
        this.isMediaMuxerStart = false;
        this.path = path;
        this.mVideoQueue = new LinkedBlockingDeque<>();
        this.mAudioQueue = new LinkedBlockingDeque<>();
        this.mAudioThread = new AudioRecordThread(this);
        this.mVideoThread = new VideoRecordThread(this, 1280, 720);
    }

    public void initMediaMuxer() {
        try {
            mAudioTrack = -1;
            mVideoTrack = -1;
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e(TAG, "initMediaMuxer: " + e.toString());
            e.printStackTrace();
        }
    }

    public void addAudioTrack(MediaFormat mediaFormat) {
        if (mMediaMuxer == null) {
            Log.e(TAG, "addAudioTrack: mMediaMuxer is null");
            return;
        }
        mAudioTrack = mMediaMuxer.addTrack(mediaFormat);
        startMediaMutex();
    }

    private void startMediaMutex() {
        if (!isMediaMuxerStart && isVideoTrackExist() && isAudioTrackExist()){
            Log.e(TAG, "run: MediaMuxerStart");
            mMediaMuxer.start();
            isMediaMuxerStart = true;
            new Thread(this).start();
        }
    }

    public void addVedioTrack(MediaFormat mediaFormat) {
        if (mMediaMuxer == null) {
            Log.e(TAG, "addAudioTrack: mMediaMuxer is null");
            return;
        }
        mVideoTrack = mMediaMuxer.addTrack(mediaFormat);
        startMediaMutex();
    }

    public boolean isVideoTrackExist() {
        return mVideoTrack >= 0;
    }

    public boolean isAudioTrackExist() {
        return mAudioTrack >= 0;
    }

    public void start() {
        initMediaMuxer();
        isRecording = true;
        isMediaMuxerStart = false;
        mVideoThread.start();
        mAudioThread.start();
    }

    public void frame(byte[] data) {
        mVideoThread.frame(data);
    }

    public void stop() {
        isRecording = false;
        mVideoThread.stop();
        mAudioThread.stop();
    }
    public void addVideoData(MutexBean data) {
        mVideoQueue.offer(data);
    }

    public void addAudioData(MutexBean data) {
        mAudioQueue.offer(data);
    }


    int frameIndex = 0;
    @Override
    public void run() {
        while(true) {
            if (!mVideoQueue.isEmpty()) {
                MutexBean data = mVideoQueue.poll();
                mMediaMuxer.writeSampleData(mVideoTrack, data.getByteBuffer(), data.getBufferInfo());
                Log.e(TAG, "mutex video : " + frameIndex++);
            } else if(!mAudioQueue.isEmpty()) {
                MutexBean data = mAudioQueue.poll();
                mMediaMuxer.writeSampleData(mAudioTrack, data.getByteBuffer(), data.getBufferInfo());
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isRecording && mVideoQueue.isEmpty() && mAudioQueue.isEmpty()) {
                    break;
                }
            }
        }
        if (mMediaMuxer != null && isMediaMuxerStart) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }
}
