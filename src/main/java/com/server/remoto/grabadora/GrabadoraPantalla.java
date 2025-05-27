package com.server.remoto.grabadora;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;

public class GrabadoraPantalla {
    private FFmpegFrameRecorder recorder;
    private volatile boolean grabando = false;
    private String archivoVideoPath;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();

    public void start(String filePath, int width, int height) throws Exception {
        FFmpegLogCallback.set();
        this.archivoVideoPath = filePath;
        recorder = new FFmpegFrameRecorder(filePath, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(24);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // Este es el esperado por la mayoría de codecs H264
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");

        recorder.start();
        grabando = true;
    }

    public void encodeFrame(BufferedImage img) throws Exception {
        if (!grabando || recorder == null || img == null) return;

        Frame rgbFrame = java2DConverter.convert(img);
        recorder.record(rgbFrame); // JavaCV ya se encarga de hacer la conversión a YUV420P internamente
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
