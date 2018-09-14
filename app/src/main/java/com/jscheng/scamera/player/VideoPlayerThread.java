package com.jscheng.scamera.player;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.jscheng.scamera.util.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/14
 */
public class VideoPlayerThread extends Thread {
    private static final int TIMEOUT_S = 12000;
    private int mWidth, mHeight;
    private Surface mSurface;
    private MediaCodec mVideoCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private String path;
    private int mFrameRate;
    private MediaExtractor mExtractor;

    public VideoPlayerThread(String path) {
        this.mWidth = 0;
        this.mHeight = 0;
        this.mSurface = null;
        this.path = path;
    }

    public void setVideoSize(int mWidth, int mHeight) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        Log.e(LogUtil.TAG, "setVideoSize: " + mWidth +"x" + mHeight );
    }

    public void setSurface(Surface surface) {
        this.mSurface = surface;
    }

    public boolean prapare() {
        if (mSurface == null || mWidth == 0 || mHeight == 0) {
            Log.e(TAG, "prapare: mSurface is null or mWidth, mHeight is 0");
            return false;
        }
        try {
            mExtractor = new MediaExtractor();
            mBufferInfo = new MediaCodec.BufferInfo();
            mExtractor.setDataSource(path);
            MediaFormat mediaFormat = null;
            int selectTrack = 0;
            String mine;
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                mediaFormat = mExtractor.getTrackFormat(i);
                mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    selectTrack = i;
                    break;
                }
            }
            mExtractor.selectTrack(selectTrack);
            mFrameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            mVideoCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mVideoCodec.configure(mediaFormat, mSurface, null, 0);
            mVideoCodec.start();
        } catch (IOException e) {
            Log.e(TAG, "video player prapare: " + e.toString() );
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        long startMs = System.currentTimeMillis();
        int index = 0;
        while(true) {
            int inputIndex = mVideoCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputIndex < 0) {
                Log.e(TAG, "decode inputIdex < 0");
                SystemClock.sleep(50);
                continue;
            }

            ByteBuffer inputBuffer = mVideoCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            int samplesize = mExtractor.readSampleData(inputBuffer, 0);
            Log.e(TAG, "decode samplesize: " + samplesize);
            if (samplesize <= 0) {
                break;
            }
            mVideoCodec.queueInputBuffer(inputIndex, 0, samplesize, getPts(index++, mFrameRate), 0);
            int outputIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            Log.e(TAG, "decode: outputIndex " + outputIndex);
            while (outputIndex > 0) {
                //帧控制
                while (mBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    SystemClock.sleep(50);
                }
                mVideoCodec.releaseOutputBuffer(outputIndex, true);
                outputIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            }
            if (!mExtractor.advance()) {
                break;
            }
        }
        release();
    }

    private long getPts(int index, int frameRate) {
        return index * 1000000 / frameRate;
    }

    private void release() {
        Log.e(TAG, "resolveVideo release ");
        try {
            mExtractor.release();
            mExtractor = null;
            mVideoCodec.stop();
            mVideoCodec.release();
            mVideoCodec = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
