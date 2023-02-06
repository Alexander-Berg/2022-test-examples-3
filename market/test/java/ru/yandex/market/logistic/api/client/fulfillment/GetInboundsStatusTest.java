package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatus;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType.ACCEPTED;

/**
 * Тест для {@link FulfillmentClient#getInboundsStatus(List, PartnerProperties)}.
 */
class GetInboundsStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "45678";
    private static final String PARTNER_ID = "HOR5435";

    @Test
    void testGetInboundsStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_inbounds_status", PARTNER_URL);

        GetInboundsStatusResponse response = fulfillmentClient
            .getInboundsStatus(Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetInboundsStatusResponse"
        );
    }

    @Test
    void testGetInboundsStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_inbounds_status", "ff_get_inbounds_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getInboundsStatus(Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties())
        );
    }

    private GetInboundsStatusResponse getExpectedResponse() {
        InboundStatus inboundStatus = new InboundStatus(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build(),
            ACCEPTED,
            new DateTime("2018-12-14T15:00:01+03:00"));
        return new GetInboundsStatusResponse(Collections.singletonList(inboundStatus));
    }
}
