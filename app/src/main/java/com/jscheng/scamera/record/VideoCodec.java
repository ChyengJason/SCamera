package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/3
 */
public class VideoCodec {
    private static final String MIME_TYPE = "video/avc";
    private static final int VIDEO_FRAME_PER_SECOND = 15;
    private static final int VIDEO_I_FRAME_INTERVAL = 5;
    private static final int VIDEO_BITRATE = 500 * 8 * 1000;
    private static final int TIMEOUT_USEC = 1000;

    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodec mVedioCodec;
    private boolean isRecording;

    public VideoCodec() {
        this.isRecording = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void prepare(Surface surface, int width, int height) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_PER_SECOND);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,VIDEO_I_FRAME_INTERVAL);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,40);

        mVedioCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        mVedioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mVedioCodec.setInputSurface(surface);
    }

    public void encode() {
        if (mVedioCodec == null) {
            throw new RuntimeException("mVedioCodec is null");
        }
        while(true) {
            int outputBufferIndex = mVedioCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.e(TAG, "encode: INFO_TRY_AGAIN_LATER");
                release();
                break;
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.e(TAG, "encode INFO_OUTPUT_FORMAT_CHANGED");
                MediaFormat format = mVedioCodec.getOutputFormat();
            } else if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mVedioCodec.getOutputBuffer(outputBufferIndex);
                Log.e(TAG, "encode: outputbuffer " + outputBuffer.array().length);
                mVedioCodec.releaseOutputBuffer(outputBufferIndex, false);
            } else {
                Log.e(TAG, "encode error: " + outputBufferIndex);
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }
        }
    }

    public void start() {
        if (mVedioCodec == null) {
            throw new RuntimeException("mVedioCodec is null");
        }
        isRecording = true;
        mVedioCodec.start();
    }

    public void stop() {
        isRecording = false;
        mVedioCodec.signalEndOfInputStream();
    }

    private void release() {
        if (mVedioCodec != null) {
            mVedioCodec.stop();
            mVedioCodec.release();
        }
    }
}
