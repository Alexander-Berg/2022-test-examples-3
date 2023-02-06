package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.actualization.services.ForeignCurrencyPriceService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DeliveryOutletService;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.order.changes.OrderChanges;
import ru.yandex.market.checkout.checkouter.order.cost.OrderCostService;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.shop.MemCachingShopServiceWrapper;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.storage.TestStubStorage;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.checkouter.storage.item.OrderItemHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.itemservice.ItemServiceDao;
import ru.yandex.market.checkout.checkouter.storage.resalespecs.ResaleSpecsDao;
import ru.yandex.market.checkout.checkouter.views.OrderCancelPolicyModifier;
import ru.yandex.market.checkout.checkouter.views.OrderFetchPostprocessor;
import ru.yandex.market.checkout.checkouter.views.OrderFetchPostprocessorImpl;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_SEPARATE_TOTAL_AMOUNT_IN_PAYMENT_BY_ORDERS;

/**
 * @author sergey-fed
 */
public class StorageOrderServiceTest {

    private OrderService storageOrderService;
    private EventService eventService;
    private OrderReadingDao readingDao;
    private ShopMetaData shopMetaData;
    private ShopService shopService;
    private ClientInfo shopInfo;

    private static OrderHistoryEvent initEvent(Order order) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(order.getId() + 100L);
        event.setType(HistoryEventType.NEW_ORDER);
        event.setOrderAfter(order);
        return event;
    }

    private static Order initOrder(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setCurrency(Currency.RUR);
        order.setBuyerCurrency(Currency.RUR);
        order.setExchangeRate(new BigDecimal(1));
        BigDecimal value = new BigDecimal(1).multiply(new BigDecimal(orderId));
        order.setBuyerItemsTotal(value);
        order.setItemsTotal(value);
        return createTestOrder(order, true);
    }

    private static Order createTestOrder(Order order, boolean withBuyer) {
        order.setStatus(OrderStatus.PENDING);

        if (withBuyer) {
            Buyer buyer = new Buyer(123L);
            buyer.setEmail("test@test.ru");
            buyer.setFirstName("Тест");
            buyer.setLastName("Тестов");
            buyer.setMiddleName("Тестович");
            buyer.setId("100500");
            buyer.setPhone("+71234567890");
            buyer.setPersonalPhoneId("0123456789abcdef0123456789abcdef");

            order.setBuyer(buyer);
        }

        OrderItem item = new OrderItem(new FeedOfferId("1", 2L), new BigDecimal("10.5"), 1);
        order.addItem(item);

        AddressImpl address = new AddressImpl();
        address.setCountry("Русь");
        address.setCity("Москва");
        address.setStreet("Красная площадь");
        address.setSubway("Охотный ряд");
        address.setHouse("0");
        address.setFloor("10");
        address.setApartment("1");
        address.setEntrance("2");
        address.setEntryPhone("777");

        Delivery delivery = new Delivery(225L, address);
        order.setDelivery(delivery);

        return order;
    }

    @BeforeEach
    public void setUp() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        Mockito.when(template.execute(Mockito.any()))
                .thenAnswer(invocation -> ((TransactionCallback) invocation.getArgument(0)).doInTransaction(null));
        readingDao = mock(OrderReadingDao.class);
        TestStubStorage storage = new TestStubStorage();
        eventService = mock(EventService.class);
        CheckouterFeatureResolverStub checkouterFeatures = new CheckouterFeatureResolverStub();
        checkouterFeatures.writeValue(ENABLE_SEPARATE_TOTAL_AMOUNT_IN_PAYMENT_BY_ORDERS, true);

        storageOrderService = new StorageOrderService(
                readingDao,
                mock(OrderWritingDao.class),
                mock(ItemServiceDao.class),
                mock(OrderItemHistoryDao.class),
                storage,
                mock(OrderFinancialService.class),
                mock(OrderUpdater.class),
                mock(OrdersWithEmptyPicUrlOnItemProcessor.class),
                mock(StatusUpdateValidator.class),
                mockOrderFetchPostprocessor(checkouterFeatures),
                mock(PaymentService.class),
                mock(ReceiptService.class),
                template,
                eventService,
                mock(ChangeRequestDao.class),
                mock(OrderCostService.class),
                checkouterFeatures,
                mock(ForeignCurrencyPriceService.class),
                mock(ResaleSpecsDao.class)
        );

        shopInfo = new ClientInfo(ClientRole.SHOP, 1L);
    }

    private OrderSecurity mockOrderSecurity() {
        shopMetaData = ShopMetaData.DEFAULT;
        shopService = mock(MemCachingShopServiceWrapper.class);
        OrderSecurity orderSecurity = new OrderSecurity();
        orderSecurity.setShopMetaDataGetterService(shopService);
        Mockito.when(shopService.getMeta(1L)).thenReturn(shopMetaData);
        return orderSecurity;
    }

    private CipherService mockCipherService() {
        CipherService buyerCipherService = mock(CipherService.class);
        when(buyerCipherService.cipher(anyString())).thenReturn("ciphered_id");
        return buyerCipherService;
    }

    private OrderFetchPostprocessor mockOrderFetchPostprocessor(CheckouterFeatureReader checkouterFeatureReader) {
        OrderFetchPostprocessorImpl postprocessor = new OrderFetchPostprocessorImpl();
        postprocessor.setOrderSecurity(mockOrderSecurity());
        postprocessor.setDeliveryOutletService(mock(DeliveryOutletService.class));
        postprocessor.setBuyerCipherService(mockCipherService());
        postprocessor.setStatusUpdateValidator(mock(StatusUpdateValidator.class));
        postprocessor.setCheckouterFeatureReader(checkouterFeatureReader);
        postprocessor.setOrderCancelPolicyModifier(mock(OrderCancelPolicyModifier.class));

        return postprocessor;
    }

    @Test
    public void testGetOrderForNonExistingOrder() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            when(readingDao.getOrder(anyLong(), any(ClientInfo.class))).thenReturn(Optional.empty());
            storageOrderService.getOrder(1L, ClientInfo.SYSTEM);
        });
    }

    @Test
    public void testGetOrderForShopPrivateInfoHidden() {
        Order testOrder = createTestOrder(new Order(), true);
        when(readingDao.getOrder(anyLong(), any(ClientInfo.class), nullable(CheckpointRequest.class)))
                .thenReturn(Optional.of(testOrder));

        Order loadedOrder = storageOrderService.getOrder(1L, shopInfo);

        assertNull(loadedOrder.getBuyer());
        Address loadedAddress = loadedOrder.getDelivery().getShopAddress();

        assertNull(loadedAddress.getApartment());
        assertNull(loadedAddress.getEntrance());
        assertNull(loadedAddress.getEntryPhone());
        assertNull(loadedAddress.getNotes());
        assertNull(loadedAddress.getPhone());
        assertNull(loadedAddress.getRecipient());
        assertNull(loadedAddress.getEstate());
    }

    @Test
    public void testBlueOrderForShopWithBuyerInfoInEarlyStatuses() {
        shopMetaData = ShopMetaDataBuilder.createCopy(shopMetaData)
                .withOrderVisibilityMap(Map.of(
                        OrderVisibility.BUYER, true,
                        OrderVisibility.BUYER_NAME, true,
                        OrderVisibility.BUYER_PHONE, true,
                        OrderVisibility.BUYER_FOR_EARLY_STATUSES, true))
                .build();
        Mockito.when(shopService.getMeta(1L)).thenReturn(shopMetaData);

        Order testOrder = createTestOrder(new Order(), true);
        testOrder.setShopId(1L);
        when(readingDao.getOrder(anyLong(), any(ClientInfo.class), nullable(CheckpointRequest.class)))
                .thenReturn(Optional.of(testOrder));

        Order loadedOrder = storageOrderService.getOrder(1L, shopInfo);

        assertNotNull(loadedOrder.getBuyer());
        assertNotNull(loadedOrder.getBuyer().getPhone());
        assertNotNull(loadedOrder.getBuyer().getFirstName());
        assertNotNull(loadedOrder.getBuyer().getMiddleName());
        assertNotNull(loadedOrder.getBuyer().getLastName());
    }

    @Test
    public void testBlueOrderForShopWithoutBuyerInfoInEarlyStatuses() {
        shopMetaData = ShopMetaDataBuilder.createCopy(shopMetaData)
                .withOrderVisibilityMap(Map.of(
                        OrderVisibility.BUYER, true,
                        OrderVisibility.BUYER_NAME, true,
                        OrderVisibility.BUYER_PHONE, true))
                .build();
        Mockito.when(shopService.getMeta(1L)).thenReturn(shopMetaData);

        Order testOrder = createTestOrder(new Order(), true);
        testOrder.setShopId(1L);
        when(readingDao.getOrder(anyLong(), any(ClientInfo.class), nullable(CheckpointRequest.class)))
                .thenReturn(Optional.of(testOrder));

        Order loadedOrder = storageOrderService.getOrder(1L, shopInfo);

        assertNull(loadedOrder.getBuyer());
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам с пустым списком заказов")
    public void bulkGetOrderChanges_withEmptyIdsList() {
        assertThat(storageOrderService.bulkGetOrderChanges(List.of(), shopInfo)).isEmpty();
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам со слишком большим списком заказов")
    public void bulkGetOrderChanges_tooManyOrderIds() {
        List<Long> orderIds = Stream.iterate(1L, n -> n + 1)
                .limit(StorageOrderService.BULK_GET_ORDERS_CHANGES_IDS_LIMIT + 1)
                .collect(Collectors.toList());
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> storageOrderService.bulkGetOrderChanges(orderIds, shopInfo));
        assertThat(exception.getMessage()).contains("Too many orderIds");
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам. Переданный заказ не существует")
    public void bulkGetOrderChanges_orderNotFound() {
        List<Long> orderIds = List.of(1L);
        when(readingDao.getAccessibleOrders(orderIds, shopInfo, null)).thenReturn(new HashSet<>());
        assertThat(storageOrderService.bulkGetOrderChanges(List.of(1L), shopInfo)).isEmpty();
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам. Передали 1 заказ")
    public void bulkGetOrderChanges_withOneOrder() {
        Order order = initOrder(1L);
        mockOrders(order);
        OrderHistoryEvent event = initEvent(order);
        mockEvents(event);
        List<OrderChanges> orderChanges = storageOrderService.bulkGetOrderChanges(List.of(1L), shopInfo);
        assertThat(orderChanges).hasSize(1);
        OrderChanges change = orderChanges.get(0);
        assertThat(change.getId()).isEqualTo(order.getId());
        assertThat(change.getEvents()).containsOnly(event.getId());
        assertThat(change.getCurrency()).isEqualTo(order.getCurrency());
        assertThat(change.getBuyerCurrency()).isEqualTo(order.getBuyerCurrency());
        assertThat(change.getExchangeRate()).isEqualTo(order.getExchangeRate());
        assertThat(change.getChangedTotal().getDeltaItemsTotal()).isEqualTo(new BigDecimal(0));
        assertThat(change.getChangedByStage()).isEmpty();
        assertThat(change.getItemsChanged()).isEmpty();
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам. Передали 1 заказ без ивентов")
    //без ивентов заказ может оказаться из за лага в репликации. Когда заказ только создан
    public void bulkGetOrderChanges_withOneOrderWithoutEvents() {
        Order order = initOrder(1L);
        mockOrders(order);
        assertThat(storageOrderService.bulkGetOrderChanges(List.of(1L), shopInfo)).isEmpty();
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам. Передали несколько заказов")
    public void bulkGetOrderChanges_withTwoOrders() {
        Order order1 = initOrder(1L);
        Order order2 = initOrder(2L);
        mockOrders(order1, order2);
        mockEvents(initEvent(order1), initEvent(order2));
        assertThat(storageOrderService.bulkGetOrderChanges(List.of(1L, 2L), shopInfo)).hasSize(2);
    }

    @Test
    @DisplayName("Групповое получение изменений по заказам. 1 заказ есть, другого нет")
    public void bulkGetOrderChanges_withTwoOrdersOneEvent() {
        Order order1 = initOrder(1L);
        mockOrders(order1);
        mockEvents(initEvent(order1));
        assertThat(storageOrderService.bulkGetOrderChanges(List.of(1L, 2L), shopInfo)).hasSize(1);
    }

    private void mockOrders(Order... orders) {
        Set<Long> orderIds = Arrays.stream(orders)
                .map(Order::getId)
                .collect(Collectors.toSet());
        when(readingDao.getAccessibleOrders(anyList(), any(ClientInfo.class), anySet()))
                .thenReturn(orderIds);
    }

    private void mockEvents(OrderHistoryEvent... events) {
        when(eventService.getOrdersHistoryEventsByOrders(
                anySet(),
                nullable(EnumSet.class),
                any(EnumSet.class),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                nullable(SortingInfo.class),
                any(ClientInfo.class),
                anySet()
        )).thenReturn(Arrays.asList(events));
    }

}
