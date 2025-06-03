package com.server.remoto.services;

import com.server.remoto.controller.VideoSenderController;
import com.server.remoto.services.recording.GrabadoraPantalla;
import com.server.remoto.services.recording.MouseClickLogger;
import com.server.remoto.services.logger.LogBroadcastService;
import com.server.remoto.ui.RemoteClientUI;
import com.server.remoto.websocket.WebSocketConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.swing.*;
import java.awt.*;
import java.io.File;

@Component
public class SessionService {

    private final LogBroadcastService logBroadcastService;
    private final VideoSenderController videoSenderController;
    private final RemoteClientUI remoteClientUI;
    private final MouseClickLogger mouseClickLogger;


    @Autowired
    public SessionService(LogBroadcastService logBroadcastService,
                                   VideoSenderController videoSenderController,
                                   RemoteClientUI remoteClientUI,
                                   MouseClickLogger mouseClickLogger) {
        this.logBroadcastService = logBroadcastService;
        this.videoSenderController = videoSenderController;
        this.remoteClientUI = remoteClientUI;
        this.mouseClickLogger = mouseClickLogger;
    }

    public GrabadoraPantalla iniciarGrabacion() {
        try {
            File carpetaVideos = new File("videos");
            if (!carpetaVideos.exists()) carpetaVideos.mkdirs();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            String ruta = "videos/rec_" + System.currentTimeMillis() + ".mp4";

            GrabadoraPantalla grabadora = new GrabadoraPantalla();
            grabadora.start(ruta, screenSize.width, screenSize.height);
            logBroadcastService.broadcastLog("Grabación iniciada: " + ruta);
            return grabadora;

        } catch (Exception e) {
            System.err.println("Error al iniciar grabación: " + e.getMessage());
            return null;
        }
    }

    public void detenerGrabacionYEnviar(GrabadoraPantalla grabadora, WebSocketSession session,
                                        WebSocketConnectionManager connectionManager) {
        if (grabadora == null) return;

        try {
            grabadora.stop();
            String pathVideo = grabadora.getArchivoVideoPath();
            logBroadcastService.broadcastLog("Grabación finalizada. Archivo: " + pathVideo);

            String hostDestino = connectionManager.getClientHost(session);
            Integer portDestino = connectionManager.getClientPort(session);
            System.out.println("Host destino: " + hostDestino);
            System.out.println("Puerto destino: " + portDestino);
            File videoFile = new File(pathVideo);
            if (videoFile.exists() && hostDestino != null && portDestino != null) {
                videoSenderController.enviarArchivo(videoFile, hostDestino);
                System.out.println("[WS] Video enviado a " + hostDestino + ":" + portDestino);
            } else {
                System.err.println("[WS] No se pudo enviar el video: archivo o datos incompletos.");
            }

        } catch (Exception e) {
            System.err.println("Error al detener grabación: " + e.getMessage());
        }
    }

    public void mostrarUIConectado() {
        SwingUtilities.invokeLater(remoteClientUI::showConnectedPanel);
    }

    public void mostrarUIEsperando() {
        SwingUtilities.invokeLater(remoteClientUI::showWaitingPanel);
    }

    public void iniciarMouseLogger() {
        mouseClickLogger.startListening(logBroadcastService::broadcastLog);
    }

    public void detenerMouseLogger() {
        mouseClickLogger.stopListening();
    }
}
