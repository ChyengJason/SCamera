package com.jscheng.scamera.view;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.jscheng.scamera.BaseActivity;
import com.jscheng.scamera.R;
import java.io.IOException;
import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/6
 */
public class VideoActivity extends BaseActivity implements TextureView.SurfaceTextureListener{
    private static final int TIMEOUT_S = 12000;

    private TextureView mTextureView;
    private String path;
    private MediaCodec mMediaCodec;
    private MediaExtractor mExtractor;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mFrameRate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video);
        path = getIntent().getStringExtra("path");
        mTextureView = findViewById(R.id.video_view);
        mTextureView.setSurfaceTextureListener(this);
    }

    private boolean initVideoView() {
        mTextureView.setRotation(90);
        return true;
    }

    private boolean initMediaCodec(SurfaceTexture surfaceTexture) {
        try {
            Surface surface = new Surface(surfaceTexture);
            mExtractor = new MediaExtractor();
            mBufferInfo = new MediaCodec.BufferInfo();
            mExtractor.setDataSource(path);
            MediaFormat mediaFormat = null;
            int selectTrack = 0;
            String mine;
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                mediaFormat = mExtractor.getTrackFormat(i);
                mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    selectTrack = i;
                    break;
                }
            }
            mExtractor.selectTrack(selectTrack);
            mFrameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat, surface, null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            Log.e(TAG, "initMediaCodec: " + e.toString() );
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void decode() {
        long startMs = System.currentTimeMillis();
        int index = 0;
        while(true) {
            int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputIndex < 0) {
                Log.e(TAG, "decode inputIdex < 0");
                SystemClock.sleep(50);
                continue;
            }

            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            int samplesize = mExtractor.readSampleData(inputBuffer, 0);
            Log.e(TAG, "decode samplesize: " + samplesize);
            if (samplesize <= 0) {
                break;
            }
            mMediaCodec.queueInputBuffer(inputIndex, 0, samplesize, getPts(index++, mFrameRate), 0);
            int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            Log.e(TAG, "decode: outputIndex " + outputIndex);
            while (outputIndex > 0) {
                //帧控制
                while (mBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    SystemClock.sleep(50);
                }
                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            }
            if (!mExtractor.advance()) {
                break;
            }
        }
        release();
    }

    private long getPts(int index, int frameRate) {
        return index * 1000000 / frameRate;
    }

    private void release() {
        Log.e(TAG, "resolveVideo release ");
        try {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (initVideoView() && initMediaCodec(surfaceTexture)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    decode();
                }
            }).start();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
}
