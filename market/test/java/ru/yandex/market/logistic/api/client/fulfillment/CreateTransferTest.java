package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Transfer;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.TransferItem;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateTransferResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTransferTest extends CommonServiceClientTest {

    @Test
    void testCreateTransferSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_transfer", PARTNER_URL);


        CreateTransferResponse response = fulfillmentClient.createTransfer(getTransfer(), getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ CreateTransferResponse"
        );
    }

    @Test
    void testCreateTransferWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_create_transfer",
            "ff_create_transfer_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createTransfer(getTransfer(), getPartnerProperties())
        );
    }

    private Transfer getTransfer() {

        List<TransferItem> transferItems = new ArrayList<>();
        TransferItem transferItem1 = new TransferItem(new UnitId.UnitIdBuilder(2L, "AAA")
            .setId("1")
            .build(),
            10);
        transferItems.add(transferItem1);

        TransferItem transferItem2 = new TransferItem(new UnitId.UnitIdBuilder(2L, "AAA")
            .setId("2")
            .build(),
            10);
        transferItems.add(transferItem2);

        TransferItem transferItem3 = new TransferItem(new UnitId.UnitIdBuilder(2L, "AAA")
            .setId("3")
            .build(),
            10);

        transferItem3.setInstances(
            ImmutableList.of(new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))))
        );
        transferItems.add(transferItem3);

        return new Transfer(new ResourceId("transferYandexId", null),
            new ResourceId("inboundYandexId", "inboundPartnerId"),
            StockType.FIT,
            StockType.SURPLUS,
            transferItems);
    }

    private CreateTransferResponse getExpectedResponse() {
        return new CreateTransferResponse(new ResourceId("transferYandexId", "transferPartnerId"));
    }

}
