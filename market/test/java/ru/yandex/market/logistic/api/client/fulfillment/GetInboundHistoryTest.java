package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusHistory;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType.ACCEPTED;

/**
 * Тест для {@link FulfillmentClient#getInboundHistory(ResourceId, PartnerProperties)}.
 */
class GetInboundHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "34567";
    private static final String PARTNER_ID = "Z115FEF5E";

    @Test
    void testGetInboundHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_inbound_history", PARTNER_URL);

        GetInboundHistoryResponse response = fulfillmentClient.getInboundHistory(new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(PARTNER_ID)
                .build(),
            getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetInboundHistoryResponse"
        );
    }

    @Test
    void testGetInboundHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_inbound_history", "ff_get_inbound_history_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getInboundHistory(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private GetInboundHistoryResponse getExpectedResponse() {
        final InboundStatus inboundStatus = new InboundStatus(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build(),
            ACCEPTED,
            new DateTime("2018-12-21T11:59:59+03:00"));

        final InboundStatusHistory inboundStatusHistory =
            new InboundStatusHistory(Collections.singletonList(inboundStatus),
                new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build());
        return new GetInboundHistoryResponse(inboundStatusHistory);
    }
}
