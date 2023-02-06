package ru.yandex.market.tpl.integration.tests.configuration;

import lombok.Data;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;

@Data
public class TestConfiguration {
    private String orderRecipientNotes;
    private Long rescheduleDays;
    private OrderPaymentType paymentType;
    private OrderPaymentStatus paymentStatus;
    private boolean recipientCallEnabled;
    private boolean deliveryPhotoEnabled;

    public static TestConfiguration getDefaultConfiguration() {
        TestConfiguration testConfiguration = new TestConfiguration();
        testConfiguration.setPaymentType(OrderPaymentType.PREPAID);
        testConfiguration.setPaymentStatus(OrderPaymentStatus.PAID);
        testConfiguration.setRecipientCallEnabled(true);
        testConfiguration.setDeliveryPhotoEnabled(true);
        return testConfiguration;
    }
}
