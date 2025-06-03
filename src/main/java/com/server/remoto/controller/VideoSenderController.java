package com.server.remoto.controller;

import org.springframework.stereotype.Component;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
@Component
public class VideoSenderController {

    public VideoSenderController() { }

    public void enviarArchivo(File file, String host) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo no existe o es null.");
        }
        if (host == null ) {
            throw new IllegalStateException("Host o puerto no definidos.");
        }

        String targetUrl = "http://" + host + ":" + 8080 + "/upload";
        HttpPost post = new HttpPost(targetUrl);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                .build();
        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            client.execute(post, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("[VideoSenderController] Respuesta de " + targetUrl +
                        " → HTTP " + statusCode + "; Cuerpo: " + responseBody);
                return null;
            });
        }
    }
}