package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.utils.common.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateCourierTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Test
    void updateCourierSuccess() throws Exception {
        prepareMockServiceNormalized("ff_update_courier", PARTNER_URL);

        fulfillmentClient.updateCourier(
            ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
            DtoFactory.createCourier("Инбаундов"),
            DtoFactory.createCourier("Аутбаундов"),
            DtoFactory.createOrderTransferCodes(),
            getPartnerProperties()
        );
    }

    @Test
    void updateCourierWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_update_courier",
            "ff_update_courier_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.updateCourier(
                ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
                DtoFactory.createCourier("Инбаундов"),
                DtoFactory.createCourier("Аутбаундов"),
                DtoFactory.createOrderTransferCodes(),
                getPartnerProperties()
            )
        );
    }

    @Test
    void updateCourierValidationError() throws Exception {
        assertThrows(
            RequestValidationException.class,
            () -> fulfillmentClient.updateCourier(
                ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createOrderId(),
                null,
                null,
                DtoFactory.createOrderTransferCodes(),
                getPartnerProperties()
            )
        );
    }
}
