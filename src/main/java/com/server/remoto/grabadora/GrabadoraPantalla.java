package com.server.remoto.grabadora;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Java2DFrameUtils;

import java.awt.image.BufferedImage;

public class GrabadoraPantalla {
    private FFmpegFrameRecorder recorder;
    private volatile boolean grabando = false;
    private String archivoVideoPath;

    public void start(String filePath, int width, int height) throws Exception {
        FFmpegLogCallback.set();
        this.archivoVideoPath = filePath;
        recorder = new FFmpegFrameRecorder(filePath, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(24);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.start();
        grabando = true;
    }

    public void encodeFrame(BufferedImage img) throws Exception {
        if (!grabando || recorder == null) return;
        if (img == null || img.getWidth() == 0 || img.getHeight() == 0) return;
        Frame frame = Java2DFrameUtils.toFrame(img);
        recorder.record(frame);
    }

    public void stop() throws Exception {
        if (grabando && recorder != null) {
            recorder.stop();
            recorder.release();
            grabando = false;
        }
    }

    public boolean isGrabando() {
        return grabando;
    }

    public String getArchivoVideoPath() {
        return archivoVideoPath;
    }
}
