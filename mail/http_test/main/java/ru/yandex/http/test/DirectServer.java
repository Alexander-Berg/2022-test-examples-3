package ru.yandex.http.test;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ru.yandex.concurrent.NamedThreadFactory;

public class DirectServer extends Thread implements Closeable {
    private final AtomicInteger requestsReceived = new AtomicInteger();
    private final ServerSocket socket = new ServerSocket(0);
    private final Function<Socket, Runnable> tasksFactory;
    private final ExecutorService executor;

    public DirectServer(final Function<Socket, Runnable> tasksFactory)
        throws IOException
    {
        this(tasksFactory, 2);
    }

    public DirectServer(
        final Function<Socket, Runnable> tasksFactory,
        final int workers)
        throws IOException
    {
        this(tasksFactory, workers, "DirectServer");
    }

    public DirectServer(
        final Function<Socket, Runnable> tasksFactory,
        final int workers,
        final String name)
        throws IOException
    {
        super(name);
        this.tasksFactory = tasksFactory;
        executor = Executors.newFixedThreadPool(
            workers,
            new NamedThreadFactory(name, true));
        setDaemon(true);
    }

    public int port() {
        return socket.getLocalPort();
    }

    public int requestsReceived() {
        return requestsReceived.get();
    }

    @Override
    public void close() throws IOException {
        socket.close();
        executor.shutdown();
    }

    @Override
    public void run() {
        while (true) {
            try {
                executor.execute(tasksFactory.apply(this.socket.accept()));
                requestsReceived.incrementAndGet();
            } catch (IOException e) {
                break;
            }
        }
    }

    public static class StaticResponseTask implements Runnable {
        private final Socket socket;
        private final byte[] response;

        public StaticResponseTask(final Socket socket, final String response) {
            this.socket = socket;
            this.response = response.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void run() {
            try (Socket socket = this.socket;
                OutputStream out = socket.getOutputStream())
            {
                out.write(response);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

