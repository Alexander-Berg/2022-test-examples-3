package ru.yandex.market.crm.lb.test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.BatchingStreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.BatchingStreamListener;
import ru.yandex.kikimr.persqueue.consumer.BatchingStreamListener.ReadResponder;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerLockMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReleaseMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.jmf.time.Now;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
public class StreamConsumerStub implements BatchingStreamConsumer {

    private final Object2LongMap<String> lockedPartitions = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());
    private final AtomicLong cookieProvider = new AtomicLong();
    private final Object2LongMap<String> commitedCookies = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());
    private volatile BatchingStreamListener listener;

    @Override
    public void startConsume(BatchingStreamListener listener) {
        this.listener = listener;
        this.listener.onInit(mock(ConsumerInitResponse.class));
    }

    @Override
    public void stopConsume() {
        this.listener = null;
    }

    public void sendLock(String partitionId, long readOffset, long endOffset) {
        assertStarted();

        Partition partition = Partition.parse(partitionId);

        listener.onLock(
                new ConsumerLockMessage(
                        partition.topic,
                        partition.number,
                        readOffset,
                        endOffset,
                        0
                ),
                (offset, verifyOffset) -> lockedPartitions.put(partitionId, offset)
        );
    }

    public void sendLock(String partitionId, long readOffset) {
        sendLock(partitionId, readOffset, readOffset);
    }

    public void sendData(String partitionId, Message... messages) {
        assertStarted();

        Partition partition = Partition.parse(partitionId);

        MessageMeta messageMeta = new MessageMeta(
                new byte[0],
                RandomUtils.nextLong(),
                Now.currentTimeMillis(),
                Now.currentTimeMillis(),
                "127.0.0.1",
                CompressionCodec.RAW,
                Collections.emptyMap()
        );

        MessageBatch batch = new MessageBatch(
                partition.topic,
                partition.number,
                Stream.of(messages)
                        .map(message -> new MessageData(
                                message.getContent().getBytes(),
                                message.getOffset(),
                                messageMeta
                        ))
                        .collect(Collectors.toList())
        );

        listener.onRead(
                new ConsumerReadResponse(
                        Collections.singletonList(batch),
                        cookieProvider.incrementAndGet()
                ),
                new ReadResponderStub(partitionId)
        );
    }

    public void sendRelease(String partitionId) {
        assertStarted();

        Partition partition = Partition.parse(partitionId);

        listener.onRelease(
                new ConsumerReleaseMessage(
                        partition.topic,
                        partition.number,
                        true,
                        0
                )
        );
    }

    public void sendError() {
        listener.onError(new RuntimeException("Failure"));
    }

    public void assertLocked(String partitionId, long expectedOffset) {
        long lockedOffset = lockedPartitions.getOrDefault(partitionId, -1);
        if (lockedOffset < 0) {
            fail("Partition " + partitionId + " is not locked by this instance");
        }

        assertEquals(expectedOffset, lockedOffset);
    }

    public void assertNotLocked(String partitionId) {
        assertFalse(lockedPartitions.containsKey(partitionId), "Partition " + partitionId + " is locked");
    }

    public void assertCommited(String partitionId, long cookie) {
        assertEquals(
                cookie, commitedCookies.getOrDefault(partitionId, -1),
                "Cookie " + cookie + " is not commited"
        );
    }

    public void assertStarted() {
        assertNotNull(listener, "Consumer is not started");
    }

    public void assertNotStarted() {
        assertNull(listener, "Consumer is started, but should not be");
    }

    private static class Partition {

        final String topic;
        final int number;

        Partition(String topic, int number) {
            this.topic = topic;
            this.number = number;
        }

        static Partition parse(String value) {
            String[] parts = value.split(":");
            String topic = parts[0];
            int number = Integer.parseInt(parts[1]);
            return new Partition(topic, number);
        }
    }

    private class ReadResponderStub implements ReadResponder {

        private final String partition;

        ReadResponderStub(String partition) {
            this.partition = partition;
        }

        @Override
        public void commit(List<Long> cookies) {
            long max = cookies.stream()
                    .mapToLong(x -> x)
                    .max().orElse(0);
            commitedCookies.put(partition, max);
        }
    }
}
