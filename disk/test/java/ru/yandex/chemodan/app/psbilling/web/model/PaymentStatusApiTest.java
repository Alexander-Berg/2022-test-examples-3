package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;

import static org.junit.Assert.assertNotNull;

public class PaymentStatusApiTest {

    @Test
    public void testToCore() {
        for (PaymentStatusApi type : PaymentStatusApi.values()) {
            assertNotNull(type.toCoreEnum());
        }
    }

    @Test
    public void testFromCore() {
        for (UserServiceBillingStatus v : UserServiceBillingStatus.values()) {
            assertNotNull(PaymentStatusApi.fromCoreEnum(v));
        }
    }
}
