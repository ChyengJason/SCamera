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
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.jscheng.scamera.R;
import com.jscheng.scamera.util.CameraUtil;
import com.jscheng.scamera.util.PermisstionUtil;
import com.jscheng.scamera.widget.CameraFocusView;
import com.jscheng.scamera.widget.CameraProgressButton;
import com.jscheng.scamera.widget.CameraSwitchView;

/**
 * Created By Chengjunsen on 2018/8/22
 */
public class CameraFragment extends Fragment implements CameraProgressButton.Listener, TextureView.SurfaceTextureListener, CameraSensor.CameraSensorListener{
    private final static String TAG = CameraFragment.class.getSimpleName();
    private final static int CAMERA_REQUEST_CODE = 1;
    private final static int STORE_REQUEST_CODE = 2;

    private TextureView mCameraView;
    private CameraSensor mCameraSensor;
    private CameraProgressButton mProgressBtn;
    private CameraFocusView mFocusView;
    private CameraSwitchView mSwitchView;
    // 是否正在对焦
    private boolean isFocusing;
    private Size mPreviewSize = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_camera, container, false);
        initView(contentView);
        return contentView;
    }

    private void initView(View contentView) {
        isFocusing = false;

        mCameraView = contentView.findViewById(R.id.camera_view);
        mProgressBtn = contentView.findViewById(R.id.progress_btn);
        mFocusView = contentView.findViewById(R.id.focus_view);
        mSwitchView = contentView.findViewById(R.id.switch_view);

        mCameraSensor = new CameraSensor(getContext());
        mCameraSensor.setCameraSensorListener(this);
        mCameraView.setSurfaceTextureListener(this);
        mProgressBtn.setProgressListener(this);

        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    focus((int)event.getX(), (int)event.getY(), false);
                    return true;
                }
                return false;
            }
        });
        mSwitchView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mFocusView.cancelFocus();
                if (mPreviewSize != null) {
                    CameraUtil.switchCamera(getActivity(),
                            !CameraUtil.isBackCamera(),
                            mCameraView.getSurfaceTexture(),
                            mPreviewSize.getWidth(),
                            mPreviewSize.getHeight());
                }
            }
        });
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
        mPreviewSize = new Size(width, height);
        startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        focus(width/2, height/2, true);
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
            if (mPreviewSize != null) {
                CameraUtil.startPreview(getActivity(), mCameraView.getSurfaceTexture(), mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mCameraSensor.start();
                mSwitchView.setOrientation(mCameraSensor.getX(), mCameraSensor.getY(), mCameraSensor.getZ());
            }
        }
    }

    public void releasePreview() {
        CameraUtil.releaseCamera();
        mCameraSensor.stop();
        mFocusView.cancelFocus();
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
        if (requestStoragePermission()) {

        }
    }

    @Override
    public void onStartLongPress() {
        if (requestStoragePermission()) {

        }
    }

    @Override
    public void onEndLongPress() {
    }

    @Override
    public void onEndMaxProgress() {
    }

    private boolean requestCameraPermission() {
        return PermisstionUtil.checkPermissionsAndRequest(getContext(), PermisstionUtil.CAMERA, CAMERA_REQUEST_CODE, "请求相机权限被拒绝");
    }

    private boolean requestStoragePermission() {
        return PermisstionUtil.checkPermissionsAndRequest(getContext(), PermisstionUtil.STORAGE, STORE_REQUEST_CODE, "请求访问SD卡权限被拒绝");
    }

    private void focus(final int x, final int y, final boolean isAutoFocus) {
        if (!CameraUtil.isBackCamera()) {
            return;
        }
        if (isFocusing && isAutoFocus) {
            return;
        }
        isFocusing = true;
        Point focusPoint = new Point(x, y);
        Size screenSize = new Size(mCameraView.getWidth(), mCameraView.getHeight());
        if (!isAutoFocus) {
            mFocusView.beginFocus(x, y);
        }
        CameraUtil.newCameraFocus(focusPoint, screenSize, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                isFocusing = false;
                if (!isAutoFocus) {
                    mFocusView.endFocus(success);
                }
            }
        });
    }

    @Override
    public void onRock() {
        if (CameraUtil.isBackCamera() && CameraUtil.getCamera() != null) {
            focus(mCameraView.getWidth() / 2, mCameraView.getHeight() / 2, true);
        }
        mSwitchView.setOrientation(mCameraSensor.getX(), mCameraSensor.getY(), mCameraSensor.getZ());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPreview();
        }
    }
}
