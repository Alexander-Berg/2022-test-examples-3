package ru.yandex.market.logistic.api.client.delivery;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Warehouse;
import ru.yandex.market.logistic.api.model.delivery.response.GetReferenceWarehousesResponse;
import ru.yandex.market.logistic.api.model.validation.LogisticApiResponseFilter;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.ValidationUtil.validate;
import static ru.yandex.market.logistic.api.utils.ValidationUtil.validateOrDie;

class GetReferenceWarehousesTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized("ds_get_reference_warehouses", PARTNER_URL);

        GetReferenceWarehousesResponse actualResponse =
            deliveryServiceClient.getReferenceWarehouses(createFilter(), getPartnerProperties());

        GetReferenceWarehousesResponse expectedResponse = createGetReferenceWarehousesResponse();
        assertEquals(
            expectedResponse,
            actualResponse,
            "Должен вернуть корректный ответ GetReferenceWarehousesResponse"
        );
    }

    @Test
    void testEmptyResponse() throws Exception {
        prepareMockServiceNormalized("ds_get_reference_warehouses_empty", PARTNER_URL);

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.getReferenceWarehouses(createFilter(), getPartnerProperties())
        );

    }

    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_warehouses",
            "ds_get_reference_warehouses_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getReferenceWarehouses(createFilter(), getPartnerProperties())
        );
    }

    @Test
    void testResponseWithBadPoints() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_reference_warehouses",
            "ds_get_reference_warehouses_with_bad_warehouse",
            PARTNER_URL
        );

        GetReferenceWarehousesResponse actualResponse = deliveryServiceClient.getReferenceWarehouses(
            createFilter(),
            getPartnerProperties()
        );

        GetReferenceWarehousesResponse expectedResponse = createGetReferenceWarehousesResponse();
        assertEquals(
            expectedResponse,
            actualResponse,
            "Должен вернуть ответ GetReferencePickupPointsResponse c одной из двух полученных точек"
        );
    }

    private GetReferenceWarehousesResponse createGetReferenceWarehousesResponse() {
        return new GetReferenceWarehousesResponse(Collections.singletonList(DtoFactory.createWarehouse()));
    }

    @Nonnull
    private LogisticApiResponseFilter<GetReferenceWarehousesResponse> createFilter() {
        return instance -> {
            validateOrDie(instance, true);
            List<Warehouse> warehouses = instance.getWarehouses();
            return new GetReferenceWarehousesResponse(
                warehouses.stream().filter(warehouse -> validate(warehouse).isEmpty()).collect(Collectors.toList()));
        };
    }
}
