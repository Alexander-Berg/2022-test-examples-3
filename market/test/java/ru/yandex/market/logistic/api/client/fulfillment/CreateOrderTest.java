package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateOrderTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void testCreateOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_order", PARTNER_URL);

        CreateOrderResponse response = fulfillmentClient.createOrder(
            DtoFactory.createOrder(),
            getCreateOrderRestrictedData(),
            getPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response, "Asserting the response is correct");
    }

    @Test
    void testCreateOrderExtended() throws Exception {
        prepareMockServiceNormalized("ff_create_order_extended", PARTNER_URL);

        CreateOrderResponse response = fulfillmentClient.createOrder(
            DtoFactory.createOrder(
                PaymentType.PREPAID,
                DtoFactory.createLocationTo(),
                DtoFactory.createCourier(12341234L),
                DtoFactory.createRecipient(),
                null,
                null,
                null
            ),
            getCreateOrderRestrictedData(),
            getPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response, "Asserting the response is correct");
    }

    @Test
    void testCreateOrderWithPersonalData() throws Exception {
        prepareMockServiceNormalized("ff_create_order_with_personal_data", INTERNAL_PARTNER_URL);

        CreateOrderResponse response = fulfillmentClient.createOrder(
            DtoFactory.createOrderWithPersonalData(),
            getCreateOrderRestrictedData(),
            getInternalPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response, "Asserting the response is correct");
    }

    @Test
    void testCreateOrderTrimmedSucceeded() throws Exception {
        prepareMockService("ff_create_order_normalized", PARTNER_URL);

        CreateOrderResponse response = fulfillmentClient.createOrder(
            DtoFactory.createOrderNotTrimmed(),
            getCreateOrderRestrictedData(),
            getPartnerProperties()
        );
    }


    @Test
    void testCreateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_create_order_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createOrder(
                DtoFactory.createOrder(),
                getCreateOrderRestrictedData(),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        Order order = DtoFactory.createOrder(null);

        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.createOrder(order, getCreateOrderRestrictedData(), getPartnerProperties())
        );
    }

    private CreateOrderResponse getExpectedResponse() {
        return new CreateOrderResponse(
            new ResourceId.ResourceIdBuilder()
                .setYandexId("5927638")
                .setPartnerId("EXT101811250")
                .setFulfillmentId("EXT101811250")
                .build());
    }
}
