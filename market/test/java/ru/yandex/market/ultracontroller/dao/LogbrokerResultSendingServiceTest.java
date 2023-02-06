package ru.yandex.market.ultracontroller.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.consumer.BatchingStreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.stream.retry.RetryingBatchingStreamConsumer;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ultracontroller.utils.logbroker.LogbrokerConnectionFactory;
import ru.yandex.market.ultracontroller.utils.logbroker.LogbrokerProducerPool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("MagicNumber")
public class LogbrokerResultSendingServiceTest {
    private static final long MAX_QUEUED_MESSAGES_COUNT = 2;
    private static final int QUEUE_SIZE_LOG_PERIOD_MS = 1000;
    private static final int NOT_ALIVE_TRIGGER_TIMEOUT_MS = 2000;
    private static final String OUTPUT_TOPIC_NAME = "outputTopicName";
    private LogbrokerResultSendingService logbrokerResultSendingServiceTest;

    @Test
    public void reconnectionTest() throws InterruptedException {
        UltraController.EnrichCacheRecord cacheRecord = UltraController.EnrichCacheRecord.newBuilder().build();
        for (int i = 0; i < 5; i++) {
            logbrokerResultSendingServiceTest.send(OUTPUT_TOPIC_NAME, cacheRecord);
        }
        Thread.sleep(QUEUE_SIZE_LOG_PERIOD_MS);
        logbrokerResultSendingServiceTest.send(OUTPUT_TOPIC_NAME, cacheRecord);
        Thread.sleep(100);
        logbrokerResultSendingServiceTest.stop();

        Assert.assertEquals(5, DummyLogbrokerConnectionFactory.MESSAGE_COUNTER.get());
    }

    @Ignore
    public static class DummyLogbrokerConnectionFactory implements LogbrokerConnectionFactory {
        static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger();

        @Override
        public AsyncProducer asyncProducer(AsyncProducerConfig config) throws InterruptedException {
            System.out.println("new AsyncProducer!");
            return new AsyncProducer() {
                private final AtomicBoolean writeFail = new AtomicBoolean(false);

                @Override
                public CompletableFuture<ProducerInitResponse> init() {
                    return CompletableFuture.completedFuture(
                        new ProducerInitResponse(0, OUTPUT_TOPIC_NAME, 0, "")
                    );
                }

                @Override
                public CompletableFuture<ProducerWriteResponse> write(byte[] data, long seqNo, long timestamp) {
                    return CompletableFuture.supplyAsync(() -> {
                        pause();
                        if (writeFail.getAndSet(true)) { // одноразовое соединение
                            System.out.println("write failed: " + seqNo);
                            throw new RuntimeException("fail");
                        } else {
                            System.out.println("message written: " + seqNo);
                            MESSAGE_COUNTER.incrementAndGet();
                            return new ProducerWriteResponse(seqNo, 0, false);
                        }
                    });
                }

                @Override
                public CompletableFuture<Void> closeFuture() {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public BatchingStreamConsumer batchingStreamConsumer(StreamConsumerConfig config) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public RetryingBatchingStreamConsumer retryingBatchingStreamConsumer(StreamConsumerConfig config) {
            throw new UnsupportedOperationException();
        }
    }

    private static void pause() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Before
    public void init() {
        String topicFolder = "test";
        Supplier<Credentials> credentialsSupplier = () -> null;
        long initTimeoutMilliseconds = 0;
        long minReconnectTimeoutMilliseconds = 1;
        long maxReconnectTimeoutMilliseconds = 1;
        int poolSize = 1;
        int maxRunningSendersCount = 1;
        long processMessageQueuePeriodMillis = 1;

        LogbrokerProducerPool logbrokerProducerPool = new LogbrokerProducerPool(
            topicFolder,
            credentialsSupplier,
            initTimeoutMilliseconds,
            minReconnectTimeoutMilliseconds,
            maxReconnectTimeoutMilliseconds,
            OUTPUT_TOPIC_NAME,
            poolSize,
            maxRunningSendersCount,
            MAX_QUEUED_MESSAGES_COUNT,
            processMessageQueuePeriodMillis,
            new DummyLogbrokerConnectionFactory(),
            null,
            1,
            1000
        );

        logbrokerResultSendingServiceTest = new LogbrokerResultSendingService(
            logbrokerProducerPool,
            QUEUE_SIZE_LOG_PERIOD_MS,
            false,
            NOT_ALIVE_TRIGGER_TIMEOUT_MS
        );

    }
}
