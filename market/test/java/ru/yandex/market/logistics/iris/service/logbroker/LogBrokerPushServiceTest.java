package ru.yandex.market.logistics.iris.service.logbroker;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerInteractionException;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerPushService;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.iris.service.system.SystemPropertySequenceKey.LOGBROKER_PUSH_SEQ_NO;


public class LogBrokerPushServiceTest {
    private Clock clock = Clock.fixed(LocalDateTime.of(2019, 1, 1, 0, 0)
        .atZone(ZoneId.systemDefault()).toInstant(), ZoneOffset.UTC);

    private LogBrokerPushService logBrokerPushService;
    private AsyncProducer asyncProducer;
    private SystemPropertyService systemPropertyService;

    @Before
    public void setUp() {
        asyncProducer = mock(AsyncProducer.class);
        systemPropertyService = mock(SystemPropertyService.class);
        logBrokerPushService = new LogBrokerPushService(clock, Duration.ofSeconds(5), () -> asyncProducer, systemPropertyService);
    }

    @Test
    public void pushSingleMessageCorrectly() {
        final String message = "message";

        final long seqNumber = 1;
        ProducerWriteResponse producerWriteResponse = new ProducerWriteResponse(seqNumber, 1, false);
        mockAsyncProducer(
            new ProducerInitResponse(seqNumber, "topic", 1, "session_1"),
            producerWriteResponse
        );

        when(systemPropertyService.incrementAndGetSequence(LOGBROKER_PUSH_SEQ_NO)).thenReturn(seqNumber);
        ProducerWriteResponse response = logBrokerPushService.push(message.getBytes());

        assertEqualsWriteResponse(producerWriteResponse, response);
        verify(asyncProducer, times(0)).close();
        verify(asyncProducer, times(1)).init();
        verify(asyncProducer, times(1)).write(message.getBytes(), 1, clock.millis());
    }

    @Test
    public void pushBatchOfMessagesCorrectly() {
        final String message = "message";

        ProducerWriteResponse producerWriteResponse1 = new ProducerWriteResponse(1, 10, false);
        ProducerWriteResponse producerWriteResponse2 = new ProducerWriteResponse(2, 11, false);
        mockAsyncProducer(
            new ProducerInitResponse(1, "topic", 1, "session_1"),
            producerWriteResponse1,
            producerWriteResponse2
        );
        when(systemPropertyService.incrementAndGetSequence(LOGBROKER_PUSH_SEQ_NO)).thenReturn(1L, 2L);

        ProducerWriteResponse response1 = logBrokerPushService.push(message.getBytes());
        ProducerWriteResponse response2 = logBrokerPushService.push(message.getBytes());
        assertEqualsWriteResponse(producerWriteResponse1, response1);
        assertEqualsWriteResponse(producerWriteResponse2, response2);

        verify(asyncProducer, times(0)).close();
        verify(asyncProducer, times(1)).init();
        verify(asyncProducer, times(1)).write(message.getBytes(), 1, clock.millis());
        verify(asyncProducer, times(1)).write(message.getBytes(), 2, clock.millis());
    }

    @Test
    public void pushMessageOnInitializationError() {
        final String message = "message";

        CompletableFuture<ProducerInitResponse> initFuture = CompletableFuture.completedFuture(null);
        initFuture.completeExceptionally(new IOException("error"));
        mockAsyncProducer(initFuture);

        CompletableFuture<ProducerWriteResponse> response = logBrokerPushService.asyncPush(message.getBytes());
        response.handle((resp, throwable) -> {
            assertEquals(new IOException("error"), throwable);
            return resp;
        });

        verify(asyncProducer, times(1)).init();
        verify(asyncProducer, times(0)).write(any(), anyLong(), anyLong());
        verify(asyncProducer, times(0)).close();
    }

    @Test
    public void pushMessageOnWriteError() {
        final String message = "message";

        CompletableFuture<ProducerWriteResponse> writeResponseFuture = CompletableFuture.completedFuture(null);
        writeResponseFuture.completeExceptionally(new IOException("error"));

        mockAsyncProducer(
            CompletableFuture.completedFuture(new ProducerInitResponse(1, "topic", 1, "session_1")),
            writeResponseFuture
        );

        CompletableFuture<ProducerWriteResponse> response = logBrokerPushService.asyncPush(message.getBytes());
        response.handle((resp, throwable) -> {
            assertEquals(new IOException("error"), throwable);
            return resp;
        });

        verify(asyncProducer, times(1)).init();
        verify(asyncProducer, times(1)).write(any(), anyLong(), anyLong());
        verify(asyncProducer, times(1)).close();
    }

    @Test
    public void pushMessageCorrectlyAfterOnWriteError() throws Exception {
        final String message = "message";

        CompletableFuture<ProducerWriteResponse> producerWriteResponse1 = CompletableFuture.completedFuture(null);
        producerWriteResponse1.completeExceptionally(new IOException("error"));
        CompletableFuture<ProducerWriteResponse> producerWriteResponse2 =
            CompletableFuture.completedFuture(new ProducerWriteResponse(2, 11, false));

        mockAsyncProducer(
            CompletableFuture.completedFuture(new ProducerInitResponse(1, "topic", 1, "session_1")),
            producerWriteResponse1,
            producerWriteResponse2
        );
        when(systemPropertyService.incrementAndGetSequence(LOGBROKER_PUSH_SEQ_NO)).thenReturn(1L, 2L);

        CompletableFuture<ProducerWriteResponse> response1 = logBrokerPushService.asyncPush(message.getBytes());
        response1.handle((resp, throwable) -> {
            assertEquals(new IOException("error"), throwable);
            return resp;
        });

        ProducerWriteResponse response2 = logBrokerPushService.push(message.getBytes());
        assertEquals(producerWriteResponse2.get(), response2);

        verify(asyncProducer, times(2)).init();
        verify(asyncProducer, times(1)).write(message.getBytes(), 1, clock.millis());
        verify(asyncProducer, times(1)).write(message.getBytes(), 2, clock.millis());
        verify(asyncProducer, times(1)).close();
    }

    @Test(expected = LogBrokerInteractionException.class)
    public void reInitializeWithReturnNullProducerFromProvider() throws Exception {
        final String message = "message";

        Supplier<AsyncProducer> asyncProducerProvider = () -> null;
        logBrokerPushService = new LogBrokerPushService(clock, Duration.ofSeconds(5), asyncProducerProvider,
                systemPropertyService);

        logBrokerPushService.asyncPush(message.getBytes());
    }

    private void mockAsyncProducer(ProducerInitResponse producerInitResponse, ProducerWriteResponse... responses) {
        @SuppressWarnings("unchecked")
        CompletableFuture<ProducerWriteResponse>[] writeResponsesArray = (CompletableFuture[]) Stream.of(responses)
            .map(CompletableFuture::completedFuture)
            .toArray(CompletableFuture[]::new);

        mockAsyncProducer(CompletableFuture.completedFuture(producerInitResponse), writeResponsesArray);
    }

    @SafeVarargs
    private final void mockAsyncProducer(CompletableFuture<ProducerInitResponse> producerInitResponse,
                                         CompletableFuture<ProducerWriteResponse>... responses) {

        when(asyncProducer.init()).thenReturn(producerInitResponse);

        if (responses.length == 1) {
            when(asyncProducer.write(any(), anyLong(), anyLong())).thenReturn(responses[0]);
        } else if (responses.length > 0) {
            when(asyncProducer.write(any(), anyLong(), anyLong())).thenReturn(
                responses[0],
                Arrays.copyOfRange(responses, 1, responses.length)
            );
        }

        doNothing().when(asyncProducer).close();
    }

    private void assertEqualsWriteResponse(ProducerWriteResponse expected, ProducerWriteResponse actual) {
        assertEquals(expected.getOffset(), actual.getOffset());
        assertEquals(expected.getSeqNo(), actual.getSeqNo());
        assertEquals(expected.isAlreadyWritten(), actual.isAlreadyWritten());
    }

}
