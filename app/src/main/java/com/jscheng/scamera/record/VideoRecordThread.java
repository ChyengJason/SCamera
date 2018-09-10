package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/8
 */
public class VideoRecordThread extends Thread implements Runnable {
    private static final int TIMEOUT_S = 100000;
    private int mFrameRate = 30;
    private int  mBitRate;
    private int mIFrameInterval = 10;
    private long generateIndex = 0;
    public Queue<byte[]> dataQueue;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private int width, height;
    private WeakReference<MediaMutexThread> mMutex;

    public VideoRecordThread(MediaMutexThread mMutex, int width, int height) {
        this.mMutex = new WeakReference<MediaMutexThread>(mMutex);
        this.width = width;
        this.height = height;
        this.dataQueue =new LinkedList<>();
        this.isRecording = false;
        this.mBitRate = height * width * 3 * 8 * mFrameRate / 256;
        initMediaCodec(width, height);
    }

    private void initMediaCodec(int width, int height) {
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void frame(byte[] data) {
        if (isRecording) {
            dataQueue.offer(data);
        }
    }

    public void begin() {
        isRecording = true;
        dataQueue.clear();
        generateIndex = 0;
        mMediaCodec.start();
        start();
    }

    public void end() {
        isRecording = false;
    }

    @Override
    public void run() {
        while (isRecording) {
            byte[] data = dataQueue.poll();
            if (data != null) {
                byte[] yuv420sp = new byte[width * height * 3 / 2];
                NV21toI420SemiPlanar(data, yuv420sp, width, height);
                encode(yuv420sp);
            }
        }
        // 停止编解码器并释放资源
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void encode(byte[] input) {
        if (input != null) {
            try {
                int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                if (inputBufferIndex >= 0) {
                    long pts = getPts();
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                    generateIndex += 1;
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.e(TAG, "vedio run: INFO_OUTPUT_FORMAT_CHANGED");
                    MediaMutexThread mediaMutex = mMutex.get();
                    if (mediaMutex != null && !mediaMutex.isVideoTrackExist()) {
                        mediaMutex.addVedioTrack(mMediaCodec.getOutputFormat());
                    }
                }

                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        Log.e(TAG, "vedio run: BUFFER_FLAG_CODEC_CONFIG" );
                        bufferInfo.size = 0;
                    }

                    if (bufferInfo.size > 0) {
                        MediaMutexThread mediaMuxer = this.mMutex.get();
                        if (mediaMuxer != null) {
                            byte[] outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            Log.e(TAG, "video presentationTimeUs : " + bufferInfo.presentationTimeUs);
                            //bufferInfo.presentationTimeUs = getPts();
                            mediaMuxer.addMutexData(new MutexBean(true, outData, bufferInfo));
                        }
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    bufferInfo = new MediaCodec.BufferInfo();
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
                }

            } catch (Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "encode: "+t.toString() );
            }
        }
    }

    private long getPts() {
        return System.nanoTime() / 1000L;
    }


    private static void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
    }
}
