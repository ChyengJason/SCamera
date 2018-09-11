package com.jscheng.scamera.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/8
 */
public class AudioRecordThread extends Thread implements Runnable {
    private static final int TIMEOUT_S = 10000;// 1s
    private WeakReference<MediaMutexThread> mMutex;
    private int mSampleRate = 16000;
    private int mBitRate = 64000;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private AudioRecord mAudioRecorder;
    private int minBufferSize;
    private long prevOutputPTSUs;

    public AudioRecordThread(MediaMutexThread mediaMutexThread) {
        this.mMutex = new WeakReference<>(mediaMutexThread);
    }

    private boolean initCodec() {
        try {
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initRecorder() {
        minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mSampleRate,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * minBufferSize);
        if (mAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "initRecord: mAudioRecord init failed");
            isRecording = false;
            return false;
        }
        mAudioRecorder.startRecording();
        return true;
    }

    @Override
    public void run() {
        byte[] bufferBytes = new byte[minBufferSize];
        int len = 0;
        while(isRecording) {
            len = mAudioRecorder.read(bufferBytes, 0, minBufferSize);
            if (len > 0) {
                record(bufferBytes, len, getPTSUs());
            }
        }
        release();
    }

    private void record(byte[] bufferBytes, final int len, final long presentationTimeUs) {
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            if (inputBuffer != null) {
                inputBuffer.put(bufferBytes);
            }
            if (len <= 0) {
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
            } else {
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, len, presentationTimeUs, 0);
            }
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.e(TAG, "audio run: INFO_OUTPUT_FORMAT_CHANGED");
            MediaMutexThread mediaMutex = mMutex.get();
            if (mediaMutex != null && !mediaMutex.isAudioTrackExist()) {
                mediaMutex.addAudioTrack(mMediaCodec.getOutputFormat());
            }
        }

        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                Log.e(TAG, "audio run: BUFFER_FLAG_CODEC_CONFIG");
                bufferInfo.size = 0;
            }
            if (bufferInfo.size > 0) {
                MediaMutexThread mediaMuxer = mMutex.get();
                if (mediaMuxer != null) {
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    bufferInfo.presentationTimeUs = getPTSUs();
                    Log.e(TAG, "audio presentationTimeUs : " + bufferInfo.presentationTimeUs);
                    mediaMuxer.addMutexData(new MutexBean(false, outData, bufferInfo));
                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                }
            }
            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            bufferInfo = new MediaCodec.BufferInfo();
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
        }
    }

    public void begin() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        prevOutputPTSUs = 0;
        isRecording = true;
        start();
    }

    public void end() {
        isRecording = false;
    }

    public void prepare() {
        initCodec();
        initRecorder();
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        return result < prevOutputPTSUs ? prevOutputPTSUs : result;
    }

    private void release() {
        if (mAudioRecorder != null ) {
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
