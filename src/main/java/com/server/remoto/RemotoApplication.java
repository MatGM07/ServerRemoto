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
		// Antes de arrancar Spring, definimos que no está en modo headless
		System.setProperty("java.awt.headless", "false");

		// Arrancamos Spring y obtenemos el contexto
		ConfigurableApplicationContext context = SpringApplication.run(RemotoApplication.class, args);

		// Luego pedimos el bean RemoteClientUI y ejecutamos la UI en el hilo Swing
		RemoteClientUI ui = context.getBean(RemoteClientUI.class);

		SwingUtilities.invokeLater(() -> {
			ui.initUI();    // Inicializa componentes gráficos
			ui.showWindow(); // Muestra la ventana en pantalla
		});
	}
}
