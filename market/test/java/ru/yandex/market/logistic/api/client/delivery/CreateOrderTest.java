package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.Order;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateOrderTest extends CommonServiceClientTest {

    @Autowired
    DeliveryServiceClient deliveryServiceClient;

    @Test
    void testCreateOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_order", PARTNER_URL);
        deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties());
    }

    @Test
    void testCreateOrderWithRestrictedDataSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_order_with_restricted_data", PARTNER_URL);
        deliveryServiceClient.createOrder(
            DtoFactory.createOrder(),
            DtoFactory.createOrderRestrictedData(),
            getPartnerProperties()
        );
    }

    @Test
    void testCreateOrderWithPersonalDataSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_order_with_personal_data", INTERNAL_PARTNER_URL);
        deliveryServiceClient.createOrder(
            DtoFactory.createOrderWithPersonalData(),
            getInternalPartnerProperties()
        );
    }

    @Test
    void testCreateOrderTrimmedSucceeded() throws Exception {
        prepareMockService("create_order_normalized", PARTNER_URL);
        deliveryServiceClient.createOrder(DtoFactory.createOrderNotTrimmed(), getPartnerProperties());
    }

    @Test
    void testCreateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_create_order_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties())
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        Location address = DtoFactory.createLocationFrom(null);
        Sender sender = DtoFactory.createSender("ИП «Тестовый виртуальный магазин проекта Фулфиллмент»", address, null);
        Order order = DtoFactory.createOrder(sender, null, null, null);

        assertThrows(ValidationException.class, () -> deliveryServiceClient.createOrder(order, getPartnerProperties()));
    }

    @Test
    void testCreateOrderAnyResourceIdValidationFailed() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_order_empty_delivery_id_and_partner_id",
            PARTNER_URL
        );
        assertThrows(
            ResponseValidationException.class,
            () -> deliveryServiceClient.createOrder(DtoFactory.createOrder(), getPartnerProperties())
        );
    }
}
