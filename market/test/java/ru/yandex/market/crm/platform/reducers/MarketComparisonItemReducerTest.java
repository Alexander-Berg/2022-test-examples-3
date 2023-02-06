package ru.yandex.market.crm.platform.reducers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.MarketComparisonItem;

import static org.junit.Assert.assertEquals;

public class MarketComparisonItemReducerTest {
    private final MarketComparisonItemReducer reducer = new MarketComparisonItemReducer();
    private YieldMock collector = new YieldMock();

    @Test
    public void testReceiveNewComparisonItemAdded() {
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        reducer.reduce(Collections.emptyList(), Collections.singleton(expected), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(1, added.size());
        assertEquals(expected, added.iterator().next());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(1, addedActions.size());
        assertEquals(expected, addedActions.iterator().next());
    }

    @Test
    public void testReceiveNewComparisonItemRemoved() {
        MarketComparisonItem expected = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926004L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        reducer.reduce(Collections.emptyList(), Collections.singleton(expected), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(0, added.size());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(1, addedActions.size());
        assertEquals(expected, addedActions.iterator().next());
    }

    @Test
    public void testReceiveComparisonItemRemoved() {
        MarketComparisonItem old = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        MarketComparisonItem newest = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998927000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        reducer.reduce(Collections.singletonList(old), Collections.singleton(newest), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(0, added.size());

        Collection<MarketComparisonItem> removed = collector.getRemoved("MarketComparisonItem");
        assertEquals(1, removed.size());
        assertEquals(old, removed.iterator().next());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(1, addedActions.size());
        assertEquals(newest, addedActions.iterator().next());
    }

    @Test
    public void testReceiveUpdate() {
        MarketComparisonItem old = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        MarketComparisonItem new1 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998927000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        MarketComparisonItem new2 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998928000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        MarketComparisonItem new3 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998929000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        MarketComparisonItem new4 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998930000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        reducer.reduce(Collections.singletonList(old), Arrays.asList(new1, new2, new3, new4), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(0, added.size());

        Collection<MarketComparisonItem> removed = collector.getRemoved("MarketComparisonItem");
        assertEquals(1, removed.size());
        assertEquals(old, removed.iterator().next());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(4, addedActions.size());

        Iterator<MarketComparisonItem> iterator = addedActions.iterator();
        assertEquals(new1, iterator.next());
        assertEquals(new2, iterator.next());
        assertEquals(new3, iterator.next());
        assertEquals(new4, iterator.next());
    }

    @Test
    public void testReceiveRemoveAndAddAtTheSameTime() {
        MarketComparisonItem new1 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        MarketComparisonItem new2 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        reducer.reduce(Collections.emptyList(), Arrays.asList(new1, new2), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(0, added.size());

        Collection<MarketComparisonItem> removed = collector.getRemoved("MarketComparisonItem");
        assertEquals(0, removed.size());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(2, addedActions.size());

        Iterator<MarketComparisonItem> iterator = addedActions.iterator();
        assertEquals(new2, iterator.next());
        assertEquals(new1, iterator.next());
    }

    @Test
    public void testReceiveRemoveAndUpdateAtTheSameTime() {
        MarketComparisonItem new2 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998927000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        MarketComparisonItem new1 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();

        MarketComparisonItem new3 = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998927000L)
            .setLastAction(MarketComparisonItem.ActionType.UPDATE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();


        reducer.reduce(Collections.emptyList(), Arrays.asList(new1, new2, new3), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(0, added.size());

        Collection<MarketComparisonItem> removed = collector.getRemoved("MarketComparisonItem");
        assertEquals(0, removed.size());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(3, addedActions.size());

        Iterator<MarketComparisonItem> iterator = addedActions.iterator();
        assertEquals(new1, iterator.next());
        assertEquals(new3, iterator.next());
        assertEquals(new2, iterator.next());
    }

    @Test
    public void testReceiveRemoveAndAddAtTheSameTimeWithDifferentId() {
        MarketComparisonItem first = MarketComparisonItem.newBuilder()
            .setId(1L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.REMOVE)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setProductId(2)
            .build();

        MarketComparisonItem second = MarketComparisonItem.newBuilder()
            .setId(2L)
            .setActionTime(1566998926000L)
            .setLastAction(MarketComparisonItem.ActionType.ADD)
            .setUid(Uids.create(UidType.PUID, 12345))
            .setCategoryId("1")
            .setProductId(2)
            .build();


        reducer.reduce(Collections.emptyList(), Arrays.asList(second, first), collector);

        Collection<MarketComparisonItem> added = collector.getAdded("MarketComparisonItem");
        assertEquals(1, added.size());
        assertEquals(second, added.iterator().next());

        Collection<MarketComparisonItem> removed = collector.getRemoved("MarketComparisonItem");
        assertEquals(0, removed.size());

        Collection<MarketComparisonItem> addedActions = collector.getAdded("MarketComparisonItemAction");
        assertEquals(2, addedActions.size());

        Iterator<MarketComparisonItem> iterator = addedActions.iterator();
        assertEquals(first, iterator.next());
        assertEquals(second, iterator.next());
    }
}
