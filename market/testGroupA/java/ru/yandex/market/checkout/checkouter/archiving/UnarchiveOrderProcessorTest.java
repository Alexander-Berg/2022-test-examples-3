package ru.yandex.market.checkout.checkouter.archiving;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.archive.OrderMovingService;
import ru.yandex.market.checkout.checkouter.order.archive.requests.MultiOrderMovingRequest;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order.UnarchiveOrderProcessor;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection.BASIC_TO_ARCHIVE;

public class UnarchiveOrderProcessorTest extends AbstractArchiveWebTestBase {

    private static final String ORDER_EXIST_QUERY = "select 1 from orders where is_archived = false and id in (%s)";
    private static final String DEARCHIVED_EVENT_EXIST_QUERY = "select 1 " +
            "from order_event oe " +
            "join order_history oh on oh.id = oe.history_id " +
            "where oh.order_id in (%s) and oe.event_type = 9";

    @Autowired
    private UnarchiveOrderProcessor unarchiveOrderProcessor;

    @Autowired
    private OrderMovingService orderMovingService;

    @Test
    @DisplayName("POSITIVE: Успешная разархивация перемещенного синего заказа")
    void unarchiveOrderTest() {
        Order order = createArchivedOrder();
        moveArchivedOrders();

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                order.getId(),
                null,
                0,
                Instant.now(),
                order.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(order);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    @Test
    @DisplayName("POSITIVE: Успешная разархивация перемещенного синего мультизаказа")
    void unarchiveMultiOrderTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        Order first = orders.get(0);
        Set<Long> orderIds = orders.stream().map(BasicOrder::getId).collect(Collectors.toSet());
        moveArchivedOrders();

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                first.getId(),
                null,
                0,
                Instant.now(),
                first.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(orderIds);
        orderIds.forEach(id -> checkOrderRecordsExistence(StorageType.ARCHIVE, id, false));
    }

    @Test
    @DisplayName("POSITIVE: Попытка разархивации несуществующего заказа")
    void unarchiveNotExistingOrderTest() {
        Order archivedOrder = createArchivedOrder();
        Order order = completeOrder();

        archiveOrders(Set.of(archivedOrder.getId()));
        moveArchivedOrders();

        Set<Long> orderIds = Set.of(order.getId(), archivedOrder.getId());
        String orderIdsString = orderIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                RandomUtils.nextLong(),
                null,
                0,
                Instant.now(),
                order.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        checkOrderRecordsExistence(StorageType.BASIC, order.getId(), true);
        checkOrderRecordsExistence(StorageType.ARCHIVE, archivedOrder.getId(), true);
        assertTrue(
                masterJdbcTemplate.queryForList(String.format(DEARCHIVED_EVENT_EXIST_QUERY, orderIdsString)).isEmpty()
        );
    }

    @Test
    @DisplayName("POSITIVE: Успешная разархивация скопированного, но не удаленного синего заказа")
    void unarchiveNotMovedOrderTest() {
        Order order = createArchivedOrder();
        copyArchivingData(BASIC_TO_ARCHIVE);

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                order.getId(),
                null,
                0,
                Instant.now(),
                order.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(order);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    @Test
    @DisplayName("POSITIVE: Успешная разархивация скопированного, но не удаленного синего мультизаказа")
    void unarchiveNotMovedMultiOrderTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        Order first = orders.get(0);
        String multiOrderId = Objects.requireNonNull(first.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        Set<Long> orderIds = orders.stream().map(BasicOrder::getId).collect(Collectors.toSet());

        orderMovingService.moveMultiOrder(new MultiOrderMovingRequest(
                multiOrderId,
                orderIds,
                BASIC_TO_ARCHIVE
        ));

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                first.getId(),
                null,
                0,
                Instant.now(),
                first.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(orderIds);
        orderIds.forEach(id -> checkOrderRecordsExistence(StorageType.ARCHIVE, id, false));
    }

    @Test
    @DisplayName("POSITIVE: Успешная разархивация помеченного, но не скопированного синего заказа")
    void unarchiveNotCopiedOrderTest() {
        Order order = completeOrder();

        archiveOrders(Set.of(order.getId()));

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                order.getId(),
                null,
                0,
                Instant.now(),
                order.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(order);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    @Test
    @DisplayName("POSITIVE: Успешная разархивация помеченного, но не скопированного синего мультизаказа")
    void unarchiveNotCopiedMultiOrderTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(BasicOrder::getId).collect(Collectors.toSet()));
        Order first = orders.get(0);
        Set<Long> orderIds = orders.stream().map(BasicOrder::getId).collect(Collectors.toSet());

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                first.getId(),
                null,
                0,
                Instant.now(),
                first.getId()
        );

        assertEquals(ExecutionResult.SUCCESS, unarchiveOrderProcessor.process(execution));

        sqlAssertings(orderIds);
        orderIds.forEach(id -> checkOrderRecordsExistence(StorageType.ARCHIVE, id, false));
    }

    @Test
    @DisplayName("POSITIVE: Одновременный запуск нескольких QC по одному заказу")
    void multithreadingUnarchivingOrderTest() throws Exception {
        Order order = createArchivedOrder();
        moveArchivedOrders();

        QueuedCallProcessor.QueuedCallExecution execution = new QueuedCallProcessor.QueuedCallExecution(
                order.getId(),
                null,
                0,
                Instant.now(),
                order.getId()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CyclicBarrier cb = new CyclicBarrier(2);
        Callable<ExecutionResult> callable = () -> {
            cb.await();
            return unarchiveOrderProcessor.process(execution);
        };

        Future<ExecutionResult> future1 = executor.submit(callable);
        Future<ExecutionResult> future2 = executor.submit(callable);

        ExecutionResult result1 = future1.get();
        ExecutionResult result2 = future2.get();

        sqlAssertings(order);
        assertEquals(result1, result2);

    }

    private void sqlAssertings(Order order) {
        sqlAssertings(Set.of(order.getId()));
    }

    private void sqlAssertings(Collection<Long> orderIds) {
        String orderIdsString = orderIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        assertEquals(
                orderIds.size(),
                masterJdbcTemplate.queryForList(String.format(ORDER_EXIST_QUERY, orderIdsString)).size()
        );
        assertEquals(
                orderIds.size(),
                masterJdbcTemplate.queryForList(String.format(DEARCHIVED_EVENT_EXIST_QUERY, orderIdsString)).size()
        );
    }
}
