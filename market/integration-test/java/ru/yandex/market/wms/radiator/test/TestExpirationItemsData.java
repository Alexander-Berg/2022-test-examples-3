package ru.yandex.market.wms.radiator.test;

import java.math.BigDecimal;

import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;

public class TestExpirationItemsData {

    public static final long VENDOR_ID = 1559L;
    public static final String M_SKU_AUTOLIFETIMECHANGED = "AUTOLIFETIMECHANGED";

    public static ItemReference mSkuAutoLifetimeChanged() {
        var unitId = new UnitId(M_SKU_AUTOLIFETIMECHANGED, VENDOR_ID, M_SKU_AUTOLIFETIMECHANGED);
        return new ItemReference(
                unitId,
                null,
                null,
                null,
                new Item.ItemBuilder(null, 0, BigDecimal.ZERO)
                        .setUnitId(unitId)
                        .setHasLifeTime(false)
                        .build()
        );
    }
}
