package com.jscheng.scamera.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.jscheng.scamera.util.StorageUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
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

    private HashMap<String, MediaCodecInfo.CodecCapabilities> mEncoderInfos;
    private long generateIndex = 0;
    private Queue<byte[]> dataQueue;
    private Thread mRecordThread;
    private boolean isRecording;
    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private FileOutputStream mVideoFile;

    public CameraRecorder(int width, int heigth) {
        dataQueue = new LinkedBlockingQueue<>();
        mRecordThread = new Thread(this);
        isRecording = false;
        initMediaCodec(width, heigth);
        initVideoFile();
    }

    private void initMediaCodec(int width, int height) {
        try {
            String mime = MediaFormat.MIMETYPE_VIDEO_AVC;
            int colorFormat = selectColorFormat(selectCodec(mime), mime);
            Log.e(TAG,"setupEncoder " + mime + " colorFormat:" + colorFormat + " w:" + width + " h:" + height);

            mBufferInfo = new MediaCodec.BufferInfo();
            mMediaCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initVideoFile() {
        try {
            String path = StorageUtil.getVedioPath();
            StorageUtil.checkDirExist(path);
            mVideoFile = new FileOutputStream(path + "vedio.mp4");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG,"couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
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
            try {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);
                byte[] outputData = new byte[mBufferInfo.size];
                outputBuffer.get(outputData);
                mVideoFile.write(outputData);
                mMediaCodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void endEncode() {
        mMediaCodec.stop();
        try {
            mVideoFile.flush();
            mVideoFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }
}
