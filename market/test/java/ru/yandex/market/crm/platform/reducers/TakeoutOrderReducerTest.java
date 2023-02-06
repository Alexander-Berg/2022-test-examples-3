package ru.yandex.market.crm.platform.reducers;

import java.util.Collections;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.models.TakeoutOrder;
import ru.yandex.market.crm.platform.models.TakeoutOrderHistory;
import ru.yandex.market.crm.platform.models.TakeoutOrderStatusHistory;
import ru.yandex.market.crm.platform.reducers.takeout.orderlog.TakeoutBlueOrderReducer;
import ru.yandex.market.crm.platform.reducers.takeout.orderlog.TakeoutRedOrderReducer;
import ru.yandex.market.crm.platform.reducers.takeout.orderlog.TakeoutWhiteOrderReducer;

//TODO entarrion написать тесты в рамках тикета MSTAT-8108
public class TakeoutOrderReducerTest {
    private static final String FACT_ID_FOR_RED = "TakeoutRedOrder";
    private static final String FACT_ID_FOR_BLUE = "TakeoutBlueOrder";
    private static final String FACT_ID_FOR_WHITE = "TakeoutWhiteOrder";


    @Test
    public void testMerge() {
        TakeoutOrder storedOrder = TakeoutOrder.newBuilder()
                .setEventId(1)
                .addStatusHistory(createStatusHistory(1, "PROCESSING", 1))
                .setId(5656l)
                .setNotes("A")
                .build();
        TakeoutOrder newOrder = TakeoutOrder.newBuilder()
                .setEventId(2)
                .addStatusHistory(createStatusHistory(2, "CANCELLED", 2))
                .setId(5656l)
                .setNotes("B")
                .build();

        YieldMock collector = new YieldMock();
        new TakeoutWhiteOrderReducer().reduce(Lists.newArrayList(storedOrder), Collections.singleton(newOrder), collector);

        TakeoutOrder reduced = Iterables.get(collector.getAdded(FACT_ID_FOR_WHITE), 0);
        Assert.assertEquals(2, reduced.getEventId());
        Assert.assertEquals("B", reduced.getNotes());
    }

    /**
     * Проверяем, что если пришел новый статус, то он добавляется в историю.
     */
    @Test
    public void testMergeNewStatusHistory() {
        TakeoutOrder storedOrder = create(1, "PROCESSING", 1);
        TakeoutOrder newOrder = create(2, "CANCELLED", 2);

        YieldMock collector = new YieldMock();
        new TakeoutBlueOrderReducer().reduce(Lists.newArrayList(storedOrder), Collections.singleton(newOrder), collector);

        TakeoutOrder reduced = Iterables.get(collector.getAdded(FACT_ID_FOR_BLUE), 0);
        Assert.assertEquals(2, reduced.getStatusHistoryCount());
        assertStatusExists(reduced, "PROCESSING");
        assertStatusExists(reduced, "CANCELLED");
    }

    /**
     * Проверяем, что если пришел новый статус, то он добавляется в историю даже при неправильной очередности событий.
     */
    @Test
    public void testMergeNewStatusHistoryRevers() {
        TakeoutOrder storedOrder = create(2, "CANCELLED", 2);
        TakeoutOrder newOrder = create(1, "PROCESSING", 1);

        YieldMock collector = new YieldMock();
        new TakeoutRedOrderReducer().reduce(Lists.newArrayList(storedOrder), Collections.singleton(newOrder), collector);

        TakeoutOrder reduced = Iterables.get(collector.getAdded(FACT_ID_FOR_RED), 0);
        Assert.assertEquals(2, reduced.getStatusHistoryCount());
        assertStatusExists(reduced, "PROCESSING");
        assertStatusExists(reduced, "CANCELLED");
    }

    void assertStatusExists(TakeoutOrder o, String s) {
        boolean exists = o.getStatusHistoryList().stream().anyMatch(h -> s.equals(h.getStatus()));
        Assert.assertTrue("Должен пристутствовать статус " + s, exists);
    }

    TakeoutOrder create(long eventId, String status, long timestamp) {
        return createOrder(eventId, createStatusHistory(eventId, status, timestamp));
    }

    private TakeoutOrderHistory createHistory(long eventId, String type, long timestamp) {
        return TakeoutOrderHistory.newBuilder()
                .setEventId(eventId)
                .setType(type)
                .setTimestamp(timestamp)
                .build();
    }

    private TakeoutOrder createOrder(long eventId, TakeoutOrderHistory... hs) {
        TakeoutOrder.Builder b = TakeoutOrder.newBuilder()
                .setEventId(eventId);
        for (TakeoutOrderHistory h : hs) {
            b.addHistory(h);
        }
        return b.build();
    }

    private TakeoutOrder createOrder(long eventId, TakeoutOrderStatusHistory... hs) {
        TakeoutOrder.Builder b = TakeoutOrder.newBuilder()
                .setEventId(eventId);
        for (TakeoutOrderStatusHistory h : hs) {
            b.addStatusHistory(h);
        }
        return b.build();
    }

    private TakeoutOrderStatusHistory createStatusHistory(long eventId, String status, long timestamp) {
        return TakeoutOrderStatusHistory.newBuilder()
                .setEventId(eventId)
                .setStatus(status)
                .setTimestamp(timestamp)
                .build();
    }
}
