package com.jscheng.scamera.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
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
public class AudioRecordThread implements Runnable {
    private static final int TIMEOUT_S = 100000;
    private WeakReference<MutexThread> mMutex;
    private int mSampleRate = 16000;
    private int mBitRate = 64000;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private AudioRecord mAudioRecorder;
    private int minBufferSize;
    private long prevOutputPTSUs;

    public AudioRecordThread(MutexThread mutexThread) {
        this.mMutex = new WeakReference<>(mutexThread);
        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, 1, AudioFormat.ENCODING_PCM_16BIT);
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // 比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            // 传入的数据大小
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] bytesBuffer = new byte[minBufferSize];
        int len = 0;
        while(isRecording) {
            len = mAudioRecorder.read(bytesBuffer, 0, minBufferSize);
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputBufferIndex >=0 ) {
                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                if (inputBuffer != null) {
                    inputBuffer.put(bytesBuffer);
                }
                long presentationTimeUs = getPTSUs();
                if (len <= 0) {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, len, presentationTimeUs, 0);
                }
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    Log.e(TAG, "run: BUFFER_FLAG_CODEC_CONFIG" );
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    MutexThread mediaMutex = mMutex.get();
                    if (mediaMutex != null) {
                        if (!mediaMutex.isAudioTrackExist()) {
                            mediaMutex.addAudioTrack(mMediaCodec.getOutputFormat());
                        }
                        prevOutputPTSUs = bufferInfo.presentationTimeUs;
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        mediaMutex.addAudioData(new MutexBean(outData, bufferInfo));
                    }
                    //Log.e(TAG, "sent " + bufferInfo.size + " audioBytes to muxer");
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                bufferInfo = new MediaCodec.BufferInfo();
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_S);
            }
        }

        if (mAudioRecorder != null && mAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
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

    public boolean start() {
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (mAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "initRecord: mAudioRecord init failed");
            isRecording = false;
            return false;
        }
        prevOutputPTSUs = 0;
        isRecording = true;
        mAudioRecorder.startRecording();
        new Thread(this).start();
        return true;
    }

    public void stop() {
        isRecording = false;
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        return result < prevOutputPTSUs ? prevOutputPTSUs : result;
    }
}
