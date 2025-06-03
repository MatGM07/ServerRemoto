package com.server.remoto.services.recording;

import com.server.remoto.controller.VideoSenderController;
import com.server.remoto.services.logger.LogBroadcastService;
import com.server.remoto.websocket.WebSocketConnectionManager;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;

@Component
public class MessageCommandService {

    private final VideoSenderController videoSenderController;
    private final WebSocketConnectionManager connectionManager;
    private final LogBroadcastService logBroadcastService;

    public MessageCommandService(VideoSenderController videoSenderController,
                                 WebSocketConnectionManager connectionManager,
                                 LogBroadcastService logBroadcastService) {
        this.videoSenderController = videoSenderController;
        this.connectionManager = connectionManager;
        this.logBroadcastService = logBroadcastService;
    }

    /**
     * Procesa el comando y devuelve true si grabadoraPantalla debe limpiarse (null).
     */
    public boolean handleCommand(String payload, WebSocketSession session, GrabadoraPantalla grabadoraPantalla) {
        try {
            JSONObject json = new JSONObject(payload);
            String comando = json.getString("comando");

            switch (comando) {
                case "enviar_video":
                    return enviarVideoDesdeGrabadora(grabadoraPantalla, session);

                // Aquí podrías agregar más comandos

                default:
                    System.err.println("[CommandHandler] Comando no reconocido: " + comando);
                    return false;
            }

        } catch (Exception e) {
            System.err.println("[CommandHandler] Error procesando comando: " + e.getMessage());
            return false;
        }
    }

    private boolean enviarVideoDesdeGrabadora(GrabadoraPantalla grabadoraPantalla, WebSocketSession session) {
        if (grabadoraPantalla == null) {
            System.err.println("[CommandHandler] No hay grabación activa.");
            return false;
        }

        try {
            grabadoraPantalla.stop();
            String path = grabadoraPantalla.getArchivoVideoPath();
            File video = new File(path);

            if (!video.exists()) {
                System.err.println("[CommandHandler] Archivo de video no encontrado.");
                return false;
            }

            String host = connectionManager.getClientHost(session);
            Integer port = connectionManager.getClientPort(session);

            if (host != null && port != null) {
                videoSenderController.enviarArchivo(video, host, port);
                logBroadcastService.broadcastLog("[CommandHandler] Video enviado por orden del servidor.");
                return true;  // Aquí indicamos que grabadoraPantalla puede ser limpiada
            } else {
                System.err.println("[CommandHandler] Host o puerto no definidos.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[CommandHandler] Error enviando video: " + e.getMessage());
            return false;
        }
    }
}