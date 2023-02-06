package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;

import static org.junit.Assert.assertNotNull;

public class UserProductTypeApiTest {

    @Test
    public void testFromCore() {
        for (BillingType v : BillingType.values()) {
            assertNotNull(UserProductTypeApi.fromCoreEnum(v));
        }
    }
}
