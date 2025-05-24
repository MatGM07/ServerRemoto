package com.server.remoto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.server.remoto.swing.RemoteClientUI;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;

@SpringBootApplication
public class RemotoApplication {

	public static void main(String[] args) {
		// Necesario para aplicaciones Swing
		System.setProperty("java.awt.headless", "false");

		// Inicializa el contexto de Spring
		ConfigurableApplicationContext context = SpringApplication.run(RemotoApplication.class, args);

		// Obtiene el bean de UI
		RemoteClientUI ui = context.getBean(RemoteClientUI.class);

		// Define comportamiento al finalizar conexión
		Runnable onDisconnect = () -> {
			System.out.println("Conexión finalizada por el usuario.");
			// Puedes hacer más cosas aquí: cerrar recursos, desconectar sockets, etc.
			System.exit(0);
		};

		// Inicializa y muestra la interfaz en el hilo de Swing
		SwingUtilities.invokeLater(() -> {
			ui.initUI(onDisconnect);
			ui.showWindow();
		});
	}
}
