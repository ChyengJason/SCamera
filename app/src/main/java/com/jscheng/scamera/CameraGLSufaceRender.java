package com.jscheng.scamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.jscheng.scamera.util.LogUtil.TAG;

/**
 * Created By Chengjunsen on 2018/8/25
 */
public class CameraGLSufaceRender extends GLAbstractRender {
    private Context mContext;
    private int mExternalTexture;
    private int mFrameBufferTexture;
    private int mFrameBuffer;
    private SurfaceTexture mExternalSurfaceTexture;
    private FloatBuffer vertexBuffer;
    private FloatBuffer backTextureBuffer;
    private FloatBuffer frontTextureBuffer;
    private CameraGLSufaceRenderCallback mRenderCallback;
    private boolean isBackCamera;

    private float vertexData[] = {
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

        mExternalTexture = loadExternelTexture(); // 外部数据纹理
//        mFrameBufferTexture = createTexture();
//        mFrameBuffer = createFrameBuffer();

        createFrameBufferAndTexture();

        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
        mExternalSurfaceTexture = new SurfaceTexture(mExternalTexture);
        mExternalSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mRenderCallback != null) {
                    mRenderCallback.onRequestRender();
                }
            }
        });
        if (mRenderCallback != null) {
            mRenderCallback.onCreate(mExternalSurfaceTexture);
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
        if (mExternalSurfaceTexture != null) {
            mExternalSurfaceTexture.updateTexImage();
        }

        //drawExternalToFrameTexture();
        drawExternalToDisplay();

        if (mRenderCallback != null) {
            mRenderCallback.onDraw();
        }
    }


    private void createFrameBufferAndTexture() {
        int[] FBO = new int[1];
        int[] texture = new int[1];

        GLES20.glGenFramebuffers(1, FBO, 0);
        //创建纹理,当把一个纹理附着到FBO上后，所有的渲染操作就会写入到该纹理上，意味着所有的渲染操作会被存储到纹理图像上，
        //这样做的好处是显而易见的，我们可以在着色器中使用这个纹理。
        GLES20.glGenTextures(1, texture, 0);
        //bind纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //创建输出纹理，方法基本相同，不同之处在于glTexImage2D最后一个参数为null，不指定数据指针。
        //使用了glTexImage2D函数，使用GLUtils#texImage2D函数加载一幅2D图像作为纹理对象，
        //这里的glTexImage2D稍显复杂，这里重要的是最后一个参数，
        //如果为null就会自动分配可以容纳相应宽高的纹理，然后后续的渲染操作就会存储到这个纹理上了。
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        //指定纹理格式
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        //绑定帧缓冲区,第一个参数是target，指的是你要把FBO与哪种帧缓冲区进行绑定,此时创建的帧缓冲对象其实只是一个“空壳”，
        //它上面还包含一些附着，因此接下来还必须往它里面添加至少一个附着才可以,
        // 使用创建的帧缓冲必须至少添加一个附着点（颜色、深度、模板缓冲）并且至少有一个颜色附着点。
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBO[0]);
        /**
         * 函数将2D纹理附着到帧缓冲对象
         * glFramebufferTexture2D()把一幅纹理图像关联到一个FBO,第二个参数是关联纹理图像的关联点,一个帧缓冲区对象可以有多个颜色关联点0~n
         * 第三个参数textureTarget在多数情况下是GL_TEXTURE_2D。第四个参数是纹理对象的ID号
         * 最后一个参数是要被关联的纹理的mipmap等级 如果参数textureId被设置为0，那么纹理图像将会被从FBO分离
         * 如果纹理对象在依然关联在FBO上时被删除，那么纹理对象将会自动从当前帮的FBO上分离。然而，如果它被关联到多个FBO上然后被删除，
         * 那么它将只被从绑定的FBO上分离，而不会被从其他非绑定的FBO上分离。
         */
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[0], 0);
        //现在已经完成了纹理的加载，不需要再绑定此纹理了解绑纹理对象
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //解绑帧缓冲对象
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = "initFrameBuffer: glError 0x" + Integer.toHexString(error);
            Log.e(TAG, "createFrameBufferAndTexture: " + msg );
            return;
        }
        mFrameBufferTexture = texture[0];
        mFrameBuffer = FBO[0];
    }


    private void drawExternalToDisplay() {
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
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTexture);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(s_Texture, 0);
        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    // 将 mExternalTexture 绘制到了 mFrameBufferTexture
    private void drawExternalToFrameTexture() {
        // 直接绘制到mFrameBuffer中
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);

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
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTexture);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(s_Texture, 0);
        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

//    private void drawFrameTextureToDisplay() {
//        GLES20.glEnableVertexAttribArray(av_Position);
//        GLES20.glEnableVertexAttribArray(af_Position);
//
//        // 设置顶点位置值
//        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, vertexBuffer);
//        // 设置纹理位置值
//        if (isBackCamera) {
//            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, backTextureBuffer);
//        } else {
//            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, frontTextureBuffer);
//        }
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
////        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTexture);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTexture);
//        GLES20.glUniform1i(s_Texture, 0);
//        // 绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
//        GLES20.glDisableVertexAttribArray(av_Position);
//        GLES20.glDisableVertexAttribArray(af_Position);
//
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//    }

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

    private final String mFragmentShaderRgba =
            "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform sampler2D s_texture;\n" +
                    "void main() {\n" +
                    "gl_FragColor = texture2D(s_texture, textureCoordinate);\n" +
                    "}";



    public SurfaceTexture getSurfaceTexture() {
        return mExternalSurfaceTexture;
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
