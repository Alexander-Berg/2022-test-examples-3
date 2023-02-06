package ru.yandex.market.communication.proxy.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;

/**
 * @author zilzilok
 */
public class LogbrokerTestUtil {
    private static final MessageMeta meta = new MessageMeta(
            "test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW, Collections.emptyMap()
    );

    private LogbrokerTestUtil() {
    }

    public static MessageBatch createMessageBatch(Class<?> clazz, String... jsonFilePaths) {
        List<byte[]> events = Arrays.stream(jsonFilePaths).map(path -> {
            try {
                return clazz.getResourceAsStream(path).readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return createMessageBatch(events);
    }

    public static MessageBatch createMessageBatch(List<byte[]> items) {
        return new MessageBatch("topic", 1,
                items.stream().map(item -> new MessageData(item, 0, meta)).collect(Collectors.toList()));
    }
}
