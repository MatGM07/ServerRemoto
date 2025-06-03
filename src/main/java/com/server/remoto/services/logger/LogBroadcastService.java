package com.server.remoto.services.logger;

import com.server.remoto.websocket.WebSocketConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
public class LogBroadcastService {

    private final WebSocketConnectionManager connectionManager;

    @Autowired
    public LogBroadcastService(WebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void broadcastLog(String logMessage) {
        try {
            String escaped = escapeJson(logMessage);
            String jsonMessage = "{\"type\":\"log\",\"message\":\"" + escaped + "\"}";
            TextMessage textMsg = new TextMessage(jsonMessage);

            for (WebSocketSession sess : connectionManager.getSessions()) {
                if (sess.isOpen()) {
                    synchronized (connectionManager.getLock(sess)) {
                        try {
                            sess.sendMessage(textMsg);
                        } catch (IOException e) {
                            System.err.println("Error enviando log a sesión " + sess.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }

            System.out.println(logMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
