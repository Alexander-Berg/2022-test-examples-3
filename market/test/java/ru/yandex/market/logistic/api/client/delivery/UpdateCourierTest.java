package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.utils.common.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateCourierTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void updateCourierSuccess() throws Exception {
        prepareMockServiceNormalized("ds_update_courier", PARTNER_URL);

        deliveryServiceClient.updateCourier(
            ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId(),
            DtoFactory.createCourier("Инбаундов"),
            DtoFactory.createCourier("Аутбаундов"),
            DtoFactory.createOrderTransferCodes(),
            getPartnerProperties()
        );
    }

    @Test
    void updateCourierWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_update_courier",
            "ds_update_courier_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateCourier(
                ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId(),
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
            () -> deliveryServiceClient.updateCourier(
                ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId(),
                null,
                null,
                DtoFactory.createOrderTransferCodes(),
                getPartnerProperties()
            )
        );
    }
}
