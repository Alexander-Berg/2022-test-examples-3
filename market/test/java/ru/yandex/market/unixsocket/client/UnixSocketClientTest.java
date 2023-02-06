package ru.yandex.market.unixsocket.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.unixsocket.server.UnixSocketServer;
import ru.yandex.market.unixsocket.serialization.impl.StringBytesParser;
import ru.yandex.market.unixsocket.protocol.impl.LengthThanMessageProtocol;
import ru.yandex.market.unixsocket.server.processor.MessageProcessor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


class UnixSocketClientTest {
    private UnixSocketServer<String, String> server;
    private UnixSocketClient client;
    private final String response = "pong!";
    private Thread serverThread;
    private final String socketFile = "socket_connection.sock";;

    @BeforeEach
    public void setUp() {
        client = new UnixSocketClient(
            socketFile,
            new LengthThanMessageProtocol(),
            UnixSocketClientSettings.newBuilder().withResponseWaitTimeoutMillis(1000).build()
        );
    }

    @AfterEach
    public void close() {
        serverThread.interrupt();
        new File(socketFile).delete();
    }

    @Test
    public void testPing() throws IOException, InterruptedException, ExecutionException {
        startStringServer(value -> response);
        String actualResponse = client.sendMessage(
            "ping!".getBytes(),
            new StringBytesParser()
        ).get();

        Assertions.assertEquals(response, actualResponse);

        actualResponse = client.sendMessage(
                "another ping!".getBytes(),
                new StringBytesParser()
        ).get();

        Assertions.assertEquals(response, actualResponse);
    }

    @Test
    public void sendMessageWithoutResponse() throws IOException, InterruptedException, ExecutionException {
        startStringServer(value -> response);
        CompletableFuture<String> actualResponse = client.sendMessage(
            "ping!".getBytes(),
            new StringBytesParser()
        );

        client.sendMessage(
                "ping!".getBytes(),
                new StringBytesParser()
        );

        client.sendMessage(
                "ping!".getBytes(),
                new StringBytesParser()
        );

        client.sendMessage(
                "ping!".getBytes(),
                new StringBytesParser()
        );

        Assertions.assertEquals(response, actualResponse.get());
    }

    @Test
    public void testTimeout() {
        startStringServer(value -> {throw new RuntimeException("error");});

        Assertions.assertThrows(
            ExecutionException.class,
            () -> client.sendMessage("ping!".getBytes(), String::new).get(),
            "There was no response after 1000 ms"
        );
    }

    private void startStringServer(MessageProcessor<String, String> processor) {
        server = new UnixSocketServer<>(
            socketFile,
            processor,
            String::new,
            String::getBytes,
            new LengthThanMessageProtocol()
        );
        serverThread = new Thread(server);
        serverThread.start();
    }
}
