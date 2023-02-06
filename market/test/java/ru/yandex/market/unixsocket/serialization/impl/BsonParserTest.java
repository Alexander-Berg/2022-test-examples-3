package ru.yandex.market.unixsocket.serialization.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.market.unixsocket.client.UnixSocketClient;
import ru.yandex.market.unixsocket.protocol.impl.LengthThanMessageProtocol;
import ru.yandex.market.unixsocket.server.UnixSocketServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class BsonParserTest {
    @Test
    public void parserTest() throws IOException, InterruptedException, ExecutionException {
        String socketFile = "socket_connection.sock";
        BsonParser<Message> messageBsonParser = new BsonParser<>(Message.class);
        LengthThanMessageProtocol protocol = new LengthThanMessageProtocol();

        String messageId = "testId";

        var server = new UnixSocketServer<>(
            socketFile,
            value -> new Message(value.getMessageId(), value.getMessageValue() + 666), messageBsonParser,
            messageBsonParser,
            protocol
        );

        Thread serverThread = new Thread(server);
        serverThread.start();

        var client = new UnixSocketClient(
            socketFile,
            protocol
        );

        Message response = client.sendMessage(
            new Message(messageId, 0), messageBsonParser, messageBsonParser
        ).get();

        Assertions.assertEquals(messageId, response.getMessageId());
        Assertions.assertEquals(666, response.getMessageValue());

        serverThread.interrupt();
    }

    private static class Message {
        private String messageId;
        private int messageValue;

        public Message(String messageId, int messageValue) {
            this.messageId = messageId;
            this.messageValue = messageValue;
        }

        public String getMessageId() {
            return messageId;
        }

        public int getMessageValue() {
            return messageValue;
        }
    }
}
