package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundsStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#getOutboundsStatus(List, PartnerProperties)}.
 */
class GetOutboundsStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "45678";
    private static final String PARTNER_ID = "HOR5435";

    @Test
    void testGetOutboundsStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_outbounds_status", PARTNER_URL);

        GetOutboundsStatusResponse response = fulfillmentClient
            .getOutboundsStatus(Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetOutboundsStatusResponse"
        );
    }

    @Test
    void testGetOutboundsStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_outbounds_status", "ff_get_outbounds_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOutboundsStatus(Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties())
        );
    }

    private GetOutboundsStatusResponse getExpectedResponse() {
        OutboundStatus outboundStatus = new OutboundStatus(
            new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(PARTNER_ID)
                .build(),
            new Status(StatusCode.ASSEMBLING, new DateTime("2019-01-10T18:42:59+03:00")));

        return new GetOutboundsStatusResponse(Collections.singletonList(outboundStatus));
    }
}
