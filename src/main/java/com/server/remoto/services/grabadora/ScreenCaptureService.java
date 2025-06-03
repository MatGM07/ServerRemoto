package com.server.remoto.services.grabadora;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScreenCaptureService {
    private Robot robot;
    private Rectangle screenRect;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Inicializa Robot y la zona de captura si no se ha inicializado aún.
     */
    public synchronized void initialize() {
        if (initialized.get()) return;
        try {
            this.robot = new Robot();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            this.screenRect = new Rectangle(d);
            initialized.set(true);
        } catch (AWTException e) {
            throw new IllegalStateException("No se pudo inicializar Robot para captura de pantalla", e);
        }
    }

    /**
     * Retorna una captura de pantalla actual con el cursor dibujado.
     * <p>
     * Debe llamarse después de initialize().
     */
    public BufferedImage captureScreenWithCursor() {
        if (!initialized.get()) {
            throw new IllegalStateException("ScreenCaptureService no inicializado. Llama a initialize() primero.");
        }
        // Captura la pantalla completa
        BufferedImage capture = robot.createScreenCapture(screenRect);
        // Obtener posición del cursor
        Point mouse = MouseInfo.getPointerInfo().getLocation();
        // Dibujar cursor
        Graphics2D g2d = capture.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        int size = 10;
        g2d.drawLine(mouse.x - size, mouse.y, mouse.x + size, mouse.y);
        g2d.drawLine(mouse.x, mouse.y - size, mouse.x, mouse.y + size);
        g2d.dispose();
        return capture;
    }
}

