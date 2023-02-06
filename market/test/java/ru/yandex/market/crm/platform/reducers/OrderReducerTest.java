package ru.yandex.market.crm.platform.reducers;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.commons.StartrekTicket;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderDelivery;
import ru.yandex.market.crm.platform.models.OrderDeliveryUpdate;
import ru.yandex.market.crm.platform.models.OrderHistory;
import ru.yandex.market.crm.platform.models.OrderProperty;
import ru.yandex.market.crm.platform.models.OrderStatusHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrderReducerTest {

    @Test
    public void mergeComplaints() {
        Order storedOrder =
                Order.newBuilder()
                        .setId(5656L)
                        .setSource(Order.Source.LOG_BROKER)
                        .setUserIds(UserIds.newBuilder().setEmail("5656@mail.ru").build())
                        .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                        .addComplaints(0,
                                StartrekTicket.newBuilder()
                                        .setId("3")
                                        .setSubject("Some problem happened")
                                        .setType("Задача")
                                        .setStatus("В работе")
                                        .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                        .setUpdateAt(Instant.parse("2018-12-21T10:55:30.00Z").getMillis())
                                        .build()).build();

        Order newOrder = Order.newBuilder()
                .setId(5656L)
                .setSource(Order.Source.STARTREK)
                .setUserIds(UserIds.newBuilder().setEmail("lalala@mail.ru").build())
                .setCreationDate(Instant.parse("2018-12-21T10:55:30.00Z").getMillis())
                .addComplaints(0,
                        StartrekTicket.newBuilder()
                                .setId("3")
                                .setSubject("Some problem happened!!!!!!!!")
                                .setType("Задача")
                                .setStatus("В работе")
                                .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                .setUpdateAt(Instant.parse("2018-12-21T10:55:30.00Z").getMillis())
                                .build()).build();

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(1, reduced.getComplaintsCount());
        assertEquals(Instant.parse("2018-12-21T10:50:30.00Z").getMillis(), reduced.getCreationDate());
        assertEquals("3", reduced.getComplaints(0).getId());
        assertEquals("Some problem happened!!!!!!!!", reduced.getComplaints(0).getSubject());
        assertEquals(Instant.parse("2018-12-21T10:55:30.00Z").getMillis(),
                reduced.getComplaints(0).getUpdateAt());
    }


    @Test
    public void mergeComplaintsReverse() {
        Order storedOrder = Order.newBuilder()
                .setId(5656L)
                .setSource(Order.Source.STARTREK)
                .setUserIds(UserIds.newBuilder().setEmail("5656@mail.ru").build())
                .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                .addComplaints(0,
                        StartrekTicket.newBuilder()
                                .setId("3")
                                .setSubject("Some problem happened!!!!!!!!")
                                .setType("Задача")
                                .setStatus("В работе")
                                .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                .setUpdateAt(Instant.parse("2018-12-21T10:55:30.00Z").getMillis())
                                .build()).build();

        Order newOrder =
                Order.newBuilder()
                        .setId(5656L)
                        .setSource(Order.Source.LOG_BROKER)
                        .setUserIds(UserIds.newBuilder().setEmail("lalala@mail.ru").build())
                        .setCreationDate(Instant.parse("2018-12-21T10:55:30.00Z").getMillis()).build();

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(1, reduced.getComplaintsCount());
        assertEquals("lalala@mail.ru", reduced.getUserIds().getEmail());
        assertEquals(Instant.parse("2018-12-21T10:55:30.00Z").getMillis(), reduced.getCreationDate());
        assertEquals("3", reduced.getComplaints(0).getId());
        assertEquals("Some problem happened!!!!!!!!", reduced.getComplaints(0).getSubject());
        assertEquals(Instant.parse("2018-12-21T10:55:30.00Z").getMillis(),
                reduced.getComplaints(0).getUpdateAt());
    }
    /**
     * Проверяем, что история событий будет мержится правильно во время переимпорта после добавления eventId к истории
     * изменения статусов.
     */
    @Test
    public void mergeExistedHistory() {
        Order storedOrder = createOrder(10,
                createHistory(1, "NEW_ORDER", 1),
                createHistory(2, "ORDER_STATUS_UPDATED", 2)
        );
        Order newOrder = createOrder(2, createHistory(3, "ORDER_STATUS_UPDATED", 3));

        Order reduced = reduce(storedOrder, newOrder);

        OrderHistory h = reduced.getHistory(2);
        assertEquals("Должен сохраняться с информацией о событии", 3, h.getEventId());
        assertEquals("ORDER_STATUS_UPDATED", h.getType());
        assertEquals(3, h.getTimestamp());
    }

    /**
     * Если событие с тем же id приходит повторно оно подменяется в истории на более новое
     */
    @Test
    public void testUpdateAHistoryRecord() {
        Order storedOrder = createOrder(10,
                createHistory(1, "NEW_ORDER", 10),
                createHistory(2, "ORDER_STATUS_UPDATED", 20),
                createHistory(3, "ORDER_STATUS_UPDATED", 30)
        );
        Order newOrder = createOrder(20, createHistory(2, "ORDER_STATUS_UPDATED", 25));

        Order reduced = reduce(storedOrder, newOrder);
        assertEquals(3, reduced.getHistoryCount());

        OrderHistory event = reduced.getHistory(1);
        assertEquals(25, event.getTimestamp());
    }

    private static Order reduce(Order storedOrder, Order newOrder) {
        YieldMock collector = new YieldMock();
        new OrderReducer().reduce(List.of(storedOrder), List.of(newOrder), collector);

        return Iterables.get(collector.getAdded("Order"), 0);
    }

    /**
     * Проверяем, что если пришел новый статус, то он добавляется в историю.
     */
    @Test
    public void mergeExistedStatusHistory() {
        Order storedOrder = create(1, "PROCESSING", 1);
        Order newOrder = create(2, "PROCESSING", 2);

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(1, reduced.getStatusHistoryCount());
        assertStatusExists(reduced, "PROCESSING");

        long eventId = reduced.getStatusHistory(0).getEventId();
        assertEquals("Должен сохраняться первый переход в статус", 1, eventId);
    }

    /**
     * Проверяем, что история событий будет мержится правильно во время переимпорта после добавления eventId к истории
     * изменения статусов.
     */
    @Test
    public void mergeExistedStatusHistory2() {
        Order storedOrder = createOrder(10,
                createStatusHistory(0, "PROCESSING", 1),
                createStatusHistory(0, "PENDING", 2),
                createStatusHistory(0, "DELIVERY", 3)
        );
        Order newOrder = createOrder(2, createStatusHistory(2, "PENDING", 2));

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(3, reduced.getStatusHistoryCount());

        OrderStatusHistory h = reduced.getStatusHistory(1);
        assertEquals("Должен сохраняться с информацией о событии в котором произошло изменение статуса", 2,
                h.getEventId());
        assertEquals("PENDING", h.getStatus());
    }

    @Test
    public void mergeNewComplaints() {
        Order storedOrder =
                Order.newBuilder()
                        .setId(5656L)
                        .setSource(Order.Source.LOG_BROKER)
                        .setUserIds(UserIds.newBuilder().setEmail("5656@mail.ru").build())
                        .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                        .addComplaints(0,
                                StartrekTicket.newBuilder()
                                        .setId("3")
                                        .setSubject("Some problem happened")
                                        .setType("Задача")
                                        .setStatus("В работе")
                                        .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                        .setUpdateAt(Instant.parse("2018-12-21T10:30:30.00Z").getMillis())
                                        .build()).build();

        Order newOrder = Order.newBuilder()
                .setId(5657L)
                .setSource(Order.Source.STARTREK)
                .setUserIds(UserIds.newBuilder().setEmail("lalala@mail.ru").build())
                .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                .addComplaints(0,
                        StartrekTicket.newBuilder()
                                .setId("1")
                                .setSubject("Some problem happened!!!!!!!!")
                                .setType("Задача")
                                .setStatus("В работе")
                                .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                .setUpdateAt(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                                .build()).build();

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(2, reduced.getComplaintsCount());
        assertEquals("3", reduced.getComplaints(0).getId());
        assertEquals("1", reduced.getComplaints(1).getId());
    }

    @Test
    public void mergeNewComplaintsReverse() {
        Order storedOrder =
                Order.newBuilder()
                        .setId(5656L)
                        .setSource(Order.Source.LOG_BROKER)
                        .setUserIds(UserIds.newBuilder().setEmail("5656@mail.ru").build())
                        .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                        .addComplaints(0,
                                StartrekTicket.newBuilder()
                                        .setId("3")
                                        .setSubject("Some problem happened")
                                        .setType("Задача")
                                        .setStatus("В работе")
                                        .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                        .setUpdateAt(Instant.parse("2018-12-21T10:55:30.00Z").getMillis())
                                        .build()).build();

        Order newOrder = Order.newBuilder()
                .setId(5656L)
                .setSource(Order.Source.STARTREK)
                .setUserIds(UserIds.newBuilder().setEmail("lalala@mail.ru").build())
                .setCreationDate(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                .addComplaints(0,
                        StartrekTicket.newBuilder()
                                .setId("1")
                                .setSubject("Some problem happened!!!!!!!!")
                                .setType("Задача")
                                .setStatus("В работе")
                                .setCreatedAt(Instant.parse("2018-12-21T10:45:30.00Z").getMillis())
                                .setUpdateAt(Instant.parse("2018-12-21T10:50:30.00Z").getMillis())
                                .build()).build();

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(2, reduced.getComplaintsCount());
        assertEquals("1", reduced.getComplaints(0).getId());
        assertEquals("3", reduced.getComplaints(1).getId());
    }

    /**
     * Проверяем, что если пришел новый статус, то он добавляется в историю.
     */
    @Test
    public void mergeNewStatusHistory() {
        Order storedOrder = create(1, "PROCESSING", 1);
        Order newOrder = create(2, "CANCELLED", 2);

        YieldMock collector = new YieldMock();
        new OrderReducer().reduce(Lists.newArrayList(storedOrder), Collections.singleton(newOrder), collector);

        Order reduced = Iterables.get(collector.getAdded("Order"), 0);
        assertEquals(2, reduced.getStatusHistoryCount());
        assertStatusExists(reduced, "PROCESSING");
        assertStatusExists(reduced, "CANCELLED");
    }

    /**
     * Проверяем, что если пришел новый статус, то он добавляется в историю даже при неправильной очередности событий.
     */
    @Test
    public void mergeNewStatusHistoryRevers() {
        Order storedOrder = create(2, "CANCELLED", 2);
        Order newOrder = create(1, "PROCESSING", 1);

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(2, reduced.getStatusHistoryCount());
        assertStatusExists(reduced, "PROCESSING");
        assertStatusExists(reduced, "CANCELLED");
    }

    /**
     * Проверяем, что новое событие без изменения доставки не перетирает текущее.
     */
    @Test
    public void newEventDoesntEraseLastDeliveryUpdate() {
        OrderDeliveryUpdate storedDeliveryUpdate = OrderDeliveryUpdate.newBuilder()
            .setEventId(5)
            .setBefore(OrderDelivery.newBuilder().setFromDate("01-01-2019").setToDate("02-01-2019"))
            .setAfter(OrderDelivery.newBuilder().setFromDate("02-01-2019").setToDate("03-01-2019"))
            .build();

        Order storedOrder = create(1, "PROCESSING", 1).toBuilder()
            .setLastOrderDeliveryUpdate(storedDeliveryUpdate).build();
        Order newOrder = create(2, "CANCELLED", 2);

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(storedDeliveryUpdate, reduced.getLastOrderDeliveryUpdate());
        assertStatusExists(reduced, "PROCESSING");
        assertStatusExists(reduced, "CANCELLED");
    }

    /**
     * Проверяем, что новое событие устанавливает последнее актульное изменение доставки.
     */
    @Test
    public void newEventUpdatesLastDeliveryUpdate() {
        OrderDeliveryUpdate storedDeliveryUpdate = OrderDeliveryUpdate.newBuilder()
            .setEventId(2)
            .setBefore(OrderDelivery.newBuilder().setFromDate("01-01-2019").setToDate("02-01-2019"))
            .setAfter(OrderDelivery.newBuilder().setFromDate("02-01-2019").setToDate("03-01-2019")
                                                .setFromTime("10:00").setToTime("18:00"))
            .build();

        OrderDeliveryUpdate newDeliveryUpdate = OrderDeliveryUpdate.newBuilder()
            .setEventId(7)
            .setBefore(OrderDelivery.newBuilder().setFromDate("04-01-2019").setToDate("05-01-2019"))
            .setAfter(OrderDelivery.newBuilder().setFromDate("05-01-2019").setToDate("06-01-2019"))
            .build();

        Order storedOrder = Order.newBuilder().setId(1).setEventId(4).setLastOrderDeliveryUpdate(storedDeliveryUpdate).build();
        Order newOrder = Order.newBuilder().setId(1).setEventId(7).setLastOrderDeliveryUpdate(newDeliveryUpdate).build();

        Order reduced = reduce(storedOrder, newOrder);

        assertEquals(newDeliveryUpdate, reduced.getLastOrderDeliveryUpdate());
    }

    /**
     * Проверяем, что изменение доставки не устанавливается, если его не было ни в одном факте.
     */
    @Test
    public void noDeliveryUpdateIsSet() {
        Order storedOrder = create(1, "PROCESSING", 1);
        Order newOrder = create(2, "CANCELLED", 2);

        YieldMock collector = new YieldMock();
        new OrderReducer().reduce(Lists.newArrayList(storedOrder), Collections.singleton(newOrder), collector);

        Order reduced = Iterables.get(collector.getAdded("Order"), 0);
        Assert.assertFalse(reduced.hasLastOrderDeliveryUpdate());
    }

    @Test
    public void oldOrderFactRemovedTest() {
        Order oldOrder = Order.newBuilder()
                .setEventId(1)
                .addHistory(createHistory(1, "ORDER_STATUS_UPDATED", 2))
                .setCreationDate(1L).build();
        Order newOrder = Order.newBuilder()
                .setEventId(2)
                .addHistory(createHistory(2, "ORDER_STATUS_UPDATED", 3))
                .setCreationDate(2L).build();

        YieldMock collector = new YieldMock();
        new OrderReducer().reduce(Collections.singletonList(oldOrder), Collections.singletonList(newOrder), collector);

        assertEquals(1, collector.getAdded("Order").size());
        Order added = Iterables.get(collector.getAdded("Order"), 0);
        assertEquals(2L, added.getEventId());

        assertEquals(1, collector.getRemoved("Order").size());
        Order deleted = Iterables.get(collector.getRemoved("Order"), 0);
        assertEquals(1L, deleted.getEventId());
    }

    @Test
    public void updatedProperties() {
        OrderProperty op1 = OrderProperty.newBuilder().setKey("key1").setTextValue("value1").build();
        Order oldOrder = Order.newBuilder()
                .setEventId(1L)
                .addOrderProperties(op1)
                .build();
        OrderProperty op2 = OrderProperty.newBuilder().setKey("key2").setTextValue("value2").build();
        Order newOrder = Order.newBuilder()
                .setEventId(2L)
                .addOrderProperties(op2)
                .setCreationDate(2L).build();

        YieldMock collector = new YieldMock();
        new OrderReducer().reduce(Collections.singletonList(oldOrder), Collections.singletonList(newOrder), collector);

        assertEquals(1, collector.getAdded("Order").size());
        Order added = Iterables.get(collector.getAdded("Order"), 0);
        assertEquals(2L, added.getEventId());

        Order reduced = Iterables.get(collector.getAdded("Order"), 0);
        assertTrue(reduced.getOrderPropertiesList().contains(op2));
        assertFalse(reduced.getOrderPropertiesList().contains(op1));
    }


    private void assertStatusExists(Order o, String s) {
        boolean exists = Iterables.any(o.getStatusHistoryList(), h -> s.equals(h.getStatus()));
        Assert.assertTrue("Должен пристутствовать статус " + s, exists);
    }

    private Order create(long eventId, String status, long timestamp) {
        return createOrder(eventId, createStatusHistory(eventId, status, timestamp));
    }

    private OrderHistory createHistory(long eventId, String type, long timestamp) {
        return OrderHistory.newBuilder()
                .setEventId(eventId)
                .setType(type)
                .setTimestamp(timestamp)
                .build();
    }

    private Order createOrder(long eventId, OrderHistory... hs) {
        Order.Builder b = Order.newBuilder()
                .setEventId(eventId);
        for (OrderHistory h : hs) {
            b.addHistory(h);
        }
        return b.build();
    }

    private Order createOrder(long eventId, OrderStatusHistory... hs) {
        Order.Builder b = Order.newBuilder()
                .setEventId(eventId);
        for (OrderStatusHistory h : hs) {
            b.addStatusHistory(h);
        }
        return b.build();
    }

    private OrderStatusHistory createStatusHistory(long eventId, String status, long timestamp) {
        return OrderStatusHistory.newBuilder()
                .setEventId(eventId)
                .setStatus(status)
                .setTimestamp(timestamp)
                .build();
    }
}
