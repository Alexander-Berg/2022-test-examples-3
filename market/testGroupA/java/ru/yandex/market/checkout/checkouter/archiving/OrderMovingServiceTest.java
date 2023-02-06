package ru.yandex.market.checkout.checkouter.archiving;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Record;
import org.jooq.Table;
import org.jooq.tools.Convert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.archive.OrderMovingService;
import ru.yandex.market.checkout.checkouter.order.archive.requests.MultiOrderMovingRequest;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.DatabaseUtils;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.checkouter.jooq.tables.records.DeliveryAddressRecord;
import ru.yandex.market.checkouter.jooq.tables.records.OrdersRecord;
import ru.yandex.market.checkouter.jooq.tables.records.ReceiptRecord;
import ru.yandex.market.checkouter.jooq.tables.records.RefundRecord;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection.BASIC_TO_ARCHIVE;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.TRACKED_ARCHIVING_TABLES;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.DatabaseUtils.SIMPLE_CONVERTER;
import static ru.yandex.market.checkout.util.DatabaseUtils.getForeignKeys;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_ADDRESS;
import static ru.yandex.market.checkouter.jooq.Tables.ORDERS;
import static ru.yandex.market.checkouter.jooq.Tables.RECEIPT;
import static ru.yandex.market.checkouter.jooq.Tables.REFUND;

public class OrderMovingServiceTest extends AbstractArchiveWebTestBase {

    @Autowired
    private OrderMovingService orderMovingService;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    void shouldMoveBlueOrder() {
        Order order = completeOrder();
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);
        Map<Table<?>, List<Object>> archivedTableIdsData = convertToArchivedTableIds(saved);

        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
        assertArchivedTableIdsEquals(archivedTableIdsData, loadArchivedTableIds());
    }

    @Test
    void shouldMoveBlueOrderWithReturn() {
        Order order = completeOrder();
        Return ret = returnHelper.createReturn(order.getId(), ReturnProvider.generateReturn(order));
        trustMockConfigurer.resetRequests();
        returnService.createAndDoRefunds(ret, order);
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    void shouldMoveBlueMultiOrderWithMultiPayment() throws Exception {
        List<Order> orders = createMultiOrder();
        Set<Long> orderIds = orders.stream().map(BasicOrder::getId).collect(Collectors.toSet());
        String multiOrderId = masterJdbcTemplate.queryForObject(
                "select text_value from order_property where order_id = "
                        + orderIds.iterator().next() + " and name = 'multiOrderId'", String.class);
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        orderMovingService.moveMultiOrder(new MultiOrderMovingRequest(multiOrderId, orderIds, BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    void shouldMoveBlueMultiOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getAnotherWarehouseOrderItem()));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order first = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order second = orderService.getOrder(multiOrder.getOrders().get(1).getId());
        orderPayHelper.payForOrders(Arrays.asList(first));
        orderPayHelper.payForOrders(Arrays.asList(second));

        String multiOrderId = first.getProperty(OrderPropertyType.MULTI_ORDER_ID);
        Set<Long> orderIds = Set.of(first.getId(), second.getId());
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        orderMovingService.moveMultiOrder(new MultiOrderMovingRequest(multiOrderId, orderIds, BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    void shouldMoveOrderWithTrackAndCheckpoint() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        OrderServiceHelper.putTrackIntoOrder(order.getId(), orderUpdateService);
        orderServiceHelper.insertCheckpoint();
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), OrderMovingDirection.BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: перенос заказа с DeliveryPromo")
    public void shouldMoveOrderWithDeliveryPromo() {
        // Arrange
        Order order = createOrderWithDeliveryPromo();
        orderStatusHelper.proceedOrderToStatus(order, DELIVERED);
        final long orderId = order.getId();
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        // Act
        orderMovingService.moveOrder(new OrderMovingRequest(orderId, OrderMovingDirection.BASIC_TO_ARCHIVE));

        // Assert
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: перенос заказа с ParcelBox")
    public void shouldMoveOrderWithParcelBox() {
        // Arrange
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        addBoxesWithItemsToOrder(order);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        clearPaymentAndUpdateIncomeReceiptToPrintedStatus();
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        // Act
        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), OrderMovingDirection.BASIC_TO_ARCHIVE));

        // Assert
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: перенос заказа с ChangeRequest")
    public void shouldMoveOrderWithChangeRequest() {
        Parameters params = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        createChangeRequest(order);
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        // Act
        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), OrderMovingDirection.BASIC_TO_ARCHIVE));

        // Assert
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: перенос заказа с ItemPromo")
    public void shouldMoveOrderWithItemPromo() {
        // Arrange
        Order order = createOrderWithItemPromo();
        Map<Table<?>, List<Record>> saved = loadArchivingData(StorageType.BASIC);

        // Act
        orderMovingService.moveOrder(new OrderMovingRequest(order.getId(), OrderMovingDirection.BASIC_TO_ARCHIVE));

        // Assert
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(saved, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: Database FK constraint test")
    void foreignKeysConstraintTest() {
        Map<Table<?>, List<Record>> orderData = generateArchivingData(1L, true);
        saveArchivingData(StorageType.BASIC, orderData);

        orderMovingService.moveOrder(new OrderMovingRequest(1L, OrderMovingDirection.BASIC_TO_ARCHIVE));

        assertThat(orderData).hasSize(ARCHIVING_TABLES.size());
        assertThat(orderData.values()).allSatisfy(records -> assertThat(records).isNotEmpty());
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(orderData, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    @DisplayName("POSITIVE: Проверка того, что перенос не затрагивает лишние данные")
    void testMovingSafety() {
        Map<Table<?>, List<Record>> orderData1 = generateArchivingData(1L, true);
        Map<Table<?>, List<Record>> orderData2 = generateArchivingData(2L, false);
        saveArchivingData(StorageType.BASIC, orderData1);
        saveArchivingData(StorageType.BASIC, orderData2);

        orderMovingService.moveOrder(new OrderMovingRequest(1L, OrderMovingDirection.BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(orderData2, loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(orderData1, loadArchivingData(StorageType.ARCHIVE));
    }

    @Test
    public void shouldMoveToSecondArchiveShard() {
        Map<Table<?>, List<Record>> orderData = generateArchivingData(1_000_000_000_000L, true);
        saveArchivingData(StorageType.BASIC, orderData);

        orderMovingService.moveOrder(new OrderMovingRequest(1_000_000_000_000L, OrderMovingDirection.BASIC_TO_ARCHIVE));

        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.BASIC));
        assertArchivingDataEquals(emptyMap(), loadArchivingData(StorageType.ARCHIVE, 0));
        assertArchivingDataEquals(orderData, loadArchivingData(StorageType.ARCHIVE, 1));
    }

    @Test
    void shouldUpdateArchivedTables() {
        Map<Table<?>, List<Record>> orderData = generateArchivingData(1L, true);
        Map<Table<?>, List<Object>> archivedTableIdsData = convertToArchivedTableIds(orderData);
        saveArchivingData(StorageType.BASIC, orderData);

        orderMovingService.moveOrder(new OrderMovingRequest(1L, OrderMovingDirection.BASIC_TO_ARCHIVE));

        assertThat(archivedTableIdsData).hasSize(TRACKED_ARCHIVING_TABLES.size());
        assertThat(archivedTableIdsData.values()).allSatisfy(ids -> assertThat(ids).isNotEmpty());
        assertArchivedTableIdsEquals(archivedTableIdsData, loadArchivedTableIds());

        orderMovingService.moveOrder(new OrderMovingRequest(1L, OrderMovingDirection.ARCHIVE_TO_BASIC));

        assertArchivedTableIdsEquals(emptyMap(), loadArchivedTableIds());
    }

    private Map<Table<?>, List<Record>> generateArchivingData(Long orderId, boolean archived) {
        Map<Table<?>, Record> recordsMap = ARCHIVING_TABLES.stream()
                .collect(Collectors.toMap(Function.identity(), DatabaseUtils::getTableRecord));

        IGNORE_FIELDS.forEach((table, field) -> {
            recordsMap.get(table).reset(field);
        });

        OrdersRecord orderRecord = (OrdersRecord) recordsMap.get(ORDERS);
        orderRecord.setId(orderId);
        orderRecord.setIsArchived(archived);
        if (orderRecord.getSubstatus() == OrderSubstatus.DELIVERY_SERIVCE_UNDELIVERED) {
            // workaround  for DELIVERY_SERIVCE_UNDELIVERED != DELIVERY_SERVICE_UNDELIVERED issue
            orderRecord.setSubstatus(OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED);
        }

        for (var fk : getForeignKeys(masterJdbcTemplate, ARCHIVING_TABLES)) {
            Record sourceRecord = recordsMap.get(fk.getSourceTable());
            if (sourceRecord == null) {
                continue;
            }
            Record referencedRecord = recordsMap.get(fk.getReferencedTable());

            sourceRecord.set(
                    fk.getSourceField(),
                    Convert.convert(referencedRecord.get(fk.getReferencedField()), fk.getSourceField().getType()),
                    SIMPLE_CONVERTER
            );
        }

        ReceiptRecord receiptRecord = (ReceiptRecord) recordsMap.get(RECEIPT);
        receiptRecord.setRefundId(null);

        RefundRecord refundRecord = (RefundRecord) recordsMap.get(REFUND);
        refundRecord.setLastHistoryId(null); // break circular constraint

        DeliveryAddressRecord deliveryAddressRecord = (DeliveryAddressRecord) recordsMap.get(DELIVERY_ADDRESS);
        deliveryAddressRecord.setOutletPhones((String) null);

        return recordsMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));
    }
}
