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
public class AudioRecordThread implements Runnable {
    private static final int TIMEOUT_S = 100000;

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

    private void initMediaCodec() {
        try {
            minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            Log.e(TAG, "initMediaCodec: minbuffersize" + minBufferSize );
            mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mSampleRate,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        final ByteBuffer bytesBuffer = ByteBuffer.allocateDirect(minBufferSize);
        int len = 0;
        while(isRecording) {
            bytesBuffer.clear();
            len = mAudioRecorder.read(bytesBuffer, minBufferSize);
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputBufferIndex >= 0) {
                bytesBuffer.position(len);
                bytesBuffer.flip();
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
                    MediaMutexThread mediaMutex = mMutex.get();
                    if (mediaMutex != null && mediaMutex.isMediaMuxerStart()) {
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        bufferInfo.presentationTimeUs = getPTSUs();
                        mediaMutex.addMutexData(new MutexBean(false, outData, bufferInfo));
                        prevOutputPTSUs = bufferInfo.presentationTimeUs;
                    }
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
        initMediaCodec();
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
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
