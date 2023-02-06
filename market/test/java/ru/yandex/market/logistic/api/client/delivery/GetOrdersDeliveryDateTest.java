package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrdersDeliveryDateTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String DELIVERY_ID = "45";
    private static final String YANDEX_ID2 = "26";
    private static final String DELIVERY_ID2 = "46";

    @Test
    void testGetOrdersDeliveryDateSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_delivery_date", PARTNER_URL);

        GetOrdersDeliveryDateResponse response = deliveryServiceClient.getOrdersDeliveryDate(
            Arrays.asList(
                new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
                new ResourceId.ResourceIdBuilder().setYandexId((YANDEX_ID2)).setDeliveryId(DELIVERY_ID2).build()
            ), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }

    @Test
    void testGetOrdersDeliveryDateWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_delivery_date",
            "ds_get_delivery_date_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getOrdersDeliveryDate(
                Arrays.asList(
                    new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
                    new ResourceId.ResourceIdBuilder().setYandexId((YANDEX_ID2)).setDeliveryId(DELIVERY_ID2).build()
                ), getPartnerProperties())
        );
    }

    @Test
    public void testGetOrdersDeliveryDateWihEmptyInterval() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_delivery_date",
            "ds_get_orders_delivery_date_empty_intervals",
            PARTNER_URL);

        GetOrdersDeliveryDateResponse response = deliveryServiceClient.getOrdersDeliveryDate(
            Arrays.asList(
                new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
                new ResourceId.ResourceIdBuilder().setYandexId((YANDEX_ID2)).setDeliveryId(DELIVERY_ID2).build()
            ), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponseWithEmptyIntervals());
    }

    @Test
    void testGetOrdersDeliveryDateWithEmptyPartnerIdResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_orders_delivery_date",
            "ds_get_orders_delivery_date_empty_partner_id",
            PARTNER_URL);

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.getOrdersDeliveryDate(
                Arrays.asList(
                    new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
                    new ResourceId.ResourceIdBuilder().setYandexId((YANDEX_ID2)).setDeliveryId(DELIVERY_ID2).build()
                ), getPartnerProperties())
        );
    }

    @Test
    void testGetOrdersDeliveryDateWithEmptyPartnerIdRequest() {

        assertThrows(
            RequestValidationException.class,
            () -> deliveryServiceClient.getOrdersDeliveryDate(
                Arrays.asList(
                    new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).build(),
                    new ResourceId.ResourceIdBuilder().setYandexId((YANDEX_ID2)).build()
                ), getPartnerProperties())
        );
    }

    private GetOrdersDeliveryDateResponse getExpectedResponse() throws Exception {
        return new GetOrdersDeliveryDateResponse(Arrays.asList(
            getObjectFromXml("fixture/response/entities/ds_get_orders_delivery_date_25.xml",
                OrderDeliveryDate.class),
            getObjectFromXml("fixture/response/entities/ds_get_orders_delivery_date_26.xml",
                OrderDeliveryDate.class)
        ));
    }

    private GetOrdersDeliveryDateResponse getExpectedResponseWithEmptyIntervals() throws Exception {
        return new GetOrdersDeliveryDateResponse(Arrays.asList(
            getObjectFromXml("fixture/response/entities/ds_get_orders_delivery_date_25_empty_interval.xml",
                OrderDeliveryDate.class),
            getObjectFromXml("fixture/response/entities/ds_get_orders_delivery_date_26_empty_interval.xml",
                OrderDeliveryDate.class)
        ));
    }
}
