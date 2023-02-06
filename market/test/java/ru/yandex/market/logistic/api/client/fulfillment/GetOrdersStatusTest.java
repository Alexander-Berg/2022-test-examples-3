package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrdersStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";


    @Test
    void testGetOrdersStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_orders_status", PARTNER_URL);

        GetOrdersStatusResponse response = fulfillmentClient.getOrdersStatus(
            Collections.singletonList(new ResourceId(YANDEX_ID, PARTNER_ID)), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }

    @Test
    void testGetOrdersStatusSucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized("ff_get_orders_status_with_empty_history", PARTNER_URL);

        GetOrdersStatusResponse response = fulfillmentClient.getOrdersStatus(
            Collections.singletonList(new ResourceId(YANDEX_ID, PARTNER_ID)), getPartnerProperties());

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetOrdersStatusResponse(Collections.singletonList(
                getObjectFromXml(
                    "fixture/response/entities/ff_order_status_history_with_empty_history.xml",
                    OrderStatusHistory.class
                )
            )));
    }

    @Test
    void testGetOrdersStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_orders_status", "ff_get_orders_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOrdersStatus(
                Collections.singletonList(new ResourceId(YANDEX_ID, PARTNER_ID)),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testGetOrdersStatusWithOverOneHistory() throws Exception {
        prepareMockServiceNormalized(
                "ff_get_orders_status",
                "ff_get_orders_status_with_two_statuses",
                PARTNER_URL
        );

        assertThrows(
                ResponseValidationException.class,
                () -> fulfillmentClient.getOrdersStatus(
                        Collections.singletonList(new ResourceId(YANDEX_ID, PARTNER_ID)),
                        getPartnerProperties()
                )
        );
    }

    private GetOrdersStatusResponse getExpectedResponse() throws Exception {
        return new GetOrdersStatusResponse(Collections.singletonList(
            getObjectFromXml("fixture/response/entities/ff_order_status_history_for_get_orders_status.xml",
                OrderStatusHistory.class)
        ));
    }
}
