package ru.yandex.market.wms.shared.libs.async.retry.impl;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.shared.libs.async.jms.ServicebusMessageConverter.REQUEST_ID_PROPERTY;

class MessageUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/2/3/4/5/6/7",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/2/3/4/5/6/7/",
    })
    void getRequestIdBaseReturnsValidValue(String requestId) {
        String requestIdBase = MessageUtils.getRequestIdBase(requestId);
        Assertions.assertEquals("0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1", requestIdBase);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/2/3/4/5/6/7",
        "0000000000000/a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1/1/2/3/4/5/6/7/",
    })
    void createIdempotencyKeyReturnsValidValue(String requestId) throws JMSException {
        Message message = mock(Message.class);
        when(message.getStringProperty(REQUEST_ID_PROPERTY)).thenReturn(requestId);
        when(message.propertyExists(REQUEST_ID_PROPERTY)).thenReturn(true);
        String idempotencyKey = MessageUtils.createIdempotencyKey(message);
        String regex = String.format("%s-\\d{13}", requestId);
        Assertions.assertTrue(idempotencyKey.matches(regex));
    }
}
