package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PaymentFormTypeApiTest {

    @Test
    public void testToCore() {
        for (PaymentFormTypeApi type : PaymentFormTypeApi.values()) {
            assertNotNull(type.getTrustFormTemplate());
        }
    }
}
