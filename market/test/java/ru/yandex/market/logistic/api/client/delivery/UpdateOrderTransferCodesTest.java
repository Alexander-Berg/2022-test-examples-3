package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateOrderTransferCodesTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void updateOrderTransferCodesSuccess() throws Exception {
        prepareMockServiceNormalized("ds_update_order_transfer_codes", PARTNER_URL);

        deliveryServiceClient.updateOrderTransferCodes(
            DtoFactory.createOrderId(),
            ru.yandex.market.logistic.api.utils.common.DtoFactory.createOrderTransferCodes(),
            getPartnerProperties()
        );
    }

    @Test
    void updateOrderTransferCodesWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ds_update_order_transfer_codes",
            "ds_update_order_transfer_codes_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateOrderTransferCodes(
                DtoFactory.createOrderId(),
                ru.yandex.market.logistic.api.utils.common.DtoFactory.createOrderTransferCodes(),
                getPartnerProperties()
            )
        );
    }
}
