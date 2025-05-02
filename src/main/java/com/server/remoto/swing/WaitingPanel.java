package com.server.remoto.swing;

import javax.swing.*;
import java.awt.*;

public class WaitingPanel extends JPanel {
    private final JLabel label;
    public WaitingPanel() {
        setLayout(new BorderLayout());
        label = new JLabel("Iniciando aplicación, por favor espere…", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 18));
        add(label, BorderLayout.CENTER);
    }
    /** Si quieres actualizar el mensaje: */
    public void setMessage(String msg) {
        SwingUtilities.invokeLater(() -> label.setText(msg));
    }
}

