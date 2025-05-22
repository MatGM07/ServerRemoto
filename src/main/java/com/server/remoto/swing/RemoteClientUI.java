package com.server.remoto.swing;

import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
public class RemoteClientUI {

    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

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

        JPanel waitingPanel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Esperando conexión...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 18));
        waitingPanel.add(label, BorderLayout.CENTER);
        mainPanel.add(waitingPanel, "waiting");

        JPanel connectedPanel = new JPanel(new BorderLayout());
        JLabel connectedLabel = new JLabel("¡Conexión exitosa!", SwingConstants.CENTER);
        connectedLabel.setFont(new Font("Arial", Font.BOLD, 18));
        connectedPanel.add(connectedLabel, BorderLayout.CENTER);
        mainPanel.add(connectedPanel, "connected");

        cardLayout.show(mainPanel, "waiting");

        frame.getContentPane().add(mainPanel);
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
    }

    public void showWindow() {
        // Mostrar ventana siempre en el hilo EDT
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void showConnectedPanel() {
        cardLayout.show(mainPanel, "connected");
    }
}
