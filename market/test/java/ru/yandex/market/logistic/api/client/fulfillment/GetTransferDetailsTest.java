package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransferDetailsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetailsItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTransferDetailsTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "12345";
    private static final String PARTNER_ID = "Zakaz";

    @Test
    void testGetTransferDetailsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_transfer_details", PARTNER_URL);

        GetTransferDetailsResponse response =
            fulfillmentClient.getTransferDetails(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties());

        assertEquals(
            getExpectedResponse(),
            response,
            "Должен вернуть корректный ответ GetTransferDetailsResponse"
        );
    }

    @Test
    void testCancelTransferWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_transfer_details",
            "ff_get_transfer_details_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getTransferDetails(new ResourceId.ResourceIdBuilder()
                    .setYandexId(YANDEX_ID)
                    .setPartnerId(PARTNER_ID)
                    .setFulfillmentId(PARTNER_ID)
                    .build(),
                getPartnerProperties())
        );
    }

    private GetTransferDetailsResponse getExpectedResponse() {

        ResourceId transferId = new ResourceId.ResourceIdBuilder()
            .setYandexId(YANDEX_ID)
            .setPartnerId(PARTNER_ID)
            .setFulfillmentId(PARTNER_ID)
            .build();

        UnitId unitId = new UnitId.UnitIdBuilder(53465L, "BlaBla")
            .setId(String.valueOf(1561))
            .build();


        TransferDetailsItem transferDetailsItem = new TransferDetailsItem(unitId, 10, 10);

        transferDetailsItem.setInstances(
            ImmutableList.of(
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
            )
        );

        TransferDetails transferDetails =
            new TransferDetails(transferId, Collections.singletonList(transferDetailsItem));

        return new GetTransferDetailsResponse(transferDetails);
    }
}
