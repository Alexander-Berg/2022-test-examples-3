package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String DELIVERY_ID = "45";

    @Test
    void testGetOrderHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_order_history", PARTNER_URL);

        GetOrderHistoryResponse response = deliveryServiceClient.getOrderHistory(
            new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setDeliveryId(DELIVERY_ID)
                .build(),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }


    @Test
    void testGetOrderHistorySucceededWithMilliseconds() throws Exception {
        prepareMockServiceNormalized("ds_get_order_history_with_millis", PARTNER_URL);

        GetOrderHistoryResponse response = deliveryServiceClient.getOrderHistory(
            new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
            getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }


    @Test
    void testGetOrderHistorySucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized("ds_get_order_history_with_empty_history", PARTNER_URL);

        GetOrderHistoryResponse response = deliveryServiceClient.getOrderHistory(
            new ResourceId.ResourceIdBuilder().setYandexId(YANDEX_ID).setDeliveryId(DELIVERY_ID).build(),
            getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetOrderHistoryResponse(
                getObjectFromXml("fixture/response/entities/ds_order_status_history_with_empty_history.xml",
                    OrderStatusHistory.class)
            ));
    }

    @Test
    void testGetOrderHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_order_history", "ds_get_order_history_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getOrderHistory(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setDeliveryId(DELIVERY_ID)
                    .build(),
                getPartnerProperties()
            ));
    }

    private GetOrderHistoryResponse getExpectedResponse() throws Exception {
        return new GetOrderHistoryResponse(
            getObjectFromXml("fixture/response/entities/ds_order_status_history.xml", OrderStatusHistory.class)
        );
    }
}
