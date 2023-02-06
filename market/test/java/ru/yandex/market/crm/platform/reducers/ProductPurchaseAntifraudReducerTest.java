package ru.yandex.market.crm.platform.reducers;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.ProductByIdPurchaseAntifraud;
import ru.yandex.market.crm.platform.models.ProductPurchaseAntifraud;
import ru.yandex.market.crm.platform.models.PurchaseInfo;

import static org.junit.Assert.assertEquals;

public class ProductPurchaseAntifraudReducerTest {

    private ProductPurchaseAntifraudReducer reducer = new ProductPurchaseAntifraudReducer();

    @Test
    public void reducerProducesFactsById() {
        ProductPurchaseAntifraud newFact = ProductPurchaseAntifraud.newBuilder()
                .setRgb(RGBType.GREEN)
                .setProductId("123")
                .setKeyUid(Uids.create(UidType.PUID, 555))
                .setTimestamp(999)
                .setDeliveryDays(3)
                .build();

        YieldMock collector = new YieldMock();
        reducer.reduce(Collections.emptyList(), Collections.singleton(newFact), collector);

        assertEquals(newFact, collector.getAdded(ProductPurchaseAntifraudReducer.FACT_ID).iterator().next());

        ProductByIdPurchaseAntifraud factById = ProductByIdPurchaseAntifraud.newBuilder().setProductId("123")
                .setRgb(RGBType.GREEN)
                .addPurchaseInfo(
                        PurchaseInfo.newBuilder()
                                .addUid(Uids.create(UidType.PUID, 555))
                                .setDeliveryDays(3)
                                .setTimestamp(999)
                ).build();


        assertEquals(factById, collector.getAdded(ProductPurchaseAntifraudReducer.PURCHASE_BY_PRODUCT_ID_FACT).iterator().next());
    }
}