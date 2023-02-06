package ru.yandex.market.crm.platform.reducers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Message;
import org.junit.Test;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.ProductByIdPurchaseAntifraud;
import ru.yandex.market.crm.platform.models.PurchaseInfo;

import static org.junit.Assert.assertEquals;

public class ProductByIdPurchaseAntifraudReducerTest {

    private ProductByIdPurchaseAntifraudReducer reducer = new ProductByIdPurchaseAntifraudReducer();
    private ProductByIdPurchaseAntifraud fact1 = ProductByIdPurchaseAntifraud.newBuilder()
            .setProductId("123")
            .setRgb(RGBType.GREEN)
            .addPurchaseInfo(
                    PurchaseInfo.newBuilder()
                            .addUid(Uids.create(UidType.PUID, 222))
                            .setTimestamp(999)
                            .setDeliveryDays(3)
            ).build();

    private ProductByIdPurchaseAntifraud fact2 = ProductByIdPurchaseAntifraud.newBuilder()
            .setProductId("123")
            .setRgb(RGBType.GREEN)
            .addPurchaseInfo(
                    PurchaseInfo.newBuilder()
                            .addUid(Uids.create(UidType.PUID, 333))
                            .setTimestamp(888)
                            .setDeliveryDays(2)
            ).build();

    private ProductByIdPurchaseAntifraud merged = ProductByIdPurchaseAntifraud.newBuilder()
            .setProductId("123")
            .setRgb(RGBType.GREEN)
            .addPurchaseInfo(
                    PurchaseInfo.newBuilder()
                            .addUid(Uids.create(UidType.PUID, 222))
                            .setTimestamp(999)
                            .setDeliveryDays(3)
            )
            .addPurchaseInfo(
                    PurchaseInfo.newBuilder()
                            .addUid(Uids.create(UidType.PUID, 333))
                            .setTimestamp(888)
                            .setDeliveryDays(2)
            ).build();

    @Test
    public void storedFactAccumulatesInfos() {
        YieldMock collector = new YieldMock();
        reducer.reduce(Collections.singletonList(fact1), Collections.singleton(fact2), collector);

        Collection<Message> added = collector.getAdded(ProductByIdPurchaseAntifraudReducer.FACT_ID);
        assertEquals(1, added.size());
        assertEquals(merged, added.iterator().next());
    }

    @Test
    public void newFactIsAdded() {
        YieldMock collector = new YieldMock();
        reducer.reduce(Collections.emptyList(), CollectionFactory.singleton(fact1), collector);

        Collection<Message> added = collector.getAdded(ProductByIdPurchaseAntifraudReducer.FACT_ID);
        assertEquals(1, added.size());
        assertEquals(fact1, added.iterator().next());
    }

    @Test
    public void newFactsAreMerged() {
        YieldMock collector = new YieldMock();
        reducer.reduce(Collections.emptyList(), List.of(fact1, fact2), collector);

        Collection<Message> added = collector.getAdded(ProductByIdPurchaseAntifraudReducer.FACT_ID);
        assertEquals(1, added.size());
        assertEquals(merged, added.iterator().next());
    }
}