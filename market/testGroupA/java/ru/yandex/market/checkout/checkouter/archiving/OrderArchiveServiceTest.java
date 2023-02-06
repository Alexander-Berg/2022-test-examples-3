package ru.yandex.market.checkout.checkouter.archiving;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveContext;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveRequest;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveService;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class OrderArchiveServiceTest extends AbstractArchiveWebTestBase {

    @Autowired
    private OrderArchiveService orderArchiveService;
    @Autowired
    private OrderInsertHelper orderInsertHelper;

    @Test
    void shouldArchivePrepaidDeliveredOrder() {
        freezeTime();
        Order order = completeOrder();
        jumpToFuture(31, ChronoUnit.DAYS);
        assertOrderMeetsArchivingRequirements(order);
    }

    @Test
    void shouldArchivePostpaidDeliveredOrder() throws Exception {
        freezeTime();
        Order order = completePostpaidOrder();
        jumpToFuture(31, ChronoUnit.DAYS);
        assertOrderMeetsArchivingRequirements(order);
    }

    @Test
    void shouldArchiveDeliveredMultiOrder() throws Exception {
        freezeTime();

        List<Order> ordersInMultiOrder = createMultiOrder();

        long multiOrderCreationStartedTimestamp = ordersInMultiOrder.stream()
                .mapToLong(o -> o.getCreationDate().getTime()).min().getAsLong();

        jumpToFuture(31, ChronoUnit.DAYS);

        OrderArchiveRequest request = new OrderArchiveRequest(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(multiOrderCreationStartedTimestamp), getClock().getZone()),
                BATCH_PERIOD,
                MAX_INTERVAL_BETWEEN_ORDERS_CREATION_IN_MULTI_ORDER,
                BATCH_SIZE,
                LocalDateTime.now(getClock()),
                LocalDateTime.now(getClock()),
                Set.of()
        );

        Set<Long> orders = orderArchiveService.getOrderIdsForArchiving(request);

        assertThat(orders.size(), equalTo(2));
        assertThat(request.getRealBatchSize(), equalTo(2L));
    }

    @Test
    void shouldArchiveWholeMultiOrder() throws Exception {
        freezeTime();

        Order order = completeOrder();

        List<Order> ordersInMultiOrder = createMultiOrder();

        long multiOrderCreationStartedTimestamp = ordersInMultiOrder.get(0).getCreationDate().getTime();
        long delta = 5L;

        moveSecondOrderInMultiOrderCreatedAtByDelta(ordersInMultiOrder, multiOrderCreationStartedTimestamp, delta);

        jumpToFuture(31, ChronoUnit.DAYS);

        OrderArchiveRequest request = new OrderArchiveRequest(
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(multiOrderCreationStartedTimestamp - BATCH_PERIOD * 1000 + delta),
                        getClock().getZone()
                ),
                BATCH_PERIOD, MAX_INTERVAL_BETWEEN_ORDERS_CREATION_IN_MULTI_ORDER, BATCH_SIZE,
                LocalDateTime.now(getClock()), LocalDateTime.now(getClock()),
                Set.of()
        );


        Set<Long> orders = orderArchiveService.getOrderIdsForArchiving(request);

        assertThat(orders.size(), equalTo(1));
        assertThat(orders.iterator().next(), equalTo(order.getId()));
        assertThat(request.getRealBatchSize(), equalTo(2L));
    }

    @Test
    void shouldArchiveCreditOrder() throws IOException {
        freezeTime();
        Order order = completeCreditOrder();
        jumpToFuture(31, ChronoUnit.DAYS);
        assertOrderMeetsArchivingRequirements(order);
    }

    private void assertOrderMeetsArchivingRequirements(Order order) {
        OrderArchiveRequest request = new OrderArchiveRequest(
                LocalDateTime.ofInstant(order.getCreationDate().toInstant(), getClock().getZone()),
                BATCH_PERIOD, MAX_INTERVAL_BETWEEN_ORDERS_CREATION_IN_MULTI_ORDER, BATCH_SIZE,
                LocalDateTime.now(getClock()), LocalDateTime.now(getClock()),
                Set.of()
        );

        Set<Long> orders = orderArchiveService.getOrderIdsForArchiving(request);

        assertThat(orders.size(), equalTo(1));
        assertThat(request.getRealBatchSize(), equalTo(1L));
    }

    @Test
    void shouldArchivingSetCorrectPartitionIndexToOldMultiOrders() {
        // создаём заказы вставкой в БД, а не через /checkout, чтобы оставить partition_index незаполненным
        String multiOrderId = UUID.randomUUID().toString();
        Order order1 = OrderProvider.getBlueOrder();
        assertThat(order1.getPartitionIndex(), nullValue());
        order1.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
        orderInsertHelper.insertOrder(1, order1);

        Order order2 = OrderProvider.getBlueOrder();
        assertThat(order2.getPartitionIndex(), nullValue());
        order2.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
        orderInsertHelper.insertOrder(2, order2);

        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERED);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);

        OrderArchiveRequest archiveRequest = new OrderArchiveRequest(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1),
                true
        );
        orderArchiveService.archiveOrders(Set.of(order1.getId(), order2.getId()), archiveRequest);

        Map<Long, Order> result;
        try {
            ArchiveContext.setArchived(true);
            result = orderService.getOrders(List.of(1L, 2L), ClientInfo.SYSTEM);
        } finally {
            ArchiveContext.setArchived(null);
        }
        assertThat(result.size(), equalTo(2));
        Integer partitionIndex1 = result.get(1L).getPartitionIndex();
        assertThat(partitionIndex1, notNullValue());
        Integer partitionIndex2 = result.get(2L).getPartitionIndex();
        assertThat(partitionIndex2, notNullValue());
        assertThat(partitionIndex1, equalTo(partitionIndex2));
    }
}
