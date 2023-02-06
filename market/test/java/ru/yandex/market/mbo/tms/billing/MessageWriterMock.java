package ru.yandex.market.mbo.tms.billing;

import ru.yandex.market.mbo.history.messaging.MessageType;
import ru.yandex.market.mbo.history.messaging.MessageWriter;

public class MessageWriterMock implements MessageWriter {
    @Override
    public long writeMessage(MessageType type, String message) {
        return 0;
    }

    @Override
    public long writeMessage(MessageType type, String message, Exception exception) {
        return 0;
    }
}
