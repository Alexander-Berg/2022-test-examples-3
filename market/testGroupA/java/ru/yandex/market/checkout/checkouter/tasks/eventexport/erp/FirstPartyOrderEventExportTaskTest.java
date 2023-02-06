package ru.yandex.market.checkout.checkouter.tasks.eventexport.erp;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.OrderEntityGroup;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RemoveItemsHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.BusinessRecipientProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.common.zk.ZooClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ITEMS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID;
import static ru.yandex.market.sdk.userinfo.service.UidConstants.NO_SIDE_EFFECT_UID;

public class FirstPartyOrderEventExportTaskTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FirstPartyOrderEventExportTaskTest.class);
    @Autowired
    private ZooTask firstPartyOrderEventExportTask;
    @Autowired
    private ZooClient zooClient;
    @Autowired
    private JdbcTemplate erpJdbcTemplate;
    @Autowired
    private JdbcTemplate masterJdbcTemplate;
    @Autowired
    private Storage storage;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private OrderHistoryDao orderHistoryDao;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private RemoveItemsHelper removeItemsHelper;
    @Value("${market.checkout.lbkx.topic.order-event.partition.count:1}")
    private int partitionCount;
    @Autowired
    private ReturnHelper returnHelper;

    @BeforeEach
    public void setup() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.EXPORT_DROPSHIP_TO_ERP_ENABLED, true);
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableBnpl(true);
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
    }

    @AfterEach
    public void tearDown() {
        b2bCustomersMockConfigurer.resetAll();
        clearFixed();
    }

    @Test
    public void importEventsToErp() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));

        verifyEventFieldExported(order, "PS_CONTRACT_EXT_ID", "test_external_id");
        verifyEventFieldExported(order, "PAYMENT_METHOD", "YANDEX");
    }

    @Test
    public void shouldImportOnlyAllowedTypeEventsToErp() {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);

        insertAllValidEventsToOrderHistory(order);

        firstPartyOrderEventExportTask.runOnce();

        Set<HistoryEventType> eventTypesToExport = ErpEventExportService.Config.ALLOWED_TYPES.stream()
                .filter(type -> !ErpEventExportService.Config.RETURN_EVENT_TYPES.contains(type))
                .collect(Collectors.toSet());
        Set<HistoryEventType> eventTypesNotToExport = Stream.of(HistoryEventType.values())
                .filter(type -> !ErpEventExportService.Config.ALLOWED_TYPES.contains(type))
                .collect(Collectors.toSet());

        SoftAssertions softAssert = new SoftAssertions();
        eventTypesToExport.forEach(type -> verifyEventExported(order, type, eventCount ->
                softAssert.assertThat(eventCount)
                        .withFailMessage("Type should be exported at least once: " + type)
                        .isGreaterThanOrEqualTo(1)
        ));
        eventTypesNotToExport.forEach(type -> verifyEventExported(order, type, eventCount ->
                softAssert.assertThat(eventCount)
                        .withFailMessage("Type should not be exported: " + type)
                        .isEqualTo(0)));
        softAssert.assertAll();
    }

    private void insertAllValidEventsToOrderHistory(Order order) {
        Order orderFromStorage = orderService.getOrder(order.getId());
        orderFromStorage.setSubstatus(OrderSubstatus.SHIPPED); // чтобы выгрузилось событие обновленя сабстатуса
        Stream.of(HistoryEventType.values())
                .filter(type -> type != HistoryEventType.UNKNOWN)
                .filter(type -> !ErpEventExportService.Config.RETURN_EVENT_TYPES.contains(type))
                .forEach(type -> storage.updateEntityGroup(
                        new OrderEntityGroup(order.getId()),
                        () -> orderHistoryDao.insertOrderHistory(orderFromStorage, type, ClientInfo.SYSTEM)
                        )
                );
    }

    @Test
    public void importEventsWithSubstatusToErp() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM,
                "12345", OrderStatus.PROCESSING, OrderSubstatus.SHIPPED);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_SUBSTATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, OrderSubstatus.SHIPPED, greaterThanOrEqualTo(1));

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    @Test
    public void importBusinessClientEventsToErp() throws Exception {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        final Long expectedBusinessBalanceId = 123L;
        final Long expectedContractId = 345L;
        final String expectedBusinessRecipientJson =
                "{\"name\":\"ООО Рога и Копыта (c)\",\"inn\":\"123_test_inn_321\",\"kpp\":\"123_test_kpp_321\"}";

        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);

        ffBlueOrder.getOrders().forEach(o -> {
            o.getBuyer().setBusinessBalanceId(expectedBusinessBalanceId);
            o.getBuyer().setContractId(expectedContractId);
            o.getDelivery().setBusinessRecipient(BusinessRecipientProvider.getDefaultBusinessRecipient());
        });

        ffBlueOrder.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                ffBlueOrder.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));

        Order order = orderCreateHelper.createOrder(ffBlueOrder);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM,
                "12345", OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        // ожидаем, что в базу Аксапты должны записаться 2 события

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, HistoryEventType.ORDER_SUBSTATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, OrderSubstatus.READY_TO_SHIP, greaterThanOrEqualTo(1));

        // смотрим, что 3 новых поля записались в базу Аксапты

        var erpRecord = erpJdbcTemplate.queryForMap(
                "select BALANCE_CLIENT_ID, CONTRACT_ID, BUSINESSRECIPIENT from COOrderEvent " +
                        "where order_id = ? and EVENT_TYPE = ?",
                order.getId(), HistoryEventType.ORDER_STATUS_UPDATED.name());

        assertEquals(expectedBusinessBalanceId, erpRecord.get("BALANCE_CLIENT_ID"));
        assertEquals(expectedContractId, erpRecord.get("CONTRACT_ID"));
        assertEquals(expectedBusinessRecipientJson, erpRecord.get("BUSINESSRECIPIENT"));

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    @Test
    public void importWarehouseToErp() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();

        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        ffBlueOrder.setWarehouseForAllItems(FulfilmentProvider.TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyItemsExported(order, 1);
        verifyItemFieldExported(order, "WAREHOUSE_ID", FulfilmentProvider.TEST_WAREHOUSE_ID);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    @Test
    public void importReturnsToErp() {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueParametersWithDelivery(222999L);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);

        OrderItem item = ffBlueOrder.getOrder().getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Empty cart"));
        item.setCount(10);

        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        ClientInfo clientInfo = new ClientInfo(ClientRole.REFEREE, 123123L);

        //Вернули айтем
        Return returnRequest1 = ReturnProvider.generatePartialReturnWithDelivery(
                deliveredOrder, 222999L, 1);
        returnRequest1.getItems().iterator().next().setCount(1);
        returnRequest1.getItems().iterator().next().setQuantity(BigDecimal.ONE);
        Return ret = returnService.initReturn(
                deliveredOrder.getId(),
                clientInfo,
                returnRequest1
        );

        //Вернем еще один айтем
        Return returnRequest2 = ReturnProvider.generatePartialReturnWithDelivery(
                deliveredOrder, 222999L, 1, true);
        returnRequest2.getItems().iterator().next().setCount(1);
        returnRequest2.getItems().iterator().next().setQuantity(BigDecimal.ONE);
        Return ret2 = returnService.initReturn(
                deliveredOrder.getId(),
                clientInfo,
                returnRequest2
        );

        // Проверим, что возвраты в статусе STARTED_BY_USER не экспортируются в ERP
        firstPartyOrderEventExportTask.runOnce();

        testReturnEvents();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyEventExported(order, HistoryEventType.ORDER_RETURN_STATUS_UPDATED, equalTo(0));

        verifyReturnExported(ret, equalTo(0));
        verifyReturnExported(ret2, equalTo(0));

        //Переведем возвраты в REFUND_IN_PROGRESS
        returnService.resumeReturn(deliveredOrder.getId(), clientInfo, ret.getId(), ret);
        returnService.resumeReturn(deliveredOrder.getId(), clientInfo, ret2.getId(), ret2);

        firstPartyOrderEventExportTask.runOnce();

        testReturnEvents();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyEventExported(order, HistoryEventType.ORDER_RETURN_STATUS_UPDATED, equalTo(2));

        verifyReturnExported(ret, equalTo(1));
        verifyReturnItemsExported(ret, equalTo(1));
        verifyPayOfflineExported(ret, equalTo(1));

        verifyReturnExported(ret2, equalTo(1));
        verifyReturnItemsExported(ret2, equalTo(1));
        verifyPayOfflineExported(ret2, equalTo(1));
    }

    @Test
    public void nonBlueEventsNotExported() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(0));
    }

    @Test
    public void nonFulfilmentEventsNotExported() {
        Parameters notFFBlueOrder = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        Order order = orderCreateHelper.createOrder(notFFBlueOrder);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(0));
    }

    @Test
    public void nonFirstPartyOrderExportedWithoutItems() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    /**
     * В заказе только 3P товар, но включен экспорт 3P (проперти erpThirdPartyExportEnabled)
     */
    @Test
    public void thirdPartyOrderExportedWithItemsWhenPropertyIsEnabled() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        ffBlueOrder.setWarehouseForAllItems(147); // Ростов
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    /**
     * В заказе только 3P товар не из Ростова, но включен экспорт 3P (проперти erpThirdPartyExportEnabled)
     */
    @Test
    public void thirdPartyOrderNotExportedWithItemsNotFromRostovWhenPropertyIsEnabled() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        ffBlueOrder.setWarehouseForAllItems(145); // Маршрут
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    /**
     * В возврате только 3P товар
     */
    @Test
    public void nonFirstPartyReturnNotExported() throws Exception {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);

        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return returnRequest = ReturnProvider.generateReturn(deliveredOrder);

        Return createdReturn = returnService.initReturn(deliveredOrder.getId(), new ClientInfo(ClientRole.REFEREE,
                123123L), returnRequest, Experiments.empty());
        createdReturn = returnService.resumeReturn(createdReturn.getOrderId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                createdReturn.getId(),
                createdReturn,
                true);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyReturnExported(createdReturn, equalTo(1));
        verifyReturnItemsExported(createdReturn, equalTo(1));
    }

    /**
     * В возврате только 3P товар, но включен экспорт 3P (проперти erpThirdPartyExportEnabled)
     */
    @Test
    public void nonFirstPartyReturnToRostovExportedWhenPropertyIsEnabled() {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        ffBlueOrder.setWarehouseForAllItems(147); // Ростов
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return returnRequest = ReturnProvider.generateReturn(deliveredOrder);
        Return createdReturn = returnService.initReturn(
                deliveredOrder.getId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                returnRequest, Experiments.empty());
        createdReturn = returnService.resumeReturn(createdReturn.getOrderId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                createdReturn.getId(),
                createdReturn,
                true);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyReturnExported(createdReturn, equalTo(1));
        verifyReturnItemsExported(createdReturn, equalTo(1));
    }

    /**
     * В возврате только 3P товар, но включен экспорт 3P (проперти erpThirdPartyExportEnabled)
     */
    @Test
    public void nonFirstPartyReturnToNotRostovNotExportedWhenPropertyIsEnabled() {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        ffBlueOrder.setWarehouseForAllItems(145); // Маршрут
        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return returnRequest = ReturnProvider.generateReturn(deliveredOrder);
        Return createdReturn = returnService.initReturn(
                deliveredOrder.getId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                returnRequest, Experiments.empty());
        createdReturn = returnService.resumeReturn(createdReturn.getOrderId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                createdReturn.getId(),
                createdReturn,
                true);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyReturnExported(createdReturn, equalTo(1));
        verifyReturnItemsExported(createdReturn, equalTo(1));
    }

    /**
     * В возврате 3P товар и доставка
     */
    @Test
    public void nonFirstPartyAndDeliveryReturnExported() {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);

        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return returnRequest = ReturnProvider.generateReturn(deliveredOrder);
        ReturnHelper.addDeliveryItemToRequest(returnRequest);

        Return createdReturn = returnService.initReturn(deliveredOrder.getId(), new ClientInfo(ClientRole.REFEREE,
                123123L), returnRequest, Experiments.empty());
        createdReturn = returnService.resumeReturn(createdReturn.getOrderId(),
                new ClientInfo(ClientRole.REFEREE, 123123L),
                createdReturn.getId(),
                createdReturn,
                true);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_RETURN_STATUS_UPDATED, equalTo(1));
        verifyReturnExported(createdReturn, equalTo(1));
        verifyReturnItemsExported(createdReturn, equalTo(2));
    }

    @Test
    public void transactionRollingBack() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        Order order1 = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order1);
        Order order2 = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order2);
        spoilOrder(order2);

        long fixedTime = 1529320860000L;
        setFixedTime(
                Instant.ofEpochMilli(fixedTime)
        );
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order1, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(0));
        verifyEventExported(order2, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(0));
        verifyItemsExported(order1, 0);
        verifyItemsExported(order2, 0);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event should not propagate!", lastEventIdBefore, equalTo(lastEventIdAfter));
        fixOrder(order2);
    }

    @Test
    public void shouldImportDropShipOrdersWithoutItems() throws Exception {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        firstPartyOrderEventExportTask.runOnce();
        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 0);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
        verifyEventFieldExported(order, "PS_CONTRACT_EXT_ID", "test_external_id");
        verifyEventFieldExported(order, "PAYMENT_METHOD", "YANDEX");
    }

    @Test
    public void shouldNotExportFreeDeliveryDropShipOrders() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters(BigDecimal.ZERO);
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        firstPartyOrderEventExportTask.runOnce();
        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, equalTo(0));
    }

    private void verifyEventFieldExported(Order order, String column, Object value) {
        final List<Object> result = erpJdbcTemplate.queryForList(
                String.format("SELECT %s FROM COOrderEvent WHERE ORDER_ID=?", column),
                order.getId()
        ).stream()
                .map(e -> e.values().iterator().next())
                .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(value));
    }

    @Test
    public void shouldExportReturnForDropShipOrders() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        OrderItem item = parameters.getOrder().getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Empty cart"));
        item.setCount(10);
        Order order = orderCreateHelper.createOrder(parameters);
        Order deliveredOrder = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        deliveredOrder = orderService.getOrder(deliveredOrder.getId());
        ClientInfo clientInfo = new ClientInfo(ClientRole.REFEREE, 123123L);
        returnHelper.mockActualDelivery(order, DROPSHIP_DELIVERY_SERVICE_ID);
        //Вернули айтем
        Return ret = returnService.initReturn(
                deliveredOrder.getId(),
                clientInfo,
                ReturnProvider.generateReturnWithDelivery(deliveredOrder, DROPSHIP_DELIVERY_SERVICE_ID)
        );
        //Переведем возврат в REFUND_IN_PROGRESS
        returnService.resumeReturn(deliveredOrder.getId(), clientInfo, ret.getId(), ret);
        firstPartyOrderEventExportTask.runOnce();
        testReturnEvents();
        verifyEventExported(order, HistoryEventType.ORDER_RETURN_CREATED, equalTo(0));
        verifyEventExported(order, HistoryEventType.ORDER_RETURN_STATUS_UPDATED, equalTo(1));
        verifyReturnExported(ret, equalTo(1));
        verifyReturnItemsExported(ret, equalTo(1));
        verifyPayOfflineExported(ret, equalTo(1));
    }

    @Test
    public void shouldNotExportEventsForShopDelivery() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyEventExported(order, ORDER_STATUS_UPDATED, equalTo(0));
    }

    @Test
    public void shouldExportBnplFlag() {
        Parameters parameters = defaultBnplParameters();
        // выставление paymentSubtype заехало под тоглом вместе с рассрочками
        checkouterProperties.setEnableInstallments(true);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        firstPartyOrderEventExportTask.runOnce();

        verifyEventWithBnplFlagExported(order, greaterThan(0));
    }

    @Test
    public void shouldExportInstallments() {
        checkouterProperties.setEnableInstallments(true);
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setShowInstallments(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        firstPartyOrderEventExportTask.runOnce();

        assertThat(order.getPaymentSubmethod().getId(), greaterThan(1));
        verifyEventWithBnplFlagExported(order, greaterThan(0));
    }

    @Test
    public void shouldExportItemsUpdatedEventWithReasonAuthorAndPartialAvailable() {
        Parameters param = BlueParametersProvider.defaultBlueOrderParameters();
        param.getOrder().setProperty(OrderPropertyType.PARTIAL_AVAILABLE.getName(), "true");
        param.getItems().forEach(item -> {
            item.setCount(100);
            item.setValidIntQuantity(100);
        }); //что бы не попасть под ограничение максимальной суммы
        Order order = orderCreateHelper.createOrder(param);
        OrderItem item = order.getItems().iterator().next();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        RemoveItemsHelper.RemoveRequest removeRequest =
                new RemoveItemsHelper.RemoveRequest(order, USER_REQUESTED_REMOVE, ClientRole.SYSTEM);
        removeRequest.removeItem(item.getId(), item.getCount() - 1);
        removeItemsHelper.remove(removeRequest);
        order = orderService.getOrder(order.getId());
        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, ITEMS_UPDATED, equalTo(1));
        verifyEventFieldExported(order, ITEMS_UPDATED.name(), "REASON", USER_REQUESTED_REMOVE.getId());
        verifyEventFieldExported(order, ITEMS_UPDATED.name(), "AUTHOR_ROLE", ClientRole.SYSTEM.getId());
        verifyEventFieldExported(order, ITEMS_UPDATED.name(), "PARTIAL_AVAILABLE", 1);
    }


    @Test
    public void shouldNotExportShootingOrders() throws KeeperException {
        Map<Integer, Long> lastEventIdBefore = getLastEventId();

        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.getBuyer().setUid(NO_SIDE_EFFECT_UID);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        ffBlueOrder.setWarehouseForAllItems(FulfilmentProvider.TEST_WAREHOUSE_ID);

        Order order = orderCreateHelper.createOrder(ffBlueOrder);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        int partitionIndx = order.calculatePartitionIndex(partitionCount);

        firstPartyOrderEventExportTask.runOnce();

        verifyItemsExported(order, 0);

        Map<Integer, Long> lastEventIdAfter = getLastEventId();
        assertThat("last event id is not propagated!",
                lastEventIdAfter.get(partitionIndx) - lastEventIdBefore.get(partitionIndx),
                greaterThan(0L));
    }

    private void testReturnEvents() {
        masterJdbcTemplate.query("SELECT * FROM return_history ", dumpQueryResult());
        LOG.info("<------------------------------------------------>");
        erpJdbcTemplate.query("SELECT * FROM coorderevent ", dumpQueryResult());
        LOG.info("<------------------------------------------------>");
        erpJdbcTemplate.query("SELECT * FROM coreturn ", dumpQueryResult());
    }

    private ResultSetExtractor<Integer> dumpQueryResult() {
        return rs -> {
            while (rs.next()) {
                LOG.info("------------------------------------------------");

                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    LOG.info(metaData.getColumnName(i) + " --- " + rs.getObject(i));
                }
            }
            return 1;
        };
    }

    private void spoilOrder(Order order) {
        storage.createEntityGroup(
                new OrderEntityGroup(order.getId()),
                () -> masterJdbcTemplate.update("UPDATE orders SET payment_type = NULL WHERE id=?", order.getId())
        );
    }

    private void fixOrder(Order order) {
        storage.createEntityGroup(
                new OrderEntityGroup(order.getId()),
                () -> masterJdbcTemplate.update("UPDATE orders SET payment_type = 0 WHERE id=?", order.getId())
        );
    }

    private void verifyItemsExported(Order order, Integer count) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(DISTINCT item_id) FROM coorderitem WHERE order_id=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId()
        );

        assertThat(res, equalTo(count));
    }

    private <T> void verifyItemFieldExported(Order order, String fieldName, T expected) {
        List result = erpJdbcTemplate.queryForList(
                String.format("SELECT %s FROM COOrderItem WHERE ORDER_ID=?", fieldName),
                order.getId()
        ).stream()
                .map(e -> e.values().iterator().next())
                .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(expected));
    }

    private <T> void verifyEventFieldExported(Order order, String eventType, String fieldName, T expected) {
        List result = erpJdbcTemplate.queryForList(
                String.format("SELECT %s FROM COOrderEvent WHERE ORDER_ID=? AND EVENT_TYPE =?", fieldName),
                order.getId(),
                eventType
        ).stream()
                .map(e -> e.values().iterator().next())
                .collect(Collectors.toList());

        assertThat(result.get(0), equalTo(expected));
    }

    private void verifyEventExported(Order order, HistoryEventType type, Matcher<Integer> matcher) {
        verifyEventExported(order, type, count -> assertThat(count, matcher));
    }

    private void verifyEventExported(Order order, HistoryEventType type, Consumer<Integer> check) {
        LOG.info("Checking event export of type: " + type);
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coorderevent WHERE order_id=? AND event_type=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId(),
                type.name()
        );

        check.accept(res);
    }

    private void verifyEventExported(Order order, OrderSubstatus substatus, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coorderevent WHERE order_id=? AND substatus=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId(),
                substatus.name()
        );

        assertThat(res, matcher);
    }

    private void verifyEventWithBnplFlagExported(Order order, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coorderevent WHERE order_id=? AND bnpl = ?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId(),
                order.getPaymentSubmethod().getId()
        );

        assertThat(res, matcher);
    }

    private void verifyReturnExported(Return singleReturn, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coreturn WHERE return_id = ?",
                (rs, rn) -> rs.getInt(1),
                singleReturn.getId()
        );

        assertThat(res, matcher);
    }

    private void verifyPayOfflineExported(Return singleReturn, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coreturn WHERE return_id = ? AND pay_offline IS NOT DISTINCT FROM ?",
                (rs, rn) -> rs.getInt(1),
                singleReturn.getId(),
                singleReturn.getPayOffline()
        );

        assertThat(res, matcher);
    }

    private void verifyReturnItemsExported(Return singleReturn, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coreturnitem WHERE return_id = ?",
                (rs, rn) -> rs.getInt(1),
                singleReturn.getId()
        );

        assertThat(res, matcher);
    }

    @Nonnull
    private Map<Integer, Long> getLastEventId() throws KeeperException {
        String data = zooClient.getStringData("/checkout/tasks/first-party-order-export-task/lastEventId");
        try {
            return new ObjectMapper().readValue(data, new TypeReference<Map<Integer, Long>>() {
            });
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            Map<Integer, Long> events = new HashMap<>();
            long eventId = Long.parseLong(data);

            for (int i = 0; i < partitionCount; i++) {
                events.put(i, eventId);
            }
            events.put(Integer.MIN_VALUE, eventId);
            return events;
        }
    }
}
