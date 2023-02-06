package ru.yandex.market.logbroker;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
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
        MockitoAnnotations.initMocks(this);

        when(event.getBytes()).thenReturn(SOME_BYTES);
        when(logbrokerProducerFactory.createProducer(TOPIC)).thenReturn(producer);
        when(producer.write(any())).thenReturn(writeFuture);
    }

    @Test
    void testPublishEvents() {
        List<LogbrokerEvent> events = Arrays.asList(event, event);
        publishingService.publishEvents(events, TOPIC);

        verify(logbrokerProducerFactory).createProducer(TOPIC);
        verify(producer, times(events.size())).write(SOME_BYTES);
        verify(producer, never()).close();
    }

    @Test
    void testPublishWithError() throws InterruptedException, ExecutionException, TimeoutException {
        List<LogbrokerEvent> events = Arrays.asList(event, event);
        when(writeFuture.get(anyLong(), any()))
                .thenThrow(new TimeoutException("timeout exception"))
                .thenReturn(null);
        assertThrows(RuntimeException.class, () -> publishingService.publishEvents(events, TOPIC));
        publishingService.publishEvents(events, TOPIC);

        verify(logbrokerProducerFactory, times(2)).createProducer(TOPIC);
        verify(producer).close();
    }

    @Test
    void producerInitException() {
        when(logbrokerProducerFactory.createProducer(TOPIC)).thenThrow(new IllegalStateException("init exception"));
        assertThrows(RuntimeException.class, () -> publishingService.publishEvents(Arrays.asList(event), TOPIC));
    }
}
