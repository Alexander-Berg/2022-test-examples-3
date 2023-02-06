package ru.yandex.direct.graphite;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

class TcpTestServer implements AutoCloseable {
    private final Thread thread;
    private final ServerSocket serverSocket;
    private final AtomicInteger accepts = new AtomicInteger(0);
    private final BlockingQueue<String> reqs = new LinkedBlockingQueue<>();

    private TcpTestServer() throws IOException {
        serverSocket = new ServerSocket(0);
        thread = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    accepts.incrementAndGet();
                    String req = CharStreams.toString(new InputStreamReader(socket.getInputStream(), Charsets.UTF_8));
                    reqs.add(req);
                    socket.close();
                }
            } catch (IOException e) {
            }
        });
        thread.setDaemon(true);
    }

    public static TcpTestServer create() throws IOException {
        TcpTestServer server = new TcpTestServer();
        server.thread.start();
        return server;
    }

    public String getHost() {
        return "localhost";
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public int getAccepts() {
        return accepts.get();
    }

    public String pollReq(Duration dur) throws InterruptedException {
        return reqs.poll(dur.toNanos(), TimeUnit.NANOSECONDS);
    }

    public String takeReq() throws InterruptedException {
        return reqs.take();
    }

    @Override
    public void close() throws IOException {
        if (thread.isAlive()) {
            thread.interrupt();
        }
        serverSocket.close();
    }
}
