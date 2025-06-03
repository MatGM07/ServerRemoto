package com.server.remoto.services.recording;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
@Component
public class GrabadoraPantalla {
    private FFmpegFrameRecorder recorder;
    private volatile boolean grabando = false;
    private String archivoVideoPath;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter java2DConverter = new Java2DFrameConverter();


    private long tiempoInicio;
    private long frameCount = 0;
    private static final double TARGET_FPS = 10.0;
    private long lastFrameTime = 0;
    private final long FRAME_INTERVAL_MS = (long)(1000.0 / TARGET_FPS);

    // Executor para procesamiento en hilo separado
    private ExecutorService encodingExecutor;
    private volatile boolean encodingActive = true;

    // Timer para controlar captura de frames
    private Timer captureTimer;
    private volatile BufferedImage ultimoFrame;

    public void start(String filePath, int width, int height) throws Exception {
        FFmpegLogCallback.set();
        this.archivoVideoPath = filePath;
        recorder = new FFmpegFrameRecorder(filePath, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");

        // Establecer framerate fijo que sea realista
        recorder.setFrameRate(TARGET_FPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // Configuración de calidad optimizada
        recorder.setVideoOption("preset", "medium");
        recorder.setVideoOption("crf", "23"); // Calidad balanceada
        recorder.setVideoOption("profile", "high");
        // Remover bitrate fijo para evitar conflictos con CRF

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

        // Iniciar timer para captura regular de frames
        iniciarCapturaRegular();
    }

    private void iniciarCapturaRegular() {
        captureTimer = new Timer();
        captureTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (grabando && ultimoFrame != null) {
                    try {
                        procesarFrameRegular(ultimoFrame);
                    } catch (Exception e) {
                        System.err.println("Error en captura regular: " + e.getMessage());
                    }
                }
            }
        }, 0, FRAME_INTERVAL_MS);
    }

    // Método para actualizar el frame actual (llamar desde tu loop de captura)
    public void actualizarFrame(BufferedImage img) {
        if (!grabando || img == null) return;

        // Crear copia del frame
        BufferedImage imageCopy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        imageCopy.getGraphics().drawImage(img, 0, 0, null);
        imageCopy.getGraphics().dispose();

        ultimoFrame = imageCopy;
    }

    // Método mejorado con control de timing
    public void encodeFrame(BufferedImage img) throws Exception {
        if (!grabando || img == null) return;

        long currentTime = System.currentTimeMillis();

        // Control de FPS: solo procesar si ha pasado suficiente tiempo
        if (currentTime - lastFrameTime >= FRAME_INTERVAL_MS) {
            BufferedImage imageCopy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
            imageCopy.getGraphics().drawImage(img, 0, 0, null);
            imageCopy.getGraphics().dispose();

            encodingExecutor.submit(() -> procesarFrame(imageCopy));
            lastFrameTime = currentTime;
        }
    }

    private void procesarFrameRegular(BufferedImage imagen) throws Exception {
        if (!grabando || recorder == null || !encodingActive || imagen == null) return;

        // Crear copia para procesamiento asíncrono
        BufferedImage imageCopy = new BufferedImage(imagen.getWidth(), imagen.getHeight(), imagen.getType());
        imageCopy.getGraphics().drawImage(imagen, 0, 0, null);
        imageCopy.getGraphics().dispose();

        encodingExecutor.submit(() -> procesarFrame(imageCopy));
    }

    private void procesarFrame(BufferedImage imageCopy) {
        try {
            if (!grabando || recorder == null || !encodingActive) return;

            Frame rgbFrame = java2DConverter.convert(imageCopy);

            // Timestamp basado en número de frame y FPS objetivo
            long timestamp = (System.currentTimeMillis() - tiempoInicio) * 1000;

            synchronized (recorder) {
                recorder.setTimestamp(timestamp);
                recorder.record(rgbFrame);
                frameCount++;
            }

        } catch (Exception e) {
            System.err.println("Error en encoding de frame: " + e.getMessage());
        }
    }

    public void stop() throws Exception {
        if (grabando && recorder != null) {
            grabando = false;
            encodingActive = false;

            // Detener timer de captura
            if (captureTimer != null) {
                captureTimer.cancel();
                captureTimer = null;
            }

            if (encodingExecutor != null) {
                encodingExecutor.shutdown();
                try {
                    if (!encodingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
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

            imprimirInfoVideo();
        }
    }

    private void imprimirInfoVideo() {
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        double fpsReal = (frameCount * 1000.0) / tiempoTotal;
        double duracionEsperada = frameCount / TARGET_FPS;

        System.out.println("Grabación finalizada:");
        System.out.println("- Frames grabados: " + frameCount);
        System.out.println("- Tiempo total de grabación: " + (tiempoTotal / 1000.0) + " segundos");
        System.out.println("- FPS objetivo: " + TARGET_FPS);
        System.out.println("- FPS efectivo de encoding: " + String.format("%.2f", fpsReal));
        System.out.println("- Duración esperada del video: " + String.format("%.2f", duracionEsperada) + " segundos");
    }

    public boolean isGrabando() {
        return grabando;
    }

    public String getArchivoVideoPath() {
        return archivoVideoPath;
    }
}