package com.server.remoto.ui;

import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
public class RemoteClientUI {

    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private PanelFactory panelFactory;

    public void initUI(Runnable onDisconnect) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        panelFactory = new PanelFactory(onDisconnect);

        frame = new JFrame("Control Remoto");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(panelFactory.createPanel("waiting").getPanel(), "waiting");
        mainPanel.add(panelFactory.createPanel("connected").getPanel(), "connected");

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
}