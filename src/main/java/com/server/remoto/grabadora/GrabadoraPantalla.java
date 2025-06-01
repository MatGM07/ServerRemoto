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

    // Variables para control de tiempo
    private long tiempoInicio;
    private long frameCount = 0;
    private static final double TARGET_FPS = 15.0; // FPS objetivo más realista para captura de pantalla
    private long lastFrameTime = 0;
    private final long frameInterval = (long)(1000.0 / TARGET_FPS); // Intervalo entre frames en ms

    public void start(String filePath, int width, int height) throws Exception {
        FFmpegLogCallback.set();
        this.archivoVideoPath = filePath;
        recorder = new FFmpegFrameRecorder(filePath, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(TARGET_FPS); // Usar FPS objetivo
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");

        recorder.start();
        grabando = true;
        tiempoInicio = System.currentTimeMillis();
        frameCount = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public void encodeFrame(BufferedImage img) throws Exception {
        if (!grabando || recorder == null || img == null) return;

        long currentTime = System.currentTimeMillis();

        // Control de velocidad: solo procesar frame si ha pasado suficiente tiempo
        if (currentTime - lastFrameTime < frameInterval) {
            return; // Saltar este frame para mantener el FPS objetivo
        }

        Frame rgbFrame = java2DConverter.convert(img);

        // Establecer timestamp basado en el tiempo real transcurrido
        long tiempoTranscurrido = currentTime - tiempoInicio;
        double timestampSegundos = tiempoTranscurrido / 1000.0;
        recorder.setTimestamp(Math.round(timestampSegundos * 1000000)); // Microsegundos

        recorder.record(rgbFrame);
        frameCount++;
        lastFrameTime = currentTime;
    }

    public void stop() throws Exception {
        if (grabando && recorder != null) {
            recorder.stop();
            recorder.release();
            grabando = false;

            // Información de debug
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            double fpsReal = (frameCount * 1000.0) / tiempoTotal;
            System.out.println("Grabación finalizada:");
            System.out.println("- Frames grabados: " + frameCount);
            System.out.println("- Tiempo total: " + (tiempoTotal / 1000.0) + " segundos");
            System.out.println("- FPS real: " + String.format("%.2f", fpsReal));
        }
    }

    public boolean isGrabando() {
        return grabando;
    }

    public String getArchivoVideoPath() {
        return archivoVideoPath;
    }
}