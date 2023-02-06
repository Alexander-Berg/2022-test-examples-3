package ru.yandex.market.wms.shared.libs.async.retry.impl;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.shared.libs.async.jms.ServicebusMessageConverter.REQUEST_ID_PROPERTY;
import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.X_IDEMPOTENCY_KEY;

class MessageDeduplicatorImplTest {
    public static final String TEST_REQUEST_ID_1 = "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1";
    public static final String TEST_REQUEST_ID_2 = "1111111111111/b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2/1/2";
    public static final String TEST_REQUEST_ID_3 = "2222222222222/c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3/1/2/3";
    public static final String TEST_REQUEST_ID_4 = "3333333333333/d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4/1/2/3/4";
    public static final String TEST_REQUEST_ID_5 = "4444444444444/e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5/1/2/3/4/5";
    public static final String TEST_REQUEST_ID_6 = "5555555555555/f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6/1/2/3/4/5/6";

    @Test
    void isDuplicateClearsTheIdempotencyRepositoryWhenItsSizeReachesTheLimit() throws JMSException {
        int idempotencyRepositoryMaxSize = 5;
        MessageDeduplicatorImpl messageDeduplicator = new MessageDeduplicatorImpl(idempotencyRepositoryMaxSize);
        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_1));
        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_2));
        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_3));
        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_4));
        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_5));
        Assertions.assertEquals(5, messageDeduplicator.getCurrentSize());

        messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_6));
        Assertions.assertEquals(1, messageDeduplicator.getCurrentSize());
    }

    @Test
    void isDuplicateDoesntAddIdempotencyKeyToRepositoryWhenItsNull() throws JMSException {
        int idempotencyRepositoryMaxSize = 5;
        MessageDeduplicatorImpl messageDeduplicator = new MessageDeduplicatorImpl(idempotencyRepositoryMaxSize);
        messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_1));
        messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_2));
        messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_3));
        messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_4));
        messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_5));
        Assertions.assertEquals(0, messageDeduplicator.getCurrentSize());
    }

    @Test
    void isDuplicateReturnsFalseWhenIdempotencyKeyIsNull() throws JMSException {
        int idempotencyRepositoryMaxSize = 5;
        MessageDeduplicatorImpl messageDeduplicator = new MessageDeduplicatorImpl(idempotencyRepositoryMaxSize);
        Assertions.assertFalse(messageDeduplicator.isDuplicate(createMessageWithoutIdempotencyKey(TEST_REQUEST_ID_1)));
    }

    @Test
    void isDuplicateReturnsTrueWhenIdempotencyKeyExistsInIdempotencyRepository() throws JMSException {
        int idempotencyRepositoryMaxSize = 5;
        MessageDeduplicatorImpl messageDeduplicator = new MessageDeduplicatorImpl(idempotencyRepositoryMaxSize);
        Message message = createMessageWithIdempotencyKey(TEST_REQUEST_ID_1);
        Assertions.assertFalse(messageDeduplicator.isDuplicate(message));
        Assertions.assertTrue(messageDeduplicator.isDuplicate(message));
        Assertions.assertEquals(1, messageDeduplicator.getCurrentSize());
    }

    @Test
    void isDuplicateReturnsFalseWhenIdempotencyKeyDoesntExistInIdempotencyRepository() throws JMSException {
        int idempotencyRepositoryMaxSize = 5;
        MessageDeduplicatorImpl messageDeduplicator = new MessageDeduplicatorImpl(idempotencyRepositoryMaxSize);
        Assertions.assertFalse(messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_1)));
        Assertions.assertFalse(messageDeduplicator.isDuplicate(createMessageWithIdempotencyKey(TEST_REQUEST_ID_2)));
        Assertions.assertEquals(2, messageDeduplicator.getCurrentSize());
    }

    private static Message createMessageWithoutIdempotencyKey(String requestId) throws JMSException {
        Message message = mock(Message.class);
        when(message.getStringProperty(REQUEST_ID_PROPERTY)).thenReturn(requestId);
        when(message.propertyExists(REQUEST_ID_PROPERTY)).thenReturn(true);
        return message;
    }

    private static Message createMessageWithIdempotencyKey(String requestId) throws JMSException {
        Message message = createMessageWithoutIdempotencyKey(requestId);
        String idempotencyKey = MessageUtils.createIdempotencyKey(message);
        when(message.getStringProperty(X_IDEMPOTENCY_KEY)).thenReturn(idempotencyKey);
        when(message.propertyExists(X_IDEMPOTENCY_KEY)).thenReturn(true);
        return message;
    }
}
