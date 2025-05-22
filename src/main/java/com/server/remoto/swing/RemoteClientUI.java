package com.server.remoto.swing;

import javax.swing.*;
import java.awt.*;

public class RemoteClientUI {
    private final JFrame frame;
    private final JPanel mainPanel;
    private final CardLayout cardLayout;

    public RemoteClientUI() {
        // Configura el look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Crea el frame principal
        frame = new JFrame("Control Remoto");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // CardLayout para alternar vistas
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Panel de "Esperando conexión"
        JPanel waitingPanel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Esperando conexión...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 18));
        waitingPanel.add(label, BorderLayout.CENTER);
        mainPanel.add(waitingPanel, "waiting");

        // Panel de "Conexión exitosa"
        JPanel connectedPanel = new JPanel(new BorderLayout());
        JLabel connectedLabel = new JLabel("¡Conexión exitosa!", SwingConstants.CENTER);
        connectedLabel.setFont(new Font("Arial", Font.BOLD, 18));
        connectedPanel.add(connectedLabel, BorderLayout.CENTER);
        mainPanel.add(connectedPanel, "connected");

        // Mostrar panel inicial
        cardLayout.show(mainPanel, "waiting");

        // Frame final
        frame.getContentPane().add(mainPanel);
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Método público para cambiar a "conectado"
    public void showConnectedPanel() {
        cardLayout.show(mainPanel, "connected");
    }
}

