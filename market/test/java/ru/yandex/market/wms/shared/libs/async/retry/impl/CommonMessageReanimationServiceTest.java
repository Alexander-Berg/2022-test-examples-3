package ru.yandex.market.wms.shared.libs.async.retry.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.wms.shared.libs.async.enums.MqProvider;
import ru.yandex.market.wms.shared.libs.async.retry.MessageReanimationService;
import ru.yandex.market.wms.shared.libs.async.retry.queuehelper.DeadQueueHelper;
import ru.yandex.market.wms.shared.libs.async.retry.queuehelper.ProcessingQueueHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.shared.libs.async.jms.ServicebusMessageConverter.REQUEST_ID_PROPERTY;
import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.X_IDEMPOTENCY_KEY;
import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.X_MESSAGE_CREATION_TIMESTAMP;
import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.X_RETRYING_QUEUE;
import static ru.yandex.market.wms.shared.libs.async.retry.impl.MessageUtils.getRequestId;
import static ru.yandex.market.wms.shared.libs.async.retry.impl.MessageUtils.getRetryingQueue;

class CommonMessageReanimationServiceTest {

    private static final MqProvider MQ_PROVIDER = MqProvider.RABBITMQ;
    private static final String WAREHOUSE_ID = "0";

    public static final String TEST_PROCESSING_QUEUE_1 = "{mq}_{wrh}_testProcessingQueue1";
    public static final String TEST_PROCESSING_QUEUE_2 = "{mq}_{wrh}_testProcessingQueue2";
    public static final String TEST_PROCESSING_QUEUE_3 = "{mq}_{wrh}_testProcessingQueue3";

    public static final String TEST_TARGET_QUEUE = "rmq_0_testProcessingQueue1";

    public static final String TEST_REQUEST_ID_1 = "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1";
    public static final String TEST_REQUEST_ID_2 = "1111111111111/b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2/1/2";
    public static final String TEST_REQUEST_ID_3 = "2222222222222/c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3/1/2/3";
    public static final String TEST_REQUEST_ID_4 = "3333333333333/d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4/1/2/3/4";
    public static final String TEST_REQUEST_ID_5 = "4444444444444/e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5/1/2/3/4/5";
    public static final String TEST_REQUEST_ID_6 = "5555555555555/f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6/1/2/3/4/5/6";
    public static final String TEST_REQUEST_ID_7 = "6666666666666/g7g7g7g7g7g7g7g7g7g7g7g7g7g7g7g7/1/2/3/4/5/6/7";
    public static final String TEST_REQUEST_ID_8 = "7777777777777/h8h8h8h8h8h8h8h8h8h8h8h8h8h8h8h8/1/2/3/4/5/6/7/8";

    private final ProcessingQueueHelper processingQueueHelper = mock(ProcessingQueueHelper.class);
    private final DeadQueueHelper deadQueueHelper = mock(DeadQueueHelper.class);
    private final MessageReanimationService messageReanimationService = new CommonMessageReanimationService(
        processingQueueHelper,
        deadQueueHelper,
            MQ_PROVIDER,
            WAREHOUSE_ID
    );

    @AfterEach
    void tearDown() {
        Mockito.reset(processingQueueHelper);
        Mockito.reset(deadQueueHelper);
    }

    @Test
    void reanimateMessagesDoesntReanimateMessagesWhenDeadQueueIsEmpty() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        when(deadQueueHelper.receiveMessage(deadQueueMessageTtlInSec)).thenReturn(null);

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec,
                count, reanimationConsumer);

        assertEquals(0, result);

        verify(deadQueueHelper).receiveMessage(deadQueueMessageTtlInSec);
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
    }

    @Test
    void reanimateMessagesReanimatesAllMessagesWhenMQContainsOnlyMessagesWithRelevantTargetQueue() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };
        Message message = createMessage(TEST_PROCESSING_QUEUE_1, "0");

        when(deadQueueHelper.receiveMessage(deadQueueMessageTtlInSec))
                .thenReturn(message)
                .thenReturn(null);

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(1, result);

        verify(deadQueueHelper, times(2)).receiveMessage(deadQueueMessageTtlInSec);
        verify(deadQueueHelper, times(0)).returnMessage(eq(deadQueueMessageTtlInSec),
                any(Message.class));
        verify(processingQueueHelper).returnMessage(any(), any());
        verify(processingQueueHelper).returnMessage(TEST_TARGET_QUEUE, message);
    }

    @Test
    void reanimateMessagesReanimatesOnlyRelevantMessagesWhenDeadQueueContainsMessagesWithDifferentTargetQueues()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_5),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_6),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_7),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_8)
        );

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(4, result);

        verify(deadQueueHelper, times(9)).receiveMessage(deadQueueMessageTtlInSec);
        verify(deadQueueHelper, times(5)).returnMessage(any(Long.class), any());

        // Проверка, что сообщения, которые НЕ соответствуют targetQueue, возвращаются в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(5)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(5, returnedToDeadQueueMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_4, getRequestId(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(2)));
        assertEquals(TEST_REQUEST_ID_5, getRequestId(returnedToDeadQueueMessages.get(2)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(returnedToDeadQueueMessages.get(3)));
        assertEquals(TEST_REQUEST_ID_8, getRequestId(returnedToDeadQueueMessages.get(3)));
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(4)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(4)));

        // Проверка, что сообщения, соответствующие targetQueue, возвращаются в processing-очередь.
        ArgumentCaptor<String> targetQueueArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> reanimatedMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(processingQueueHelper, times(4)).returnMessage(
            targetQueueArgumentCaptor.capture(),
            reanimatedMessageArgumentCaptor.capture()
        );

        List<String> targetQueues = targetQueueArgumentCaptor.getAllValues();
        assertEquals(4, targetQueues.size());
        targetQueues.forEach(targetQueue -> assertEquals(TEST_TARGET_QUEUE, targetQueue));

        List<Message> reanimatedMessages = reanimatedMessageArgumentCaptor.getAllValues();
        assertEquals(4, reanimatedMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(reanimatedMessages.get(0)));
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(reanimatedMessages.get(1)));
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(2)));
        assertEquals(TEST_REQUEST_ID_6, getRequestId(reanimatedMessages.get(2)));
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(3)));
        assertEquals(TEST_REQUEST_ID_7, getRequestId(reanimatedMessages.get(3)));
    }

    @Test
    void reanimateMessagesDoesntReanimateMessagesWhenDeadQueueContainsOnlyMessagesWithNotRelevantTargetQueue()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4)
        );

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(0, result);

        verify(deadQueueHelper, times(5)).receiveMessage(deadQueueMessageTtlInSec);

        // Проверка, что сообщения, которые НЕ соответствуют targetQueue, возвращаются в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(5)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(5, returnedToDeadQueueMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(2)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(returnedToDeadQueueMessages.get(2)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(returnedToDeadQueueMessages.get(3)));
        assertEquals(TEST_REQUEST_ID_4, getRequestId(returnedToDeadQueueMessages.get(3)));
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(4)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(4)));

        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
    }

    @Test
    void reanimateMessagesReanimatesAllMessagesWhenTargetQueueIsNullAndDeadQueueIsNotEmpty() throws JMSException {
        final String targetQueue = null;
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4)
        );

        int result = messageReanimationService.reanimateMessages(targetQueue, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(4, result);

        verify(deadQueueHelper, times(5)).receiveMessage(deadQueueMessageTtlInSec);
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());

        // Проверка, что сообщения, соответствующие targetQueue, возвращаются в processing-очередь.
        ArgumentCaptor<Message> reanimatedMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(processingQueueHelper, times(4)).returnMessage(
            any(),
            reanimatedMessageArgumentCaptor.capture()
        );
        List<Message> reanimatedMessages = reanimatedMessageArgumentCaptor.getAllValues();
        assertEquals(4, reanimatedMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(reanimatedMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(reanimatedMessages.get(0)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(reanimatedMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(reanimatedMessages.get(1)));
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(reanimatedMessages.get(2)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(reanimatedMessages.get(2)));
        assertEquals(TEST_PROCESSING_QUEUE_3, getRetryingQueue(reanimatedMessages.get(3)));
        assertEquals(TEST_REQUEST_ID_4, getRequestId(reanimatedMessages.get(3)));
    }

    @Test
    void reanimateMessagesReanimatesMaximumNeededCountOfMessagesWhenAmountOfMessagesInDeadQueueIsGreaterThanCountParam()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 2;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_5)
        );

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(2, result);

        verify(deadQueueHelper, times(3)).receiveMessage(deadQueueMessageTtlInSec);
        verify(deadQueueHelper).returnMessage(any(Long.class), any());

        // Проверка, что сообщения, которые НЕ соответствуют targetQueue, возвращаются в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(1, returnedToDeadQueueMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_2, getRetryingQueue(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(0)));

        // Проверка, что сообщения, соответствующие targetQueue, отправляются в processing-очередь.
        ArgumentCaptor<Message> reanimatedMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(processingQueueHelper, times(2)).returnMessage(any(),
                reanimatedMessageArgumentCaptor.capture());
        List<Message> reanimatedMessages = reanimatedMessageArgumentCaptor.getAllValues();
        assertEquals(2, reanimatedMessages.size());
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(reanimatedMessages.get(0)));
        assertEquals(TEST_PROCESSING_QUEUE_1, getRetryingQueue(reanimatedMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(reanimatedMessages.get(1)));
    }

    @Test
    void reanimateMessagesThrowsExceptionWhenReceivingMessageFromDeadQueueThrowsException() {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        when(deadQueueHelper.receiveMessage(any(Long.class))).thenThrow(RuntimeException.class);

        assertThrows(
            CommonMessageReanimationService.MessageReanimationException.class,
            () -> messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                    reanimationConsumer)
        );
    }

    @Test
    void reanimateMessagesThrowsExceptionWhenReturningMessageToDeadQueueThrowsJMSException() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_1)
        );

        doThrow(JMSException.class).when(deadQueueHelper).returnMessage(any(Long.class), any());

        assertThrows(
            CommonMessageReanimationService.MessageReanimationException.class,
            () -> messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                    reanimationConsumer)
        );
    }

    @Test
    void reanimateMessagesThrowsExceptionWhenReanimationConsumerThrowsException() {
        int deadQueueMessageTtlInSec = 0;
        int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> {
            throw new RuntimeException();
        };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1)
        );

        assertThrows(
            CommonMessageReanimationService.MessageReanimationException.class,
            () -> messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                    reanimationConsumer)
        );
    }

    @Test
    void reanimateMessagesSkipMessageWhenRetryingQueueHeaderIsNotExistsInMessage() throws JMSException {
        final int deadQueueMessageTtlInSec = 0;
        final int count = Integer.MAX_VALUE;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            fillCreationTimestampAndRequestId(mock(Message.class), TEST_REQUEST_ID_1)
        );

        int result = messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        assertEquals(0, result);

        // Проверка, что сообщение возвращается в dead-очередь.
        // Вызывается два раза, так как сообщение вновь считывается из очереди после возврата
        // и уже не проходит условие по времени создания.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(2)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(2, returnedToDeadQueueMessages.size());
        assertNull(getRetryingQueue(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertNull(getRetryingQueue(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(1)));

        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
    }

    @Test
    void reanimateMessagesDoesntDeduplicateMessageWhenIdempotencyKeyIsNotSet() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 2;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1)
        );

        messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        verify(processingQueueHelper, times(2)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void reanimateMessagesDoesntDeduplicateMessageWithDifferentIdempotencyKey() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 2;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        Message message1 = fillIdempotencyKey(
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            TEST_REQUEST_ID_1
        );

        Message message2 = fillIdempotencyKey(
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_2),
            TEST_REQUEST_ID_2
        );

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            message1,
            message2
        );

        messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        verify(processingQueueHelper, times(2)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void reanimateMessagesDeduplicatesMessageWhenThereIsSeveralMessagesWithTheSameIdempotencyKey() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 2;
        MessageReanimationService.ReanimationConsumer reanimationConsumer = m -> { };

        Message message = fillIdempotencyKey(
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            TEST_REQUEST_ID_1
        );

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            message,
            message
        );

        messageReanimationService.reanimateMessages(TEST_TARGET_QUEUE, deadQueueMessageTtlInSec, count,
                reanimationConsumer);

        verify(processingQueueHelper, times(1)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void deleteMessageDeletesOnlyRelevantMessage() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        String requestIdBase = "6666/7777";

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_1, String.format("%s/4", requestIdBase)),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_5)
        );

        boolean result = messageReanimationService.deleteMessage(deadQueueMessageTtlInSec, requestIdBase);

        assertTrue(result);

        verify(deadQueueHelper, times(4)).receiveMessage(any(Long.class));

        verify(processingQueueHelper, times(0)).returnMessage(any(), any());

        // Проверка, что сообщения с несоответствующими id запроса возвращается в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(3)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(3, returnedToDeadQueueMessages.size());
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(returnedToDeadQueueMessages.get(2)));
    }

    @Test
    void deleteMessageDoesntDeleteAnyMessageWhenRelativeMessageIsNotExisted() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        String requestIdBase = "9999/9999";

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_5)
        );

        boolean result = messageReanimationService.deleteMessage(deadQueueMessageTtlInSec, requestIdBase);

        assertFalse(result);

        verify(deadQueueHelper, times(6)).receiveMessage(any(Long.class));

        verify(processingQueueHelper, times(0)).returnMessage(any(), any());

        // Проверка, что сообщения с несоответствующими id запроса возвращается в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(6)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(6, returnedToDeadQueueMessages.size());
        assertEquals(TEST_REQUEST_ID_1, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_3, getRequestId(returnedToDeadQueueMessages.get(2)));
        assertEquals(TEST_REQUEST_ID_4, getRequestId(returnedToDeadQueueMessages.get(3)));
        assertEquals(TEST_REQUEST_ID_5, getRequestId(returnedToDeadQueueMessages.get(4)));
    }

    @Test
    void deleteMessagesDoesntDeleteAnyMessageWhenDeadQueueIsEmpty() throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        new TestDeadQueue(deadQueueMessageTtlInSec);

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec);

        assertEquals(0, result);

        verify(deadQueueHelper).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void deleteMessagesDeletesAllMessagesWhenDeadQueueIsNotEmptyAndTargetQueueIsNull() throws JMSException {
        int deadQueueMessageTtlInSec = 0;

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_5)
        );

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec);

        assertEquals(5, result);

        verify(deadQueueHelper, times(6)).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void deleteMessagesDeletesOnlyRelevantMessagesWhenDeadQueueIsNotEmptyAndTargetQueueIsNotNull()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_5)
        );

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec, TEST_TARGET_QUEUE);

        assertEquals(3, result);

        verify(deadQueueHelper, times(6)).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());

        // Проверка, что сообщения с несоответствующими id запроса возвращается в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(3)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(3, returnedToDeadQueueMessages.size());
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_4, getRequestId(returnedToDeadQueueMessages.get(1)));
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(2)));
    }

    @Test
    void deleteMessagesDoesntDeleteAnyMessageWhenDeadQueueIsNotEmptyAndMessagesWithTargetQueueIsNotPresent()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_5)
        );

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec, TEST_TARGET_QUEUE);

        assertEquals(0, result);

        verify(deadQueueHelper, times(6)).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
        verify(deadQueueHelper, times(6)).returnMessage(any(Long.class), any());
    }

    @Test
    void deleteMessagesDeletesMaximumNeededCountOfMessagesWhenAmountOfMessagesInDeadQueueIsGreaterThanCountParam()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 4;

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_5)
        );

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec, count);

        assertEquals(4, result);

        verify(deadQueueHelper, times(4)).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());
        verify(deadQueueHelper, times(0)).returnMessage(any(Long.class), any());
    }

    @Test
    void deleteMessagesDeletesMaximumNeededCountOfRelevantMessagesWhenAmountOfMessagesInDeadMQIsGreaterThanCountParam()
            throws JMSException {
        int deadQueueMessageTtlInSec = 0;
        int count = 4;

        new TestDeadQueue(
            deadQueueMessageTtlInSec,
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_1),
            createMessage(TEST_PROCESSING_QUEUE_2, TEST_REQUEST_ID_2),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_3),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_4),
            createMessage(TEST_PROCESSING_QUEUE_3, TEST_REQUEST_ID_5),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_6),
            createMessage(TEST_PROCESSING_QUEUE_1, TEST_REQUEST_ID_7)
        );

        int result = messageReanimationService.deleteMessages(deadQueueMessageTtlInSec, TEST_TARGET_QUEUE, count);

        assertEquals(4, result);

        verify(deadQueueHelper, times(6)).receiveMessage(any(Long.class));
        verify(processingQueueHelper, times(0)).returnMessage(any(), any());

        // Проверка, что сообщения с несоответствующими id запроса возвращается в dead-очередь.
        ArgumentCaptor<Message> returnedToDeadQueueMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(deadQueueHelper, times(2)).returnMessage(any(Long.class),
                returnedToDeadQueueMessageArgumentCaptor.capture());
        List<Message> returnedToDeadQueueMessages = returnedToDeadQueueMessageArgumentCaptor.getAllValues();
        assertEquals(2, returnedToDeadQueueMessages.size());
        assertEquals(TEST_REQUEST_ID_2, getRequestId(returnedToDeadQueueMessages.get(0)));
        assertEquals(TEST_REQUEST_ID_5, getRequestId(returnedToDeadQueueMessages.get(1)));
    }

    @SneakyThrows(JMSException.class)
    private static Message cloneMessage(Message message) {
        String retryingQueue = getRetryingQueue(message);
        String requestId = getRequestId(message);
        return createMessage(retryingQueue, requestId);
    }

    @SneakyThrows(JMSException.class)
    private static Message createMessage(String retryingQueue, String requestId) {
        Message message = fillCreationTimestampAndRequestId(mock(Message.class), requestId);

        when(message.getStringProperty(X_RETRYING_QUEUE)).thenReturn(retryingQueue);
        when(message.propertyExists(X_RETRYING_QUEUE)).thenReturn(true);

        return message;
    }

    @SneakyThrows(JMSException.class)
    private static Message fillCreationTimestampAndRequestId(Message message, String requestId) {
        when(message.getLongProperty(X_MESSAGE_CREATION_TIMESTAMP)).thenReturn(System.currentTimeMillis());
        when(message.propertyExists(X_MESSAGE_CREATION_TIMESTAMP)).thenReturn(true);

        when(message.getStringProperty(REQUEST_ID_PROPERTY)).thenReturn(requestId);
        when(message.propertyExists(REQUEST_ID_PROPERTY)).thenReturn(true);

        return message;
    }

    private static Message fillIdempotencyKey(Message message, String requestId) throws JMSException {
        String idempotencyKey = MessageUtils.createIdempotencyKey(message);
        when(message.getStringProperty(X_IDEMPOTENCY_KEY)).thenReturn(idempotencyKey);
        when(message.propertyExists(X_IDEMPOTENCY_KEY)).thenReturn(true);
        return message;
    }

    private class TestDeadQueue {
        private final Queue<Message> mq;
        private final long deadQueueMessageTtlInSec;

        private TestDeadQueue(long deadQueueMessageTtlInSec, Message... messages) {
            this(deadQueueMessageTtlInSec, List.of(messages));
        }

        private TestDeadQueue(long deadQueueMessageTtlInSec, List<Message> messages) {
            this.deadQueueMessageTtlInSec = deadQueueMessageTtlInSec;
            mq = new LinkedList<>(messages);
            emulateWorking();
        }

        private TestDeadQueue(long deadQueueMessageTtlInSec) {
            this.deadQueueMessageTtlInSec = deadQueueMessageTtlInSec;
            mq = new LinkedList<>();
            emulateWorking();
        }

        private TestDeadQueue() {
            this.deadQueueMessageTtlInSec = 0;
            mq = new LinkedList<>();
            emulateWorking();
        }

        @SneakyThrows(JMSException.class)
        private void emulateWorking() {
            when(deadQueueHelper.receiveMessage(deadQueueMessageTtlInSec)).then(invocation -> mq.poll());

            doAnswer(invocation -> {
                Message message = invocation.getArgument(1, Message.class);
                mq.offer(cloneMessage(message));
                return null;
            }).when(deadQueueHelper).returnMessage(any(Long.class), any());
        }

    }
}
