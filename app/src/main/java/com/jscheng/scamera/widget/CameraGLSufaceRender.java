package com.jscheng.scamera.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
/**
 * Created By Chengjunsen on 2018/8/25
 */
public class CameraGLSufaceRender extends GLAbstractRender {
    private Context mContext;
    private int mTexture;
    private SurfaceTexture mSurfaceTexture;
    private FloatBuffer vertexBuffer;
    private FloatBuffer backTextureBuffer;
    private FloatBuffer frontTextureBuffer;
    private CameraGLSufaceRenderCallback mRenderCallback;
    private boolean isBackCamera;

    private float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // 左下角
            1f, -1f, 0.0f, // 右下角
            -1f, 1f, 0.0f, // 左上角
            1f, 1f, 0.0f,  // 右上角
    };

    // 纹理坐标对应顶点坐标与后置摄像头映射
    private float backTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };

    // 纹理坐标对应顶点坐标与前置摄像头映射
    private float frontTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f // 右下角
    };

    // 每次取点的数量
    private final int CoordsPerVertexCount = 3;
    // 顶点坐标数量
    private final int VertexCount = vertexData.length / CoordsPerVertexCount;
    // 一次取出的大小
    private final int VertexStride = CoordsPerVertexCount * 4;

    // 每次取点的数量
    private final int CoordsPerTextureCount = 2;
    // 一次取出的大小
    private final int TextureStride = CoordsPerTextureCount * 4;

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    public CameraGLSufaceRender(Context context, CameraGLSufaceRenderCallback mRenderCallback) {
        super(context);
        this.isBackCamera = true;
        this.mContext = context;
        this.mRenderCallback = mRenderCallback;

        this.vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        this.vertexBuffer.position(0);

        this.backTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        this.backTextureBuffer.position(0);

        this.frontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        this.frontTextureBuffer.position(0);
    }

    @Override
    protected void onCreate() {
        mTexture = loadExternelTexture();

        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
        mSurfaceTexture = new SurfaceTexture(mTexture);
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mRenderCallback != null) {
                    mRenderCallback.onRequestRender();
                }
            }
        });
        if (mRenderCallback != null) {
            mRenderCallback.onCreate(mSurfaceTexture);
        }
    }

    @Override
    protected void onChanged() {
        if (mRenderCallback != null) {
            mRenderCallback.onChanged(width, height);
        }
    }

    @Override
    protected void onDraw() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
        }

        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);

        // 设置顶点位置值
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, vertexBuffer);
        // 设置纹理位置值
        if (isBackCamera) {
            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, backTextureBuffer);
        } else {
            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, frontTextureBuffer);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(s_Texture, 0);
        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

        if (mRenderCallback != null) {
            mRenderCallback.onDraw();
        }
    }

    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setBackCamera(boolean isBackCamera) {
        this.isBackCamera = isBackCamera;
    }

    public interface CameraGLSufaceRenderCallback {
        void onRequestRender();
        void onCreate(SurfaceTexture texture);
        void onChanged(int width, int height);
        void onDraw();
    }
}
