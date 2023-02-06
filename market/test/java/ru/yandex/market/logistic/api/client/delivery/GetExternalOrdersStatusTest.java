package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetExternalOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.ExternalOrderStatusHistory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GetExternalOrdersStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "25";
    private static final String PARTNER_ID = "45";
    private static final String DELIVERY_SERVICE_ID = "77";

    @Test
    void testGetExternalOrdersStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_external_orders_status", PARTNER_URL);

        GetExternalOrdersStatusResponse response = deliveryServiceClient.getExternalOrdersStatus(
            Collections.singletonList(new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID)),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(getExpectedResponse());
    }

    @Test
    void testExternalGetOrdersStatusSucceededWithMilliseconds() throws Exception {
        prepareMockServiceNormalized("ds_get_external_orders_status_with_millis", PARTNER_URL);

        GetExternalOrdersStatusResponse response = deliveryServiceClient.getExternalOrdersStatus(
            Arrays.asList(
                new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID),
                new ExternalResourceId("26", "46", "78"),
                new ExternalResourceId("27", "47", "79")
            ),
            getPartnerProperties()
        );

        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(
                getObjectFromXml(
                    "fixture/response/entities/ds_external_order_status_history_with_millis.xml",
                    GetExternalOrdersStatusResponse.class
                )
            );
    }


    @Test
    void testGetExternalOrdersStatusSucceededWithEmptyHistory() throws Exception {
        prepareMockServiceNormalized("ds_get_external_orders_status_with_empty_history",
            PARTNER_URL);

        GetExternalOrdersStatusResponse response = deliveryServiceClient.getExternalOrdersStatus(
            Collections.singletonList(new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID)),
            getPartnerProperties()
        );

        ExternalOrderStatusHistory expectedResponse = getObjectFromXml(
            "fixture/response/entities/ds_external_orders_status_with_empty_history.xml",
            ExternalOrderStatusHistory.class);
        assertions.assertThat(response)
            .as("Asserting the response is correct")
            .isEqualToComparingFieldByFieldRecursively(new GetExternalOrdersStatusResponse(Collections.singletonList(
                expectedResponse
            )));
    }

    @Test
    void testGetOrdersStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_external_orders_status",
            "ds_get_external_orders_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getExternalOrdersStatus(
                Collections.singletonList(new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID)),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testGetOrdersStatusWithOverOneHistory() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_external_orders_status",
            "ds_get_external_orders_status_with_two_statuses",
            PARTNER_URL
        );

        assertThrows(
            ResponseValidationException.class,
            () -> deliveryServiceClient.getExternalOrdersStatus(
                Collections.singletonList(new ExternalResourceId(YANDEX_ID, PARTNER_ID, DELIVERY_SERVICE_ID)),
                getPartnerProperties()
            )
        );
    }

    private GetExternalOrdersStatusResponse getExpectedResponse() throws Exception {
        return new GetExternalOrdersStatusResponse(Collections.singletonList(
            getObjectFromXml("fixture/response/entities/ds_external_order_status_history_for_get_orders_status.xml",
                ExternalOrderStatusHistory.class)
        ));
    }
}
