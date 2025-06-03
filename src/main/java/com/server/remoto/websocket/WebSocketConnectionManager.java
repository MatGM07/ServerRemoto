package com.server.remoto.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketConnectionManager {
    // Conjunto de sesiones activas
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    // Map para locks por sesión (para envío concurrente seguro)
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();

    // Map para almacenar host remoto de cada sesión
    private final Map<WebSocketSession, String> clienteHost = new ConcurrentHashMap<>();

    // Map para almacenar puerto remoto de cada sesión
    private final Map<WebSocketSession, Integer> clientePort = new ConcurrentHashMap<>();

    /**
     * Registra una nueva sesión en el manager.
     */
    public void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    /**
     * Elimina una sesión del manager y limpia sus datos asociados.
     */
    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
        sessionLocks.remove(session);
        clienteHost.remove(session);
        clientePort.remove(session);
    }

    /**
     * Devuelve el conjunto de sesiones activas.
     */
    public Set<WebSocketSession> getSessions() {
        return sessions;
    }

    /**
     * Obtiene (o crea) un lock para uso de envío concurrente hacia esta sesión.
     */
    public Object getLock(WebSocketSession session) {
        return sessionLocks.computeIfAbsent(session, s -> new Object());
    }

    /**
     * Almacena la información de host y puerto para una sesión.
     */
    public void setClientInfo(WebSocketSession session, String host, int port) {
        clienteHost.put(session, host);
        clientePort.put(session, port);
    }

    /**
     * Obtiene el host remoto de una sesión.
     */
    public String getClientHost(WebSocketSession session) {
        return clienteHost.get(session);
    }

    /**
     * Obtiene el puerto remoto de una sesión.
     */
    public Integer getClientPort(WebSocketSession session) {
        return clientePort.get(session);
    }
}
