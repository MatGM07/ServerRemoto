package com.server.remoto.services.recording;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class MouseClickLogger {
    private NativeMouseListener mouseClickListener;
    private boolean mouseListenerActive = false;

    public void startListening(Consumer<String> logConsumer) {
        try {
            // Siempre intentar desregistrar antes de registrar de nuevo.
            if (GlobalScreen.isNativeHookRegistered()) {
                try {
                    GlobalScreen.unregisterNativeHook();
                    System.out.println("Hook anterior desregistrado antes de iniciar uno nuevo.");
                } catch (NativeHookException e) {
                    System.err.println("Error al intentar limpiar hook existente: " + e.getMessage());
                }
            }

            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();

            mouseClickListener = new NativeMouseListener() {
                @Override
                public void nativeMousePressed(NativeMouseEvent e) {
                    String button = getMouseButtonName(e.getButton());
                    String log = String.format("Clic %s en coordenadas: (%d, %d)", button, e.getX(), e.getY());
                    logConsumer.accept(log);
                }

                @Override public void nativeMouseReleased(NativeMouseEvent e) {}
                @Override public void nativeMouseClicked(NativeMouseEvent e) {}
            };

            GlobalScreen.addNativeMouseListener(mouseClickListener);
            mouseListenerActive = true;
            System.out.println("Listener de clics del mouse iniciado correctamente.");
        } catch (NativeHookException e) {
            System.err.println("Error iniciando listener de mouse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopListening() {
        try {
            if (mouseClickListener != null && mouseListenerActive) {
                GlobalScreen.removeNativeMouseListener(mouseClickListener);
                mouseListenerActive = false;
                System.out.println("Listener de clics del mouse detenido.");
            }

            // Dar tiempo a que los eventos pendientes se procesen
            Thread.sleep(100); // Ajustar si es necesario

            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
                System.out.println("Native hook desregistrado.");
            }

            // Solo después de desregistrar, eliminamos el dispatcher (opcional)
            GlobalScreen.setEventDispatcher(null);

        } catch (NativeHookException e) {
            System.err.println("Error deteniendo listener de mouse: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaurar estado de interrupción
        } catch (Exception e) {
            System.err.println("Error deteniendo mouse logger: " + e.getMessage());
        }
    }
    private String getMouseButtonName(int button) {
        return switch (button) {
            case NativeMouseEvent.BUTTON1 -> "izquierdo";
            case NativeMouseEvent.BUTTON2 -> "derecho";
            case NativeMouseEvent.BUTTON3 -> "rueda";
            case NativeMouseEvent.BUTTON4 -> "lateral 1";
            case NativeMouseEvent.BUTTON5 -> "lateral 2";
            default -> "botón " + button;
        };
    }
}