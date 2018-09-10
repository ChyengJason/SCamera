package com.jscheng.scamera.record;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created By Chengjunsen on 2018/9/8
 */
public class MutexBean {
    private ByteBuffer byteBuffer;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isMedia;

    public MutexBean(boolean isMedia, byte[] bytes, MediaCodec.BufferInfo bufferInfo) {
        this.isMedia = isMedia;
        this.byteBuffer = ByteBuffer.wrap(bytes);
        this.bufferInfo = bufferInfo;
    }

    public boolean isMedia() {
        return isMedia;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }
}
