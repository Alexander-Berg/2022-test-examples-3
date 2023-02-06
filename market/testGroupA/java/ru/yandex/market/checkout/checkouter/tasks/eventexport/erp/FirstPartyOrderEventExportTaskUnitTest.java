package ru.yandex.market.checkout.checkouter.tasks.eventexport.erp;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.zookeeper.KeeperException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.history.OrderHistoryEventsRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.storage.promo.OrderPromoHistoryDao;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.checkouter.tdb.ErpOrderEvent;
import ru.yandex.market.checkout.checkouter.tdb.ErpTransitionalDataBase;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.zk.ZooClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FirstPartyOrderEventExportTaskUnitTest {

    private static final Set<OrderSubstatus> FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED =
            ErpEventExportService.Config.FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED;
    private static final Set<OrderSubstatus> FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED_BUSINESS_CLIENT =
            ErpEventExportService.Config.FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED_BUSINESS_CLIENT;
    private static final Set<OrderSubstatus> ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED =
            ErpEventExportService.Config.ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED;
    private static final Set<OrderStatus> ALLOWED_ORDER_STATUSES_IN_ORDER_STATUS_UPDATED =
            ErpEventExportService.Config.ALLOWED_ORDER_STATUSES_IN_ORDER_STATUS_UPDATED;
    private static final Set<OrderSubstatus> ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED_BUSINESS_CLIENT =
            ErpEventExportService.Config.ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED_BUSINESS_CLIENT;

    @Mock
    private ColorConfig colorConfig;
    @Mock
    private EventService eventService;
    @Mock
    private ErpTransitionalDataBase erpTransitionalDataBase;
    @Mock
    private ZooClient zooClient;
    @Mock
    private ReturnService returnService;
    @Mock
    private Clock clock;
    @Mock
    private ZooTask zooTask;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private OrderPromoHistoryDao orderPromoHistoryDao;
    @Mock
    protected CheckouterProperties checkouterProperties;
    @Mock
    protected CheckouterFeatureReader checkouterFeatureReader;

    private FirstPartyOrderEventExportTask task;

    @Mock
    private PersonalDataService personalDataService;

    @BeforeEach
    public void setUp() {
        task = new FirstPartyOrderEventExportTask(
                eventService,
                erpTransitionalDataBase,
                createErpEventExportService(),
                10);
    }

    private ErpEventExportService createErpEventExportService() {
        ErpEventExportService service = new ErpEventExportService(colorConfig, orderPromoHistoryDao, returnService,
                erpTransitionalDataBase, receiptService, mock(PaymentService.class), eventService,
                checkouterFeatureReader, personalDataService);

        Mockito.lenient().when(colorConfig.colorsWith(Mockito.any())).thenReturn(Set.of(Color.BLUE));
        Mockito.lenient().when(erpTransitionalDataBase.executeInTransaction(Mockito.any()))
                .then(i -> ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(
                        new DefaultTransactionStatus(null, true, true,
                                false, false, null)
                ));

        return service;
    }

    @Test
    public void shouldExportOnlyAllowedStatusEvents() throws KeeperException {
        mockPersonalDataService(testAddress());

        setupMocksToReturnEvents(() ->
                Stream.of(OrderStatus.values())
                        .map(status -> generateEvent(generateOrder(status, null),
                                HistoryEventType.ORDER_STATUS_UPDATED))
                        .collect(Collectors.toList())
        );

        List<ErpOrderEvent> exportedEvents = exportEvents();

        Set<String> whiteList = ALLOWED_ORDER_STATUSES_IN_ORDER_STATUS_UPDATED.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        Assertions.assertThat(exportedEvents)
                .extracting(ErpOrderEvent::getStatus)
                .containsExactlyInAnyOrderElementsOf(whiteList);
    }

    @Test
    public void shouldExportOnlyAllowedSubStatusEvents() throws KeeperException {
        mockPersonalDataService(testAddress());

        setupMocksToReturnEvents(() ->
                Stream.of(OrderSubstatus.values())
                        .map(subStatus -> generateEvent(
                                generateOrder(OrderStatus.PROCESSING, subStatus, null),
                                HistoryEventType.ORDER_SUBSTATUS_UPDATED
                        )).collect(Collectors.toList())
        );

        List<ErpOrderEvent> exportedEvents = exportEvents();

        Set<String> whiteList = ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        Assertions.assertThat(exportedEvents)
                .extracting(ErpOrderEvent::getSubStatus)
                .containsExactlyInAnyOrderElementsOf(whiteList);
    }

    @Test
    public void shouldExportOnlyAllowedSubStatusBusinessClientEvents() throws KeeperException {
        mockPersonalDataService(testAddress());

        setupMocksToReturnEvents(() ->
                Stream.of(OrderSubstatus.values())
                        .map(subStatus -> generateEvent(
                                generateOrderWithCustomBuyerAddress(OrderStatus.PROCESSING, subStatus, 123L),
                                HistoryEventType.ORDER_SUBSTATUS_UPDATED
                        )).collect(Collectors.toList())
        );

        List<ErpOrderEvent> exportedEvents = exportEvents();

        Set<String> whiteList = ALLOWED_ORDER_SUBSTATUSES_IN_ORDER_SUBSTATUS_UPDATED_BUSINESS_CLIENT.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        Assertions.assertThat(exportedEvents)
                .extracting(ErpOrderEvent::getSubStatus)
                .containsExactlyInAnyOrderElementsOf(whiteList);

        // https://st.yandex-team.ru/MARKETCHECKOUT-25425
        // В Аксапту должен уходить адрес в таком формате (пустые части исключаются):
        // индекс, страна, город, улица, дом, строение, блок, квартира
        // (некоторые поля адреса незаполнены)
        String expectedDeliveryAddress = "123456, Страна1, Город1, Улица1, дом 11, квартира 33";

        String exportedAddress = exportedEvents.get(0).getDeliveryAddress();
        assertEquals(expectedDeliveryAddress, exportedAddress);
    }

    @Test
    public void shouldExportOnlyAllowedSubStatusesForOrderCancelEvents() throws KeeperException {
        mockPersonalDataService(testAddress());

        setupMocksToReturnEvents(() ->
                Stream.of(OrderSubstatus.values())
                        .map(subStatus -> generateEvent(
                                generateOrder(OrderStatus.CANCELLED, subStatus, null),
                                HistoryEventType.ORDER_STATUS_UPDATED
                        )).collect(Collectors.toList())
        );

        List<ErpOrderEvent> exportedEvents = exportEvents();

        Set<String> whiteList = Stream.of(OrderSubstatus.values())
                .filter(subStatus -> !FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED.contains(subStatus))
                .map(Enum::name)
                .collect(Collectors.toSet());

        Assertions.assertThat(exportedEvents)
                .extracting(ErpOrderEvent::getSubStatus)
                .containsExactlyInAnyOrderElementsOf(whiteList);
    }

    @Test
    public void shouldExportOnlyAllowedSubStatusesForOrderCancelBusinessClientEvents() throws KeeperException {
        mockPersonalDataService(AddressProvider.getAddress());

        setupMocksToReturnEvents(() ->
                Stream.of(OrderSubstatus.values())
                        .map(subStatus -> generateEvent(
                                generateOrder(OrderStatus.CANCELLED, subStatus, 123L),
                                HistoryEventType.ORDER_STATUS_UPDATED
                        )).collect(Collectors.toList())
        );

        List<ErpOrderEvent> exportedEvents = exportEvents();

        Set<String> whiteList = Stream.of(OrderSubstatus.values())
                .filter(subStatus ->
                        !FORBIDDEN_SUBSTATUSES_FOR_ORDER_STATUS_UPDATED_BUSINESS_CLIENT.contains(subStatus))
                .map(Enum::name)
                .collect(Collectors.toSet());

        Assertions.assertThat(exportedEvents)
                .extracting(ErpOrderEvent::getSubStatus)
                .containsExactlyInAnyOrderElementsOf(whiteList);

        // адрес доставки при всех заполненных полях, должен уходить в Аксапту в таком виде:
        String expectedDeliveryAddress = "131488, Русь, Питер, Победы, дом 13, строение 222, блок 666, квартира 303";

        String exportedAddress = exportedEvents.get(0).getDeliveryAddress();
        assertEquals(expectedDeliveryAddress, exportedAddress);
    }

    @Test
    public void shouldNotExportDeliveryUpdatedEvents() {
        assertFalse(ErpEventExportService.Config.ALLOWED_TYPES.contains(
                HistoryEventType.ORDER_DELIVERY_UPDATED));
    }

    private void setupMocksToReturnEvents(Supplier<List<OrderHistoryEvent>> eventsSupplier) throws KeeperException {
        Long minEventId = eventsSupplier.get().stream()
                .map(OrderHistoryEvent::getId)
                .min(Long::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("No events provided to export"));

        Mockito.when(zooClient.getStringData(Mockito.anyString()))
                .thenReturn(minEventId.toString());

        Mockito.when(eventService.getOrderHistoryEvents(Mockito.any(OrderHistoryEventsRequest.class)))
                .thenAnswer(invocation -> {
                    OrderHistoryEventsRequest req = invocation.getArgument(0);

                    return Partition.NULL.equals(req.getPartition()) ?
                            eventsSupplier.get() : Collections.emptyList();
                });
    }

    private List<ErpOrderEvent> exportEvents() {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.doNothing()
                .when(erpTransitionalDataBase).insertOrderEvent(argumentCaptor.capture());

        Mockito.when(zooTask.getRepeatPeriodMsActual())
                .thenReturn(10L);
        Mockito.when(zooTask.getZooClient()).thenReturn(zooClient);
        Mockito.when(zooTask.getClock()).thenReturn(clock);
        task.run(zooTask, () -> false);

        return (List<ErpOrderEvent>) argumentCaptor.getValue();
    }

    private Order generateOrder(OrderStatus os, Long businessBalanceId) {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1234L);
        order.setUid(135135L);
        order.setStatus(os);
        order.getBuyer().setBusinessBalanceId(businessBalanceId);

        return order;
    }

    private Order generateOrder(OrderStatus status, OrderSubstatus subStatus, Long businessBalanceId) {
        Order order = generateOrder(status, businessBalanceId);
        order.setSubstatus(subStatus);

        return order;
    }

    private Address testAddress() {
        var address = new AddressImpl();
        address.setCountry("Страна1");
        address.setPostcode("123456");
        address.setCity("Город1");
        address.setStreet("Улица1");
        address.setHouse("11");
        address.setFloor("22");
        address.setApartment("33");
        address.setEntrance("44");

        return address;
    }

    private Order generateOrderWithCustomBuyerAddress(OrderStatus status, OrderSubstatus subStatus,
                                                      Long businessBalanceId) {
        Order order = generateOrder(status, businessBalanceId);
        order.setSubstatus(subStatus);


        order.getDelivery().setBuyerAddress(testAddress());

        return order;
    }

    private OrderHistoryEvent generateEvent(Order order, HistoryEventType type) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(123L);
        event.setType(type);
        event.setOrderBefore(order);
        event.setOrderAfter(order);

        return event;
    }

    private void mockPersonalDataService(Address baseAddress) {
        PersAddress persAddress = PersAddress.convertToPersonal(baseAddress);

        when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
    }
}
