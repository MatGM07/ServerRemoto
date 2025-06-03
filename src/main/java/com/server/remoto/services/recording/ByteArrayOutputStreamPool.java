package com.server.remoto.services.recording;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ByteArrayOutputStreamPool {
    private final BlockingQueue<ByteArrayOutputStream> pool = new LinkedBlockingQueue<>(20);

    public ByteArrayOutputStream borrow() {
        ByteArrayOutputStream baos = pool.poll();
        return (baos != null) ? baos : new ByteArrayOutputStream(8192);
    }

    public void release(ByteArrayOutputStream baos) {
        baos.reset();
        pool.offer(baos);
    }
}
