package com.server.remoto.grabadora;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GrabadoraPantalla {
    private FFmpegFrameRecorder recorder;
    private volatile boolean grabando = false;
    private String archivoVideoPath;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();

    // Variables para control de tiempo
    private long tiempoInicio;
    private long frameCount = 0;
    private static final double TARGET_FPS = 15.0; //10 FPS
    private long lastFrameTime = 0;

    // Executor para procesamiento en hilo separado
    private ExecutorService encodingExecutor;
    private volatile boolean encodingActive = true;

    public void start(String filePath, int width, int height) throws Exception {
        FFmpegLogCallback.set();
        this.archivoVideoPath = filePath;
        recorder = new FFmpegFrameRecorder(filePath, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(TARGET_FPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // Configuración de calidad
        recorder.setVideoOption("preset", "ultrafast"); // Mejor balance calidad/velocidad
        recorder.setVideoOption("crf", "20"); // Mejor calidad (menor número = mejor calidad)
        recorder.setVideoOption("profile", "high"); // Perfil H.264 alto
        recorder.setVideoBitrate(500000); // 2 Mbps para buena calidad

        encodingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VideoEncoder");
            t.setDaemon(true);
            return t;
        });
        encodingActive = true;

        recorder.start();
        grabando = true;
        tiempoInicio = System.currentTimeMillis();
        frameCount = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public void encodeFrame(BufferedImage img) throws Exception {
        if (!grabando || recorder == null || img == null || !encodingActive) return;

        BufferedImage imageCopy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        imageCopy.getGraphics().drawImage(img, 0, 0, null);
        imageCopy.getGraphics().dispose();

        long captureTime = System.currentTimeMillis();

        encodingExecutor.submit(() -> {
            try {
                if (!grabando || recorder == null || !encodingActive) return;

                Frame rgbFrame = java2DConverter.convert(imageCopy);

                long tiempoTranscurrido = captureTime - tiempoInicio;
                double timestampSegundos = tiempoTranscurrido / 1000.0;
                recorder.setTimestamp(Math.round(timestampSegundos * 1000000));

                synchronized (recorder) {
                    recorder.record(rgbFrame);
                    frameCount++;
                }

            } catch (Exception e) {
                System.err.println("Error en encoding de frame: " + e.getMessage());
            }
        });
    }

    public void stop() throws Exception {
        if (grabando && recorder != null) {
            grabando = false;
            encodingActive = false;

            if (encodingExecutor != null) {
                encodingExecutor.shutdown();
                try {
                    if (!encodingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        encodingExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    encodingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            synchronized (recorder) {
                recorder.stop();
                recorder.release();
            }

            // Información de grabacion
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