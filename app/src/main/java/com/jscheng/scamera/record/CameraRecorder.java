package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/5
 */
public class CameraRecorder implements Runnable {
    private static final int TIMEOUT_S = 10000;
    private int mFrameRate = 30;
    private int mBitRate = 500000;
    private int mIFrameInterval = 1;

    private long generateIndex = 0;
    private Queue<byte[]> dataQueue;
    private Thread mRecordThread;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private byte[] mMediaConfigBytes;

    public CameraRecorder(int width, int heigth) {
        dataQueue = new LinkedBlockingQueue<>();
        mRecordThread = new Thread(this);
        isRecording = false;
        initMediaCodec(width, heigth);
    }

    private void initMediaCodec(int width, int height) {
        try {
            mBufferInfo = new MediaCodec.BufferInfo();
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void start() {
        isRecording = true;
        dataQueue.clear();
        mRecordThread.start();
        startEncode();
    }

    public synchronized void end() {
        isRecording = false;
        notifyAll();
        endEncode();
    }

    public synchronized void push(byte[] data) {
        dataQueue.offer(data);
        notifyAll();
    }

    @Override
    public synchronized void run() {
        while (true) {
            if (dataQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!isRecording) {
                break;
            }
            byte[] data = dataQueue.poll();
            if (data != null) {
                encode(data);
            }
        }
    }

    private void startEncode() {
        mMediaCodec.start();
        generateIndex = 0;
    }

    private void encode(byte[] bytes) {
        int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
        if (inputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.e(TAG, "encode: INFO_OUTPUT_FORMAT_CHANGED");
        } else if(inputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.e(TAG, "encode: INFO_TRY_AGAIN_LATER");
        } else if (inputIndex > 0) {
            long pts = computePresentationTime(generateIndex++);
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            inputBuffer.put(bytes);
            mMediaCodec.queueInputBuffer(inputIndex, 0, bytes.length, pts, 0);
        }
        int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
        while(outputIndex > 0) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);
            byte[] outputData = new byte[mBufferInfo.size];
            outputBuffer.get(outputData);
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) !=0) { // 配置
                Log.e(TAG, "encode: BUFFER_FLAG_CODEC_CONFIG");
                mMediaConfigBytes = new byte[outputData.length];
                mMediaConfigBytes = outputData;
            } else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) !=0) { // 关键帧
                Log.e(TAG, "encode: BUFFER_FLAG_KEY_FRAME");
                if (mMediaConfigBytes != null) {
                    byte[] keyframe = new byte[mBufferInfo.size + mMediaConfigBytes.length];
                    System.arraycopy(mMediaConfigBytes, 0, keyframe, 0, mMediaConfigBytes.length);
                    System.arraycopy(outputData, 0, keyframe, mMediaConfigBytes.length, outputData.length);
                }
            } else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) !=0){
                Log.e(TAG, "encode: BUFFER_FLAG_PARTIAL_FRAME");
            } else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) !=0){
                Log.e(TAG, "encode: BUFFER_FLAG_END_OF_STREAM");
            } else {
                Log.e(TAG, "encode: other " + mBufferInfo.flags);
            }
            mMediaCodec.releaseOutputBuffer(outputIndex, false);
            outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
        }
     }

    private void endEncode() {
        mMediaCodec.stop();
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }
}
