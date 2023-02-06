package ru.yandex.market.wms.shared.libs.async.retry.impl;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.wms.shared.libs.async.retry.DeadQueueMessageTtl;
import ru.yandex.market.wms.shared.libs.async.retry.queuehelper.DeadQueueHelper;
import ru.yandex.market.wms.shared.libs.async.retry.queuehelper.DelayQueueHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.X_RETRIES;

class CommonJmsRetryTest {

    private final DelayQueueHelper delayQueueHelper = mock(DelayQueueHelper.class);
    private final DeadQueueHelper deadQueueHelper = mock(DeadQueueHelper.class);

    @AfterEach
    void tearDown() {
        Mockito.reset(delayQueueHelper);
        Mockito.reset(deadQueueHelper);
    }

    @Test
    void retryDoesntProcessWhenRetriesNotEnabled() {
        String retryingQueue = "testQueue";
        MessageHeaders headers = new MessageHeaders(Map.of(X_RETRIES, 0));
        Object request = new Object();

        CommonJmsRetry commonJmsRetry = new CommonJmsRetry(delayQueueHelper, deadQueueHelper, false);
        commonJmsRetry.retry(retryingQueue, request, headers);

        verify(delayQueueHelper, times(0)).sendMessage(any(), any(), any(MessageHeaders.class));
        verify(deadQueueHelper).sendMessage(any(), any(), any(MessageHeaders.class), any(Long.class));
        verify(deadQueueHelper).sendMessage(eq(retryingQueue), eq(request), any(MessageHeaders.class), eq(0L));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void retrySendMessageToDelayWhenRetriesEnabledAndRetriesValueIsLessThanThree(int retries) {
        String retryingQueue = "testQueue";
        MessageHeaders headers = new MessageHeaders(Map.of(X_RETRIES, retries));
        Object request = new Object();

        CommonJmsRetry commonJmsRetry = new CommonJmsRetry(delayQueueHelper, deadQueueHelper, true);
        commonJmsRetry.retry(retryingQueue, request, headers);

        verify(delayQueueHelper).sendMessage(any(), any(), any(MessageHeaders.class));
        verify(delayQueueHelper).sendMessage(eq(retryingQueue), eq(request), any(MessageHeaders.class));
        verify(deadQueueHelper, times(0)).sendMessage(any(), any(), any(MessageHeaders.class), any(Long.class));
    }

    @Test
    void retrySendMessageToDeadQueueWhenRetriesValueIsThree() {
        String retryingQueue = "testQueue";
        MessageHeaders headers = new MessageHeaders(Map.of(X_RETRIES, 3));
        Object request = new Object();

        CommonJmsRetry commonJmsRetry = new CommonJmsRetry(delayQueueHelper, deadQueueHelper, true);
        commonJmsRetry.retry(retryingQueue, request, headers);

        verify(delayQueueHelper, times(0)).sendMessage(any(), any(), any(MessageHeaders.class));
        verify(deadQueueHelper).sendMessage(any(), any(), any(MessageHeaders.class), any(Long.class));
        verify(deadQueueHelper).sendMessage(eq(retryingQueue), eq(request), any(MessageHeaders.class), eq(0L));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 2 * 60 * 60L})
    void retrySendMessageToRelevantDeadQueueWhenMessageTtlIsSet(long ttlInSec) {
        DeadQueueMessageTtl deadQueueMessageTtl = DeadQueueMessageTtl.of(ttlInSec);
        String retryingQueue = "testQueue";
        MessageHeaders headers = new MessageHeaders(Map.of(X_RETRIES, 3));
        Object request = new Object();

        CommonJmsRetry commonJmsRetry = new CommonJmsRetry(delayQueueHelper, deadQueueHelper, true);
        commonJmsRetry.retry(retryingQueue, request, headers, deadQueueMessageTtl);

        verify(delayQueueHelper, times(0)).sendMessage(any(), any(), any(MessageHeaders.class));
        verify(deadQueueHelper).sendMessage(any(), any(), any(MessageHeaders.class), any(Long.class));
        verify(deadQueueHelper).sendMessage(eq(retryingQueue), eq(request), any(MessageHeaders.class),
                eq(deadQueueMessageTtl.getValueInSec()));
    }

    @Test
    void retryWithTtlForDeadQueueSendMessageToRelevantDeadQueue() {
        String retryingQueue = "testQueue";
        MessageHeaders headers = new MessageHeaders(Map.of(X_RETRIES, 3));
        Object request = new Object();

        CommonJmsRetry commonJmsRetry = new CommonJmsRetry(delayQueueHelper, deadQueueHelper, true);
        commonJmsRetry.retryWithTtlForDeadQueue(retryingQueue, request, headers);

        verify(delayQueueHelper, times(0)).sendMessage(any(), any(), any(MessageHeaders.class));
        verify(deadQueueHelper).sendMessage(any(), any(), any(MessageHeaders.class), any(Long.class));
        verify(deadQueueHelper).sendMessage(eq(retryingQueue), eq(request), any(MessageHeaders.class), eq(7200L));
    }
}
