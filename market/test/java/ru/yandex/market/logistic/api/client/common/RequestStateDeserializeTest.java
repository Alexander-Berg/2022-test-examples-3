package ru.yandex.market.logistic.api.client.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestStateDeserializeTest extends CommonServiceClientTest {

    @Autowired
    DeliveryServiceClient deliveryServiceClient;

    @Test
    void testRequestStateEmptyCollapsedTagSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_order",
            "request_state/empty_tag_collapsed",
            "https://localhost/query-gateway");
        deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties());
    }

    @Test
    void testRequestStateEmptyExpandedTagSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_order",
            "request_state/empty_tag_expanded",
            "https://localhost/query-gateway");
        deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties());
    }

    @Test
    void testRequestStateEmptyTagWithWhitespacesSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_order",
            "request_state/empty_tag_whitespace",
            "https://localhost/query-gateway");
        deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties());
    }

    @Test
    void testWithoutRequestStateFailed() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_order",
            "request_state/without_tag",
            "https://localhost/query-gateway");

        assertThrows(
            ResponseValidationException.class,
            () -> deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties())
        );
    }
}
