package com.server.remoto.services.recording;

import com.server.remoto.websocket.WebSocketConnectionManager;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class ScreenBroadcastService {

    private final ScreenCaptureService screenCaptureService;
    private final WebSocketConnectionManager connectionManager;

    public ScreenBroadcastService(ScreenCaptureService screenCaptureService,
                                  WebSocketConnectionManager connectionManager) {
        this.screenCaptureService = screenCaptureService;
        this.connectionManager = connectionManager;
    }

    public void broadcastScreen(GrabadoraPantalla grabadoraPantalla) {
        try {
            BufferedImage capture = screenCaptureService.captureScreenWithCursor();
            grabFrame(grabadoraPantalla, capture);
            sendImageToClients(capture);
        } catch (Exception e) {
            System.err.println("Error en broadcastScreen: " + e.getMessage());
        }
    }

    private void grabFrame(GrabadoraPantalla grabadoraPantalla, BufferedImage capture) {
        if (grabadoraPantalla != null && grabadoraPantalla.isGrabando()) {
            try {
                BufferedImage videoFrame = new BufferedImage(
                        capture.getWidth(),
                        capture.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR
                );
                Graphics2D g = videoFrame.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(capture, 0, 0, null);
                g.dispose();
                grabadoraPantalla.encodeFrame(videoFrame);
            } catch (Exception e) {
                System.err.println("Error grabando frame: " + e.getMessage());
            }
        }
    }

    private void sendImageToClients(BufferedImage image) {
        CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", out);
                byte[] payload = out.toByteArray();

                for (WebSocketSession sess : connectionManager.getSessions()) {
                    if (sess.isOpen()) {
                        synchronized (connectionManager.getLock(sess)) {
                            try {
                                sess.sendMessage(new org.springframework.web.socket.BinaryMessage(payload));
                            } catch (IOException e) {
                                System.err.println("Error enviando imagen a sesión " + sess.getId() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando WebSocket: " + e.getMessage());
            }
        });
    }
}
