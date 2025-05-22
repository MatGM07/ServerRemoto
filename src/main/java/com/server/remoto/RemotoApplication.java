package com.server.remoto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.server.remoto.swing.RemoteClientUI;

import javax.swing.*;
import java.awt.*;

@SpringBootApplication
public class RemotoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemotoApplication.class, args);
		System.setProperty("java.awt.headless", "false");

		// Lanzamos la UI en el hilo de eventos de Swing
		SwingUtilities.invokeLater(RemoteClientUI::new);
	}
}
