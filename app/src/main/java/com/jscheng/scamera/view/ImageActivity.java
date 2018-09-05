package com.jscheng.scamera.view;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.jscheng.scamera.BaseActivity;
import com.jscheng.scamera.R;

import java.io.File;

/**
 * Created By Chengjunsen on 2018/9/5
 */
public class ImageActivity extends BaseActivity {
    private String path = null;
    private ImageView mImageView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image);
        mImageView = findViewById(R.id.image_view);
        path = getIntent().getStringExtra("path");
        resolvImage();
    }

    private void resolvImage() {
        if (path.isEmpty()) {
            return;
        }
        mImageView.setImageURI(Uri.fromFile(new File(path)));
    }
}
