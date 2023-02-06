package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetExternalOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.ExternalOrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetExternalOrderHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";
    private static final String DELIVERY_SERVICE_ID = "77";

    @Test
    void testGetOrderHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_external_order_history", PARTNER_URL);

        GetExternalOrderHistoryResponse response = deliveryServiceClient.getExternalOrderHistory(
            new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }


    @Test
    void testGetOrderHistorySucceededWithMilliseconds() throws Exception {
        prepareMockServiceNormalized("ds_get_external_order_history_with_millis", PARTNER_URL);

        GetExternalOrderHistoryResponse response = deliveryServiceClient.getExternalOrderHistory(
            new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }


    @Test
    void testGetOrderHistorySucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_external_order_history_with_empty_history",
            PARTNER_URL
        );

        GetExternalOrderHistoryResponse response = deliveryServiceClient.getExternalOrderHistory(
            new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetExternalOrderHistoryResponse(
                getObjectFromXml("fixture/response/entities/ds_external_order_status_history_with_empty_history.xml",
                    ExternalOrderStatusHistory.class)
            ));
    }

    @Test
    void testGetOrderHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_external_order_history", "ds_get_external_order_history_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getExternalOrderHistory(
                new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID),
                getPartnerProperties()
            )
        );
    }

    private GetExternalOrderHistoryResponse getExpectedResponse() throws Exception {
        return new GetExternalOrderHistoryResponse(
            getObjectFromXml(
                "fixture/response/entities/ds_external_order_status_history.xml",
                ExternalOrderStatusHistory.class
            )
        );
    }
}
