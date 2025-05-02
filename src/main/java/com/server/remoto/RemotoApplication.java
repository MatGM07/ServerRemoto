package com.server.remoto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import javax.swing.*;
import java.awt.*;

@SpringBootApplication
public class RemotoApplication {

	public static void main(String[] args) {
		// 1) Arranca Spring Boot
		SpringApplication.run(RemotoApplication.class, args);
		System.setProperty("java.awt.headless", "false");

		// 2) Lanza la GUI Swing en el EDT
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {}

			// Frame principal
			JFrame frame = new JFrame("Control Remoto");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// CardLayout con un único panel “waiting”
			CardLayout cardLayout = new CardLayout();
			JPanel mainPanel = new JPanel(cardLayout);

			// Panel de espera
			JPanel waitingPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("Esperando conexión...", SwingConstants.CENTER);
			label.setFont(new Font("Arial", Font.PLAIN, 18));
			waitingPanel.add(label, BorderLayout.CENTER);

			// Añadimos solo esa tarjeta
			mainPanel.add(waitingPanel, "waiting");

			// Mostramos la tarjeta “waiting”
			cardLayout.show(mainPanel, "waiting");

			frame.getContentPane().add(mainPanel);
			frame.setSize(400, 200);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
