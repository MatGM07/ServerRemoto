package com.server.remoto.swing;

import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
public class RemoteClientUI {

    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private Runnable onDisconnectListener; // Callback para desconexión

    public RemoteClientUI() {
        // Constructor vacío, no crea la UI todavía
    }

    public void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("Control Remoto");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Panel "Esperando conexión"
        JPanel waitingPanel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Esperando conexión...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 18));
        waitingPanel.add(label, BorderLayout.CENTER);
        mainPanel.add(waitingPanel, "waiting");

        // Panel "Conexión exitosa"
        JPanel connectedPanel = new JPanel(new BorderLayout());

        JLabel connectedLabel = new JLabel("¡Conexión exitosa!", SwingConstants.CENTER);
        connectedLabel.setFont(new Font("Arial", Font.BOLD, 18));
        connectedPanel.add(connectedLabel, BorderLayout.CENTER);

        JButton endConnectionButton = new JButton("Finalizar conexión");
        endConnectionButton.setFont(new Font("Arial", Font.PLAIN, 14));
        endConnectionButton.addActionListener(e -> {
            if (onDisconnectListener != null) {
                onDisconnectListener.run();
            }
        });
        connectedPanel.add(endConnectionButton, BorderLayout.SOUTH);

        mainPanel.add(connectedPanel, "connected");

        // Mostrar panel inicial
        cardLayout.show(mainPanel, "waiting");

        frame.getContentPane().add(mainPanel);
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
    }

    public void showWindow() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void showConnectedPanel() {
        cardLayout.show(mainPanel, "connected");
    }

    public void showWaitingPanel() {
        cardLayout.show(mainPanel, "waiting");
    }

    // Permitir que otras clases definan qué hacer cuando se presiona "Finalizar conexión"
    public void setOnDisconnectListener(Runnable listener) {
        this.onDisconnectListener = listener;
    }
}
