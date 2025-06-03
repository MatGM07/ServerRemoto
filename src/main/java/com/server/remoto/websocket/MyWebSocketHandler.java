package com.server.remoto.websocket;


import com.server.remoto.services.logger.WindowTracker;
import com.server.remoto.controller.VideoSenderController;
import com.server.remoto.services.grabadora.GrabadoraPantalla;
import com.server.remoto.services.grabadora.ScreenCaptureService;
import com.server.remoto.services.logger.LogBroadcastService;
import com.server.remoto.swing.RemoteClientUI;
import com.server.remoto.services.grabadora.MouseClickLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.awt.RenderingHints;
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketConnectionManager connectionManager;
    private final ScreenCaptureService screenCaptureService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean initialized = false;

    private GrabadoraPantalla grabadoraPantalla;
    private ScheduledFuture<?> windowTrackerTask;


    @Autowired
    private WindowTracker windowTracker;

    @Autowired
    private MouseClickLogger mouseClickLogger;

    @Autowired
    private final RemoteClientUI remoteClientUI;

    @Autowired
    private VideoSenderController videoSenderController;

    @Autowired
    private LogBroadcastService logBroadcastService;

    @Autowired
    public MyWebSocketHandler(RemoteClientUI remoteClientUI,
                              WebSocketConnectionManager connectionManager,
                              ScreenCaptureService screenCaptureService,
                              LogBroadcastService logBroadcastService, WindowTracker windowTracker) {
        this.remoteClientUI = remoteClientUI;
        this.connectionManager = connectionManager;
        this.screenCaptureService = screenCaptureService;
        this.logBroadcastService = logBroadcastService;
        this.windowTracker = windowTracker;
        remoteClientUI.initUI(this::closeAllConnections);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connectionManager.registerSession(session);
        InetSocketAddress remoteAddr = session.getRemoteAddress();
        if (remoteAddr != null) {
            connectionManager.setClientInfo(
                    session,
                    remoteAddr.getAddress().getHostAddress(),
                    remoteAddr.getPort()
            );
        }

        System.out.println("Nueva conexión establecida: " + session.getId());
        initializeServices();

        mouseClickLogger.startListening(logBroadcastService::broadcastLog);

        File carpetaVideos = new File("videos");
        if (!carpetaVideos.exists()) {
            carpetaVideos.mkdirs();
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        try {
            String ruta = "videos/rec_" + System.currentTimeMillis() + ".mp4";
            grabadoraPantalla = new GrabadoraPantalla();
            grabadoraPantalla.start(ruta, width, height);
            logBroadcastService.broadcastLog("Grabación iniciada: " + ruta);
        } catch (Exception e) {
            System.err.println("Error al iniciar grabación: " + e.getMessage());
        }

        SwingUtilities.invokeLater(remoteClientUI::showConnectedPanel);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionManager.unregisterSession(session);
        System.out.println("Conexión cerrada: " + session.getId());

        mouseClickLogger.stopListening();

        if (grabadoraPantalla != null) {
            try {
                grabadoraPantalla.stop();
                String pathVideo = grabadoraPantalla.getArchivoVideoPath();
                logBroadcastService.broadcastLog("Grabación finalizada. Archivo: " + pathVideo);

                String hostDestino = connectionManager.getClientHost(session);
                Integer portDestino = connectionManager.getClientPort(session);
                File videoFile = new File(pathVideo);
                if (videoFile.exists() && hostDestino != null && portDestino != null) {
                    enviarVideo(videoFile, hostDestino, portDestino);
                } else {
                    System.err.println("[WS] No existe el archivo o no tengo host/puerto.");
                }
            } catch (Exception e) {
                System.err.println("Error al detener grabación: " + e.getMessage());
            } finally {
                grabadoraPantalla = null;
            }
        }

        if (windowTrackerTask != null && !windowTrackerTask.isCancelled()) {
            windowTrackerTask.cancel(true);
            System.out.println("Tarea de rastreo de ventanas detenida.");
        }

        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JSONObject json = new JSONObject(payload);
            String comando = json.getString("comando");

            if ("enviar_video".equals(comando) && grabadoraPantalla != null) {
                try {
                    grabadoraPantalla.stop();
                    String path = grabadoraPantalla.getArchivoVideoPath();
                    File video = new File(path);
                    if (video.exists()) {
                        String hostDestino = connectionManager.getClientHost(session);
                        Integer portDestino = connectionManager.getClientPort(session);
                        videoSenderController.enviarArchivo(video, hostDestino, portDestino);
                        System.out.println("[WS] Video enviado por orden del servidor.");
                    } else {
                        System.err.println("Archivo de video no encontrado.");
                    }
                } catch (Exception e) {
                    System.err.println("Error enviando video: " + e.getMessage());
                } finally {
                    grabadoraPantalla = null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }

    private synchronized void initializeServices() {
        if (initialized) return;
        screenCaptureService.initialize();


        scheduler.scheduleAtFixedRate(this::broadcastScreen, 0, 41, TimeUnit.MILLISECONDS);
        windowTrackerTask = scheduler.scheduleAtFixedRate(
                windowTracker::checkChanges,
                0, 1, TimeUnit.SECONDS
        );
        initialized = true;
    }

    private void broadcastScreen() {
        try {
            BufferedImage capture = screenCaptureService.captureScreenWithCursor();
            grabarFrame(capture);
            enviarImagenPorWebSocket(capture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void grabarFrame(BufferedImage capture) {
        if (grabadoraPantalla != null && grabadoraPantalla.isGrabando()) {
            try {
                BufferedImage videoFrame = new BufferedImage(
                        capture.getWidth(),
                        capture.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR
                );
                Graphics2D g = videoFrame.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(capture, 0, 0, null);
                g.dispose();
                grabadoraPantalla.encodeFrame(videoFrame);
            } catch (Exception e) {
                System.err.println("Error grabando frame: " + e.getMessage());
            }
        }
    }

    private void enviarImagenPorWebSocket(BufferedImage image) {
        CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", out);
                byte[] payload = out.toByteArray();

                for (WebSocketSession sess : connectionManager.getSessions()) {
                    if (sess.isOpen()) {
                        synchronized (connectionManager.getLock(sess)) {
                            try {
                                sess.sendMessage(new org.springframework.web.socket.BinaryMessage(payload));
                            } catch (IOException e) {
                                System.err.println("Error enviando imagen a sesión " + sess.getId() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando WebSocket: " + e.getMessage());
            }
        });
    }

    public void dibujarPuntero(Graphics2D g2d, Point mouse) {
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        int size = 10;
        g2d.drawLine(mouse.x - size, mouse.y, mouse.x + size, mouse.y);
        g2d.drawLine(mouse.x, mouse.y - size, mouse.x, mouse.y + size);
        g2d.dispose();
    }

    public void closeAllConnections() {
        for (WebSocketSession sess : connectionManager.getSessions()) {
            try {
                sess.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                System.err.println("Error al cerrar sesión: " + sess.getId());
            }
        }
        connectionManager.getSessions().clear();
        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    public void enviarVideo(File videoFile, String hostDestino, Integer portDestino) {
        try {
            videoSenderController.enviarArchivo(videoFile, hostDestino, portDestino);
            System.out.println("[WS] Video enviado a " + hostDestino + ":" + portDestino);
        } catch (Exception ex) {
            System.err.println("[WS] Error enviando video: " + ex.getMessage());
        }
    }
}
