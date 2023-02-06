package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundDetailsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundUnitDetails;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Тест для {@link FulfillmentClient#getInboundDetails(ResourceId, PartnerProperties)}.
 */
class GetInboundDetailsTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "12345";
    private static final String PARTNER_ID = "Zakaz";

    @Test
    void testGetInboundDetailsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_inbound_details", PARTNER_URL);

        GetInboundDetailsResponse response =
            fulfillmentClient.getInboundDetails(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetInboundDetailsResponse"
        );
    }

    @Test
    void testGetInboundDetailsWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_inbound_details",
            "ff_get_inbound_details_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getInboundDetails(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private GetInboundDetailsResponse getExpectedResponse() {

        ResourceId inboundId = new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build();

        UnitId unitId = new UnitId.UnitIdBuilder(53465L, "BlaBla")
            .setId(String.valueOf(1561))
            .build();

        InboundUnitDetails inboundUnitDetails = new InboundUnitDetails(unitId, 1, 1, 0, 2);
        List<InboundUnitDetails> inboundUnitDetailsList = new ArrayList<>();
        inboundUnitDetailsList.add(inboundUnitDetails);

        InboundDetails inboundDetails = new InboundDetails(inboundId, inboundUnitDetailsList);

        return new GetInboundDetailsResponse(inboundDetails);
    }
}
