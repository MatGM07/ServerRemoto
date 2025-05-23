package com.server.remoto.websocket;

import com.server.remoto.WindowTracker;
import com.server.remoto.grabadora.GrabadoraPantalla;
import com.server.remoto.swing.RemoteClientUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private Robot robot;
    private Rectangle screenRect;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private boolean initialized = false;
    private WindowTracker windowTracker;

    private GrabadoraPantalla grabadoraPantalla;

    @Autowired
    private final RemoteClientUI remoteClientUI;

    @Autowired
    public MyWebSocketHandler(RemoteClientUI remoteClientUI) {
        this.remoteClientUI = remoteClientUI;
        remoteClientUI.setOnDisconnectListener(this::closeAllConnections);
    }

    private synchronized void initializeRobotIfNeeded() {
        if (initialized) return;
        try {
            this.robot = new Robot();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenRect = new Rectangle(d);

            this.windowTracker = new WindowTracker(this);

            scheduler.scheduleAtFixedRate(this::broadcastScreen, 0, 41, TimeUnit.MILLISECONDS); // 24 fps ~41ms
            scheduler.scheduleAtFixedRate(windowTracker::checkChanges, 0, 1, TimeUnit.SECONDS);

            initialized = true;
        } catch (AWTException e) {
            throw new IllegalStateException("No se pudo inicializar Robot para captura de pantalla", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Nueva conexión establecida: " + session.getId());
        initializeRobotIfNeeded();
        sessions.add(session);

        // Crear carpeta videos si no existe
        File carpetaVideos = new File("videos");
        if (!carpetaVideos.exists()) {
            carpetaVideos.mkdirs();
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;

        try {
            grabadoraPantalla = new GrabadoraPantalla();
            String ruta = "videos/rec_" + System.currentTimeMillis() + ".mp4";
            grabadoraPantalla.start(ruta, width, height);
            broadcastLog("Grabación iniciada: " + ruta);
        } catch (Exception e) {
            System.err.println("Error al iniciar grabación: " + e.getMessage());
        }

        SwingUtilities.invokeLater(remoteClientUI::showConnectedPanel);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("Conexión cerrada: " + session.getId());

        if (grabadoraPantalla != null) {
            try {
                grabadoraPantalla.stop();
                broadcastLog("Grabación finalizada. Archivo: " + grabadoraPantalla.getArchivoVideoPath());
            } catch (Exception e) {
                System.err.println("Error al detener grabación: " + e.getMessage());
            } finally {
                grabadoraPantalla = null;
            }
        }

        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    private void broadcastScreen() {
        if (robot == null || screenRect == null) return;

        try {
            BufferedImage capture = robot.createScreenCapture(screenRect);

            // Dibuja cursor
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            Graphics2D g2d = capture.createGraphics();
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));
            int size = 10;
            g2d.drawLine(mouse.x - size, mouse.y, mouse.x + size, mouse.y);
            g2d.drawLine(mouse.x, mouse.y - size, mouse.x, mouse.y + size);
            g2d.dispose();

            // Enviar a clientes WebSocket
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(capture, "jpg", out);
            byte[] payload = out.toByteArray();
            BinaryMessage msg = new BinaryMessage(payload);

            for (WebSocketSession sess : sessions) {
                if (sess.isOpen()) {
                    try {
                        sess.sendMessage(msg);
                    } catch (Exception e) {
                        System.err.println("Error enviando imagen a sesión " + sess.getId() + ": " + e.getMessage());
                    }
                }
            }

            // Enviar frame a grabadora
            if (grabadoraPantalla != null && grabadoraPantalla.isGrabando()) {
                try {
                    grabadoraPantalla.encodeFrame(capture);
                } catch (Exception e) {
                    System.err.println("Error grabando frame: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastLog(String logMessage) {
        try {
            String jsonMessage = "{\"type\":\"log\",\"message\":\"" + escapeJson(logMessage) + "\"}";
            TextMessage textMsg = new TextMessage(jsonMessage);

            for (WebSocketSession sess : sessions) {
                if (sess.isOpen()) {
                    try {
                        sess.sendMessage(textMsg);
                    } catch (Exception e) {
                        System.err.println("Error enviando log a sesión " + sess.getId() + ": " + e.getMessage());
                    }
                }
            }
            System.out.println(logMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeAllConnections() {
        for (WebSocketSession sess : sessions) {
            try {
                sess.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                System.err.println("Error al cerrar sesión: " + sess.getId());
            }
        }
        sessions.clear();
        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    private String escapeJson(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
