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
public class MediaMutexThread extends Thread implements Runnable{
    private Queue<MutexBean> mMutexBeanQueue;
    private boolean isRecording;
    private AudioRecordThread mAudioThread;
    private VideoRecordThread mVideoThread;
    private String path;
    private MediaMuxer mMediaMuxer;
    private int mAudioTrack;
    private int mVideoTrack;
    private boolean isMediaMuxerStart;

    public MediaMutexThread(String path) {
        this.isRecording = false;
        this.isMediaMuxerStart = false;
        this.path = path;
        this.mMutexBeanQueue = new ArrayBlockingQueue(100);
    }

    public void initMediaMuxer(int width, int height) {
        try {
            mAudioTrack = -1;
            mVideoTrack = -1;
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            this.mAudioThread = new AudioRecordThread(this);
            this.mVideoThread = new VideoRecordThread(this, width, height);
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
        initMediaMuxer(width, height);
        isRecording = true;
        isMediaMuxerStart = false;
        mVideoThread.begin();
        mAudioThread.begin();
    }

    public void frame(byte[] data) {
        if (isRecording) {
            //Log.e(TAG, "video frame: " + data.length );
            mVideoThread.frame(data);
        }
    }

    public void end() {
        try {
            isRecording = false;
            mVideoThread.end();
            mVideoThread.join();
            mAudioThread.end();
            mVideoThread.join();
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
//                    Log.e(TAG, "Muxer video size: " + data.getBufferInfo().size);
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
        if (mMediaMuxer != null && isMediaMuxerStart) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }
}
