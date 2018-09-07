package com.jscheng.scamera.view;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import com.jscheng.scamera.BaseActivity;
import com.jscheng.scamera.R;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/6
 */
public class VideoActivity extends BaseActivity implements SurfaceHolder.Callback{
    private static final int TIMEOUT_S = 12000;
    private int mFrameRate = 30;
    private int mBitRate = 500000;
    private int mIFrameInterval = 1;
    private int frameIndex;

    private SurfaceView mSurfaceView;
    private String path;
    private int width, height;
    private MediaCodec mMediaCodec;
    private FileInputStream mMediaFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video);
        path = getIntent().getStringExtra("path");
        width = getIntent().getIntExtra("width", 1280);
        height = getIntent().getIntExtra("height", 720);
        mSurfaceView = findViewById(R.id.video_view);
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            initMediaCodec(surfaceHolder);
            initVideoFile();
            resolveVideo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initVideoFile() {
        try {
            mMediaFile = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "initVideoFile: " + e.toString());
            e.printStackTrace();
        }
    }

    private void initMediaCodec(SurfaceHolder surfaceHolder) {
        try {
            frameIndex = 0;
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
//            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mMediaCodec.configure(mediaFormat, surfaceHolder.getSurface(), null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void resolveVideo() throws IOException {
        byte[] data = new byte[width * height * 5];
        int len = 0;
        while((len = mMediaFile.read(data)) > 0) {
            decode(data, 0, len);
        }
        release();
    }

    private void decode(byte[] data, int offset, int length) {
        Log.e(TAG, "decode: " + length );
        int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
        if (inputIndex < 0) {
            Log.e(TAG, "decode: inputIdex < 0");
        }
        long pts = computePresentationTime(frameIndex);
        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
        inputBuffer.clear();
        inputBuffer.put(data, offset, length);
        mMediaCodec.queueInputBuffer(inputIndex, offset, length, frameIndex * mIFrameInterval, 0);
        frameIndex++;

        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
        Log.e(TAG, "endecode outputIndex: " + outputIndex );
        while(outputIndex > 0) {
            ByteBuffer outBuffer = mMediaCodec.getOutputBuffer(outputIndex);
            Log.e(TAG, "endecode outputIndex: " + outputIndex );
            mMediaCodec.releaseOutputBuffer(outputIndex, true);
            outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
        }
        Log.e(TAG, "endecode: " + length );
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }

    public void release() {
        Log.e(TAG, "resolveVideo: release ");
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMediaFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
