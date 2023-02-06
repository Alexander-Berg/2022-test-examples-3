package ru.yandex.market.loyalty.admin.tms.checkouter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.loyalty.admin.event.CheckouterEventHandler;
import ru.yandex.market.loyalty.admin.event.EventError;
import ru.yandex.market.loyalty.admin.event.dao.EventErrorDao;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.CoinRevokeQueueProcessor;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.api.model.red.RedOrder;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryTraceDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinHistoryTraceRecord;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.coin.EmissionRestriction;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.coupon.CouponHistoryTraceRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventPersistentDataRepository;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.antifraud.AntiFraudFuture;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.coin.CoinHistoryTraceDao;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountAntifraudService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.service.mail.AlertNotificationService;
import ru.yandex.market.loyalty.core.service.perks.StatusFeaturesSet;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.test.BREMockUtils;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.trigger.restrictions.SetRelation;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.OrderRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor.MAX_RETRY_COUNT;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.WELCOME_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.REVOKED;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.TERMINATED;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.USED;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.EFFECTIVELY_PROCESSING;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.SKIP_OLD_ORDER_ENABLED_ENABLED;
import static ru.yandex.market.loyalty.core.service.discount.DiscountChangeSource.TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_BUDGET;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SHOP_PROMO_ID;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderAmountRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.userSegmentsRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 06.07.17
 */
@TestFor({CheckouterEventRestProcessor.class, TriggerEventTmsProcessor.class, CheckouterEventProcessor.class, CoinRevokeQueueProcessor.class})
public class CheckouterEventProcessorTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final long ORDER_ID = 4524543L;
    private static final long ANOTHER_ORDER_ID = ORDER_ID + 1;
    private static final String MULTI_ORDER_ID = "MO9999";

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponDao couponDao;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoService promoService;
    @Autowired
    private MetaTransactionDao metaTransactionDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private EventErrorDao eventErrorDao;
    @Autowired
    private AlertNotificationService alertNotificationService;
    @Autowired
    private BREMockUtils breMockUtils;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private CheckouterEventProcessor eventProcessor;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private DiscountAntifraudService discountAntifraudService;
    @Qualifier("orderPaidDataDao")
    @Autowired
    private TriggerEventPersistentDataRepository<OrderPaidData> triggerEventPersistentDataRepository;
    @Autowired
    private CoinHistoryTraceDao coinHistoryTraceDao;
    @Autowired
    private CouponHistoryTraceDao couponHistoryTraceDao;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private CoinRevokeQueueProcessor coinRevokeQueueProcessor;

    @Test
    public void shouldRevertCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(
                builder.build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertEquals(USED, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET)
        );

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());
    }

    @Test
    public void shouldRevertCoinAsync() {
        configurationService.set(ConfigurationService.ASYNC_REVOKE_COINS, true);

        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(EFFECTIVELY_PROCESSING));

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<Coin> coins = coinService.search.getCoinsByUid(DEFAULT_UID, 10);
        assertThat(coins, hasSize(1));
        assertEquals(INACTIVE, coins.get(0).getStatus());
        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        coins = coinService.search.getCoinsByUid(DEFAULT_UID, 10);
        assertThat(coins, hasSize(1));
        assertEquals(INACTIVE, coins.get(0).getStatus());

        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );

        coinRevokeQueueProcessor.processCoinRevokeQueue(Duration.ofMinutes(1));

        coins = coinService.search.getCoinsByUid(DEFAULT_UID, 10);
        assertThat(coins, hasSize(1));
        assertEquals(REVOKED, coins.get(0).getStatus());
        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());
    }

    @Test
    public void shouldNotReturnBudgetAfterOrderItemsUpdated() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(
                builder.build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertEquals(USED, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        OrderItem orderItem1 = defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.valueOf(2))
                .build();

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrderId(ORDER_ID)
                        .addItem(orderItem1)
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        orderItem1.setCount(1);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrderId(ORDER_ID)
                        .addItem(orderItem1)
                        .build(),
                HistoryEventType.ITEMS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setOrderId(ORDER_ID)
                        .addItems(List.of(orderItem1))
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );
    }

    @Test
    public void shouldReturnBudgetAfterOrderCancelledForEndedPromo() {
        clock.setDate(valueOf("2022-07-25 16:00:00"));

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(valueOf("2022-07-25 15:00:00"))
                        .setEndDate(valueOf("2022-07-30 15:00:00"))
                        .setExpiration(ExpirationPolicy.toEndOfPromo()));

        CoinKey coin = coinService.create.createCoin(
                smartShoppingPromo,
                defaultAuth().build()
        );

        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coin);

        discountService.spendDiscount(
                builder.build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        assertEquals(USED, coinService.search.getCoin(coin).orElseThrow(AssertionError::new).getStatus());

        clock.setDate(valueOf("2022-08-25 15:00:00"));

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(coin).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET)
        );
    }

    @Test
    public void shouldRevertTwoCoins() {
        CoinKey firstCoin = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()), defaultAuth().build()
        );
        CoinKey secondCoin = coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()), defaultAuth().build()
        );
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(firstCoin, secondCoin);

        discountService.spendDiscount(
                builder.build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertEquals(USED, coinService.search.getCoin(firstCoin).orElseThrow(AssertionError::new).getStatus());
        assertEquals(USED, coinService.search.getCoin(secondCoin).orElseThrow(AssertionError::new).getStatus());

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(firstCoin).orElseThrow(AssertionError::new).getStatus());
        assertEquals(ACTIVE, coinService.search.getCoin(secondCoin).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldRevertCoinWhenCreatedOneAlreadySpend() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smartShoppingPromo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);
        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                .build()
        ).withCoins(coinService.search.getCreatedCoinsByOrderId(ORDER_ID).get(0).getCoinKey());

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        List<CoinHistoryTraceRecord> coinHistoryTraceDaoRecords =
                coinHistoryTraceDao.getRecordsByDiscountChangeSource(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS);
        assertThat(coinHistoryTraceDaoRecords, hasSize(1));
        assertThat(coinHistoryTraceDaoRecords.get(0).getTriggerEventId(), notNullValue());
        assertThat(coinHistoryTraceDaoRecords.get(0).getCheckouterEventId(), notNullValue());
    }

    @Test
    public void shouldRevertCoinsOnMultiOrderWithZeroDiscountOnOneOfThem() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(100_000)))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.TEN))
                        .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                        .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ANOTHER_ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldRevokeCoinOnCancelOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                contains(hasProperty("status", equalTo(REVOKED)))
        );

        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldRevokeCoinOnCancelAndCreateNewOneAfterActivateForOneCoinPromo() {
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setClid(12345L)
                .setShopPromoId(SHOP_PROMO_ID)
                .setAnaplanId("ANAPLAN_ID")
                .setCode("PROMOCODE")
                .setEmissionRestriction(EmissionRestriction.ONE_COIN));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(100L)
                .externalPromocodes(Set.of("PROMOCODE"))
                .build());

        assertThat(
                coinService.search.getCoinsByUid(CoinSearchRequest.forUserId(100L)),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(REVOKED))
                        ),
                        allOf(
                                hasProperty("status", equalTo(ACTIVE))
                        )
                )
        );
    }

    @Test
    public void shouldRevokeCoinOnCancelAndCreateNewOneAfterActivateForOneActiveCoinPromo() {
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setClid(12345L)
                .setShopPromoId(SHOP_PROMO_ID)
                .setAnaplanId("ANAPLAN_ID")
                .setCode("PROMOCODE")
                .setEmissionRestriction(EmissionRestriction.ONE_ACTIVE_COIN));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(100L)
                .externalPromocodes(Set.of("PROMOCODE"))
                .build());

        assertThat(
                coinService.search.getCoinsByUid(CoinSearchRequest.forUserId(100L)),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(REVOKED))
                        ),
                        allOf(
                                hasProperty("status", equalTo(ACTIVE))
                        )
                )
        );
    }

    @Test
    public void shouldReissueCoinOnItemsUpdated() {
        Promo bigPromo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(500)));
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                bigPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.valueOf(5000), null)
        );

        Promo smallPromo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(100)));
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smallPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );

        OrderItem orderItem1 = defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();
        OrderItem orderItem2 = defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrderId(ORDER_ID)
                        .addItems(List.of(orderItem1, orderItem2))
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrderId(ORDER_ID)
                        .addItems(List.of(orderItem1))
                        .build(),
                HistoryEventType.ITEMS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setOrderId(ORDER_ID)
                        .addItems(List.of(orderItem1))
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(REVOKED)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        ),
                        allOf(
                                hasProperty("status", equalTo(ACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(100)))
                        )
                )
        );
    }

    @Test
    public void shouldRevokeCoinWithInactiveStatusOnCancelOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                contains(hasProperty("status", equalTo(REVOKED)))
        );

        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotRevokeCoinOnCancelOrderIfCoinWasUsed() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.DELIVERED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .build()
        ).withCoins(coinService.search.getCreatedCoinsByOrderId(ORDER_ID).get(0).getCoinKey());

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                contains(hasProperty("status", equalTo(USED)))
        );
    }

    @Test
    public void shouldRetryAfterCheckouter500() {
        Order order = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(ORDER_ID)
                .addItems(Collections.emptyList())
                .build();
        OrderHistoryEvent event = CheckouterUtils.getEvent(order, HistoryEventType.ORDER_STATUS_UPDATED, clock);

        when(checkouterClient.orderHistoryEvents().getOrderHistoryEvents(anyLong(), anyInt(), anySet(), eq(false),
                anySet(), any(OrderFilter.class)
        ))
                .thenThrow(new ErrorCodeException("code", "message", 500))
                .thenReturn(new OrderHistoryEvents(Collections.singleton(event)));

        processor.processCheckouterEvents(0, 4, 0);
    }

    @Test
    public void shouldRetryErrorEvent() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        assertEquals(USED, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.CANCELLED)
                .setOrderId(ORDER_ID)
                .addItems(Collections.emptyList())
                .build();

        OrderHistoryEvent event = CheckouterUtils.getEvent(order, HistoryEventType.ORDER_STATUS_UPDATED, clock);

        eventErrorDao.insertEventError(new EventError(
                1L, eventProcessor.writeJson(event), new RuntimeException().toString(),
                CheckouterEventHandler.PERSIST_CHANGE_STATUS_TRIGGER_EVENT
        ));

        processor.retryCheckouterEvents(1);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(ACTIVE, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());

        assertThat(
                promoService.getPromo(smartShoppingPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET)
        );

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        assertThat(eventErrorDao.getEventsForRetry(MAX_RETRY_COUNT, 1), empty());
    }

    @Test
    public void shouldRevertSpendingOnRedOrder() {
        long redPromoId = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse(PromoSubType.RED_ORDER)
                        .setPlatform(CoreMarketPlatform.RED)
        ).getPromoId().getId();
        BigDecimal fullDiscount = BigDecimal.valueOf(300);

        OrderRequestWithBundlesBuilder orderRequestBuilder = orderRequestWithBundlesBuilder()
                .withOrderId(String.valueOf(ORDER_ID));
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 1, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithBundlesRequest orderRequest = orderRequestBuilder.build();
        discountService.spendDiscount(
                builder(orderRequest)
                        .withPlatform(MarketPlatform.RED)
                        .withRedOrder(new RedOrder(redPromoId, fullDiscount))
                        .build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertThat(
                promoService.getPromo(redPromoId).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(fullDiscount))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(promoService.getPromo(redPromoId).getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

        List<CouponHistoryTraceRecord> coinHistoryTraceDaoRecords =
                couponHistoryTraceDao.getRecordsByCouponChangeSource(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS);
        assertThat(coinHistoryTraceDaoRecords, hasSize(1));
        assertThat(coinHistoryTraceDaoRecords.get(0).getTriggerEventId(), notNullValue());
    }

    @Test
    public void testRevertCouponByCancellationOnSingleUseCouponRule() {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Coupon coupon = createCoupon(couponPromo);


        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build());

        discountService.spendDiscount(
                builder.withCoupon(coupon.getCode()).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertEquals(CouponStatus.USED, couponDao.getCouponById(coupon.getId()).getStatus());

        assertThat(
                promoService.getPromo(couponPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COUPON_VALUE))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());


        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertEquals(CouponStatus.ACTIVE, couponDao.getCouponById(coupon.getId()).getStatus());

        assertThat(
                promoService.getPromo(couponPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET)
        );

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());
    }

    @Test
    public void testRevertCouponByCancellationOnInfinityUseCouponRule() {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        Coupon coupon = couponService.getCouponByPromo(couponPromo);

        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build());

        discountService.spendDiscount(
                builder.withCoupon(coupon.getCode()).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        assertEquals(CouponStatus.ACTIVE, couponDao.getCouponById(coupon.getId()).getStatus());

        assertThat(
                promoService.getPromo(couponPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COUPON_VALUE))
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());


        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(CouponStatus.ACTIVE, couponDao.getCouponById(coupon.getId()).getStatus());

        assertThat(
                promoService.getPromo(couponPromo.getPromoId().getId()).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET)
        );

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());

    }

    @Test
    public void testTransactionCommitOnOrderDeliver() {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Coupon coupon = createCoupon(couponPromo);


        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build());

        discountService.spendDiscount(
                builder.withCoupon(coupon.getCode()).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getCommitTime());

        processEvent(OrderStatus.DELIVERY, HistoryEventType.NEW_SUBSIDY, ORDER_ID);

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getCommitTime());
    }

    @Test
    public void testSubsidyRefund() {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Coupon coupon = createCoupon(couponPromo);


        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build());

        discountService.spendDiscount(
                builder.withCoupon(coupon.getCode()).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        long transactionId = metaTransactionDao.getTransactionsToCommit(ORDER_ID).get(0).getId();
        assertNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());
        assertNull(metaTransactionDao.getTransaction(transactionId).getCommitTime());

        processEvent(OrderStatus.DELIVERY, HistoryEventType.NEW_SUBSIDY, ORDER_ID);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.SUBSIDY_REFUND, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertNotNull(metaTransactionDao.getTransaction(transactionId).getCommitTime());
        assertNotNull(metaTransactionDao.getTransaction(transactionId).getRevertTime());
    }

    @Test
    public void shouldBindCoinsByOrder() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(SmartShopping.defaultPercent()),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), hasSize(2));

        long newUid = 123123L;
        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(false)
                        .setUid(newUid)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_UID_UPDATED
        );

        assertThat(coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(newUid)), hasSize(2));
    }

    @Test
    public void shouldActivateCoinIfOrderDelivered() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(INACTIVE))
        ));


        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(ACTIVE))
        ));
    }

    @Test
    public void shouldSkipEventsForOldOrders() {
        configurationService.set(SKIP_OLD_ORDER_ENABLED_ENABLED, true);

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setCreationDate(getDateBeforeTriggerEventKeepLowBound())
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        List<TriggerEvent> all = triggerEventDao.getAll();
        assertThat(all, is(empty()));
    }

    @Test
    public void shouldActivateCoinIfOrderUserReceived() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(INACTIVE))
        ));


        processEvent(
                order -> order.setSubstatus(USER_RECEIVED),
                HistoryEventType.ORDER_SUBSTATUS_UPDATED,
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERY)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build()
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(ACTIVE))
        ));

        processEvent(
                order -> {
                    order.setStatus(OrderStatus.DELIVERED);
                    order.setSubstatus(null);
                },
                HistoryEventType.ORDER_STATUS_UPDATED,
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERY)
                        .setOrderSubstatus(USER_RECEIVED)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build()
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(ACTIVE))
        ));
    }

    @Test
    public void shouldCorrectlySaveOrderPaidEvent() {
        OrderPaidDataDao orderPaidDataDao =
                (OrderPaidDataDao) this.triggerEventPersistentDataRepository;
        String multiOrderId = UUID.randomUUID().toString();
        Long orderId1 = 123451L;
        Long orderId2 = 123452L;
        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setMultiOrderId(multiOrderId)
                        .setOrdersCount(2)
                        .setOrderId(orderId1)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setPaymentType(PaymentType.PREPAID)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "MasterCard")
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setMultiOrderId(multiOrderId)
                        .setOrdersCount(2)
                        .setOrderId(orderId2)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setPaymentType(PaymentType.PREPAID)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "Visa")
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> orderPaidDataList = orderPaidDataDao.findAll();
        assertThat(orderPaidDataList, hasSize(2));
    }

    @Test
    public void shouldActivateCoinIfOrderDeliveredAndSkippedUserReceived() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                order -> {
                    order.setStatus(OrderStatus.DELIVERY);
                    order.setSubstatus(DELIVERY_SERVICE_RECEIVED);
                },
                HistoryEventType.ORDER_STATUS_UPDATED,
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build()
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(INACTIVE))
        ));


        processEvent(
                order -> {
                    order.setStatus(OrderStatus.DELIVERED);
                    order.setSubstatus(null);
                },
                HistoryEventType.ORDER_STATUS_UPDATED,
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERY)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build()
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(ACTIVE))
        ));
    }

    @Test
    public void shouldNotActivateCoinIfShopDelivery() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setUid(DEFAULT_UID)
                        .setNoAuth(false)
                        .setDeliveryPartnerType(DeliveryPartnerType.SHOP)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID),
                contains(hasProperty("status", equalTo(INACTIVE))
                ));

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERY)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setUid(DEFAULT_UID)
                        .setNoAuth(false)
                        .setDeliveryPartnerType(DeliveryPartnerType.SHOP)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID),
                contains(hasProperty("status", not(equalTo(ACTIVE))))
        );
    }

    @Test
    public void shouldSetUsedAndDeliveredIfOrderDelivered() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coin = coinService.create.createCoin(promo, defaultAuth().build());

        long orderId = 123;
        discountService.spendDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem()
                        .withOrderId(String.valueOf(orderId)).build()
                )
                        .withCoins(coin)
                        .build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(orderId)
                .setOrdersCount(2)
                .setNoAuth(true)
                .addItem(defaultOrderItem().build()
                );

        processEvent(
                orderBuilder1.build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(
                orderBuilder1.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCoin(coin).orElseThrow(AssertionError::new).getStatus(), equalTo(TERMINATED));
    }

    @Test
    public void shouldChangeEndDateOnActivation() {
        clock.setDate(valueOf("2019-01-01 15:00:00"));

        int daysToExpire = 10;
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setExpiration(ExpirationPolicy.expireByDays(daysToExpire))
        );

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(allOf(
                hasProperty("status", equalTo(INACTIVE)),
                hasProperty("roundedEndDate", equalTo(valueOf("2019-01-11 23:59:59")))
        )));

        //заказ ехал больше месяца
        clock.setDate(valueOf("2019-02-01 15:00:00"));

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(allOf(
                hasProperty("status", equalTo(ACTIVE)),
                hasProperty("roundedEndDate", equalTo(valueOf("2019-02-11 23:59:59")))
        )));
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldNotChangeEndDateOnActivation() {
        clock.setDate(valueOf("2019-01-01 15:00:00"));

        int daysToExpire = 35;
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setExpiration(ExpirationPolicy.expireByDaysFromCreation(daysToExpire))
        );

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(allOf(
                hasProperty("status", equalTo(INACTIVE)),
                hasProperty("roundedEndDate", equalTo(valueOf("2019-02-05 23:59:59")))
        )));

        //заказ ехал больше месяца
        clock.setDate(valueOf("2019-02-01 15:00:00"));

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setNoAuth(true)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(allOf(
                hasProperty("status", equalTo(ACTIVE)),
                hasProperty("roundedEndDate", equalTo(valueOf("2019-02-05 23:59:59")))
        )));
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldNotGiveBlueCoinForRedOrder() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.BLUE)),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .setRgb(Color.RED)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), empty());
    }

    @Test
    public void shouldCreateCoinIfUserSegmentRestrictionMatch() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.BLUE)),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .setRgb(Color.RED)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), empty());
    }

    @Test
    public void shouldEmmitCoinIfUserSegmentRestrictionMatches() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.BLUE)),
                orderRestriction(EFFECTIVELY_PROCESSING),
                userSegmentsRestriction(SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED, "segment1")
        );

        breMockUtils.mockBREGetUserSegmentsResponse("segment1");

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setNoAuth(false)
                        .setRgb(BLUE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), hasSize(1));
    }

    @Test
    public void shouldNotGiveRedCoinForBlueOrder() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.RED)),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .setRgb(BLUE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), empty());
    }

    @Test
    public void shouldEmmitCoinForWhiteOrder() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.BLUE))
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setUid(DEFAULT_UID)
                        .setNoAuth(false)
                        .setRgb(Color.WHITE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), hasSize(1));
    }

    @Test
    public void shouldActivateCoinIfWhiteOrderDelivered() {
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()),
                orderRestriction(OrderStatusPredicate.PROCESSING_PREPAID)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .setRgb(Color.WHITE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(INACTIVE))
        ));


        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.DELIVERED)
                        .setNoAuth(true)
                        .setRgb(Color.WHITE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(coinService.search.getCreatedCoinsByOrderId(CheckouterUtils.DEFAULT_ORDER_ID), contains(
                hasProperty("status", equalTo(ACTIVE))
        ));
    }

    @Test
    public void shouldRevokeCoinOnCancelWhiteOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, Color.WHITE, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, Color.WHITE, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                contains(hasProperty("status", equalTo(REVOKED)))
        );

        assertThat(
                promoService.getPromo(promo.getPromoId().getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldNotRevertUsedCoinWhenOrderCancelledByAntifraud() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smartShoppingPromo, orderRestriction(EFFECTIVELY_PROCESSING)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        DiscountRequestWithBundlesBuilder builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ORDER_ID))
                .build()
        )
                .withCoins(coinKey);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);
        processEvent(OrderStatus.PROCESSING, HistoryEventType.ORDER_STATUS_UPDATED, ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        builder = builder(orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                .build()
        ).withCoins(coinService.search.getCreatedCoinsByOrderId(ORDER_ID).get(0).getCoinKey());

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);
        processEvent(
                OrderStatus.CANCELLED, OrderSubstatus.USER_FRAUD, HistoryEventType.ORDER_STATUS_UPDATED, BLUE,
                ORDER_ID);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertEquals(TERMINATED, coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldSkipEventsTillLastEventId() {

        configurationService.set(ConfigurationService.CHECKOUTER_LAST_EVENT_ID, Long.MAX_VALUE);

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promoManager.createSmartShoppingPromo(defaultFixed()
                        .setPlatform(CoreMarketPlatform.BLUE)),
                orderRestriction(EFFECTIVELY_PROCESSING)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setNoAuth(true)
                        .setRgb(BLUE)
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        assertThat(triggerEventDao.getNotProcessed(Duration.ofMillis(100)), empty());
    }

    @Test
    public void shouldEnqueueActualTransactionForTerminatedSingleOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.TEN)
        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        DiscountRequestWithBundlesBuilder builder = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(1000)))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build()
        );

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        CheckouterUtils.OrderBuilder orderBuilder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ORDER_ID)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(1000))
                        .build()
                );


        processEvent(orderBuilder.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions1 = yandexWalletTransactionDao.findByOrderId(
                ORDER_ID, YandexWalletTransactionStatus.PENDING);
        assertThat(transactions1, hasSize(1));

        processEvent(orderBuilder.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions2 = yandexWalletTransactionDao.findByOrderId(
                ORDER_ID, YandexWalletTransactionStatus.IN_QUEUE);
        assertThat(transactions2, hasSize(1));
    }

    @Test
    public void shouldCancelNotActualTransactionForTerminatedMultiOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))

        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        final MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(BigDecimal.valueOf(2000)))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(ANOTHER_ITEM_KEY), price(BigDecimal.valueOf(2000)))
                        .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                        .build()
        ).withMultiOrderId(MULTI_ORDER_ID)
                .build();

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .build()
                );

        discountService.spendDiscount(request, configurationService.currentPromoApplicabilityPolicy(), null);

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions1 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID, YandexWalletTransactionStatus.PENDING);
        assertThat(transactions1, hasSize(1));

        processEvent(orderBuilder1.setOrderStatus(OrderStatus.CANCELLED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions2 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID,
                YandexWalletTransactionStatus.CANCELLED
        );
        assertThat(transactions2, hasSize(1));
    }

    @Test
    @Ignore("Исправить после добавления проверки антифрода и включить")
    public void shouldCancelFraudTransactionForTerminatedMultiOrder() {
        final BigDecimal welcomeCashbackPromoThreshold = BigDecimal.valueOf(3000);
        final BigDecimal welcomeCashbackPromoNominal = BigDecimal.valueOf(100);
        final Instant welcomeCashbackPromoStartDate = clock.instant()
                .minus(7, ChronoUnit.DAYS);
        final Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(welcomeCashbackPromoNominal)
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setStartDate(Date.from(welcomeCashbackPromoStartDate))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, welcomeCashbackPromoThreshold)
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, Set.of(WELCOME_CASHBACK))
        );
        cashbackCacheService.reloadCashbackPromos();

        final String welcomeCashbackPromoKey = cashbackPromo.getPromoKey();

        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_ENABLED, true);
        configurationService.set(ConfigurationService.BF_ORDERS_LIMIT_COUNT, 3);
        configurationService.set(ConfigurationService.WELCOME_APP_500_ANTIFRAUD_GLUE_LIMIT, 1);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_KEY,
                welcomeCashbackPromoKey);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_PROMO_START_DATE,
                welcomeCashbackPromoStartDate);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_CASHBACK_AMOUNT,
                welcomeCashbackPromoNominal);
        configurationService.set(ConfigurationService.MARKET_LOYALTY_WELCOME_APP_500_THRESHOLD,
                welcomeCashbackPromoThreshold);
        BlackboxUtils.mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        antiFraudMockUtil.previousOrders(0, 0);

        final MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(2000)), itemKey(DEFAULT_ITEM_KEY))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(2000)), itemKey(ANOTHER_ITEM_KEY))
                        .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                        .build()
        ).withMultiOrderId(MULTI_ORDER_ID)
                .build();

        discountService.spendDiscounts(
                request,
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(
                        SpendMode.SPEND,
                        Collections.emptyMap(),
                        PromoApplicabilityPolicy.ANY,
                        StatusFeaturesSet.enabled(Set.of(
                                WELCOME_CASHBACK,
                                YANDEX_PLUS
                        )),
                        discountAntifraudService.createAntifraudCheckFuture(DEFAULT_UID),
                        discountAntifraudService.createAntifraudMobileOrdersCheckFuture(DEFAULT_UID),
                        UsageClientDeviceType.APPLICATION,
                        null
                ),
                configurationService.currentPromoApplicabilityPolicy(),
                null

        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(DEFAULT_ITEM_KEY)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .setItemKey(ANOTHER_ITEM_KEY)
                        .build()
                );

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions1 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID, YandexWalletTransactionStatus.PENDING);
        assertThat(transactions1, hasSize(1));

        processEvent(orderBuilder1.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);

        antiFraudMockUtil.previousOrders(2, 2);

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions2 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID,
                YandexWalletTransactionStatus.ANTI_FRAUD_REJECT
        );
        assertThat(transactions2, hasSize(1));
    }

    @Test
    public void shouldEnqueueActualTransactionForTerminatedMultiOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))

        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        DiscountRequestWithBundlesBuilder builder = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(2000)))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(2000)))
                        .withOrderId(String.valueOf(ANOTHER_ORDER_ID))
                        .build()
        ).withMultiOrderId(MULTI_ORDER_ID);

        discountService.spendDiscount(builder.build(), configurationService.currentPromoApplicabilityPolicy(), null);

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .build()
                );
        CheckouterUtils.OrderBuilder orderBuilder2 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ANOTHER_ORDER_ID)
                .setMultiOrderId(MULTI_ORDER_ID)
                .setOrdersCount(2)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(2000))
                        .setCount(BigDecimal.ONE)
                        .build()
                );

        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions1 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID, YandexWalletTransactionStatus.PENDING);
        assertThat(transactions1, hasSize(1));

        processEvent(orderBuilder1.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        processEvent(orderBuilder2.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions2 = yandexWalletTransactionDao.findByMultiOrderId(
                MULTI_ORDER_ID, YandexWalletTransactionStatus.IN_QUEUE);
        assertThat(transactions2, hasSize(1));
    }

    @Test
    public void shouldEnqueueActualTransactionForPromoWithClientPlatformUsageRestriction() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(100))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(3000))
                        .addCashbackRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM,
                                UsageClientDeviceType.APPLICATION)

        );

        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        DiscountRequestWithBundlesBuilder builder = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(price(BigDecimal.valueOf(5000)))
                        .withOrderId(String.valueOf(ORDER_ID))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                        .withClientDevice(UsageClientDeviceType.APPLICATION)
                        .buildOperationContext());

        discountService.spendDiscounts(
                builder.build(),
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(
                        SpendMode.SPEND,
                        Collections.emptyMap(),
                        PromoApplicabilityPolicy.ANY,
                        StatusFeaturesSet.enabled(YANDEX_PLUS, YANDEX_CASHBACK),
                        AntiFraudFuture.withDefault(true),
                        AntiFraudFuture.withDefault(true),
                        UsageClientDeviceType.APPLICATION,
                        null
                ),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        CheckouterUtils.OrderBuilder orderBuilder1 = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(ORDER_ID)
                .addItem(defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.ONE)
                        .build()
                );


        processEvent(orderBuilder1.build(), HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions1 = yandexWalletTransactionDao
                .findByOrderId(ORDER_ID, YandexWalletTransactionStatus.PENDING);
        assertThat(transactions1, hasSize(1));

        processEvent(orderBuilder1.setOrderStatus(OrderStatus.DELIVERED).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        final List<YandexWalletTransaction> transactions2 = yandexWalletTransactionDao
                .findByOrderId(ORDER_ID, YandexWalletTransactionStatus.IN_QUEUE);
        assertThat(transactions2, hasSize(1));
    }

    private Coupon createCoupon(Promo couponPromo) {
        CouponCreationRequest request = CouponCreationRequest.builder(
                        PromoUtils.DEFAULT_COUPON_CODE, couponPromo.getPromoId().getId())
                .identity(new Uid(DEFAULT_UID))
                .forceActivation(true)
                .build();
        return couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
    }

    @NotNull
    private java.util.Date getDateBeforeTriggerEventKeepLowBound() {
        return Date.from(clock.instant().minus(configurationService.getProcessedTriggerEventKeepPeriod()).minus(1,
                ChronoUnit.DAYS));
    }
}
