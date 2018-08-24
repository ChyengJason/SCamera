package com.jscheng.scamera.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.jscheng.scamera.R;
import com.jscheng.scamera.util.CameraUtil;
import com.jscheng.scamera.util.PermisstionUtil;
import com.jscheng.scamera.widget.CameraFocusView;
import com.jscheng.scamera.widget.CameraProgressButton;

import java.util.List;

/**
 * Created By Chengjunsen on 2018/8/22
 */
public class CameraFragment extends Fragment implements CameraProgressButton.Listener, TextureView.SurfaceTextureListener{
    private final static String TAG = CameraFragment.class.getSimpleName();
    private final static int CAMERA_REQUEST_CODE = 1;
    private TextureView mCameraView;
    private CameraProgressButton mProgressBtn;
    private CameraFocusView mFocusView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_camera, container, false);
        mCameraView = contentView.findViewById(R.id.camera_view);
        mProgressBtn = contentView.findViewById(R.id.progress_btn);
        mFocusView = contentView.findViewById(R.id.focus_view);
        mCameraView.setSurfaceTextureListener(this);
        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    cameraFocus((int)event.getX(), (int)event.getY(), false);
                    return true;
                }
                return false;
            }
        });
        return contentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        CameraUtil.releaseCamera();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        cameraFocus(width/2, height/2, true);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releasePreview();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public void startPreview() {
        if (requestCameraPermission()) {
            if (CameraUtil.getCamera() == null) {
                CameraUtil.openCamera();
            }
            CameraUtil.startPreview(getActivity(), mCameraView.getSurfaceTexture(), mCameraView.getWidth(), mCameraView.getHeight());
        }
    }

    public void releasePreview() {
        CameraUtil.releaseCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPreview();
    }

    @Override
    public void onShortPress() {
    }

    @Override
    public void onStartLongPress() {
    }

    @Override
    public void onEndLongPress() {
    }

    @Override
    public void onEndMaxProgress() {
    }

    public boolean requestCameraPermission() {
        List<String> notGrantPermissions = PermisstionUtil.isPermissionsAllGranted(getContext(), PermisstionUtil.CAMERA);
        if(notGrantPermissions.isEmpty()){
          return true;
        }
        if (PermisstionUtil.judgePermission(getContext(), PermisstionUtil.CAMERA)) {
            PermisstionUtil.showPermissionAlterDialog(getContext(), "请求相机权限被拒绝");
        } else {
            PermisstionUtil.requestPermissions(getContext(), PermisstionUtil.CAMERA, CAMERA_REQUEST_CODE);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPreview();
        }
    }

    private void cameraFocus(final int x, final int y, final boolean isAutoFocus) {
        Point focusPoint = new Point(x, y);
        Size cameraSize = new Size(mCameraView.getWidth(), mCameraView.getHeight());
        mFocusView.beginFocus(x, y);
        CameraUtil.newCameraFocus(focusPoint, cameraSize, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (!isAutoFocus) {
                        mFocusView.endFocus(success);
                }
            }
        });
    }
}
