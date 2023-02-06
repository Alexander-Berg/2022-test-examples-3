package ru.yandex.market.loyalty.back.controller;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderStatsDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyRestrictionType;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.OCRMUserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.OrderStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersUpdatedCoinsForFront;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.dao.trigger.DryRunCoinsRecord;
import ru.yandex.market.loyalty.core.dao.trigger.ProhibitedTriggerEventDryRunCoinsDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.model.trigger.RestrictionDescription;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.TriggerRestriction;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderTerminationEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.MatcherUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.coin.CoinCreationReason.ORDER;
import static ru.yandex.market.loyalty.api.model.coin.CoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.api.model.coin.CoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.back.controller.CoinsControllerCoinsForMultiOrderTest.DEFAULT_MULTI_ORDER;
import static ru.yandex.market.loyalty.back.controller.CoinsControllerCoinsForMultiOrderTest.orderStatusUpdatedRequest;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.PROCESSING_PREPAID;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.ORDER_STATUS_UPDATED;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.test.CheckouterMockUtils.getOrderInStatus;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.Formatters.makeNonBreakingSpaces;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(CoinsController.class)
public class CoinsControllerCoinsForOrderTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    private ProhibitedTriggerEventDryRunCoinsDao prohibitedTriggerEventDryRunCoinsDao;
    @Autowired
    private UserBlacklistService userBlacklistService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;

    @Test
    public void shouldReturnCoinForOrder() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, contains(hasProperty("status", equalTo(INACTIVE))));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coins.get(0).getTitle()
        );
    }

    @Test
    public void shouldLogProhibitedTriggerEventCoinForOrder() {
        userBlackListDao.addRecord(new BlacklistRecord.Uid(DEFAULT_UID));
        userBlacklistService.reloadBlacklist();

        Promo promo = createPromoWithTrigger(defaultFixed());

        mockCheckouterGetOrder();

        final OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEvent(
                defaultStatusUpdatedRequest());

        assertEmptyResult(coins);

        assertDryRunCoinCreated(promo.getId(), DEFAULT_ORDER_ID);
    }


    @Test
    public void shouldReturnCoinsForOrderWithinMultiOrder() {
        createPromoWithTrigger(defaultFixed());

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;

        sendOrderStatusUpdatedEvent(
                firstOrderId,
                OrderStatus.PROCESSING
        );

        sendOrderStatusUpdatedEvent(
                secondOrderId,
                OrderStatus.PROCESSING
        );

        assertCreatedCoinsForOrder(
                firstOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(INACTIVE))
                                )
                        )
                )
        );

        assertCreatedCoinsForOrder(
                secondOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(INACTIVE))
                                )
                        )
                )
        );

        addDeliveredOrderEventForMultiOrder(firstOrderId);

        addDeliveredOrderEventForMultiOrder(secondOrderId);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertActivatedCoinsForOrder(
                firstOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(ACTIVE))
                                )
                        )
                )
        );

        assertActivatedCoinsForOrder(
                secondOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(ACTIVE))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldReturnNoCoinsForCancelledOrderWithinMultiOrder() {
        createPromoWithTrigger(defaultFixed());

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;

        sendOrderStatusUpdatedEvent(
                firstOrderId,
                OrderStatus.PROCESSING
        );

        sendOrderStatusUpdatedEvent(
                secondOrderId,
                OrderStatus.PROCESSING
        );

        assertCreatedCoinsForOrder(
                firstOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(INACTIVE))
                                )
                        )
                )
        );

        assertCreatedCoinsForOrder(
                secondOrderId,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("status", equalTo(INACTIVE))
                                )
                        )
                )
        );

        addCancelledOrderEventForMultiOrder(firstOrderId);

        addCancelledOrderEventForMultiOrder(secondOrderId);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertActivatedCoinsForOrder(
                firstOrderId,
                empty()
        );

        assertActivatedCoinsForOrder(
                secondOrderId,
                empty()
        );
    }

    private void addDeliveredOrderEventForMultiOrder(long orderId) {
        triggerEventQueueService.addEventToQueue(
                OrderTerminationEvent.createEvent(CheckouterUtils.getEvent(
                        CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                                .setOrderId(orderId)
                                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                                .setProperty(OrderPropertyType.SELECTED_CASHBACK_OPTION, CashbackOption.EMIT)
                                .setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 2)
                                .addItem(CheckouterUtils.defaultOrderItem().build())
                                .build(),
                        HistoryEventType.ORDER_STATUS_UPDATED,
                        clock
                ), "request_id")
        );
    }

    private void addCancelledOrderEventForMultiOrder(long orderId) {
        triggerEventQueueService.addEventToQueue(
                OrderTerminationEvent.createEvent(CheckouterUtils.getEvent(
                        CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                                .setOrderId(orderId)
                                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                                .setProperty(OrderPropertyType.SELECTED_CASHBACK_OPTION, CashbackOption.EMIT)
                                .setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 2)
                                .addItem(CheckouterUtils.defaultOrderItem().build())
                                .build(),
                        HistoryEventType.ORDER_STATUS_UPDATED,
                        clock
                ), "request_id")
        );
    }

    private void assertActivatedCoinsForOrder(
            long firstOrderId, Matcher<Collection<? extends UserCoinResponse>> listMatcher
    ) {
        final List<UserCoinResponse> deliveredCoins1 = marketLoyaltyClient.getCoinsForOrder(
                firstOrderId, OrderStatus.DELIVERED.name());

        assertThat(
                deliveredCoins1,
                listMatcher
        );
    }

    private void assertCreatedCoinsForOrder(long firstOrderId, Matcher<List<UserCoinResponse>> listMatcher) {
        final List<UserCoinResponse> createdCoins1 = marketLoyaltyClient.getCoinsForOrder(
                firstOrderId, OrderStatus.PROCESSING.name());

        assertThat(
                createdCoins1,
                listMatcher
        );
    }

    private void sendOrderStatusUpdatedEvent(long secondOrderId, OrderStatus status) {
        checkouterMockUtils.mockCheckoutGetOrdersResponse(
                mockOrderForTwoOrderMultiOrder(secondOrderId, status)
        );

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(
                orderStatusUpdatedRequest(secondOrderId)
        );
    }

    private static Order mockOrderForTwoOrderMultiOrder(long orderId, OrderStatus status) {
        return CheckouterUtils.defaultOrder(status)
                .setOrderId(orderId)
                .setMultiOrderId(DEFAULT_MULTI_ORDER)
                .setOrdersCount(2)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
    }

    @Test
    public void shouldReturnRevocableCoinForOrder() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getRevocableCoinsForOrder(orderId);

        assertThat(coins, contains(hasProperty("status", equalTo(INACTIVE))));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coins.get(0).getTitle()
        );
    }

    @Test
    public void shouldNotReturnRevocableCoinForOrderWithUsedCoin() {
        createPromoWithTrigger(defaultFixed());

        final long orderId = 123213L, orderIdToSpendCoin = 456789L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        coinService.lifecycle.activateInactiveCoins(CoreMarketPlatform.BLUE,
                coinService.search.getCreatedCoinsByOrderId(orderId));
        List<CoinKey> coinKeysByOrderId = coinService.search.getCreatedCoinsByOrderId(orderId)
                .stream()
                .map(Coin::getCoinKey)
                .collect(Collectors.toList());
        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderItem()
                                .withOrderId(String.valueOf(orderIdToSpendCoin))
                                .build()
                )
                .withCoins(coinKeysByOrderId)
                .build()
        );

        assertThat(marketLoyaltyClient.getRevocableCoinsForOrder(orderId), empty());
    }

    @Test
    public void shouldCreateTwoCoinForOrder() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(PROCESSING_PREPAID)
        );

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultPercent()),
                orderRestriction(PROCESSING_PREPAID)
        );

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coinsForFront =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());
        assertThat(
                coinsForFront.getNewCoins(),
                containsInAnyOrder(
                        hasProperty("coinType", equalTo(CoinType.FIXED)),
                        hasProperty("coinType", equalTo(CoinType.PERCENT))
                )
        );
    }


    @Test
    public void shouldSetOrderId() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, hasSize(1));
        assertThat(coins, contains(allOf(
                hasProperty("reason", equalTo(ORDER)),
                hasProperty("reasonParam", equalTo(Long.toString(orderId)))
        )));
    }

    @Test
    public void shouldReturnCoinForOrderNoAuth() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setNoAuth(true)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, contains(MatcherUtils.coinStatus(INACTIVE, true)));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coins.get(0).getTitle()
        );
    }

    @Test
    public void shouldFailIfRequestedCoinsForOrderThatNotProcessedYet() {
        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.getCoinsForOrder(123213L, OrderStatus.PROCESSING.name())
        );
        assertEquals(MarketLoyaltyErrorCode.EVENT_WAS_NOT_PROCESSED_YET, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldReturnCoinsForOrderWithStatusIfEventWasProcessed() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getCoinsForOrder(orderId, OrderStatus.PROCESSING.name());

        assertThat(coins, contains(MatcherUtils.coinStatus(INACTIVE, false)));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coins.get(0).getTitle()
        );
    }

    @Test
    public void shouldNotReturnFlashCoinsForOrderWithStatusIfEventWasProcessed() {
        createPromoWithTrigger(defaultFixed().setExpiration(ExpirationPolicy.flash(Duration.ofDays(1))));

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getCoinsForOrder(orderId, OrderStatus.PROCESSING.name());

        assertThat(coins, is(empty()));
    }

    @Test
    public void shouldProcessOrderStatusUpdate() {
        String mskuForFirstCoin = "123141412";
        int categoryForSecondCoin = 879465459;

        createPromoWithTrigger(
                defaultFixed().addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuForFirstCoin));

        createPromoWithTrigger(
                defaultFixed().addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryForSecondCoin));

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());


        assertThat(coins.getNewCoins(), hasSize(2));
    }

    @Test
    public void shouldNotWriteNoteForNoTriggers() {
        createPromoWithTrigger(defaultFixed());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .setPaymentType(PaymentType.POSTPAID)
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());


        assertThat(coins.getNewCoins(), empty());
        List<TriggerEvent> events = triggerEventDao.getAll();
        assertThat(events, hasSize(1));
        String note = jdbcTemplate.queryForObject(
                "select note from trigger_event where id = " + events.get(0).getId(), String.class);
        assertThat(note, oneOf("", nullValue()));
    }

    @Test
    public void shouldWriteLogForNoTriggers() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                firstPromo, orderRestriction(PROCESSING_PREPAID));

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .setPaymentType(PaymentType.POSTPAID)
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEvent(
                defaultStatusUpdatedRequest());

        assertThat(coins.getNewCoins(), empty());
        List<TriggerEvent> events = triggerEventDao.getAll();
        assertThat(events, hasSize(1));
    }

    @Test
    public void shouldReturnCoinForNoAuthOnOrderStatusUpdate() {
        createPromoWithTrigger(defaultFixed());

        Order order = getOrderInStatus(OrderStatus.PROCESSING).build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());

        assertThat(coins.getNewCoins(), contains(allOf(
                hasProperty("status", equalTo(INACTIVE)),
                hasProperty("requireAuth", equalTo(true)),
                hasProperty("activationToken", notNullValue())
        )));
    }

    /**
     * Тест на поведение ручки /coins/orderStatusUpdated при активации монетки.
     */
    @Test
    public void shouldNotReturnCoinForNoAuthOnOrderStatusUpdateWhenOrdersStatusAlreadyWasUpdated() {
        createPromoWithTrigger(defaultFixed());

        // Заказ в PENDING
        Order order = getOrderInStatus(OrderStatus.PENDING).build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        // Активируем монетки
        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());
        assertThat(coins.getNewCoins(), hasSize(1));

        // Переводим заказ в PROCESSING
        order = getOrderInStatus(OrderStatus.PROCESSING).build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        // Монетки уже активированы, поэтому не возвращаются
        coins = marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());
        assertThat(coins.getNewCoins(), empty());
    }

    /**
     * Тест на поведение ручки /coins/ordersStatusUpdatedAndGetCoins при активации монетки и
     * последующих вызовах при смене статуса.
     */
    @Test
    public void shouldAlwaysReturnCoinForNoAuthOnOrdersStatusUpdate() {
        createPromoWithTrigger(defaultFixed());

        Order order = getOrderInStatus(OrderStatus.PENDING).build();
        checkouterMockUtils.mockCheckouterPagedEvents(order);

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                defaultOrdersStatusUpdatedRequestNoAuth()
        );
        assertThat(coins.getOldCoins(), empty());
        assertThat(coins.getNewCoins(), hasSize(1));

        order = getOrderInStatus(OrderStatus.PROCESSING).build();
        checkouterMockUtils.mockCheckouterPagedEvents(order);

        coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                defaultOrdersStatusUpdatedRequestNoAuth()
        );

        assertThat(
                coins.getOldCoins(),
                contains(allOf(
                        hasProperty("status", equalTo(INACTIVE)),
                        hasProperty("requireAuth", equalTo(true)),
                        hasProperty("activationToken", notNullValue())
                ))
        );
    }

    /**
     * Тест на поведение ручки /coins/ordersStatusUpdatedAndGetCoins при активации монетки и
     * последующих вызовах при смене статуса.
     */
    @Test
    public void shouldAlwaysReturnCoinForNoAuthMultiOrderOnOrdersStatusUpdate() {
        createPromoWithTrigger(defaultFixed());

        Long orderId1 = 123456L;
        Long orderId2 = 456789L;
        Order order1 = getOrderInStatus(orderId1, OrderStatus.PENDING)
                .setOrdersCount(2)
                .setMultiOrderId("multiOrderId")
                .build();
        Order order2 = getOrderInStatus(orderId2, OrderStatus.PENDING)
                .setOrdersCount(2)
                .setMultiOrderId("multiOrderId")
                .build();
        checkouterMockUtils.mockCheckouterPagedEvents(order1, order2);

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                ordersStatusUpdatedRequestNoAuth(orderId1, orderId2)
        );
        assertThat(coins.getNewCoins(), hasSize(1));
        assertThat(coins.getOldCoins(), empty());

        order1 = getOrderInStatus(orderId1, OrderStatus.PROCESSING)
                .setOrdersCount(2)
                .setMultiOrderId("multiOrderId")
                .build();
        order2 = getOrderInStatus(orderId2, OrderStatus.PROCESSING)
                .setOrdersCount(2)
                .setMultiOrderId("multiOrderId")
                .build();
        checkouterMockUtils.mockCheckouterPagedEvents(order1, order2);

        coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                ordersStatusUpdatedRequestNoAuth(orderId1, orderId2)
        );

        assertThat(coins.getNewCoins(), empty());
        assertThat(coins.getOldCoins(), hasSize(1));
    }

    /**
     * При ретраях /coins/ordersStatusUpdatedAndGetCoins возвращает уже привязанные монетки в oldCoins,
     * а только что привязанные в newCoins.
     */
    @Test
    public void shouldProcessOrderStatusUpdateAndGetOrdersOnRetry() {
        String mskuForFirstCoin = "123141412";
        int categoryForSecondCoin = 879465459;
        createPromoWithTrigger(
                defaultFixed().addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuForFirstCoin));

        createPromoWithTrigger(
                defaultFixed().addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryForSecondCoin));

        Order order = getOrderInStatus(OrderStatus.PROCESSING).build();
        checkouterMockUtils.mockCheckouterPagedEvents(order);

        OrdersUpdatedCoinsForFront coins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                defaultOrdersStatusUpdatedRequestNoAuth());
        assertThat(coins.getNewCoins(), hasSize(2));
        assertThat(coins.getOldCoins(), empty());

        OrdersUpdatedCoinsForFront retryCoins = marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                defaultOrdersStatusUpdatedRequestNoAuth()
        );
        assertThat(retryCoins.getNewCoins(), empty());
        assertThat(retryCoins.getOldCoins(), hasSize(2));
    }

    @Test
    public void shouldProcessOrderStatusUpdateRetry() {
        createPromoWithTrigger(defaultFixed());

        createPromoWithTrigger(defaultFixed());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());


        assertThat(coins.getNewCoins(), hasSize(2));

        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront retryCoins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());


        assertThat(retryCoins.getOldCoins(), hasSize(2));
        assertThat(coins.getOldCoins(), containsInAnyOrder(retryCoins.getNewCoins()
                .stream()
                .map(c -> samePropertyValuesAs(
                        c,
                        "coinRestrictions"
                )) // костыль списки не сравниваются по содержимому
                .collect(Collectors.toList())));
    }


    @Test
    public void shouldGetCoin() {
        String mskuForFirstCoin = "123141412";
        createPromoWithTrigger(
                defaultFixed().addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuForFirstCoin));

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        UserCoinResponse coin = marketLoyaltyClient.getCoin(coins.getNewCoins().get(0).getId());

        assertThat(coin, samePropertyValuesAs(coins.getNewCoins().get(0), "coinRestrictions"));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coin.getTitle()
        );
    }

    @Test
    public void shouldGetCoinForNoAuth() {
        createPromoWithTrigger(defaultFixed());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .setNoAuth(true)
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());

        assertThat(coins.getNewCoins(), hasSize(1));
    }

    @Test
    public void shouldNotReturnCoinForStickOnlyOrder() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().setCategoryId(STICK_CATEGORY).build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, is(empty()));
    }

    @Test
    public void shouldReturnCoinForStickWithDefaultItemOrder() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .addItem(CheckouterUtils.defaultOrderItem()
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .setCategoryId(STICK_CATEGORY)
                        .build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, contains(hasProperty("status", equalTo(INACTIVE))));
        assertEquals(
                makeNonBreakingSpaces(CoreCoinType.FIXED.getDefaultTitle(DEFAULT_COIN_FIXED_NOMINAL), "test"),
                coins.get(0).getTitle()
        );
    }

    @Test
    public void shouldAddPromoKeyToOrderResponse() {
        Promo promo = createPromoWithTrigger(defaultFixed());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .setNoAuth(true)
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        OrdersUpdatedCoinsForFront coins =
                marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequestNoAuth());
        assertThat(
                coins.getNewCoins()
                ,
                everyItem(
                        hasProperty("promoKey", equalTo(promo.getPromoKey()))
                )
        );
    }

    @Test
    public void shouldReturnSameUsedCoinsForEachPartOfMultiOrderOnOcrmOrderCoinsRequest() {
        createPromoWithTrigger(defaultFixed());

        final long firstOrderId = 1L;
        final long secondOrderId = 2L;

        sendOrderStatusUpdatedEvent(
                firstOrderId,
                OrderStatus.PROCESSING
        );

        sendOrderStatusUpdatedEvent(
                secondOrderId,
                OrderStatus.PROCESSING
        );

        addDeliveredOrderEventForMultiOrder(firstOrderId);
        addDeliveredOrderEventForMultiOrder(secondOrderId);

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        Coin coinForMultiOrder = coinService.search.getCreatedCoinsByMultiOrderId(DEFAULT_MULTI_ORDER).get(0);

        spendCoinForTwoMultiOrderPart(firstOrderId, secondOrderId, coinForMultiOrder);

        List<OCRMUserCoinResponse> usedCoinsForFirstMultiOrderPart =
                marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(firstOrderId).getUsedCoins();
        List<OCRMUserCoinResponse> usedCoinsForSecondMultiOrderPart =
                marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(secondOrderId).getUsedCoins();

        Matcher<List<OCRMUserCoinResponse>> sizeAndCorrectFieldsMatcher = allOf(
                hasSize(1),
                everyItem(hasProperty("id", equalTo(coinForMultiOrder.getCoinKey().getId()))),
                everyItem(hasProperty("promoId", equalTo(coinForMultiOrder.getPromoId()))),
                everyItem(hasProperty("requireAuth", equalTo(coinForMultiOrder.getRequireAuth()))),
                everyItem(hasProperty("activationToken", equalTo(coinForMultiOrder.getActivationToken()))),
                everyItem(hasProperty("reasonParam", equalTo(coinForMultiOrder.getReasonParam()))),
                everyItem(hasProperty("nominal", equalTo(coinForMultiOrder.getNominal())))
        );

        assertThat(
                usedCoinsForFirstMultiOrderPart,
                sizeAndCorrectFieldsMatcher
        );
        assertThat(
                usedCoinsForSecondMultiOrderPart,
                sizeAndCorrectFieldsMatcher
        );
    }


    @Test
    public void shouldProhibitCoinEmissionForUserWithTooMuchOrders() {
        configurationService.set("market.loyalty.config.max.per.day.user.orders.count", 2);

        mockUserRestrictions(2, 0);

        Promo promo = createPromoWithTrigger(defaultFixed());

        mockCheckouterGetOrder();

        final OrdersUpdatedCoinsForFront coins = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        assertEmptyResult(coins);

        assertDryRunCoinCreated(promo.getId(), DEFAULT_ORDER_ID);
    }

    @Test
    public void shouldProhibitCoinEmissionForGlueWithTooMuchOrders() {
        configurationService.set("market.loyalty.config.max.per.day.glue.orders.count", 5);

        mockUserRestrictions(5, 0);

        Promo promo = createPromoWithTrigger(defaultFixed());

        mockCheckouterGetOrder();

        final OrdersUpdatedCoinsForFront coins = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        assertEmptyResult(coins);

        assertDryRunCoinCreated(promo.getId(), DEFAULT_ORDER_ID);
    }

    @Test
    public void shouldAllowCoinEmissionForUserWithNotMuchOrders() {
        configurationService.set("market.loyalty.config.max.per.day.glue.orders.count", 5);

        mockUserRestrictions(2, 0);

        createPromoWithTrigger(defaultFixed());

        mockCheckouterGetOrder();

        final OrdersUpdatedCoinsForFront coins = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        assertThat(coins, allOf(
                hasProperty("newCoins", not(empty())),
                hasProperty("oldCoins", empty())
        ));
    }

    @Test
    public void shouldReturnCoinForWhiteOrder() {
        createPromoWithTrigger(defaultFixed());

        long orderId = 123213L;
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .setRgb(Color.WHITE)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.sendOrderStatusUpdatedEvent(defaultStatusUpdatedRequest());

        List<UserCoinResponse> coins = marketLoyaltyClient.getAllCoinsForOrder(orderId);

        assertThat(coins, contains(hasProperty("status", equalTo(INACTIVE))));
    }

    private void mockUserRestrictions(int active, int cancelled) {
        final OrderCountDto stat = new OrderCountDto(active, 0, cancelled, active + cancelled);
        antiFraudMockUtil.mockUserRestrictions(DEFAULT_UID, LoyaltyRestrictionType.OK, new OrderStatsDto(
                stat,
                null,
                stat,
                null
        ));
    }

    private void mockCheckouterGetOrder() {
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setUid(DEFAULT_UID)
                .setOrderId(DEFAULT_ORDER_ID)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);
    }

    private Promo createPromoWithTrigger(SmartShoppingPromoBuilder builder) {
        Promo promo = promoManager.createSmartShoppingPromo(builder);
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(PROCESSING_PREPAID));
        return promo;
    }

    private static void assertEmptyResult(OrdersUpdatedCoinsForFront coins) {
        assertThat(coins, allOf(
                hasProperty("newCoins", empty()),
                hasProperty("oldCoins", empty())
        ));
    }

    private void assertDryRunCoinCreated(long promoId, long orderId) {
        final List<DryRunCoinsRecord> records = prohibitedTriggerEventDryRunCoinsDao.getAllRecords();
        assertThat(
                records,
                allOf(
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("orderId", equalTo(orderId)),
                                        hasProperty(
                                                "dryRunCoins",
                                                contains(
                                                        allOf(
                                                                hasProperty("promoId", equalTo(promoId))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private void spendCoinForTwoMultiOrderPart(long firstOrderId, long secondOrderId, Coin coin) {
        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder().withOrderId(String.valueOf(firstOrderId))
                                .withOrderItem().build(),
                        orderRequestBuilder().withOrderId(String.valueOf(secondOrderId))
                                .withOrderItem().build()
                )
                .withCoins(coin.getCoinKey())
                .build()
        );
    }

    private static OrderStatusUpdatedRequest defaultStatusUpdatedRequest() {
        return CheckouterUtils.defaultStatusUpdatedRequest(DEFAULT_ORDER_ID);
    }

    private static OrderStatusUpdatedRequest defaultStatusUpdatedRequestNoAuth() {
        return new OrderStatusUpdatedRequest(DEFAULT_ORDER_ID, null, DEFAULT_MUID, true);
    }

    private static OrdersStatusUpdatedRequest defaultOrdersStatusUpdatedRequestNoAuth() {
        return ordersStatusUpdatedRequestNoAuth(DEFAULT_ORDER_ID);
    }

    private static OrdersStatusUpdatedRequest ordersStatusUpdatedRequestNoAuth(Long... orderIds) {
        return new OrdersStatusUpdatedRequest(
                Arrays.asList(orderIds),
                null, DEFAULT_MUID, true
        );
    }

    @NotNull
    private static List<String> getTriggerRestrictions(Trigger<OrderStatusUpdatedEvent> trigger) {
        return Stream
                .concat(
                        trigger.getRestrictions().stream()
                                .map(TriggerRestriction::getFactoryName),
                        ORDER_STATUS_UPDATED.getRestrictionDescriptions().stream()
                                .filter(RestrictionDescription::isByDefault)
                                .map(RestrictionDescription::getRestrictionType)
                                .map(TriggerRestrictionType::getFactoryName)
                )
                .collect(Collectors.toList());
    }
}
