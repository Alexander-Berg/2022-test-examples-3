package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrdersStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String DELIVERY_ID = "45";

    @Test
    void testGetOrdersStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_status", PARTNER_URL);

        GetOrdersStatusResponse response = deliveryServiceClient.getOrdersStatus(
            Collections.singletonList(
                new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build()
            ),
            getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }

    @Test
    void testGetOrdersStatusSucceededWithMilliseconds() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_status_with_millis", PARTNER_URL);

        GetOrdersStatusResponse response = deliveryServiceClient.getOrdersStatus(
            Arrays.asList(
                new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
                new ResourceId.ResourceIdBuilder().setYandexId("26").setDeliveryId("46").build(),
                new ResourceId.ResourceIdBuilder().setYandexId("27").setDeliveryId("47").build()
            ),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(
                getObjectFromXml(
                    "fixture/response/entities/ds_order_status_history_with_millis.xml",
                    GetOrdersStatusResponse.class
                )
            );
    }


    @Test
    void testGetOrdersStatusSucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_status_with_empty_history", PARTNER_URL);

        GetOrdersStatusResponse response = deliveryServiceClient.getOrdersStatus(
            Collections.singletonList(
                new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build()
            ),
            getPartnerProperties());

        OrderStatusHistory expectedResponse = getObjectFromXml(
            "fixture/response/entities/ds_orders_status_with_empty_history.xml",
            OrderStatusHistory.class);
        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetOrdersStatusResponse(Collections.singletonList(
                expectedResponse
            )));
    }

    @Test
    void testGetOrdersStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_orders_status", "ds_get_orders_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getOrdersStatus(
                Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setDeliveryId(DELIVERY_ID)
                    .build()),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testGetOrdersStatusWithOverOneHistory() throws Exception {
        prepareMockServiceNormalized(
                "ds_get_orders_status",
                "ds_get_orders_status_with_two_statuses",
                PARTNER_URL
        );

        assertThrows(
                ResponseValidationException.class,
                () -> deliveryServiceClient.getOrdersStatus(
                        Collections.singletonList(new ResourceId.ResourceIdBuilder()
                                .setYandexId(YANDEX_ID)
                                .setDeliveryId(DELIVERY_ID)
                                .build()),
                        getPartnerProperties()
                )
        );
    }

    private GetOrdersStatusResponse getExpectedResponse() throws Exception {
        return new GetOrdersStatusResponse(Collections.singletonList(
            getObjectFromXml("fixture/response/entities/ds_order_status_history_for_get_orders_status.xml",
                OrderStatusHistory.class)
        ));
    }
}
