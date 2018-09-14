package com.jscheng.scamera.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

/**
 * Created By Chengjunsen on 2018/9/14
 */
public class AudioPlayerThread extends Thread{
    private static final int TIMEOUT_S = 10000;
    private AudioTrack mAudioTrack;
    private MediaExtractor mExtractor;
    private String path;
    private MediaCodec mAudioCodec;
    private MediaCodec.BufferInfo mBufferInfo;

    public AudioPlayerThread(String path) {
        this.path = path;
    }

    public boolean prapare() {
        try {
            mExtractor = new MediaExtractor();
            mBufferInfo = new MediaCodec.BufferInfo();
            mExtractor.setDataSource(path);
            MediaFormat mediaFormat = null;
            int selectTrack = 0;
            String mine;
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                mediaFormat = mExtractor.getTrackFormat(i);
                mine = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                    selectTrack = i;
                    break;
                }
            }
            mExtractor.selectTrack(selectTrack);
            int samplerate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int changelConfig = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int minBufferSize = AudioTrack.getMinBufferSize(samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT);
            minBufferSize = Math.max(minBufferSize, 1024);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            mAudioTrack.play();
            mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mAudioCodec.configure(mediaFormat, null, null, 0);
            mAudioCodec.start();
        } catch (IOException e) {
            Log.e(TAG, "video player prapare: " + e.toString() );
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        boolean isFinish = false;
        while (!isFinish) {
            int inputIdex = mAudioCodec.dequeueInputBuffer(TIMEOUT_S);
            if (inputIdex < 0) {
                isFinish = true;
            }
            ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputIdex);
            inputBuffer.clear();
            int samplesize = mExtractor.readSampleData(inputBuffer, 0);
            if (samplesize > 0) {
                mAudioCodec.queueInputBuffer(inputIdex, 0, samplesize, 0, 0);
                mExtractor.advance();
            } else {
                isFinish = true;
            }
            int outputIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);
            ByteBuffer outputBuffer;
            byte[] chunkPCM;
            while (outputIndex >= 0) {// 每次解码完成的数据不一定能一次吐出 所以用 while 循环，保证解码器吐出所有数据
                outputBuffer = mAudioCodec.getOutputBuffer(outputIndex);// 拿到用于存放 PCM 数据的 Buffer
                chunkPCM = new byte[mBufferInfo.size];//BufferInfo 内定义了此数据块的大小
                outputBuffer.get(chunkPCM);// 将 Buffer 内的数据取出到字节数组中
                outputBuffer.clear();// 数据取出后一定记得清空此 Buffer MediaCodec 是循环使用这些 Buffer 的，不清空下次会得到同样的数据
                mAudioTrack.write(chunkPCM, 0, mBufferInfo.size);
                mAudioCodec.releaseOutputBuffer(outputIndex, false);// 此操作一定要做，不然 MediaCodec 用完所有的 Buffer 后 将不能向外输出数据
                outputIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_S);// 再次获取数据，如果没有数据输出则 outputIndex=-1 循环结束
            }
        }
        release();
    }

    private void release() {
        if (mAudioCodec != null) {
            mAudioCodec.stop();
            mAudioCodec.release();
            mAudioCodec = null;
        }

        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }
}
