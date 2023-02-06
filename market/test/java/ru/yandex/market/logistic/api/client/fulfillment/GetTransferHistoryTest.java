package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransferHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusEvent;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusType.ACCEPTED;

class GetTransferHistoryTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "34567";
    private static final String PARTNER_ID = "Z115FEF5E";

    @Test
    void testGetTransferHistorySucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_transfer_history", PARTNER_URL);

        GetTransferHistoryResponse response = fulfillmentClient.getTransferHistory(new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(PARTNER_ID)
                .build(),
            getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetTransferHistoryResponse"
        );
    }

    @Test
    void testGetTransferHistoryWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_transfer_history", "ff_get_transfer_history_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getTransferHistory(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private GetTransferHistoryResponse getExpectedResponse() {
        TransferStatusEvent transferStatusEvent = new TransferStatusEvent(ACCEPTED,
            new DateTime("2018-12-21T11:59:59+03:00"));

        final TransferStatusHistory transferStatusHistory = new TransferStatusHistory(
            Collections.singletonList(transferStatusEvent),
            new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(PARTNER_ID)
                .build()
        );
        return new GetTransferHistoryResponse(transferStatusHistory);
    }
}
