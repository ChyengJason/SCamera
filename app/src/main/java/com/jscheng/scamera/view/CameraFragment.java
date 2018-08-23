package com.jscheng.scamera.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.jscheng.scamera.R;
import com.jscheng.scamera.util.CameraUtil;
import com.jscheng.scamera.util.PermisstionUtil;
import com.jscheng.scamera.widget.VideoProgressButton;

import java.util.List;

/**
 * Created By Chengjunsen on 2018/8/22
 */
public class CameraFragment extends Fragment implements VideoProgressButton.Listener, TextureView.SurfaceTextureListener{
    private final static String TAG = CameraFragment.class.getSimpleName();
    private final static int CAMERA_REQUEST_CODE = 1;
    private TextureView mCameraView;
    private VideoProgressButton mProgressBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_camera, container, false);
        mCameraView = contentView.findViewById(R.id.camera_view);
        mProgressBtn = contentView.findViewById(R.id.progress_btn);
        mProgressBtn.setListener(this);
        mCameraView.setSurfaceTextureListener(this);
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

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        CameraUtil.releaseCamera();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public void startPreview() {
        if (requestCameraPermission()) {
            if (CameraUtil.getCamera() == null) {
                CameraUtil.openCamera(getActivity());
            }
            CameraUtil.startPreview(mCameraView.getSurfaceTexture());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        requestPermissions(PermisstionUtil.CAMERA, CAMERA_REQUEST_CODE);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPreview();
            } else {
                PermisstionUtil.showPermissionAlterDialog(getContext(), "获取相机权限被拒绝");
            }
        }
    }
}
