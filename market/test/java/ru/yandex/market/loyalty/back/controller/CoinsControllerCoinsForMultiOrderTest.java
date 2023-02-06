package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.api.model.coin.OrderStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersUpdatedCoinsForFront;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.OrderEventInfoDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.trigger.InsertResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderTerminationEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.RefundInOrderEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.PROCESSING_PREPAID;
import static ru.yandex.market.loyalty.core.test.CheckouterMockUtils.getOrderInStatus;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.getRulesContainerWithMinOrderTotal;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CoinsController.class)
public class CoinsControllerCoinsForMultiOrderTest extends MarketLoyaltyBackMockedDbTestBase {
    public static final String DEFAULT_MULTI_ORDER = "multiOrder";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private OrderEventInfoDao orderEventInfoDao;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;

    @Test
    public void shouldReturnCoinForMultiOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        final long firstOrderId = 1L;
        final Order firstOrder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        checkouterMockUtils.mockCheckoutGetOrdersResponse(firstOrder);

        final OrdersUpdatedCoinsForFront coins1 = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(orderStatusUpdatedRequest(firstOrderId));

        assertThat(
                coins1,
                allOf(
                        hasProperty("newCoins", empty()),
                        hasProperty("oldCoins", empty()),
                        hasProperty("recommendedCoins", empty())
                )
        );

        final long secondOrderId = 2L;
        final Order secondOrder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(secondOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        checkouterMockUtils.mockCheckoutGetOrdersResponse(secondOrder);

        final OrdersUpdatedCoinsForFront coins2 = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(orderStatusUpdatedRequest(secondOrderId));

        assertThat(
                coins2,
                allOf(
                        hasProperty("newCoins", hasSize(1)),
                        hasProperty("oldCoins", empty()),
                        hasProperty("recommendedCoins", empty())
                )
        );
    }

    @Test
    public void shouldReturnOldCoinsForMultiOrderStatusUpdatedEvent() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(firstPromo, orderRestriction(PROCESSING_PREPAID));

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;
        final String multiOrderId = "12345";
        checkouterMockUtils.mockCheckouterPagedEvents(
                getOrderInStatus(OrderStatus.PENDING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(firstOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build(),
                getOrderInStatus(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(secondOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build()
        );

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                new OrdersStatusUpdatedRequest(
                        Arrays.asList(firstOrderId, secondOrderId),
                        DEFAULT_UID, null, false
                )
        );
        assertThat(coins.getNewCoins(), hasSize(1));
        assertThat(coins.getOldCoins(), empty());

        checkouterMockUtils.mockCheckouterPagedEvents(
                getOrderInStatus(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(firstOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build(),
                getOrderInStatus(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(secondOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build()
        );
        OrdersUpdatedCoinsForFront retryCoins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                new OrdersStatusUpdatedRequest(
                        Arrays.asList(firstOrderId, secondOrderId),
                        DEFAULT_UID, null, false
                )

        );
        assertThat(retryCoins.getNewCoins(), empty());
        assertThat(retryCoins.getOldCoins(), hasSize(1));
    }

    // хотфикс https://st.yandex-team.ru/MARKETDISCOUNT-3218
    @Test
    public void shouldReturnOrderIdInCoinReasonParamFieldInsteadOfMultiOrderId() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(firstPromo, orderRestriction(PROCESSING_PREPAID));

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;
        final String multiOrderId = "001fa7f4-4b2f-442d-9746-8ccaec71db94";
        checkouterMockUtils.mockCheckouterPagedEvents(
                getOrderInStatus(OrderStatus.PENDING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(firstOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build(),
                getOrderInStatus(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(secondOrderId)
                        .setMultiOrderId(multiOrderId)
                        .build()
        );

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                new OrdersStatusUpdatedRequest(
                        Arrays.asList(firstOrderId, secondOrderId),
                        DEFAULT_UID, null, false
                )
        );
        assertThat(
                coins.getNewCoins(),
                contains(
                        allOf(
                                hasProperty(
                                        "reasonParam",
                                        equalTo(Long.toString(firstOrderId))
                                ),
                                hasProperty(
                                        "reasonParamExt",
                                        hasProperty(
                                                "orderIds",
                                                containsInAnyOrder(
                                                        Long.toString(firstOrderId),
                                                        Long.toString(secondOrderId)
                                                )
                                        )
                                )
                        )
                )
        );
    }


    @Test
    public void shouldReturnCoinForMultiOrderAndDowngradeIfOneOrderNotReachProcessingStatus() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        final long firstOrderId = 1L;
        final Order firstOrder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        checkouterMockUtils.mockCheckoutGetOrdersResponse(firstOrder);

        final OrdersUpdatedCoinsForFront coins1 = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(orderStatusUpdatedRequest(firstOrderId));

        assertThat(
                coins1,
                allOf(
                        hasProperty("newCoins", empty()),
                        hasProperty("oldCoins", empty()),
                        hasProperty("recommendedCoins", empty())
                )
        );

        final long secondOrderId = 2L;
        final Order secondOrder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(secondOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        checkouterMockUtils.mockCheckoutGetOrdersResponse(secondOrder);

        final OrdersUpdatedCoinsForFront coins2 = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(orderStatusUpdatedRequest(secondOrderId));

        assertThat(
                coins2,
                allOf(
                        hasProperty("newCoins", hasSize(1)),
                        hasProperty("oldCoins", empty()),
                        hasProperty("recommendedCoins", empty())
                )
        );

        Order firstOrderTerminated = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        Order secondOrderTerminated = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(secondOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        addOrderTerminationEvent(firstOrderTerminated);
        addOrderTerminationEvent(secondOrderTerminated);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertTrue(triggerEventDao.wasProcessed(
                TriggerEventTypes.ORDER_TERMINATION,
                OrderTerminationEvent.toUniqueKey(DEFAULT_MULTI_ORDER)
        ));
        final List<Coin> coins = coinService.search.getCreatedCoinsByMultiOrderId(
                DEFAULT_MULTI_ORDER);

        assertThat(coins, containsInAnyOrder(
                hasProperty("status", equalTo(CoreCoinStatus.REVOKED)),
                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE)))
        );
    }

    @Test
    public void shouldReturnCoinForMultiOrderAndDowngradeIfAllOrdersNotReachProcessingStatus() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;

        addTerminatedOrderEvents(
                CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                        .setOrderId(firstOrderId)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER)
                        .setOrdersCount(2)
                        .addItem(CheckouterUtils.defaultOrderItem().build())
                        .build()
        );
        addTerminatedOrderEvents(
                CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                        .setOrderId(secondOrderId)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER)
                        .setOrdersCount(2)
                        .addItem(CheckouterUtils.defaultOrderItem().build())
                        .build()
        );

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertTrue(triggerEventDao.wasProcessed(
                TriggerEventTypes.ORDER_TERMINATION,
                OrderTerminationEvent.toUniqueKey(DEFAULT_MULTI_ORDER)
        ));

        final List<Coin> coins = coinService.search.getCreatedCoinsByMultiOrderId(
                DEFAULT_MULTI_ORDER);

        assertThat(coins, containsInAnyOrder(
                hasProperty("status", equalTo(CoreCoinStatus.REVOKED)))
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    public void shouldTerminationEventWaitProcessingEvent() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;

        final Order firstOrder = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        final Order secondOrder = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(secondOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();

        final List<TriggerEvent> terminateEvents = Stream.of(firstOrder, secondOrder)
                .map(o -> addOrderTerminationEvent(o))
                .map(r -> r.getData())
                .collect(Collectors.toList());
        final List<TriggerEvent> processEvents = Stream.of(firstOrder, secondOrder)
                .map(o -> addOrderStatusUpdatedEvent(o))
                .map(r -> r.getData())
                .collect(Collectors.toList());

        triggerEventQueueService.processEvents(terminateEvents, false);
        triggerEventQueueService.processEvents(processEvents, false);
        triggerEventQueueService.processEvents(terminateEvents, false);

        assertTrue(triggerEventDao.wasProcessed(
                TriggerEventTypes.ORDER_TERMINATION,
                OrderTerminationEvent.toUniqueKey(DEFAULT_MULTI_ORDER)
        ));

        final List<Coin> coins = coinService.search.getCreatedCoinsByMultiOrderId(DEFAULT_MULTI_ORDER);

        assertThat(coins, containsInAnyOrder(
                hasProperty("status", equalTo(CoreCoinStatus.REVOKED)))
        );
    }

    @Test
    public void shouldNotRefundCashbackWhenOrderTotalCostFitPromo() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setCashbackRulesContainer(getRulesContainerWithMinOrderTotal(BigDecimal.valueOf(5000)))
        );

        long firstOrderId = 1L;
        long secondOrderId = 2L;
        var itemPrice = BigDecimal.valueOf(1900);   //Total count = 2 + 2 = 4 by default

        var item = CheckouterUtils.defaultOrderItem().setPrice(itemPrice).build();

        var firstOrder = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(item)
                .build();
        var secondOrder = CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                .setOrderId(secondOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(item)
                .build();

        var refundEvent = RefundInOrderEvent.builder()
                .setOrderId(firstOrderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .build();

        orderEventInfoDao.saveOrderEventInfo(OrderEventInfo.createOrderEventInfoFromOrder(firstOrder).build(), true);
        orderEventInfoDao.saveOrderEventInfo(OrderEventInfo.createOrderEventInfoFromOrder(secondOrder).build(), true);

        yandexWalletTransactionDao.enqueueTransactions(
                null,
                "0",
                List.of(YandexWalletNewTransaction.builder()
                        .setAmount(DEFAULT_COIN_FIXED_NOMINAL)
                        .setUid(100000)
                        .setProductId("0")
                        .build()
                ),
                null,
                null,
                promo.getId(),
                YandexWalletTransactionStatus.CONFIRMED,
                YandexWalletTransactionPriority.LOW,
                null,
                DEFAULT_MULTI_ORDER
        );
        triggerEventQueueService.addEventToQueue(refundEvent);
        triggerEventQueueService.processEvents(List.of(refundEvent), false);

        assertTrue("Event wasn't processed", triggerEventDao.wasProcessed(
                TriggerEventTypes.REFUND_IN_ORDER,
                refundEvent.getUniqueKey()
        ));

        assertEquals(
                yandexWalletTransactionDao.findAllByMultiOrderId(DEFAULT_MULTI_ORDER).get(0).getRefundStatus(),
                YandexWalletRefundTransactionStatus.NOT_QUEUED
        );
    }

    private void addTerminatedOrderEvents(Order order) {
        addOrderTerminationEvent(order);
        addOrderStatusUpdatedEvent(order);
    }

    private InsertResult<OrderStatusUpdatedEvent> addOrderStatusUpdatedEvent(Order order) {
        return triggerEventQueueService.addEventToQueue(OrderStatusUpdatedEvent.createEvent(
                null,
                order,
                Collections.emptySet(),
                "request_id"
        ));
    }

    private InsertResult<OrderTerminationEvent> addOrderTerminationEvent(Order order) {
        return triggerEventQueueService.addEventToQueue(
                OrderTerminationEvent.createEvent(
                        CheckouterUtils.getEvent(
                                order,
                                HistoryEventType.ORDER_STATUS_UPDATED,
                                clock
                        ),
                        "request_id"
                )
        );
    }

    static OrderStatusUpdatedRequest orderStatusUpdatedRequest(long orderId) {
        return new OrderStatusUpdatedRequest(orderId, DEFAULT_UID, null, false);
    }
}
