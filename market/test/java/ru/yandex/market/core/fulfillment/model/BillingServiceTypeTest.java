package ru.yandex.market.core.fulfillment.model;

import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BillingServiceTypeTest {
    /**
     * Проверяем, что {@link BillingServiceType#name} равен соответствующему {@link DeliveryEventType#name}.
     */
    @Test
    public void testGetDeliveryEventType() {
        Arrays.stream(BillingServiceType.values())
                .forEach(t -> assertThat(t.getDeliveryEventType().name(), is(t.name())));
    }
}
