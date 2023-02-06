package ru.yandex.market.logbroker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 17/07/19.
 */
class LogbrokerReadingServiceTest {
    @InjectMocks
    LogbrokerReadingService logbrokerReadingService;
    @Mock
    LogbrokerClientFactory logbrokerClientFactory;
    @Mock
    SyncConsumer syncConsumer;
    @Mock
    StreamConsumer streamConsumer;
    @Mock
    MessageBatch batch;

    private static final long COOKIE = 111;
    private static final LogbrokerConsumerConfig READ_CONFIG = new LogbrokerConsumerConfig(
            "topic", "consumerPath", 50, LbInstallation.LBKX);

    @BeforeEach
    void setUp() throws TimeoutException, InterruptedException {
        MockitoAnnotations.initMocks(this);
        when(logbrokerClientFactory.syncConsumer(any())).thenReturn(syncConsumer);
        when(logbrokerClientFactory.streamConsumer(any())).thenReturn(streamConsumer);
        when(syncConsumer.init()).thenReturn(new ConsumerInitResponse("sessionId"));
    }

    @Test
    void testRead() throws TimeoutException, InterruptedException {
        when(syncConsumer.read()).thenReturn(new ConsumerReadResponse(Arrays.asList(batch), COOKIE));
        int consumerLimit = 3;
        List<MessageBatch> read = new ArrayList<>();
        logbrokerReadingService.read(READ_CONFIG, messages -> {
            read.addAll(messages);
            return read.size() < consumerLimit;
        });

        assertEquals(consumerLimit, read.size());
        verifyConnectionLifecycle(consumerLimit);
    }

    @Test
    void readNull() throws InterruptedException, TimeoutException {
        when(syncConsumer.read()).thenReturn(null);
        List<MessageBatch> read = new ArrayList<>();
        logbrokerReadingService.read(READ_CONFIG, messages -> {
            read.addAll(messages);
            return !messages.isEmpty();
        });

        assertTrue(read.isEmpty());
        verifyConnectionLifecycle(0);
    }

    @Test
    void readExc() throws InterruptedException, TimeoutException {
        when(syncConsumer.read()).thenThrow(new IllegalStateException("logbroker read exception"));
        assertThrows(RuntimeException.class, () -> logbrokerReadingService.read(READ_CONFIG, msg -> true));
        verifyConnectionLifecycle(0);
    }

    private void verifyConnectionLifecycle(int expectedMessages) throws TimeoutException, InterruptedException {
        verify(syncConsumer).init();
        verify(syncConsumer, times(expectedMessages == 0 ? 1 : expectedMessages)).read();
        verify(syncConsumer, times(expectedMessages)).commit(anyLong());
        verify(syncConsumer).close();
    }
}
