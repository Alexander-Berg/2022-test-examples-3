package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateOrderShipmentTest extends CommonServiceClientTest {
    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void updateOrderShipmentSuccess() throws Exception {
        prepareMockServiceNormalized("ff_update_order_shipment", PARTNER_URL);

        fulfillmentClient.updateOrderShipment(
            ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
            new DateTime("2012-12-21T11:59:00"),
            new DateTime("2012-12-21T13:59:00"),
            getPartnerProperties()
        );
    }

    @Test
    void updateOrderShipmentWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_order_shipment",
            "ff_update_order_shipment_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.updateOrderShipment(
                ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
                new DateTime("2012-12-21T11:59:00"),
                new DateTime("2012-12-21T13:59:00"),
                getPartnerProperties()
            )
        );
    }

    @Test
    void updateOrderShipmentValidationError() {
        assertThrows(
            RequestValidationException.class,
            () -> fulfillmentClient.updateOrderShipment(
                ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
                null,
                null,
                getPartnerProperties()
            )
        );
    }
}
