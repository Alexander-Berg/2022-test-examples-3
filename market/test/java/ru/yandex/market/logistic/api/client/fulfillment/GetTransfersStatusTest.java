package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransfersStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusEvent;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusType;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTransfersStatusTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "45678";
    private static final String PARTNER_ID = "HOR5435";

    @Test
    void testGetTransfersStatusSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_transfers_status", PARTNER_URL);

        GetTransfersStatusResponse response = fulfillmentClient
            .getTransfersStatus(Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetTransfersStatusRequest"
        );
    }

    @Test
    void testGetTransfersStatusWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_transfers_status",
            "ff_get_transfers_status_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getTransfersStatus(
                Collections.singletonList(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build()),
                getPartnerProperties())
        );
    }

    private GetTransfersStatusResponse getExpectedResponse() {
        TransferStatus transferStatus = new TransferStatus(new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build(),
            new TransferStatusEvent(TransferStatusType.ACCEPTED,
                new DateTime("2018-12-14T15:00:01+03:00")));

        return new GetTransfersStatusResponse(Collections.singletonList(transferStatus));
    }
}
