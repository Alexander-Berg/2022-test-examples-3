package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundDetailsXDocResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundDetailsXDoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetInboundDetailsXDocTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "12345";
    private static final String PARTNER_ID = "Zakaz";

    @Test
    void testGetInboundDetailsXDocSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_xdoc_inbound_details", PARTNER_URL);

        GetInboundDetailsXDocResponse response =
            fulfillmentClient.getInboundDetailsXDoc(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetInboundDetailsXDocResponse"
        );
    }

    @Test
    void testGetInboundDetailsXDocWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_xdoc_inbound_details",
            "ff_get_xdoc_inbound_details_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getInboundDetailsXDoc(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private GetInboundDetailsXDocResponse getExpectedResponse() {

        ResourceId inboundId = new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build();

        InboundDetailsXDoc inboundDetailsXDoc = new InboundDetailsXDoc(inboundId, 10, 15);

        return new GetInboundDetailsXDocResponse(inboundDetailsXDoc);
    }
}
