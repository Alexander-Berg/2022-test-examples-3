package ru.yandex.market.wms.common.spring.service.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.Korobyte;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.common.UnitCount;
import ru.yandex.market.logistic.api.model.common.UnitCountType;
import ru.yandex.market.logistic.api.model.common.UnitInfo;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.common.model.enums.LocStatus;
import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailItemIdentities;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dto.ReceiptSkuDto;
import ru.yandex.market.wms.common.spring.dto.SkuDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptDetailsExtractorTest {

    @Test
    public void test() {
        ReceiptDetailsExtractor rde = new ReceiptDetailsExtractor();

        List<PartialId> pids = new ArrayList<>();
        pids.add(new PartialId(PartialIdType.UIT, "uit"));
        pids.add(new PartialId(PartialIdType.UIT, "uit2"));
        pids.add(new PartialId(PartialIdType.CIS, "cis_1"));
        pids.add(new PartialId(PartialIdType.CIS, "cis_2"));
        pids.add(new PartialId(PartialIdType.IMEI, "imei_1"));
        pids.add(new PartialId(PartialIdType.IMEI, "imei_2"));
        pids.add(new PartialId(PartialIdType.IMEI, "imei_3"));
        pids.add(new PartialId(PartialIdType.SERIAL_NUMBER, null));
        pids.add(new PartialId(PartialIdType.BOX_ID, "box_1"));

        CompositeId cid = new CompositeId(pids);

        ReceiptDetailItemIdentities universal = rde.universalFromCompositeId(cid);
        assertEquals(2, universal.getCis().size(), "should be 2 CIS");
        assertEquals(3, universal.getImei().size(), "should be 3 IMEI");
        assertEquals(0, universal.getSn().size(), "should be no SN");
        assertTrue(universal.getUits().contains("uit"));
        assertTrue(universal.getUits().contains("uit2"));

    }

    @Test
    public void extractDetailsFromItemsForReturnWithoutUnitIds() {
        ReceiptDetailsExtractor rde = new ReceiptDetailsExtractor();
        SkuId sku = SkuId.of("1", "SKU");
        RegistryItem registryItem = registryItem(sku.getSku(), sku.getStorerKey());
        List<Pair<SkuId, RegistryItem>> items = List.of(Pair.of(sku, registryItem));

        List<ReceiptSkuDto> actual = rde.extractDetailsFromItems(items, ReceiptType.UPDATABLE_CUSTOMER_RETURN);
        List<ReceiptSkuDto> expected = List.of(expected(sku));
        assertEquals(expected, actual);
    }

    private ReceiptSkuDto expected(SkuId sku) {
        return ReceiptSkuDto.builder()
                .sku(SkuDto.of(sku))
                .unitPrice(BigDecimal.valueOf(12.34))
                .expectedQty(BigDecimal.valueOf(1))
                .manufactureDate(Instant.parse("2020-12-01T11:00:00+03:00"))
                .externOrderKey("1")
                .subreceipt("1")
                .name("name")
                .returnId("2")
                .conditionCode(LocStatus.OK)
                .sourceContainerId("")
                .master(true)
                .manufacturerSku(sku.getSku())
                .build();
    }

    private RegistryItem registryItem(String sku, String vendorId) {
        return RegistryItem.builder(
                        UnitInfo.builder()
                                .setCompositeId(
                                        CompositeId.builder(List.of(
                                                new PartialId(PartialIdType.ARTICLE, sku),
                                                new PartialId(PartialIdType.VENDOR_ID, vendorId),
                                                new PartialId(PartialIdType.ORDER_ID, "1"),
                                                new PartialId(PartialIdType.ORDER_RETURN_ID, "2")
                                        )).build()
                                )
                                .setCounts(List.of(new UnitCount.UnitCountBuilder()
                                        .setCountType(UnitCountType.FIT)
                                        .setQuantity(1)
                                        .build()
                                ))
                                .setKorobyte(new Korobyte.KorobyteBuiler(
                                        10, 11, 12, new BigDecimal(2))
                                        .setWeightNet(new BigDecimal("0.26"))
                                        .setWeightTare(new BigDecimal("0.1"))
                                        .build()
                                )
                                .build())
                .setName("name")
                .setPrice(new BigDecimal("12.34"))
                .setManufacturedDate(new DateTime("2020-12-01T11:00:00+03:00"))
                .setHasLifeTime(true)
                .setLifeTime(365)
                .setBoxCount(0)
                .setRemainingLifetimes(new RemainingLifetimes(
                        new ShelfLives(new ShelfLife(60), new ShelfLife(70)),
                        new ShelfLives(new ShelfLife(30), new ShelfLife(80))))
                .setUpdated(new DateTime("2021-05-19T13:31:54+03:00"))
                .setBarcodes(List.of(new Barcode.BarcodeBuilder("barcode").setSource(BarcodeSource.UNKNOWN).build()))
                .build();
    }

}
