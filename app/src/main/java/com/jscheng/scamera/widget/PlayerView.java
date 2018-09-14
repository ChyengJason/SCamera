package com.jscheng.scamera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/9/13
 */
public class PlayerView extends TextureView{
    private int mVideoWidth, mVideoHeight;

    public PlayerView(Context context) {
        super(context);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.mVideoHeight = videoHeight;
        this.mVideoWidth = videoWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int videoWidth = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int videoHeight = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(videoHeight, videoWidth);
    }

}
