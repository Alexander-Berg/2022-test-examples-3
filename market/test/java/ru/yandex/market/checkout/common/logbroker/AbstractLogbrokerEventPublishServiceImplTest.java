package ru.yandex.market.checkout.common.logbroker;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AbstractLogbrokerEventPublishServiceImplTest {

    private static final String SOURCE_ID_PREFIX = "somePrefix";

    private List<TestEvent> createTestEvents(int eventsCount) {
        List<TestEvent> events = new ArrayList<>();
        Random rnd = new Random();
        for (long eventId = 0; eventId < eventsCount; eventId++) {
            Long orderId = Integer.valueOf(rnd.nextInt(eventsCount / 2)).longValue();
            events.add(new TestEvent(orderId, eventId));
        }
        Collections.shuffle(events);
        return events;
    }

    @Test
    void publishEvents_shouldUseSameSourceId() {
        // given:
        LogbrokerClientFactoryMock clientFactoryMock = new LogbrokerClientFactoryMock(null);
        AsyncProducerConfig asyncProducerConfig = AsyncProducerConfig.builder("topicName", SOURCE_ID_PREFIX.getBytes())
                .build();

        AbstractLogbrokerEventPublishServiceImpl<TestEvent> service = new SimpleServiceImpl<>(
                clientFactoryMock,
                100,
                asyncProducerConfig,
                Clock.systemUTC(),
                100
        );

        int eventsCount = 1205;
        final List<TestEvent> events = createTestEvents(eventsCount);

        AsyncProducer producer = service.createAndInitProducer(null);
        // when:
        service.publishEvents(events, TestEvent::getEventId, producer);

        // then:
        Map<String, Long> timesCalledProducerBySourceId =
                clientFactoryMock.getCreatedProducers().stream().collect(toMap(AsyncProducerMock::getSourceId,
                        AsyncProducerMock::getTimesCalled));
        assertThat(timesCalledProducerBySourceId.keySet()).hasSize(1);
        String sourceId = timesCalledProducerBySourceId.keySet().iterator().next();
        assertThat(sourceId).isEqualTo(SOURCE_ID_PREFIX);
        assertThat(timesCalledProducerBySourceId.get(sourceId)).isEqualTo(eventsCount);
    }

    private static class SimpleServiceImpl<T extends TestEvent> extends AbstractLogbrokerEventPublishServiceImpl<T> {

        SimpleServiceImpl(
                LogbrokerClientFactory logbrokerClientFactory,
                int logbrokerWriteTimeout,
                AsyncProducerConfig asyncProducerConfig,
                Clock clock,
                int logbrokerInitTimeout
        ) {
            super(logbrokerClientFactory, logbrokerWriteTimeout, asyncProducerConfig, clock, logbrokerInitTimeout);
        }

        @Override
        protected byte[] eventToBytes(T event) {
            return new byte[0];
        }
    }

    private static class AsyncProducerMock implements AsyncProducer {

        private final String sourceId;
        private long timesCalled = 0;
        private long lastSeqNo = Integer.MIN_VALUE;

        AsyncProducerMock(String sourceId) {
            this.sourceId = sourceId;
        }

        @Override
        public CompletableFuture<ProducerInitResponse> init() {
            return CompletableFuture.supplyAsync(() -> new ProducerInitResponse(1, "topic", 1, "sessionID"));
        }

        @Override
        public CompletableFuture<ProducerWriteResponse> write(byte[] data, long seqNo, long timestamp) {
            assertThat(seqNo)
                    .describedAs("seqNo must always increase within same sourceId")
                    .isGreaterThan(lastSeqNo);
            ++timesCalled;
            lastSeqNo = seqNo;
            return CompletableFuture.supplyAsync(() -> new ProducerWriteResponse(1, 0, false));
        }

        @Override
        public CompletableFuture<Void> closeFuture() {
            return null;
        }

        @Override
        public void close() {
        }

        public long getTimesCalled() {
            return timesCalled;
        }

        public String getSourceId() {
            return sourceId;
        }
    }

    private static class LogbrokerClientFactoryMock extends LogbrokerClientFactory {

        private final List<AsyncProducerMock> createdProducers;

        LogbrokerClientFactoryMock(ProxyBalancer proxyBalancer) {
            super(proxyBalancer);
            this.createdProducers = new ArrayList<>();
        }

        @Override
        public AsyncProducer asyncProducer(AsyncProducerConfig config) {
            String sourceId = new String(config.getSourceId());
            AsyncProducerMock asyncProducer = new AsyncProducerMock(sourceId);
            createdProducers.add(asyncProducer);
            return asyncProducer;
        }

        public List<AsyncProducerMock> getCreatedProducers() {
            return createdProducers;
        }
    }

    private static class TestEvent {

        private final Long orderId;
        private final Long eventId;

        TestEvent(Long orderId, Long eventId) {
            this.orderId = orderId;
            this.eventId = eventId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public Long getEventId() {
            return eventId;
        }
    }
}
