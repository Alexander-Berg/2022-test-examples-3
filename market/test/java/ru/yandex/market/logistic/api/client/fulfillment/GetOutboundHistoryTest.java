package ru.yandex.market.logistic.api.client.fulfillment;


import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundStatusHistory;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#getOutboundHistory(ResourceId, PartnerProperties)}.
 */
class GetOutboundHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "98765";
    private static final String PARTNER_ID = "YAYAY123";

    @Test
    void testGetOutboundHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_outbound_history", PARTNER_URL);
        GetOutboundHistoryResponse response = fulfillmentClient.getOutboundHistory(getOutboundId(),
            getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetOutboundHistoryResponse"
        );
    }

    @Test
    void testGetOutboundHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_outbound_history", "ff_get_outbound_history_with_errors",
            PARTNER_URL);
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOutboundHistory(getOutboundId(), getPartnerProperties())
        );
    }

    private GetOutboundHistoryResponse getExpectedResponse() {
        return new GetOutboundHistoryResponse(
            new OutboundStatusHistory(getOutboundId(),
                Collections.singletonList(
                    new Status(StatusCode.ASSEMBLING, new DateTime("2019-01-10T18:42:59+03:00")))
            ));
    }

    private ResourceId getOutboundId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build();
    }
}
