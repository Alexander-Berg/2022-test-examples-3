package ru.yandex.market.checkout.checkouter.storage;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReasonWithDetails;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderEventPublishService;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEventReasonDetails;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.ItemServiceUpdateService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorization;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorizationType;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.common.trace.TvmAuthorizationContextHolder;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.apache.curator.shaded.com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.ClientInfo.createFromJson;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.EXCEPTION_ON_SERVICE_TERMINAL_STATUS_CHANGE;

/**
 * @author apershukov
 */
public class OrderHistoryDaoTest extends AbstractServicesTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderSequences orderSequences;
    @Autowired
    private OrderHistoryDao orderHistoryDao;
    @Autowired
    private OrderEventPublishService orderEventPublishService;
    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    private ItemServiceUpdateService itemServiceUpdateService;
    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;

    /**
     * Если у заказа в событии есть трек но нет посылок, трек помещяется в фейковую посылку
     */
    @Test
    public void testOrderHistoryWithNoShipmentRecords() {
        Order order = orderServiceHelper.createOrder();

        Delivery delivery = new Delivery();
        delivery.setPrice(BigDecimal.valueOf(9.99));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        insertOldFashionedTrackHistory(order, "iddqd");

        OrderHistoryEvent event = getLastEvent(order);

        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), empty());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        Parcel shipment = deliveryAfter.getParcels().get(0);
        assertNull(shipment.getId());
        assertThat(shipment.getTracks(), hasSize(1));

        assertEquals("iddqd", shipment.getTracks().get(0).getTrackCode());
        assertEquals(123, (long) shipment.getTracks().get(0).getDeliveryServiceId());
    }

    /**
     * Есил у заказа в событии есть трек и посылка у которой есть соответствующая запись в таблице истории и при этом
     * трек привязан напрямую к доставке, трек привязывается к единственной посылке
     */
    @Test
    public void testOrderHistoryWithShipmentHistoryRecordButWithUnboundTracks() {
        Order order = orderServiceHelper.createPostOrder(o -> o.setDelivery(DeliveryProvider.yandexDelivery().build()));

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);

        Parcel shipment = new Parcel();
        shipment.setWidth(10L);
        shipment.setHeight(20L);

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(shipment));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long deliveryId = order.getInternalDeliveryId();
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.update(
                    "UPDATE ORDER_DELIVERY SET ORDER_SHIPMENT_ID = ? WHERE ID = ?",
                    shipmentId, deliveryId
            );
            return null;
        });

        insertOldFashionedTrackHistory(order, "iddqd");

        OrderHistoryEvent event = getLastEvent(order);

        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), empty());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        shipment = deliveryAfter.getParcels().get(0);
        assertEquals(shipmentId, (long) shipment.getId());
        assertEquals(10, (long) shipment.getWidth());
        assertEquals(20, (long) shipment.getHeight());
        assertThat(shipment.getTracks(), hasSize(1));

        assertEquals("iddqd", shipment.getTracks().get(0).getTrackCode());
        assertEquals(123, (long) shipment.getTracks().get(0).getDeliveryServiceId());
    }

    private OrderHistoryEvent getLastEvent(Order order) {
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        List<OrderHistoryEvent> events = orderHistoryDao.getOrderHistoryEvents(order.getId(), Pager.atPage(1, 10), null,
                ClientInfo.SYSTEM, null, null, false, null);

        return events.get(0);
    }

    private void insertOldFashionedTrackHistory(Order order, String trackCode) {
        long deliveryId = order.getInternalDeliveryId();
        long trackId = orderSequences.getNextTrackId();

        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.update(
                    "INSERT INTO DELIVERY_TRACK (ID, ORDER_ID, DELIVERY_ID, TRACK_CODE, DELIVERY_SERVICE_ID)\n" +
                            "VALUES (?, ?, ?, ?, ?)",
                    trackId, order.getId(), deliveryId, trackCode, 123
            );

            masterJdbcTemplate.update(
                    "INSERT INTO DELIVERY_TRACK_HISTORY (ID, TRACK_ID, DELIVERY_ID, STATUS) " +
                            "VALUES (?, ?, ?, ?)",
                    trackId, trackId, deliveryId, TrackStatus.NEW.getId()
            );
            return null;
        });
    }

    public static Stream<Arguments> terminalStatuses() {
        return Stream.of(new Object[]{ItemServiceStatus.COMPLETED, ItemServiceStatus.CANCELLED},
                        new Object[]{ItemServiceStatus.CANCELLED, ItemServiceStatus.COMPLETED},
                        new Object[]{ItemServiceStatus.CANCELLED, ItemServiceStatus.NEW})
                .map(Arguments::of);
    }

    @Test
    public void migrateOrderHistoryToOrderEvent() {
        Order order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new java.util.Date());
            o.setInternalDeliveryId(5678L);
        });
        orderInsertHelper.insertOrder(order);

        transactionTemplate.execute(tc -> {
            long historyId = orderHistoryDao.insertOrderHistory(
                    order, HistoryEventType.ORDER_SUBSTATUS_UPDATED, ClientInfo.SYSTEM
            );
            masterJdbcTemplate.update("delete from order_event where history_id = ?", historyId);
            masterJdbcTemplate.update("update order_history set event_id = ? where id = ?", 3456L, historyId);

            int migrated = orderHistoryDao.migrateOrderHistoryToOrderEvent(10);
            assertThat(migrated, equalTo(1));

            migrated = orderHistoryDao.migrateOrderHistoryToOrderEvent(10);
            assertThat(migrated, equalTo(0));
            return null;
        });
    }

    @Test
    public void testDeleteIncorrectEvents() {
        Order order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new Date());
            o.setInternalDeliveryId(5678L);
        });
        orderInsertHelper.insertOrder(order);

        transactionTemplate.execute(tc -> {
            long historyId = 123; // несуществующий historyId

            Long incorrectEventId = masterJdbcTemplate.queryForObject("insert into order_event " +
                            "(history_id, event_type, bucket, event_id) " +
                            "select ?, ?, ?, nextval('order_event_seq')" +
                            "returning event_id",
                    Long.class,
                    historyId,
                    HistoryEventType.ORDER_STATUS_UPDATED.getId(),
                    order.getId() % 60
            );
            assertNotNull(incorrectEventId);

            orderHistoryDao.deleteIncorrectEvents(incorrectEventId - 5, incorrectEventId + 5);

            Integer incorrectEventCount = masterJdbcTemplate.queryForObject("select count(*) from order_event where " +
                            "event_id = ?",
                    Integer.class, incorrectEventId);

            assertThat(incorrectEventCount, equalTo(0));
            return null;
        });
    }

    @Test
    public void shouldSaveInitiatorInfo() {
        try {
            long tvmClientId = 200300L;
            TvmAuthorization serviceTvmAuthorization = new TvmAuthorization(TvmAuthorizationType.SERVICE, tvmClientId);
            TvmAuthorizationContextHolder.setTvmAuthorization(serviceTvmAuthorization);

            Order order = OrderProvider.getBlueOrder(o -> {
                o.setFulfilment(true);
                o.setDelivery(DeliveryProvider.yandexDelivery().build());
            });
            orderServiceHelper.saveOrder(order);
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY, ClientInfo.SYSTEM);
            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED, ClientInfo.SYSTEM);

            orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
            orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
            List<OrderHistoryEvent> events = orderHistoryDao.getOrderHistoryEvents(order.getId(), Pager.atPage(1, 100),
                    null,
                    ClientInfo.SYSTEM, null, null, false, null);

            assertFalse(events.isEmpty(), "No history events were created");
            boolean allEventsSavedWithInitiatorInfo = events.stream()
                    .allMatch(e -> e.getInitiatorType() == TvmAuthorizationType.SERVICE
                            && e.getInitiatorId().equals(tvmClientId));
            assertTrue(allEventsSavedWithInitiatorInfo, "Wrong initiator info for some of the history events");
        } finally {
            TvmAuthorizationContextHolder.setTvmAuthorization(TvmAuthorization.notAuthorized());
        }
    }

    @ParameterizedTest
    @MethodSource("getReasonDetailsParams")
    void emptyReasonDetailsTest(boolean isDetailsEmpty, OrderHistoryEventReasonDetails reasonDetails) {
        Order order = OrderProvider.getBlueOrder();
        orderServiceHelper.saveOrder(order);
        orderUpdateService.cancelOrderWithReasonDetails(order.getId(), OrderSubstatus.CUSTOM, ClientInfo.SYSTEM,
                HistoryEventReasonWithDetails.withNullableReason(reasonDetails));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        Object lastEventReasonDetails = masterJdbcTemplate.queryForList(
                        "SELECT id, reason_details FROM order_history WHERE order_id = " + order.getId()).stream()
                .max(Comparator.comparing(recordMap -> ((Long) recordMap.get("id"))))
                .map(lastRecord -> lastRecord.get("reason_details"))
                .orElse(null);

        if (isDetailsEmpty) {
            assertNull(lastEventReasonDetails);
        } else {
            assertNotNull(lastEventReasonDetails);
        }
    }

    private static Stream<Arguments> getReasonDetailsParams() {
        return Stream.of(
                Arguments.of(true, getAnyReasonDetails(true)),
                Arguments.of(false, getAnyReasonDetails(false))
        );
    }

    @Nonnull
    private static OrderHistoryEventReasonDetails getAnyReasonDetails(boolean shouldBeEmpty) {
        OrderHistoryEventReasonDetails details;
        if (shouldBeEmpty) {
            details = new OrderHistoryEventReasonDetails(null);
            assertTrue(details.isEmpty());
        } else {
            details = new OrderHistoryEventReasonDetails(List.of());
            assertFalse(details.isEmpty());
        }
        return details;
    }

    @Test
    public void findCancellationRequestAuthorsShouldReturnEmptyResultOnEmptyDatabase() {
        final Map<Long, ClientInfo> result = orderHistoryDao.findCancellationRequestAuthors(Set.of(1L, 2L, 3L));
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void findCancellationRequestAuthorsShouldReturnOneRow() {
        final Order order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new Date());
            o.setInternalDeliveryId(5678L);
        });
        final long orderId = orderInsertHelper.insertOrder(order);

        final Long historyId = transactionTemplate.execute(tc -> orderHistoryDao.insertOrderHistory(
                order, HistoryEventType.ORDER_CANCELLATION_REQUESTED,
                new ClientInfo(ClientRole.USER, BuyerProvider.UID)));
        assertNotNull(historyId);

        final Map<Long, ClientInfo> result = orderHistoryDao.findCancellationRequestAuthors(Set.of(1L, 2L, orderId));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(createFromJson(ClientRole.USER, BuyerProvider.UID, BuyerProvider.UID, 0L, null),
                result.get(orderId));
    }

    @Test
    public void findCancellationRequestAuthorsShouldReturnOneRowForMultipleCancellations() {
        final Order order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new Date());
            o.setInternalDeliveryId(5678L);
        });
        final long orderId = orderInsertHelper.insertOrder(order);

        final Long historyId = transactionTemplate.execute(tc -> orderHistoryDao.insertOrderHistory(
                order, HistoryEventType.ORDER_CANCELLATION_REQUESTED,
                new ClientInfo(ClientRole.USER, BuyerProvider.UID)));
        assertNotNull(historyId);

        final Long historyId2 = transactionTemplate.execute(tc -> orderHistoryDao.insertOrderHistory(
                order, HistoryEventType.ORDER_CANCELLATION_REQUESTED,
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123L)));
        assertNotNull(historyId2);

        final Map<Long, ClientInfo> result = orderHistoryDao.findCancellationRequestAuthors(Set.of(1L, 2L, orderId));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123L, 0L, null),
                result.get(orderId));
    }

    @Test
    public void hasHistoryEventReasons() {
        Order order = orderServiceHelper.createOrder(Color.BLUE);
        assertFalse(orderHistoryDao.hasHistoryEventReasons(order.getId(),
                HistoryEventReason.SERVICE_FAULT_REASONS));

        transactionTemplate.execute(a -> {
            Long eventId = orderHistoryDao.insertOrderHistory(order, HistoryEventType.ORDER_DELIVERY_UPDATED,
                    ClientInfo.SYSTEM,
                    HistoryEventReason.DELAYED_DUE_EXTERNAL_CONDITIONS);
            assertNotNull(eventId);
            return null;
        });

        assertTrue(orderHistoryDao.hasHistoryEventReasons(order.getId(),
                HistoryEventReason.SERVICE_FAULT_REASONS));
    }

    @Test
    public void testPublishPendingEvents() {
        assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.of(1, 5)).isEmpty());
        assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.NULL).isEmpty());

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new java.util.Date());
            o.setPartitionIndex(1);
            o.setInternalDeliveryId(5678L);
        });
        orderInsertHelper.insertOrder(order);
        transactionTemplate.execute(a -> {
            assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.NULL).isEmpty());
            assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.of(2, 3)).isEmpty());
            assertEquals(1, orderHistoryDao.publishPendingEvents(100, Partition.of(1, 2)).size());
            return null;
        });

        order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new java.util.Date());
            o.setPartitionIndex(null);
            o.setInternalDeliveryId(5678L);
        });
        orderInsertHelper.insertOrder(order);
        transactionTemplate.execute(a -> {
            assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.of(1, 3)).isEmpty());
            assertEquals(1, orderHistoryDao.publishPendingEvents(100, Partition.NULL).size());
            return null;
        });

        order = OrderProvider.getBlueOrder((o) -> {
            o.setStatus(OrderStatus.PROCESSING);
            o.setUpdateDate(new java.util.Date());
            o.setPartitionIndex(null);
            o.setInternalDeliveryId(5678L);
        });
        orderInsertHelper.insertOrder(order);
        transactionTemplate.execute(a -> {
            assertTrue(orderHistoryDao.publishPendingEvents(100, Partition.of(1, 3)).isEmpty());
            assertEquals(1, orderHistoryDao.publishPendingEvents(100, Partition.NULL).size());
            return null;
        });
    }

    @Test
    public void getOrderHistoryEventsShouldFetchItemServices() {
        Order order = orderServiceHelper.createPostOrder(orderBeforeSave ->
                getOnlyElement(orderBeforeSave.getItems())
                        .addService(ItemServiceProvider.defaultItemService()));
        Long orderId = order.getId();

        OrderItem orderItem = Iterables.getLast(order.getItems());
        ItemService itemService = Iterables.getLast(orderItem.getServices());
        Date dateBefore = itemService.getDate();
        LocalTime fromTime = itemService.getFromTime();
        LocalTime toTime = itemService.getToTime();
        Date dateAfter = new Date();
        itemServiceUpdateService.assignTimeSlot(order.getId(), itemService.getId(), dateAfter,
                ClientInfo.SYSTEM);

        OrderHistoryEvent event = orderHistoryEventsTestHelper.getEventsOfType(orderId,
                        HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED)
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ITEM_SERVICE_TIMESLOT_ASSIGNED)
                .findFirst()
                .orElse(null);

        assertNotNull(event);

        assertEquals(itemService.getId(), event.getItemServiceId());

        Set<ItemService> servicesBefore = event.getOrderBefore().getItem(orderItem.getId()).getServices();
        assertThat(servicesBefore, hasSize(1));
        ItemService serviceBefore = Iterables.getOnlyElement(servicesBefore);
        assertEquals(itemService.getId(), serviceBefore.getId());
        assertEquals(dateBefore, serviceBefore.getDate());
        assertEquals(fromTime, serviceBefore.getFromTime());
        assertEquals(toTime, serviceBefore.getToTime());

        Set<ItemService> servicesAfter = event.getOrderAfter().getItem(orderItem.getId()).getServices();
        assertThat(servicesAfter, hasSize(1));
        ItemService serviceAfter = Iterables.getOnlyElement(servicesAfter);
        assertEquals(itemService.getId(), serviceAfter.getId());
        assertEquals(dateAfter, serviceAfter.getDate());
        assertEquals(fromTime, serviceAfter.getFromTime());
        assertEquals(toTime, serviceAfter.getToTime());
    }

    @ParameterizedTest(name = "Status: {0}, to {1}")
    @MethodSource("terminalStatuses")
    public void checkTerminalStatusExceptionTest(ItemServiceStatus cur, ItemServiceStatus to) {
        checkouterFeatureWriter.writeValue(EXCEPTION_ON_SERVICE_TERMINAL_STATUS_CHANGE, true);
        Order order = orderServiceHelper.createPostOrder(orderBeforeSave ->
                getOnlyElement(orderBeforeSave.getItems())
                        .addService(ItemServiceProvider.defaultItemService()));

        OrderItem orderItem = Iterables.getLast(order.getItems());
        ItemService itemService = Iterables.getLast(orderItem.getServices());
        itemService.setStatus(cur);

        Assertions.assertThrows(ItemServiceUpdateNotAllowedException.class,
                () -> itemServiceUpdateService.updateItemServiceStatus(order, itemService, to, ClientInfo.SYSTEM));
    }
}
