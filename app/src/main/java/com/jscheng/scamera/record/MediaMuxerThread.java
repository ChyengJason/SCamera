package com.jscheng.scamera.record;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/8
 */
public class MediaMuxerThread extends Thread implements Runnable{
    private Queue<MutexBean> mMutexBeanQueue;
    private boolean isRecording;
    private AudioRecordThread mAudioThread;
    private VideoRecordThread mVideoThread;
    private String path;
    private MediaMuxer mMediaMuxer;
    private int mAudioTrack;
    private int mVideoTrack;
    private boolean isMediaMuxerStart;
    private MediaMuxerCallback mMediaMuxerCallback;

    public MediaMuxerThread(String path) {
        this.isRecording = false;
        this.isMediaMuxerStart = false;
        this.path = path;
        this.mMutexBeanQueue = new ArrayBlockingQueue(100);
        this.mMediaMuxerCallback = null;
    }

    public void prepareMediaMuxer(int width, int height) {
        try {
            mAudioTrack = -1;
            mVideoTrack = -1;
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mAudioThread = new AudioRecordThread(this);
            mVideoThread = new VideoRecordThread(this, width, height);
            mAudioThread.prepare();
            mVideoThread.prapare();
        } catch (IOException e) {
            Log.e(TAG, "initMediaMuxer: " + e.toString());
            e.printStackTrace();
        }
    }

    private void startMediaMutex() {
        if (!isMediaMuxerStart && isVideoTrackExist() && isAudioTrackExist()){
            Log.e(TAG, "run: MediaMuxerStart");
            mMediaMuxer.start();
            isMediaMuxerStart = true;
            start();
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

    public void begin(int width, int height) {
        prepareMediaMuxer(width, height);
        isRecording = true;
        isMediaMuxerStart = false;
        mVideoThread.begin();
        mAudioThread.begin();
    }

    public void frame(byte[] data) {
        if (isRecording) {
            mVideoThread.frame(data);
        }
    }

    public void end() {
        try {
            isRecording = false;
            mVideoThread.end();
            mVideoThread.join();
            mVideoThread = null;
            mAudioThread.end();
            mAudioThread.join();
            mAudioThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addMutexData(MutexBean data) {
        mMutexBeanQueue.offer(data);
    }

    @Override
    public void run() {
        while(true) {
            if (!mMutexBeanQueue.isEmpty()) {
                MutexBean data = mMutexBeanQueue.poll();
                if (data.isVedio()) {
                    mMediaMuxer.writeSampleData(mVideoTrack, data.getByteBuffer(), data.getBufferInfo());
                } else {
                    mMediaMuxer.writeSampleData(mAudioTrack, data.getByteBuffer(), data.getBufferInfo());
                }
            }else {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isRecording && mMutexBeanQueue.isEmpty()) {
                    break;
                }
            }
        }
        release();
        if (mMediaMuxerCallback != null) {
            mMediaMuxerCallback.onFinishMediaMutex(path);
        }
    }

    private void release() {
        if (mMediaMuxer != null && isMediaMuxerStart) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public boolean isMediaMuxerStart() {
        return isMediaMuxerStart;
    }

    public void setMediaMuxerCallback(MediaMuxerCallback callback) {
        this.mMediaMuxerCallback = callback;
    }

    public interface MediaMuxerCallback {
        void onFinishMediaMutex(String path);
    }
}
