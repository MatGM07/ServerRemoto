package com.server.remoto.websocket;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyWebSocketHandler extends TextWebSocketHandler {
    private Robot robot;
    private Rectangle screenRect;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private boolean initialized = false;

    private synchronized void initializeRobotIfNeeded() {
        if (initialized) return;
        try {
            this.robot = new Robot();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenRect = new Rectangle(d);
            scheduler.scheduleAtFixedRate(this::broadcastScreen, 0, 67, TimeUnit.MILLISECONDS);
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
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    private void broadcastScreen() {
        if (robot == null || screenRect == null) return;

        try {
            BufferedImage capture = robot.createScreenCapture(screenRect);

            // Obtener la posición actual del mouse
            Point mouse = MouseInfo.getPointerInfo().getLocation();

            // Dibujar el cursor sobre la imagen capturada
            Graphics2D g2d = capture.createGraphics();
            g2d.setColor(Color.RED); // color visible
            g2d.setStroke(new BasicStroke(2));

            // Dibujar una cruz simple como cursor
            int size = 10;
            g2d.drawLine(mouse.x - size, mouse.y, mouse.x + size, mouse.y);
            g2d.drawLine(mouse.x, mouse.y - size, mouse.x, mouse.y + size);
            g2d.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(capture, "jpg", out);
            byte[] payload = out.toByteArray();
            BinaryMessage msg = new BinaryMessage(payload);

            for (WebSocketSession sess : sessions) {
                if (sess.isOpen()) {
                    sess.sendMessage(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
