package ru.yandex.market.abo.core.logbroker;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 19/06/19.
 */
class LogbrokerPublishingServiceTest {
    private static final byte[] SOME_BYTES = new byte[]{1, 2, 22};
    private static final String TOPIC = "la-la lend";

    @InjectMocks
    LogbrokerPublishingService publishingService;
    @Mock
    LogbrokerProducerFactory logbrokerProducerFactory;

    @Mock
    LogbrokerEvent event;
    @Mock
    AsyncProducer producer;
    @Mock
    CompletableFuture<ProducerWriteResponse> writeFuture;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(event.getBytes()).thenReturn(SOME_BYTES);
        when(logbrokerProducerFactory.createProducer(TOPIC)).thenReturn(producer);
        when(producer.write(any())).thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 2L, true)));
    }

    @Test
    void testPublishEvents() {
        List<LogbrokerEvent> events = List.of(event, event);
        publishingService.publishEvents(events, TOPIC);

        verify(logbrokerProducerFactory).createProducer(TOPIC);
        verify(producer, times(events.size())).write(SOME_BYTES);
        verify(producer, never()).close();
    }

    @Test
    void testPublishWithError() {
        List<LogbrokerEvent> events = List.of(event, event);
        // first - timeout
        CompletableFuture<ProducerWriteResponse> f = new CompletableFuture<>();
        f.completeExceptionally(new TimeoutException());
        when(producer.write(any())).thenReturn(f);
        assertThrows(RuntimeException.class, () -> publishingService.publishEvents(events, TOPIC));

        // second - success
        when(producer.write(any())).thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 2L, true)));
        publishingService.publishEvents(events, TOPIC);

        verify(logbrokerProducerFactory, times(2)).createProducer(TOPIC);
        verify(producer).close();
    }

    @Test
    void producerInitException() {
        when(logbrokerProducerFactory.createProducer(TOPIC)).thenThrow(new IllegalStateException("init exception"));
        assertThrows(RuntimeException.class, () -> publishingService.publishEvents(List.of(event), TOPIC));
    }
}
