package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/8
 */
public class VideoRecordThread implements Runnable{
    private static final int TIMEOUT_S = 100000;
    private int mFrameRate = 25;
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
        this.dataQueue =new LinkedBlockingDeque<>();
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

    public void start() {
        isRecording = true;
        dataQueue.clear();
        generateIndex = 0;
        mMediaCodec.start();
        new Thread(this).start();
    }

    public void stop() {
        isRecording = false;
    }

    @Override
    public void run() {
        while (!isRecording && dataQueue.isEmpty()) {
            byte[] data = dataQueue.poll();
            if (data != null) {
                byte[] yuv420sp = new byte[width * height * 3 / 2];
                NV21ToNV12(data, yuv420sp, width, height);
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
        MediaMutexThread mediaMuxer = this.mMutex.get();
        if (mediaMuxer != null ) {
            mediaMuxer.isVedioReallyStop();
        }
    }

    private void encode(byte[] input) {
        if (input != null) {
            try {
                int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                if (inputBufferIndex >= 0) {
                    long pts = System.nanoTime() / 1000L;
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
                        if (mediaMuxer != null && mediaMuxer.isMediaMuxerStart()) {
                            byte[] outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
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

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }


    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) {
            return;
        }
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }
}
