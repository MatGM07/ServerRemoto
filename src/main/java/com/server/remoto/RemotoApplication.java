package com.server.remoto;

import com.server.remoto.websocket.MyWebSocketHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.server.remoto.ui.RemoteClientUI;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class RemotoApplication {

	public static void main(String[] args) {
		// Necesario para aplicaciones Swing
		System.setProperty("java.awt.headless", "false");

		// Inicializa el contexto de Spring
		ConfigurableApplicationContext context = SpringApplication.run(RemotoApplication.class, args);

		// Obtiene el bean de UI
		RemoteClientUI ui = context.getBean(RemoteClientUI.class);
		MyWebSocketHandler wsHandler = context.getBean(MyWebSocketHandler.class);

		// Define comportamiento al finalizar conexión
		Runnable onDisconnect = () -> {
			System.out.println("Conexión finalizada por el usuario.");

			// Cierra todas las sesiones WebSocket abiertas
			wsHandler.closeAllConnections();

			// Vuelve a la ventana “esperando conexión”
			SwingUtilities.invokeLater(ui::showWaitingPanel);
		};

		// Inicializa y muestra la interfaz en el hilo de Swing
		SwingUtilities.invokeLater(() -> {
			ui.initUI(onDisconnect);
			ui.showWindow();
		});
	}
}
