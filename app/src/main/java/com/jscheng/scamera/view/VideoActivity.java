package com.jscheng.scamera.view;

import android.media.MediaCodec;
import android.media.MediaExtractor;
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
import java.io.IOException;
import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/6
 */
public class VideoActivity extends BaseActivity implements SurfaceHolder.Callback{
    private static final int TIMEOUT_S = 12000;

    private SurfaceView mSurfaceView;
    private String path;
    private MediaCodec mMediaCodec;
    private MediaExtractor mExtractor;
    private boolean isFinish;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video);
        path = getIntent().getStringExtra("path");
        //path = StorageUtil.getVedioPath() + "test.mp4";
        mSurfaceView = findViewById(R.id.video_view);
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (initMediaCodec(surfaceHolder)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    decode();
                }
            }).start();
        }
    }

    private boolean initMediaCodec(SurfaceHolder surfaceHolder) {
        try {
            isFinish = false;
            mExtractor = new MediaExtractor();
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
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(mediaFormat, surfaceHolder.getSurface(), null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            Log.e(TAG, "initMediaCodec: " + e.toString() );
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private void decode() {
        while(!isFinish) {
            int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputIndex < 0) {
                Log.e(TAG, "decode: inputIdex < 0");
                continue;
            }
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            int samplesize = mExtractor.readSampleData(inputBuffer, 0);
            Log.e(TAG, "decode: samplesize " + samplesize);
            if (samplesize > 0) {
                mMediaCodec.queueInputBuffer(inputIndex, 0, samplesize, 0, 0);
                mExtractor.advance();
            } else {
                isFinish = true;
            }

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            Log.e(TAG, "endecode outputIndex: " + outputIndex);
            while (outputIndex > 0) {
                ByteBuffer outBuffer = mMediaCodec.getOutputBuffer(outputIndex);
                Log.e(TAG, "endecode outputIndex: " + outputIndex);
                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            }
        }
        release();
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
    }
}
