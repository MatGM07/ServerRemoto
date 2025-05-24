package com.server.remoto.controller;

import com.server.remoto.swing.RemoteClientUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private final RemoteClientUI remoteClientUI;

    @Autowired
    public FileUploadController(RemoteClientUI remoteClientUI) {
        this.remoteClientUI = remoteClientUI;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        System.out.println(">>> [DEBUG] Iniciando manejo de archivo");

        try {
            if (file.isEmpty()) {
                System.out.println(">>> [DEBUG] El archivo está vacío");
                return ResponseEntity.badRequest().body("Archivo vacío");
            }

            System.out.println(">>> [DEBUG] Nombre original del archivo: " + file.getOriginalFilename());
            System.out.println(">>> [DEBUG] Tamaño del archivo: " + file.getSize() + " bytes");

            // Crear carpeta si no existe
            File uploads = new File(System.getProperty("user.dir"), "uploads");
            if (!uploads.exists()) {
                boolean creada = uploads.mkdirs();
                System.out.println(">>> [DEBUG] Carpeta 'uploads' creada: " + creada);
            } else {
                System.out.println(">>> [DEBUG] Carpeta 'uploads' ya existe");
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = timestamp + "_" + file.getOriginalFilename();
            File destino = new File(uploads, fileName);

            System.out.println(">>> [DEBUG] Ruta destino: " + destino.getAbsolutePath());

            // Transferir archivo
            file.transferTo(destino);

            System.out.println(">>> [DEBUG] Archivo transferido exitosamente");

            // Notificar a la interfaz Swing
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Archivo recibido y guardado como:\n" + fileName,
                        "Notificación",
                        JOptionPane.INFORMATION_MESSAGE);
                remoteClientUI.showConnectedPanel(); // Opcional
            });

            return ResponseEntity.ok("Archivo guardado como: " + fileName);

        } catch (IOException e) {
            System.out.println(">>> [ERROR] Error al guardar el archivo:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al guardar archivo");
        } catch (Exception e) {
            System.out.println(">>> [ERROR] Excepción inesperada:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado");
        }
    }
}
