package com.server.remoto.ui;

public class PanelFactory {
    private final Runnable onDisconnect;

    public PanelFactory(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public ViewPanel createPanel(String type) {
        return switch (type) {
            case "waiting" -> new WaitingPanel();
            case "connected" -> new ConnectedPanel(onDisconnect);
            default -> throw new IllegalArgumentException("Unknown panel: " + type);
        };
    }
}
