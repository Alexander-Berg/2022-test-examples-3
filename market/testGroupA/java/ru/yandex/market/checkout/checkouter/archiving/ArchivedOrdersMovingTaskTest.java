package ru.yandex.market.checkout.checkouter.archiving;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkouter.jooq.tables.Orders.ORDERS;

public class ArchivedOrdersMovingTaskTest extends AbstractArchiveWebTestBase {

    private static final String ORDER_SEARCH_QUERY = "select id from orders where id in (%s)";

    @BeforeEach
    void initProperties() {
        checkouterProperties.setEnableArchivingBulkInsert(false);
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего заказа")
    void shouldMoveArchivedOrderTest() throws Exception {
        Order order = createArchivedOrder();
        assertSuccessfulTaskRun(Set.of(order.getId()));
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего заказа(пакетная вставка)")
    void shouldMoveArchivedOrderBulkInsertTest() throws Exception {
        assertSuccessfulTaskRun(
                Set.of(
                        createArchivedOrder(),
                        createArchivedOrder(),
                        createArchivedOrder()
                ).stream()
                        .map(BasicOrder::getId)
                        .collect(Collectors.toList())
        );
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего мульти заказа")
    void shouldMoveArchivedMultiOrderTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        assertSuccessfulTaskRun(orders.stream().map(Order::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего мульти заказа(несколько потоков)")
    void shouldMoveArchivedMultiOrderMultithreadingTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        List<Order> orders1 = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));

        assertSuccessfulTaskRun(
                Stream.concat(orders.stream(), orders1.stream()).map(Order::getId).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего заказа и мульти заказа")
    void shouldMoveArchivedOrderAndMultiOrderTest() throws Exception {
        Order order = createArchivedOrder();

        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));

        Set<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        orderIds.add(order.getId());

        assertSuccessfulTaskRun(orderIds);
    }

    @Test
    @DisplayName("POSITIVE: Успешное копирование и удаление синего заказа и мульти заказа, при превышении батча")
    void shouldMoveArchivedOrderAndMultiOrderOverBatchSizeTest() throws Exception {
        Order order = createArchivedOrder();

        List<Order> orders = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        Set<Long> multiOrderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());

        List<Order> orders1 = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));
        Set<Long> multiOrderIds1 = orders1.stream().map(Order::getId).collect(Collectors.toSet());

        List<Order> orders2 = createArchivedMultiOrder(
                o -> o.stream().map(Order::getId).collect(Collectors.toSet()));

        multiOrderIds.add(order.getId());
        multiOrderIds.addAll(multiOrderIds1);

        assertSuccessfulTaskRun(multiOrderIds);

        List<Map<String, Object>> multiOrders = getOrders(getParametrizedQuery(orders2));
        assertEquals(0, multiOrders.size());
    }

    @Test
    @DisplayName("NEGATIVE: Мульти заказ не должен удалиться, в случае неудачного копирования")
    void shouldNotMovePartlyArchivingMultiOrderTest() throws Exception {
        List<Order> orders = createArchivedMultiOrder(o -> Set.of(o.get(0).getId()));
        moveArchivedOrders();

        assertEquals(2, getOrders(getParametrizedQuery(orders)).size());
    }

    @Test
    @DisplayName("POSITIVE: Заказ должен удалиться, если он уже есть в архивной БД")
    void shouldDeleteCopiedOrderTest() throws Exception {
        Order order = createArchivedOrder();
        copyArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        moveArchivedOrders();

        assertSuccessfulTaskRun(Set.of(order.getId()));
    }

    @Test
    @DisplayName("POSITIVE: Заказ не должен копироваться, если для него создан UNARCHIVE_ORDER queued call")
    void shouldSkipOrderWithUnarchiveQCTest() throws Exception {
        Order order = createArchivedOrder();

        transactionTemplate.execute(ts -> {
            queuedCallService.addQueuedCall(CheckouterQCType.UNARCHIVE_ORDER, order.getId());
            return null;
        });

        moveArchivedOrders();

        checkOrderRecordsExistence(StorageType.BASIC, order.getId(), true);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    @Test
    @DisplayName("POSITIVE: Заказ с признаком archived=false должен разархивироваться")
    void shouldUnarchiveArchivedOrder() throws Exception {
        Order order = createArchivedOrder();
        moveArchivedOrders();

        int nArchivedReset = archiveStorageManager.doWithArchiveContext(Set.of(order.getId()), () ->
            archiveStorageManager.executeTransaction(StorageType.ARCHIVE, ts ->
                getDsl(StorageType.ARCHIVE)
                        .update(ORDERS)
                        .set(ORDERS.IS_ARCHIVED, false)
                        .where(ORDERS.ID.eq(order.getId()))
                        .execute()
            )
        );
        assertEquals(1, nArchivedReset);

        transactionTemplate.execute(ts -> {
            queuedCallService.addQueuedCall(CheckouterQCType.UNARCHIVE_ORDER, order.getId());
            return null;
        });
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.UNARCHIVE_ORDER, order.getId());

        checkOrderRecordsExistence(StorageType.BASIC, order.getId(), true);
        checkOrderRecordsExistence(StorageType.ARCHIVE, order.getId(), false);
    }

    @Test
    @DisplayName("NEGATIVE: Заказ не должен архивироваться, если событие архивации не отправлено в логброкер")
    void shouldNotMoveIfEventNotExportedTest() throws Exception {
        Order order = createArchivedOrder();
        runArchiveOrderTasks();
        assertEquals(1, getOrders(getParametrizedQuery(order)).size());
    }

    @Test
    @DisplayName("NEGATIVE: Заказ не должен архивироваться, если он не помечен как архивный")
    void shouldNotMoveIfOrderNotArchivedTest() throws Exception {
        Order order = createBlueOrder();
        moveArchivedOrders();
        assertEquals(1, getOrders(getParametrizedQuery(order)).size());
    }

    private String getParametrizedQuery(Order order) {
        return getQuery(order.getId().toString());
    }

    private String getParametrizedQuery(List<Order> orders) {
        return getQuery(
                orders.stream()
                        .map(o -> o.getId().toString())
                        .collect(Collectors.joining(","))
        );
    }

    @Nonnull
    private List<Map<String, Object>> getOrders(String parametrizedQuery) {
        return masterJdbcTemplate.queryForList(parametrizedQuery);
    }

    private String getQuery(String params) {
        return String.format(ORDER_SEARCH_QUERY, params);
    }

    private void assertSuccessfulTaskRun(@Nonnull Collection<Long> ids) {
        moveArchivedOrders();

        ids.forEach(id -> checkOrderRecordsExistence(StorageType.BASIC, id, false));
        ids.forEach(id -> checkOrderRecordsExistence(StorageType.ARCHIVE, id, true));
    }
}
