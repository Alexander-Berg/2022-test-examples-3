package ru.yandex.market.wrap.infor.service.transfer.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Transfer;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.TransferItem;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.InforPartialIdDTO;
import ru.yandex.market.wrap.infor.client.model.TransferDTO;
import ru.yandex.market.wrap.infor.client.model.TransferStorerSkuDTO;
import ru.yandex.market.wrap.infor.entity.InforUnitId;

class TransferStorerSkuDTOConverterTest extends SoftAssertionSupport {

    private static final String TRANSFER_KEY = "transferKey";
    private static final String EXTERNAL_KEY = "externalKey";

    private static final String SKU_1 = "SKU_1";
    private static final String SKU_2 = "SKU_2";

    private static final Long VENDOR_ID_1 = 47L;
    private static final Long VENDOR_ID_2 = 55L;

    private static final Integer QUANTITY_1 = 5;
    private static final Integer QUANTITY_2 = 10;

    private static final StockType STOCK_TYPE_FROM = StockType.SURPLUS;
    private static final StockType STOCK_TYPE_TO = StockType.FIT;

    private static final String INFOR_STOCK_TYPE_FROM = "0";
    private static final String INFOR_STOCK_TYPE_TO = "1";

    private static final UnitId UNIT_ID_1 = new UnitId(SKU_1, VENDOR_ID_1, SKU_1);
    private static final UnitId UNIT_ID_2 = new UnitId(SKU_2, VENDOR_ID_2, SKU_2);

    private static final InforUnitId INFOR_UNIT_ID_1 = InforUnitId.of(SKU_1, VENDOR_ID_1);
    private static final InforUnitId INFOR_UNIT_ID_2 = InforUnitId.of(SKU_2, VENDOR_ID_2);

    private static final String CIS_1 = "011004391854891121mbg:zCaRlU%c06";
    private static final String CIS_2 = "011004391854891121mbg:zCaRlU%c07";

    private TransferStorerSkuDTOConverter transferStorerSkuDTOConverter = new TransferStorerSkuDTOConverter();

    @Test
    void convertToTransferDto() {
        final Transfer transfer = getTransferWithTransferItems();
        final Map<UnitId, InforUnitId> transferItemsMapping = getTransferItemsMapping();

        List<TransferStorerSkuDTO> transferDtoList =
            transferStorerSkuDTOConverter.convert(transfer, transferItemsMapping);

        // First TransferStorerSkuDTO
        softly
            .assertThat(transferDtoList.get(0).getTosku())
            .as("Asserting sku")
            .isEqualTo(SKU_1);

        softly
            .assertThat(transferDtoList.get(0).getTostorerkey())
            .as("Asserting storer key")
            .isEqualTo(VENDOR_ID_1.toString());

        softly
            .assertThat(transferDtoList.get(0).getToqty())
            .as("Asserting quantity")
            .isEqualTo(QUANTITY_1.floatValue());

        softly
            .assertThat(transferDtoList.get(0).getFromLOTTABLE08())
            .as("Asserting stock type from")
            .isEqualTo(INFOR_STOCK_TYPE_FROM);

        softly
            .assertThat(transferDtoList.get(0).getToLOTTABLE08())
            .as("Asserting stock type to")
            .isEqualTo(INFOR_STOCK_TYPE_TO);

        softly
            .assertThat(transferDtoList.get(0).getInstances())
            .as("Asserting instances size")
            .hasSize(2);

        softly
            .assertThat(transferDtoList.get(0).getInstances().get(0).getPartialIds())
            .as("Asserting partialIds size")
            .hasSize(1);

        softly
            .assertThat(transferDtoList.get(0).getInstances().get(1).getPartialIds())
            .as("Asserting partialIds size")
            .hasSize(1);

        InforPartialIdDTO inforPartialIdDTO1 = transferDtoList.get(0).getInstances().get(0).getPartialIds().get(0);
        InforPartialIdDTO inforPartialIdDTO2 = transferDtoList.get(0).getInstances().get(1).getPartialIds().get(0);

        softly
             .assertThat(inforPartialIdDTO1.getIdType())
             .as("Asserting partialId name")
             .isEqualTo(PartialIdType.CIS.getName());

        softly
            .assertThat(inforPartialIdDTO2.getIdType())
            .as("Asserting partialId name")
            .isEqualTo(PartialIdType.CIS.getName());

        softly
            .assertThat(inforPartialIdDTO1.getValue())
            .as("Asserting partialId value")
            .isEqualTo(CIS_1);

        softly
            .assertThat(inforPartialIdDTO2.getValue())
            .as("Asserting partialId value")
            .isEqualTo(CIS_2);

        // Second TransferStorerSkuDTO
        softly
            .assertThat(transferDtoList.get(1).getTosku())
            .as("Asserting sku")
            .isEqualTo(SKU_2);

        softly
            .assertThat(transferDtoList.get(1).getTostorerkey())
            .as("Asserting storer key")
            .isEqualTo(VENDOR_ID_2.toString());

        softly
            .assertThat(transferDtoList.get(1).getToqty())
            .as("Asserting quantity")
            .isEqualTo(QUANTITY_2.floatValue());

        softly
            .assertThat(transferDtoList.get(1).getFromLOTTABLE08())
            .as("Asserting stock type from")
            .isEqualTo(INFOR_STOCK_TYPE_FROM);

        softly
            .assertThat(transferDtoList.get(1).getToLOTTABLE08())
            .as("Asserting stock type to")
            .isEqualTo(INFOR_STOCK_TYPE_TO);
    }

    @Test
    void convertToResourceIds() {
        ResourceId resourceId = transferStorerSkuDTOConverter.convertToResourceIds(getTransferDTO());

        softly
            .assertThat(resourceId.getYandexId())
            .as("Asserting YandexId (first element)")
            .isEqualTo(EXTERNAL_KEY);

        softly
            .assertThat(resourceId.getPartnerId())
            .as("Asserting PartnerId (first element)")
            .isEqualTo(TRANSFER_KEY);
    }

    private Transfer getTransferWithTransferItems() {
        TransferItem transferItem1 = new TransferItem(UNIT_ID_1, QUANTITY_1);
        transferItem1.setInstances(List.of(getCompositeId(PartialIdType.CIS, CIS_1),
                                           getCompositeId(PartialIdType.CIS, CIS_2)));

        TransferItem transferItem2 = new TransferItem(UNIT_ID_2, QUANTITY_2);

        return new Transfer(null, null, STOCK_TYPE_FROM, STOCK_TYPE_TO, Arrays.asList(transferItem1, transferItem2));
    }

    private Map<UnitId, InforUnitId> getTransferItemsMapping() {
        return ImmutableMap.of(
            UNIT_ID_1, INFOR_UNIT_ID_1,
            UNIT_ID_2, INFOR_UNIT_ID_2
        );
    }

    private TransferDTO getTransferDTO() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setExterntransferkey(EXTERNAL_KEY);
        transferDTO.setTransferkey(TRANSFER_KEY);
        return transferDTO;
    }

    private CompositeId getCompositeId(PartialIdType idType, String value) {
        return new CompositeId(List.of(new PartialId(idType, value)));
    }
}
