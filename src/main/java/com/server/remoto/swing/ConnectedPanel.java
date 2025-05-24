package com.server.remoto.swing;

import javax.swing.*;
import java.awt.*;

public class ConnectedPanel implements ViewPanel {
    private final Runnable onDisconnect;

    public ConnectedPanel(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    @Override
    public JPanel getPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("¡Conexión exitosa!", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(label, BorderLayout.CENTER);

        JButton button = new JButton("Finalizar conexión");
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.addActionListener(e -> {
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        });

        panel.add(button, BorderLayout.SOUTH);
        return panel;
    }
}
