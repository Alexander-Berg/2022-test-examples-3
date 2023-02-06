package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";

    @Test
    void testGetOrderHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_order_history", PARTNER_URL);

        GetOrderHistoryResponse response = fulfillmentClient.getOrderHistory(new ResourceId(YANDEX_ID, PARTNER_ID),
            getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }

    @Test
    void testGetOrderHistorySucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized("ff_get_order_history_with_empty_history", PARTNER_URL);

        GetOrderHistoryResponse response = fulfillmentClient.getOrderHistory(new ResourceId(YANDEX_ID, PARTNER_ID),
            getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetOrderHistoryResponse(
                getObjectFromXml(
                    "fixture/response/entities/ff_order_status_history_with_empty_history.xml",
                    OrderStatusHistory.class
                )
            ));
    }

    @Test
    void testGetOrderHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_order_history", "ff_get_order_history_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOrderHistory(new ResourceId(YANDEX_ID, PARTNER_ID), getPartnerProperties())
        );
    }

    private GetOrderHistoryResponse getExpectedResponse() throws Exception {
        return new GetOrderHistoryResponse(
            getObjectFromXml("fixture/response/entities/ff_order_status_history.xml", OrderStatusHistory.class)
        );
    }
}
