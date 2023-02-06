package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;

import static org.junit.Assert.assertNotNull;

public class OrderStatusApiTest {

    @Test
    public void testToCore() {
        for (OrderStatusApi type : OrderStatusApi.values()) {
            assertNotNull(type.toCoreEnum());
        }
    }

    @Test
    public void testFromCore() {
        for (OrderStatus v : OrderStatus.values()) {
            assertNotNull(OrderStatusApi.fromCoreEnum(v));
        }
    }
}
