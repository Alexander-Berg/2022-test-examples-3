package ru.yandex.market.logistic.api.client.delivery;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PutReferenceWarehousesTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_put_reference_warehouses",
            "ds_put_reference_warehouses",
            PARTNER_URL
        );

        deliveryServiceClient.putReferenceWarehouses(
            getPartnerProperties(),
            Collections.singletonList(DtoFactory.createWarehouse()),
            true
        );
    }

    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_put_reference_warehouses",
            "ds_put_reference_warehouses_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.putReferenceWarehouses(
                getPartnerProperties(),
                Collections.singletonList(DtoFactory.createWarehouse()),
                true
            )
        );
    }
}
