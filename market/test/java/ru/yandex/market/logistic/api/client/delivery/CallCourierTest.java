package ru.yandex.market.logistic.api.client.delivery;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.common.DtoFactory.createResourceId;

class CallCourierTest extends CommonServiceClientTest {

    private static final Duration WAITING_TIME = Duration.ofSeconds(30);

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void callCourierSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_call_courier", PARTNER_URL);
        deliveryServiceClient.callCourier(createResourceId(123), WAITING_TIME, getPartnerProperties());
    }

    @Test
    void callCourierWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_call_courier",
            "ds_call_courier_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.callCourier(createResourceId(123), WAITING_TIME, getPartnerProperties())
        );
    }

    @Test
    void callCourierOrderIdValidationFailed() {
        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.callCourier(null, WAITING_TIME, getPartnerProperties())
        );
    }

    @Test
    void callCourierWaitingTimeValidationFailed() {
        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.callCourier(createResourceId(123), null, getPartnerProperties())
        );
    }
}
