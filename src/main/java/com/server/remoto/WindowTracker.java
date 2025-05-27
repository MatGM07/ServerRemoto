package com.server.remoto;

import com.server.remoto.websocket.MyWebSocketHandler;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class WindowTracker {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Set<String> previousVisibleWindows = new HashSet<>();
    private Set<String> previousProcesses = new HashSet<>();
    private String lastActiveWindow = "";
    private final MyWebSocketHandler webSocketHandler;

    public WindowTracker(MyWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void checkChanges() {
        boolean cambios = false;

        if (logActiveWindow()) cambios = true;
        if (logVisibleWindows()) cambios = true;
        if (logClosedProcesses()) cambios = true;

    }

    private boolean logActiveWindow() {
        char[] buffer = new char[1024];
        HWND ventana = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.GetWindowText(ventana, buffer, 1024);
        String title = Native.toString(buffer);

        if (title != null && !title.isBlank() && !title.equals(lastActiveWindow)) {
            lastActiveWindow = title;
            log("Ventana activa: " + title);
            return true;
        }
        return false;
    }

    private boolean logVisibleWindows() {
        Set<String> currentVisibleWindows = new HashSet<>();

        User32.INSTANCE.EnumWindows((ventana, data) -> {
            if (User32.INSTANCE.IsWindowVisible(ventana)) {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(ventana, windowText, 512);
                String title = Native.toString(windowText).trim();
                if (!title.isBlank()) {
                    currentVisibleWindows.add(title);
                }
            }
            return true;
        }, null);

        boolean huboCambios = false;

        for (String title : currentVisibleWindows) {
            if (!previousVisibleWindows.contains(title)) {
                log("Ventana abierta: " + title);
                huboCambios = true;
            }
        }

        for (String title : previousVisibleWindows) {
            if (!currentVisibleWindows.contains(title)) {
                log("Ventana cerrada: " + title);
                huboCambios = true;
            }
        }

        previousVisibleWindows = currentVisibleWindows;
        return huboCambios;
    }

    private boolean logClosedProcesses() {
        boolean huboCambios = false;
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Set<String> currentProcesses = new HashSet<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && !line.startsWith("Image Name") && !line.startsWith("=")) {
                    String processName = line.split("\\s+")[0];
                    currentProcesses.add(processName);
                }
            }

            Set<String> closedProcesses = new HashSet<>(previousProcesses);
            closedProcesses.removeAll(currentProcesses);

            for (String proc : closedProcesses) {
                log("Proceso cerrado: " + proc);
                huboCambios = true;
            }

            previousProcesses = currentProcesses;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return huboCambios;
    }

    private void log(String message) {
        // Formato con timestamp
        String formattedMessage = "[" + dateFormat.format(new Date()) + "] " + message;

        // Enviar al websocket en lugar de imprimir directamente
        webSocketHandler.broadcastLog(formattedMessage);
    }
}