package ru.yandex.market.logistics.cs.util;

import java.util.Collections;

import lombok.experimental.UtilityClass;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;

@UtilityClass
public class LogbrokerMessageFactory {
    private static final MessageMeta EMPTY_META =
        new MessageMeta(new byte[0], 0, 0, 0, "", CompressionCodec.RAW, Collections.emptyMap());

    public MessageMeta emptyMessageMeta() {
        return EMPTY_META;
    }
}
