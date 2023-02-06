package ru.yandex.market.utils.logbroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.common.util.IOUtils;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;

public class MessageBatchBuilder<T extends MessageBatchItem> {

    private MessageMeta messageMeta;
    private List<T> items;
    private boolean doZip;

    public MessageBatchBuilder(MessageMeta messageMeta) {
        this.messageMeta = messageMeta;
        this.items = new ArrayList<>();
        this.doZip = false;
    }

    public MessageBatchBuilder() {
        this(
                new MessageMeta(
                        "test".getBytes(),
                        0,
                        0,
                        0,
                        "::1",
                        CompressionCodec.RAW,
                        Collections.emptyMap()
                )
        );
    }

    public MessageBatchBuilder<T> setMessageMeta(MessageMeta messageMeta) {
        this.messageMeta = messageMeta;
        return this;
    }

    public MessageBatchBuilder<T> setItems(List<T> items) {
        this.items = items;
        return this;
    }

    public MessageBatchBuilder<T> addItem(T item) {
        items.add(item);
        return this;
    }

    public MessageBatchBuilder<T> addAllMessages(Collection<T> messages) {
        this.items.addAll(messages);
        return this;
    }

    public MessageBatchBuilder<T> setDoZip(boolean doZip) {
        this.doZip = doZip;
        return this;
    }

    public MessageBatch build() {
        return items.stream()
                .map(item -> new MessageData(toByteArray(item), 0, messageMeta))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        msgs -> new MessageBatch("topic", 1, msgs)
                ));
    }

    private byte[] toByteArray(T item) {
        return doZip ? IOUtils.zip(item.toByteArray()) : item.toByteArray();
    }
}
