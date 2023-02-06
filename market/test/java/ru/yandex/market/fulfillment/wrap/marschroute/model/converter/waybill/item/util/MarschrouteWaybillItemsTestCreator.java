package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.waybill.item.util;

import com.google.common.collect.ImmutableList;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;

import java.math.BigDecimal;
import java.util.Arrays;

public class MarschrouteWaybillItemsTestCreator {

    public Consignment createFirstConsignment() {
        Item item = new Item.ItemBuilder("name", 10, BigDecimal.valueOf(110))
                .setUnitId(new UnitId("", 123L, "shop_sku"))
                .setBarcodes(ImmutableList.of(
                        new Barcode("code1", "type"),
                        new Barcode("code2", "type"))
                )
                .setDescription("desc")
                .setUntaxedPrice(BigDecimal.valueOf(100))
                .setTax(new Tax(TaxType.VAT, VatValue.TEN))
                .setComment("desc")
                .build();

        return new Consignment(null, item, null);
    }

    public Consignment createSecondConsignment() {
        return new Consignment(null,
            new Item.ItemBuilder("name", 2, null).setUnitId(new UnitId("", 123L, "shop_sku")).build(),
            null);
    }

    public Consignment createThirdConsignment() {
        return new Consignment(null,
            new Item.ItemBuilder("name", 2, null).setUnitId(new UnitId("", 123L, "shop_sku")).setVendorCodes(Arrays.asList("VENDOR1", "VENDOR2")).build(),
            null);
    }

    public Consignment createFourthConsignment() {
        return new Consignment(null,
            new Item.ItemBuilder("name", 2, null).setUnitId(new UnitId("", 123L, "shop_sku"))
                .setVendorCodes(Arrays.asList("VENDOR1", null)).build(),
            null);
    }

    public Consignment createFifthConsignment() {
        return new Consignment(null,
            new Item.ItemBuilder("name", 2, null).setUnitId(new UnitId("", 123L, "shop_sku"))
                .setVendorCodes(Arrays.asList(null, null)).build(),
            null);
    }

    public MarschrouteWaybillItem getFirstWaybillItem() {
        return new MarschrouteWaybillItem().setItemId("shop_sku.123")
            .setBarcode(Arrays.asList("code1", "code2"))
            .setName("name (shop_sku)")
            .setComment("desc")
            .setPriceNds(BigDecimal.valueOf(110).setScale(2, BigDecimal.ROUND_UP))
            .setSumNds(BigDecimal.valueOf(1100).setScale(2, BigDecimal.ROUND_UP))
            .setQuantity(10);
    }

    public MarschrouteWaybillItem getSecondWaybillItem() {
        return new MarschrouteWaybillItem().setItemId("shop_sku.123").setName("name (shop_sku)")
            .setQuantity(2);
    }

    public MarschrouteWaybillItem getThirdWaybillItem() {
        return new MarschrouteWaybillItem().setItemId("shop_sku.123").setName("name (VENDOR1, VENDOR2)")
            .setQuantity(2);
    }

    public MarschrouteWaybillItem getFourthWaybillItem() {
        return new MarschrouteWaybillItem().setItemId("shop_sku.123").setName("name (VENDOR1)")
            .setQuantity(2);
    }

    public MarschrouteWaybillItem getFifthWaybillItem() {
        return new MarschrouteWaybillItem().setItemId("shop_sku.123").setName("name (shop_sku)")
            .setQuantity(2);
    }
}
