package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.emptyReturns;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.prepareOrderItems;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.preparePagedReturns;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.pickupDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withMarketBrandedPickup;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID_LONG;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStageRefundsTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private TriggerEventDao triggerEventDao;

    @Before
    public void setUp() throws Exception {
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.CASHBACK_PROMOS_FROM_REPORT_ENABLED, true);
    }

    @Test
    public void testRefundSingleOrderFull() {
        final Long ORDER_ID = 1L;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отправляем событие доставки
        Order orderDelivered = buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build();
        processEvent(
                orderDelivered,
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, null));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(orderDelivered));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions, everyItem(
                hasProperty("refundStatus", not(equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED))))
        );
    }

    @Test
    public void shouldRefundExcludingCurrentOrderFromMaxAccrualsCuttingRule() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(500))
                        .addCashbackRule(RuleType.MAX_PROMO_ACCRUALS_CUTTING_RULE,
                                RuleParameterName.MAX_LOYALTY_ACCRUALS_BY_PROMO,
                                BigDecimal.ONE)
        );

        reloadPromoCache();

        OrderItem item1 = defaultOrderItem()
                .setWareId(String.valueOf(MARKET_WAREHOUSE_ID))
                .setCount(1)
                .setPrice(900)
                .setItemKey(DEFAULT_ITEM_KEY)
                .setId(DEFAULT_ITEM_KEY.getFeedId())
                .build();
        OrderItem item2 = defaultOrderItem()
                .setWareId(String.valueOf(MARKET_WAREHOUSE_ID))
                .setCount(1)
                .setPrice(900)
                .setItemKey(ANOTHER_ITEM_KEY)
                .setId(ANOTHER_ITEM_KEY.getFeedId())
                .build();

        spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(DEFAULT_ORDER_ID)
                        .withOrderItem(adaptItem(item1))
                        .withOrderItem(adaptItem(item2))
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING,
                        DEFAULT_ORDER_ID_LONG,
                        null,
                        1,
                        false,
                        item1,
                        item2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отправляем событие доставки
        Order orderDelivered = buildOrder(OrderStatus.DELIVERED,
                DEFAULT_ORDER_ID_LONG,
                null,
                1,
                false,
                item1,
                item2).build();

        processEvent(orderDelivered, HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(null, List.of(item1)));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(new OrderItems(List.of(item1, item2)));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(DEFAULT_ORDER_ID_LONG);
        assertThat(walletTransactions, hasSize(2));
        assertThat(
                walletTransactions,
                containsInAnyOrder(
                        allOf(
                                hasProperty("refundStatus",
                                        equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED)),
                                hasProperty("amount",
                                        comparesEqualTo(BigDecimal.valueOf(90)))),
                        allOf(
                                hasProperty("refundStatus",
                                        equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE)),
                                hasProperty("amount",
                                        comparesEqualTo(BigDecimal.valueOf(180)))
                        )
                )
        );
    }

    private Collection<BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder>> adaptItem(OrderItem item) {
        return List.of(
                itemKey(item.getFeedId(), item.getOfferId()),
                warehouse(item.getWarehouseId()),
                price(item.getPrice()),
                quantity(item.getCount())
        );
    }

    @Test
    public void fixMarketdiscount7877() {
        final Long ORDER_ID = 1L;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.MARKET_BRANDED_PICKUP_FILTER_RULE,
                                RuleParameterName.MARKET_BRANDED_PICKUP,
                                true)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse response =
                spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(pickupDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                withMarketBrandedPickup(true),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(500), 3, 1, true).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(500), 3, 1, true).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(buildOrder(OrderStatus.DELIVERED, ORDER_ID, null, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(500), 1, 1, true).build(), null));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(buildOrder(OrderStatus.DELIVERED, ORDER_ID, null, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(500), 3, 1, true).build()));

        processEvent(buildOrder(OrderStatus.DELIVERED, ORDER_ID, null, DEFAULT_ITEM_KEY,
                BigDecimal.valueOf(500), 2, 1, true).build(),
                HistoryEventType.REFUND
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(2));
        assertThat(walletTransactions,
                containsInAnyOrder(
                        allOf(
                                hasProperty(
                                        "amount",
                                        comparesEqualTo(BigDecimal.valueOf(150))
                                ),
                                hasProperty(
                                        "status",
                                        equalTo(YandexWalletTransactionStatus.IN_QUEUE)
                                ),
                                hasProperty(
                                        "refundStatus",
                                        equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE)
                                )
                        ),
                        allOf(
                                hasProperty(
                                        "amount",
                                        comparesEqualTo(BigDecimal.valueOf(100))
                                ),
                                hasProperty(
                                        "status",
                                        equalTo(YandexWalletTransactionStatus.IN_QUEUE)
                                ),
                                hasProperty(
                                        "refundStatus",
                                        equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED)
                                )
                        )
                )
        );
    }

    @Test
    public void testRefundSingleOrderPart() {
        final Long ORDER_ID = 2L;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(3))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отправляем событие доставки
        Order orderDelivered = buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build();
        processEvent(
                orderDelivered,
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, 1));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(orderDelivered));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(2));
        assertThat(walletTransactions, containsInAnyOrder(
                allOf(
                        hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE)),
                        hasProperty("amount", equalTo(BigDecimal.valueOf(150)))
                ),
                allOf(
                        hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED)),
                        hasProperty("amount", equalTo(BigDecimal.valueOf(100))),
                        hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE))
                )
        ));
    }

    @Test
    public void testRefundSingleOrderAfterMinOrderTotalDroppedDown() {
        final Long ORDER_ID = 2L;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1200))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отправляем событие доставки
        Order orderDelivered = buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build();
        processEvent(
                orderDelivered,
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, 1));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(orderDelivered));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions, everyItem(
                hasProperty("refundStatus", not(equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED))))
        );
    }

    @Test
    public void testRefundSingleOrderDoublePart() {
        final Long ORDER_ID = 2L;
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(3))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse = spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build())
                .build());

        // Была замечена ошибка "Exception at perk request: [YANDEX_CASHBACK]"
        assertThat("Consider the reason of flakiness is PerkService", discountResponse.getCashback().getEmit(),
                hasProperty("amountByPromoKey", equalTo(Map.of(promo.getPromoKey(), BigDecimal.valueOf(150))))
        );

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(1));

        //Отправляем событие доставки
        Order orderDelivered = buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 3, 1, false).build();
        processEvent(
                orderDelivered,
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(1));

        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(orderDelivered));

        //Возврат первого товара
        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, 1));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(2));

        //Возврат второго товара
        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, 2));
        processEvent(orderDelivered, HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAllByOrderId(ORDER_ID);
        assertThat(walletTransactions, hasSize(3));
        assertThat(walletTransactions, containsInAnyOrder(
                allOf(
                        hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE)),
                        hasProperty("amount", equalTo(BigDecimal.valueOf(150)))
                ),
                allOf(
                        hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE)),
                        hasProperty("amount", equalTo(BigDecimal.valueOf(100)))
                ),
                allOf(
                        hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.NOT_QUEUED)),
                        hasProperty("amount", equalTo(BigDecimal.valueOf(50))),
                        hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE))
                )
        ));
    }

    @Test
    public void shouldOnlyUpdateOrderItemsAfterReturn() {
        final Long ORDER_ID_1 = 3L;
        final Long ORDER_ID_2 = 4L;
        final String MULTI_ORDER_ID = UUID.randomUUID().toString();
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.MASTERCARD)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(500))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        spendRequest(DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                                .withOrderId(ORDER_ID_1.toString())
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(300),
                                        quantity(3)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ORDER_ID_2.toString())
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(200),
                                        quantity(3)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                .withMultiOrderId(MULTI_ORDER_ID)
                .build());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(300), 3, 2, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                        BigDecimal.valueOf(200), 3, 2, false).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        //Отменяем часть заказа
        Order order2 = buildOrder(OrderStatus.CANCELLED, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                BigDecimal.valueOf(200), 3, 2, false).build();
        processEvent(order2,
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(order2));
        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(emptyReturns());

        processEvent(buildOrder(OrderStatus.CANCELLED, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                BigDecimal.valueOf(200), 3, 2, false).build(), HistoryEventType.REFUND);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        TriggerEvent refundTriggerEvent = triggerEventDao.getAll().stream()
                .filter(triggerEvent -> triggerEvent.getEventType() == TriggerEventTypes.REFUND_IN_ORDER)
                .filter(triggerEvent -> triggerEvent.getUniqueKey().contains(ORDER_ID_2.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(refundTriggerEvent, hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)));
    }

    private MultiCartWithBundlesDiscountResponse spendRequest(MultiCartWithBundlesDiscountRequest discountRequest) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(discountRequest, applicabilityPolicy, "");
    }

    private CheckouterUtils.OrderBuilder buildOrder(OrderStatus orderStatus, Long orderId, String multiOrder,
                                                    ItemKey itemKey, BigDecimal itemPrice, int itemsCount,
                                                    Integer ordersCount, boolean marketBrandedDelivery) {
        return buildOrder(orderStatus, orderId, multiOrder, ordersCount,
                marketBrandedDelivery, defaultOrderItem()
                .setWareId(String.valueOf(MARKET_WAREHOUSE_ID))
                .setCount(BigDecimal.valueOf(itemsCount))
                .setPrice(itemPrice)
                .setItemKey(itemKey)
                .setId(itemKey.getFeedId())
                .build());
    }
    private CheckouterUtils.OrderBuilder buildOrder(OrderStatus orderStatus, Long orderId, String multiOrder,
                                                    Integer ordersCount, boolean marketBrandedDelivery, OrderItem... items) {
        CheckouterUtils.OrderBuilder order = CheckouterUtils.defaultOrder(orderStatus)
                .setOrdersCount(ordersCount)
                .setOrderId(orderId)
                .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                .setPaymentType(ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setNoAuth(false)
                .setUid(DEFAULT_UID)
                .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "MasterCard")
                .addItems(Arrays.asList(items));

        if (multiOrder != null) {
            order = order.setMultiOrderId(multiOrder);
        }
        if (marketBrandedDelivery) {
            order.setMarketBrandedDelivery(true);
            order.setDeliveryType(DeliveryType.PICKUP);
        }
        return order;
    }
}
