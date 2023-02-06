package ru.yandex.market.logistic.gateway.service.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Transfer;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusEvent;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetailsItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.FulfillmentConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.TransfersConverter;
import ru.yandex.market.logistic.gateway.utils.delivery.DtoFactory;

public class TransfersConverterTest extends BaseTest {

    @Test
    public void convertTransferDetailsFromApi() {
        String yandexId = "111";
        String partnerId = "222";

        int actual = 2;
        int declared = 2;

        String unitId = "444";
        Long unitVendorId = 100L;
        String unitArticle = "TestArticle";

        ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId resourceId =
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(yandexId)
                .setPartnerId(partnerId)
                .build();

        UnitId unit = new UnitId(unitId, unitVendorId, unitArticle);
        List<TransferDetailsItem> transferDetailsItems = new ArrayList<>();
        TransferDetailsItem transferDetailsItem = new TransferDetailsItem(unit, declared, actual);
        transferDetailsItem.setInstances(
            ImmutableList.of(
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
            )
        );
        transferDetailsItems.add(transferDetailsItem);
        TransferDetails expectedTransferDetails = new TransferDetails(resourceId, transferDetailsItems);

        List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetailsItem>
            transferDetailsItemList = new ArrayList<>();
        ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetailsItem apiDetailsItem =
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetailsItem(
                new ru.yandex.market.logistic.api.model.fulfillment.UnitId.UnitIdBuilder(unitVendorId, unitArticle)
                    .setId(unitId)
                    .build(),
                actual,
                declared
            );
        apiDetailsItem.setInstances(
            ImmutableList.of(
                new ru.yandex.market.logistic.api.model.common.CompositeId(ImmutableList.of(
                    new ru.yandex.market.logistic.api.model.common.PartialId(
                        ru.yandex.market.logistic.api.model.common.PartialIdType.CIS, "cis123")
                )),
                new ru.yandex.market.logistic.api.model.common.CompositeId(ImmutableList.of(
                    new ru.yandex.market.logistic.api.model.common.PartialId(
                        ru.yandex.market.logistic.api.model.common.PartialIdType.CIS, "cis222")
                ))
            )
        );
        transferDetailsItemList.add(apiDetailsItem);
        TransferDetails transferDetails = TransfersConverter.convertTransferDetailsFromApi(
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferDetails(
                new ResourceId(yandexId, partnerId),
                transferDetailsItemList)
        ).orElse(null);

        assertions.assertThat(transferDetails)
            .as("Asserting the actual transfer details is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedTransferDetails);
    }

    @Test
    public void convertTransferToApi() {
        Transfer expTr = DtoFactory.createTransfer();
        Transfer transfer = TransfersConverter.convertTransferToApi(getTransfer(expTr));

        assertions.assertThat(transfer)
            .as("Asserting the actual transfer details is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expTr);
    }

    @Test
    public void convertTransferStatusFromApi() {
        TransferStatus expectedTS = DtoFactory.createTransferStatus();

        TransferStatus actualTS = TransfersConverter.convertTransferStatusFromApi(
            getTransferStatus(expectedTS)).orElse(null);

        assertions.assertThat(actualTS)
            .as("Asserting the actual transfer status is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedTS);
    }

    @Test
    public void convertTransferStatusListFromApi() {
        TransferStatus expectedTS = DtoFactory.createTransferStatus();

        List<TransferStatus> actualTSList = TransfersConverter.convertTransferStatusListFromApi(
            Collections.singletonList(getTransferStatus(expectedTS))
        );

        assertions.assertThat(actualTSList).isNotNull();
        assertions.assertThat(actualTSList).hasSize(1);
        assertions.assertThat(actualTSList.get(0))
            .as("Asserting the actual transfer status is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedTS);
    }

    @Test
    public void convertTransferHistoryFromApi() {
        TransferStatusHistory expectedTSH = DtoFactory.createTransferStatusHistory();
        TransferStatusHistory actualTSH = TransfersConverter.convertTransferHistoryFromApi(
            new ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusHistory(
                Collections.singletonList(new TransferStatusEvent(TransferStatusType.ACCEPTED,
                    new DateTime(expectedTSH.getHistory().get(0).getDate().getFormattedDate()))),
                new ResourceId(expectedTSH.getTransferId().getYandexId(),
                    expectedTSH.getTransferId().getPartnerId())
            )
        ).orElse(null);

        assertions.assertThat(actualTSH)
            .as("Asserting the actual transfer history is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expectedTSH);

    }

    private ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatus getTransferStatus(TransferStatus tS) {
        return new ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatus(
            new ResourceId(tS.getTransferId().getYandexId(),
                tS.getTransferId().getPartnerId()),
            new TransferStatusEvent(TransferStatusType.ACCEPTED,
                new DateTime(tS.getTransferStatusEvent().getDate().getFormattedDate()))
        );
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer getTransfer(Transfer tr) {
        List<TransferItem> transferItems = new ArrayList<>();
        ru.yandex.market.logistic.api.model.fulfillment.UnitId unitId = tr.getTransferItems().get(0).getUnitId();
        TransferItem transferItem = new TransferItem(
            new UnitId(
                unitId.getId(),
                unitId.getVendorId(),
                unitId.getArticle()
            ),
            tr.getTransferItems().get(0).getCount()
        );
        transferItem.setInstances(
            ImmutableList.of(
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
                new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
            )
        );
        transferItems.add(transferItem);
        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.Transfer(

            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(tr.getTransferId().getYandexId())
                .setPartnerId(tr.getTransferId().getPartnerId())
                .build(),
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId(tr.getInboundId().getYandexId())
                .setPartnerId(tr.getInboundId().getPartnerId())
                .build(),
            FulfillmentConverter.convertStockTypeFromApi(tr.getFrom()).orElse(null),
            FulfillmentConverter.convertStockTypeFromApi(tr.getTo()).orElse(null),
            transferItems
        );
    }
}
