package com.server.remoto.ui;

import javax.swing.*;
import java.awt.*;

public class WaitingPanel implements ViewPanel {
    @Override
    public JPanel getPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Esperando conexión...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 18));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}

