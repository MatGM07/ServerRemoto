package com.server.remoto.websocket;


import com.server.remoto.services.SessionService;
import com.server.remoto.services.recording.MessageCommandService;
import com.server.remoto.services.logger.WindowTracker;
import com.server.remoto.services.recording.GrabadoraPantalla;
import com.server.remoto.services.recording.ScreenBroadcastService;
import com.server.remoto.services.recording.ScreenCaptureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketConnectionManager connectionManager;
    private final ScreenCaptureService screenCaptureService;
    private final ScreenBroadcastService screenBroadcastService;
    private final WindowTracker windowTracker;
    private final MessageCommandService messageCommandService;
    private final SessionService sessionService;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> windowTrackerTask;
    private boolean initialized = false;

    private GrabadoraPantalla grabadoraPantalla;

    @Autowired
    public MyWebSocketHandler(WebSocketConnectionManager connectionManager,
                              ScreenCaptureService screenCaptureService,
                              ScreenBroadcastService screenBroadcastService,
                              WindowTracker windowTracker,
                              MessageCommandService messageCommandService,
                              SessionService sessionService) {
        this.connectionManager = connectionManager;
        this.screenCaptureService = screenCaptureService;
        this.screenBroadcastService = screenBroadcastService;
        this.windowTracker = windowTracker;
        this.messageCommandService = messageCommandService;
        this.sessionService = sessionService;
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

        if (activeConnections.incrementAndGet() == 1) {
            startWindowTracker();
        }

        sessionService.iniciarMouseLogger();
        grabadoraPantalla = sessionService.iniciarGrabacion();
        sessionService.mostrarUIConectado();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("Conexión cerrada: " + session.getId());

        try {
            sessionService.detenerMouseLogger();
        } catch (Exception e) {
            System.err.println("Error deteniendo mouse logger: " + e.getMessage());
        }
        sessionService.detenerGrabacionYEnviar(grabadoraPantalla, session, connectionManager);
        grabadoraPantalla = null;

        connectionManager.unregisterSession(session);


        if (activeConnections.decrementAndGet() == 0) {
            stopWindowTracker();
        }

        sessionService.mostrarUIEsperando();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        boolean shouldClearRecorder = messageCommandService.handleCommand(payload, session, grabadoraPantalla);
        if (shouldClearRecorder) {
            grabadoraPantalla = null;
        }
    }

    private synchronized void initializeServices() {
        if (initialized) return;

        screenCaptureService.initialize();
        scheduler.scheduleAtFixedRate(() -> screenBroadcastService.broadcastScreen(grabadoraPantalla), 0, 41, TimeUnit.MILLISECONDS);

        initialized = true;
    }

    private void startWindowTracker() {
        windowTrackerTask = scheduler.scheduleAtFixedRate(windowTracker::checkChanges, 0, 1, TimeUnit.SECONDS);
        System.out.println("Tarea de rastreo de ventanas iniciada.");
    }

    private void stopWindowTracker() {
        if (windowTrackerTask != null && !windowTrackerTask.isCancelled()) {
            windowTrackerTask.cancel(true);
            System.out.println("Tarea de rastreo de ventanas detenida.");
        }
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
        sessionService.mostrarUIEsperando();
    }
}