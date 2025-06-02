package com.server.remoto.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService() {
        this.uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        ensureUploadDirectoryExists();
    }

    private void ensureUploadDirectoryExists() {
        File dir = uploadDir.toFile();
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            System.out.println(">>> [DEBUG] Carpeta 'uploads' creada: " + created);
        } else {
            System.out.println(">>> [DEBUG] Carpeta 'uploads' ya existe");
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        System.out.println(">>> [DEBUG] Nombre original del archivo: " + file.getOriginalFilename());
        System.out.println(">>> [DEBUG] Tamaño del archivo: " + file.getSize() + " bytes");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + file.getOriginalFilename();
        Path destination = uploadDir.resolve(fileName);

        System.out.println(">>> [DEBUG] Ruta destino: " + destination.toAbsolutePath());

        file.transferTo(destination.toFile());

        System.out.println(">>> [DEBUG] Archivo transferido exitosamente");

        return fileName;
    }
}