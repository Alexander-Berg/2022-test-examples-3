package ru.yandex.direct.test.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.utils.Checked;

public class SocketProxyTest {

    private SoftAssertions softly;

    @BeforeEach
    public void setUp() {
        softly = new SoftAssertions();
    }


    @Test
    public void testSingleConnection() throws InterruptedException, IOException {
        Queue<Throwable> exceptionHolder = new ArrayDeque<>();
        Runnable checkExceptions = () -> {
            Throwable exception = exceptionHolder.poll();
            if (exception != null) {
                while (!exceptionHolder.isEmpty()) {
                    exception.addSuppressed(exceptionHolder.remove());
                }
                throw new IllegalStateException(exception);
            }
        };
        try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLocalHost())) {
            Thread serverThread = new Thread(new SingleShotEchoServer(serverSocket, 3, 1));
            serverThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.add(e));
            serverThread.setDaemon(true);
            serverThread.setName("SocketProxyTest server thread");

            try (SocketProxy proxyThread = new SocketProxy((InetSocketAddress) serverSocket.getLocalSocketAddress())) {
                serverThread.start();

                proxyThread.setDaemon(true);
                proxyThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.add(e));
                proxyThread.setName("SocketProxyTest proxy thread");
                proxyThread.setDebug(true);
                proxyThread.start();

                SocketAddress proxyAddress = proxyThread.getListenAddress();

                try (Socket socket = new Socket()) {
                    socket.setSoTimeout((int) Duration.ofSeconds(5).toMillis());
                    checkExceptions.run();
                    socket.connect(proxyAddress);
                    checkExceptions.run();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    for (int i = 1; i <= 3; ++i) {
                        checkExceptions.run();
                        writer.write("proxy server " + i + "\r\n");
                        checkExceptions.run();
                        writer.flush();
                        checkExceptions.run();
                        String result = reader.readLine();
                        checkExceptions.run();
                        softly.assertThat(result).isEqualTo("echo: proxy server " + i);
                    }
                    softly.assertAll();
                }
            } finally {
                serverThread.interrupt();
            }
        } finally {
            checkExceptions.run();
        }
    }

    @Test
    public void testInterceptor() throws InterruptedException, IOException {
        Queue<Throwable> exceptionHolder = new ArrayDeque<>();
        Runnable checkExceptions = () -> {
            Throwable exception = exceptionHolder.poll();
            if (exception != null) {
                while (!exceptionHolder.isEmpty()) {
                    exception.addSuppressed(exceptionHolder.remove());
                }
                throw new IllegalStateException(exception);
            }
        };
        try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLocalHost())) {
            Thread serverThread = new Thread(new SingleShotEchoServer(serverSocket, 3, 1));
            serverThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.add(e));
            serverThread.setDaemon(true);
            serverThread.setName("SocketProxyTest server thread");

            try (SocketProxy proxyThread = new SocketProxy((InetSocketAddress) serverSocket.getLocalSocketAddress())) {
                serverThread.start();

                proxyThread.addDataInterceptor(new DisconnectAfterOneLine(serverSocket));
                proxyThread.setDaemon(true);
                proxyThread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.add(e));
                proxyThread.setName("SocketProxyTest proxy thread");
                proxyThread.setDebug(true);
                proxyThread.start();

                SocketAddress proxyAddress = proxyThread.getListenAddress();

                try (Socket socket = new Socket()) {
                    socket.setSoTimeout((int) Duration.ofSeconds(3).toMillis());
                    checkExceptions.run();
                    socket.connect(proxyAddress);
                    checkExceptions.run();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer.write("first line\r\n");
                    writer.flush();
                    String result = reader.readLine();
                    softly.assertThat(result).isEqualTo("echo: first line");
                    try {
                        writer.write("second line\r\n");
                        writer.flush();
                        // В этом тесте гонка данных, потому позволительны две ситуации:
                        // 1. Сокет закрылся по-нормальному, тогда readLine вернёт null, что равносильно EOF
                        // 2. Будет брошено ConnectionReset
                        softly.assertThat(reader.readLine()).isNull();
                    } catch (SocketException exc) {
                        softly.assertThat(exc.getMessage()).isEqualTo("Connection reset");
                    }
                    softly.assertAll();
                }
            } finally {
                serverThread.interrupt();
            }
        } finally {
            checkExceptions.run();
        }
    }

    private static class SingleShotEchoServer implements Runnable {
        private final ServerSocket serverSocket;
        private final int lineCount;
        private int connectionCount;

        private SingleShotEchoServer(ServerSocket serverSocket, int lineCount, int connectionCount) {
            this.serverSocket = serverSocket;
            this.lineCount = lineCount;
            this.connectionCount = connectionCount;
        }

        @Override
        public void run() {
            try {
                while (connectionCount-- > 0) {
                    Socket socket = serverSocket.accept();
                    int linesRemained = lineCount;
                    while (linesRemained-- > 0) {
                        byte[] buffer = new byte[1024];
                        int i = 0;
                        buffer[i++] = 'e';
                        buffer[i++] = 'c';
                        buffer[i++] = 'h';
                        buffer[i++] = 'o';
                        buffer[i++] = ':';
                        buffer[i++] = ' ';
                        while (!Thread.currentThread().isInterrupted()) {
                            int b = socket.getInputStream().read();
                            if (b < 0) {
                                break;
                            } else {
                                buffer[i++] = (byte) b;
                                if (b == '\n') {
                                    socket.getOutputStream().write(buffer, 0, i);
                                    socket.getOutputStream().flush();
                                    break;
                                }
                            }
                        }
                    }
                    socket.close();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                if (serverSocket.isClosed()) {
                    Checked.run(serverSocket::close);
                }
            }
        }
    }

    private static class DisconnectAfterOneLine implements SocketProxy.ManInTheMiddle {
        private final ServerSocket serverSocket;
        private boolean seenEof = false;

        private DisconnectAfterOneLine(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public boolean connectionRequested(SocketChannel source) {
            return true;
        }

        @Override
        public Pair<byte[], Boolean> transferRequested(SocketChannel source, SocketChannel sink,
                                                       byte[] data) {
            SocketAddress remoteAddress;
            try {
                remoteAddress = sink.getRemoteAddress();
            } catch (ClosedChannelException e) {
                // сокет уже закрылся, но буферы не обнулились
                return Pair.of(new byte[0], false);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            if (remoteAddress != null && !remoteAddress.equals(serverSocket.getLocalSocketAddress())) {
                boolean terminateAfter = false;
                if (seenEof) {
                    for (byte b : data) {
                        if (b == '\n') {
                            terminateAfter = true;
                            break;
                        }
                    }
                }
                return Pair.of(data, !terminateAfter);
            } else if (seenEof) {
                return Pair.of(new byte[0], false);
            } else {
                List<Byte> result = new ArrayList<>(data.length);
                for (byte b : data) {
                    result.add(b);
                    if (b == '\n') {
                        seenEof = true;
                        break;
                    }
                }
                return Pair.of(ArrayUtils.toPrimitive(result.toArray(new Byte[0])), true);
            }
        }
    }
}
