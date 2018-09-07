package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/5
 */
public class CameraRecorder implements Runnable {
    private static final int TIMEOUT_S = 12000;
    private int mFrameRate = 30;
    private int mBitRate = 500000;
    private int mIFrameInterval = 1;
    private long generateIndex = 0;
    public Queue<byte[]> dataQueue;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private FileOutputStream mVideoFile;
    private int width, height;
    public byte[] configbyte;

    public CameraRecorder(int width, int height, String path) {
        this.width = width;
        this.height= height;
        this.dataQueue =new LinkedBlockingDeque<>();
        this.isRecording = false;
        initMediaCodec(width, height);
        initVideoFile(path);
    }

    private void initMediaCodec(int width, int height) {
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initVideoFile(String path) {
        try {
            mVideoFile = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void startEncoder() {
        isRecording = true;
        dataQueue.clear();
        generateIndex = 0;
        mMediaCodec.start();
        new Thread(this).start();
    }

    public synchronized void stopEncoder() {
        isRecording = false;
        notifyAll();
    }

    public synchronized void putData(byte[] data) {
        if (isRecording) {
            if (dataQueue.size() >= 10) {
                dataQueue.poll();
            }
            dataQueue.add(data);
        }
        notifyAll();
    }

    private void encode(byte[] input) {
        if (input != null) {
            try {
                int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                if (inputBufferIndex >= 0) {
                    long pts = computePresentationTime(generateIndex);
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                    generateIndex += 1;
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        Log.e(TAG, "run: BUFFER_FLAG_CODEC_CONFIG" );
                        configbyte = new byte[bufferInfo.size];
                        configbyte = outData;
                    } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                        Log.e(TAG, "run: BUFFER_FLAG_KEY_FRAME" );
                        byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
                        System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                        System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
                        mVideoFile.write(keyframe, 0, keyframe.length);
                    } else {
                        mVideoFile.write(outData, 0, outData.length);
                    }

                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
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

    @Override
    public synchronized void run() {
        while (isRecording) {
            byte[] data = dataQueue.poll();
            if (data != null) {
                byte[] yuv420sp = new byte[width * height * 3 / 2];
                // 必须要转格式，否则录制的内容播放出来为绿屏
                NV21ToNV12(data, yuv420sp, width, height);
                encode(yuv420sp);
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // 停止编解码器并释放资源
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 关闭数据流
        try {
            mVideoFile.flush();
            mVideoFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
