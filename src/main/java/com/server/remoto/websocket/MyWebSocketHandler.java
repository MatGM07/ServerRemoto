package com.server.remoto.websocket;


import com.server.remoto.WindowTracker;
import com.server.remoto.controller.VideoSenderController;
import com.server.remoto.grabadora.GrabadoraPantalla;
import com.server.remoto.swing.RemoteClientUI;
import com.server.remoto.grabadora.MouseClickLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
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
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.awt.RenderingHints;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private Robot robot;
    private Rectangle screenRect;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private boolean initialized = false;
    private WindowTracker windowTracker;
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();
    private GrabadoraPantalla grabadoraPantalla;
    @Autowired
    private MouseClickLogger mouseClickLogger;

    @Autowired
    private final RemoteClientUI remoteClientUI;

    @Autowired
    private VideoSenderController videoSenderController;

    private final Map<WebSocketSession, String> clienteHost = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, Integer> clientePort = new ConcurrentHashMap<>();

    @Autowired
    public MyWebSocketHandler(RemoteClientUI remoteClientUI) {
        this.remoteClientUI = remoteClientUI;
        remoteClientUI.initUI(this::closeAllConnections);
    }

    private synchronized void initializeRobotIfNeeded() {
        if (initialized) return;
        try {
            this.robot = new Robot();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenRect = new Rectangle(d);

            this.windowTracker = new WindowTracker(this);

            scheduler.scheduleAtFixedRate(this::broadcastScreen, 0, 41, TimeUnit.MILLISECONDS); // ~24 fps
            scheduler.scheduleAtFixedRate(windowTracker::checkChanges, 0, 1, TimeUnit.SECONDS);

            initialized = true;
        } catch (AWTException e) {
            throw new IllegalStateException("No se pudo inicializar Robot para captura de pantalla", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        InetSocketAddress remoteAddr = session.getRemoteAddress();

        if (remoteAddr != null) {
            obtenerDatosDelHost(remoteAddr, session);
        }

        System.out.println("Nueva conexión establecida: " + session.getId());
        initializeRobotIfNeeded();

        mouseClickLogger.startListening(this::broadcastLog);

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
            broadcastLog("Grabación iniciada: " + ruta);
        } catch (Exception e) {
            System.err.println("Error al iniciar grabación: " + e.getMessage());
        }

        SwingUtilities.invokeLater(remoteClientUI::showConnectedPanel);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            JSONObject json = new JSONObject(payload);
            String comando = json.getString("comando");

            if (comando.equals("enviar_video")) {
                if (grabadoraPantalla != null) {
                    try {
                        grabadoraPantalla.stop(); // Detiene la grabación
                        String path = grabadoraPantalla.getArchivoVideoPath();

                        File video = new File(path);
                        if (video.exists()) {
                            videoSenderController.enviarArchivo(video, clienteHost.get(session), clientePort.get(session));
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
            }
            // Aquí puedes manejar otros comandos si los necesitas más adelante

        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }



    public void obtenerDatosDelHost(InetSocketAddress remoteAddr, WebSocketSession session){
        String hostRemoto = remoteAddr.getAddress().getHostAddress();
        int puertoRemoto = remoteAddr.getPort();
        clienteHost.put(session, hostRemoto);
        clientePort.put(session, puertoRemoto);
        System.out.println("[WS] Nueva conexión de " + hostRemoto + ":" + puertoRemoto);
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sessionLocks.remove(session);
        System.out.println("Conexión cerrada: " + session.getId());

        mouseClickLogger.stopListening();

        if (grabadoraPantalla != null) {
            try {
                grabadoraPantalla.stop();
                String pathVideo = grabadoraPantalla.getArchivoVideoPath();
                broadcastLog("Grabación finalizada. Archivo: " + grabadoraPantalla.getArchivoVideoPath());

                String hostDestino = clienteHost.get(session);
                Integer portDestino = clientePort.get(session);
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
        clienteHost.remove(session);
        clientePort.remove(session);
        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    public void enviarVideo(File videoFile, String hostDestino, Integer portDestino) {
        try {
            videoSenderController.enviarArchivo(videoFile, hostDestino, portDestino);
            System.out.println("[WS] Video enviado a " +
                    hostDestino + ":" + portDestino);
        } catch (Exception ex) {
            System.err.println("[WS] Error enviando video: " + ex.getMessage());
        }
    }

    private void broadcastScreen() {
        if (robot == null || screenRect == null) return;

        try {
            BufferedImage capture = capturarPantallaConCursor();
            grabarFrame(capture);
            enviarImagenPorWebSocket(capture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private BufferedImage capturarPantallaConCursor() {
        BufferedImage capture = robot.createScreenCapture(screenRect);
        Point mouse = MouseInfo.getPointerInfo().getLocation();
        Graphics2D g2d = capture.createGraphics();
        dibujarPuntero(g2d, mouse);
        return capture;
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
                BinaryMessage msg = new BinaryMessage(payload);

                for (WebSocketSession sess : sessions) {
                    if (sess.isOpen()) {
                        sessionLocks.putIfAbsent(sess, new Object());
                        synchronized (sessionLocks.get(sess)) {
                            try {
                                sess.sendMessage(msg);
                            } catch (Exception e) {
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

    public void closeAllConnections() {
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

}