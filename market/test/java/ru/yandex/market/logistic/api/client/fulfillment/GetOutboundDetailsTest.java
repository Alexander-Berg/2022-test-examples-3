package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundDetailsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundUnitDetails;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#getOutboundDetails(ResourceId, PartnerProperties)}.
 */
class GetOutboundDetailsTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "87654";
    private static final String PARTNER_ID = "561FFF";

    @Test
    void testGetOutboundDetailsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_outbound_details", PARTNER_URL);
        GetOutboundDetailsResponse response = fulfillmentClient.getOutboundDetails(getOutboundId(),
            getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetOutboundDetailsResponse"
        );
    }

    @Test
    void testGetOutboundDetailsWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_outbound_details", "ff_get_outbound_details_with_errors",
            PARTNER_URL);
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getOutboundDetails(getOutboundId(), getPartnerProperties())
        );
    }

    private GetOutboundDetailsResponse getExpectedResponse() {
        return new GetOutboundDetailsResponse(
            new OutboundDetails(getOutboundId(),
                Collections.singletonList(
                    new OutboundUnitDetails(new UnitId("UNITID21654", 465L, "UnitArticle"), 2, 2)
                ))
        );
    }

    private ResourceId getOutboundId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build();
    }
}
