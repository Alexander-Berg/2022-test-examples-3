package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.stub.YdbAllUsersOrdersDaoStub;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_ERROR;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.marketBrandedPickupDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStagePromoFlowTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private PromoManager promoManager;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private OrderPaidDataDao orderPaidDataDao;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private AllUserOrdersDao allUserOrdersDao;
    @Autowired
    private MultiStageTestUtils multiStageTestUtils;

    @Before
    public void setUp() {
        multiStageTestUtils.setUpCashback();
    }

    @Test
    public void testSingleOrderDirectFlow() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
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
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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


        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        // для мультистейдж промки событие отправляем по факту начисления
        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(150))),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("email", is(nullValue())),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(promo.getPromoKey())),
                hasProperty("clientDeviceType", is(nullValue()))
        )));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(3));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
    }

    @Test
    public void testMarketBrandedPickupCashbackSingleOrderDirectFlow() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.YANDEX_CASHBACK))
                        .addCashbackRule(RuleType.MARKET_BRANDED_PICKUP_FILTER_RULE,
                                RuleParameterName.MARKET_BRANDED_PICKUP,
                                true
                        )
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
                                .withDeliveries(marketBrandedPickupDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build());


        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1, DeliveryType.PICKUP,
                        DEFAULT_UID)
                        .setMarketBrandedDelivery(true)
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();

        calculations = orderCashbackCalculationDao.findAll();

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1, DeliveryType.PICKUP, DEFAULT_UID)
                        .setMarketBrandedDelivery(true)
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(1));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
    }

    @Test
    public void testSingleOrderDirectFlow1() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
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

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
                        .withCashbackOptionType(CashbackType.SPEND)
                        .build());


        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(0));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000)));

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testMultiOrderDirectFlow() {
        final Long ORDER_ID_1 = 2L;
        final Long ORDER_ID_2 = 3L;
        final String MULTI_ORDER_ID = UUID.randomUUID().toString();
        Promo promo = promoManager.createCashbackPromo(
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

        multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("multiOrderId", equalTo(MULTI_ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(300), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                        BigDecimal.valueOf(200), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY, BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(buildOrder(OrderStatus.DELIVERED, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
    }

    @Test
    public void testCancelledMultiOrderPartOnPaidEvent() {
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

        multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("multiOrderId", equalTo(MULTI_ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.CANCELLED, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY, BigDecimal.valueOf(200),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(90)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(90))));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY, BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        assertThat(walletTransactions.get(0), hasProperty("amount", equalTo(BigDecimal.valueOf(90))));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(90))));
    }

    @Test
    public void testCancelledMultiOrderPartOnTerminationEvent() {
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

        multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("multiOrderId", equalTo(MULTI_ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY,
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                        BigDecimal.valueOf(200),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID, DEFAULT_ITEM_KEY, BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(buildOrder(OrderStatus.CANCELLED, ORDER_ID_2, MULTI_ORDER_ID, ANOTHER_ITEM_KEY,
                BigDecimal.valueOf(200), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        assertThat(walletTransactions.get(0), hasProperty("amount", equalTo(BigDecimal.valueOf(90))));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(90))));
    }


    @Test
    public void testPromoRestrictedByPerk() {
        final Long ORDER_ID = 1L;
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                Set.of(PerkType.BERU_PLUS))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void correctlyCalculateDeliveredIfPromoEndedAfterPay() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
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
                        .setEndDate(Date.from(
                                Instant.now()
                                        .plus(1, ChronoUnit.DAYS))
                        )
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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


        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        // для мультистейдж промки событие отправляем по факту начисления
        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(150))),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("email", is(nullValue())),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(promo.getPromoKey())),
                hasProperty("clientDeviceType", is(nullValue()))
        )));

        clock.spendTime(7, ChronoUnit.DAYS);
        reloadPromoCache();
        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
    }

    @Test
    public void testOldPromoCashbackProps() {
        final Long ORDER_ID = 1L;
        CashbackPromoBuilder cashbackPromoBuilder = PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
                CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                        RuleParameterName.PERK_TYPE,
                        Set.of(PerkType.YANDEX_CASHBACK))
                .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                        RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                        PaymentSystem.MASTERCARD)
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1000))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID);
        Promo promo = promoManager.createCashbackPromo(
                cashbackPromoBuilder
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        cashbackPromoBuilder.setNominal(BigDecimal.valueOf(15));
        promoManager.updateCashbackPromo(cashbackPromoBuilder);
        reloadPromoCache();

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
    }

    @Test
    public void testPromoWithoutMultiStageRules() {
        final Long ORDER_ID = 1L;
        CashbackPromoBuilder cashbackPromoBuilder = PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
                CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                        RuleParameterName.PERK_TYPE,
                        Set.of(PerkType.YANDEX_CASHBACK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID);
        Promo promo = promoManager.createCashbackPromo(
                cashbackPromoBuilder
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        //Отправляем событие доставк

        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testSingleOrderDirectFlowRejectByFirstOrderRuleOnInitialCheckStage() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build()
                )
                .build();

        ((YdbAllUsersOrdersDaoStub) allUserOrdersDao).addToOrdersMap(new UserOrder(
                request.getOperationContext().getUid(),
                OrderStatus.DELIVERED.name(),
                clock.instant(),
                "123",
                Platform.DESKTOP
        ));

        MultiCartWithBundlesDiscountResponse discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(0));
        assertEquals(BigDecimal.ZERO, discountResponse.getCashback().getEmit().getAmount());

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000)));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is("NO_SUITABLE_PROMO")),
                hasProperty("discount", is(nullValue())),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(nullValue()))
        )));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testSingleOrderDirectFlowRejectByFirstOrderRuleOnOrderTerminationStage() {
        final Long ORDER_ID = 1L;
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID.toString())
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build()
                )
                .build();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId",
                                equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))
                        ),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150)))
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is("NO_SUITABLE_PROMO")),
                hasProperty("discount", is(nullValue())),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(nullValue()))
        )));

        ((YdbAllUsersOrdersDaoStub) allUserOrdersDao).addToOrdersMap(new UserOrder(
                request.getOperationContext().getUid(),
                OrderStatus.DELIVERED.name(),
                clock.instant().minus(1, ChronoUnit.DAYS),
                "123",
                Platform.DESKTOP
        ));

        //Отправляем событие доставки
        processEvent(
                multiStageTestUtils.buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0),
                hasProperty("status", equalTo(YandexWalletTransactionStatus.CANCELLED))
        );
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(2));
        assertThat(calculations, hasItem(
                hasProperty("result", equalTo(ResolvingState.CANCELLED))
        ));
    }

    @Test
    public void testOrderPaidIdempotence() {
        final Long ORDER_ID = 88L;
        Promo promo = promoManager.createCashbackPromo(
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
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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


        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(150)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        //Отправляем повторное событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));


    }

    @Test
    public void testSeveralPromosInOneBucket() {
        final Long ORDER_ID = 1L;
        Promo promo = promoManager.createCashbackPromo(
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
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(2)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        Promo secondPromo = promoManager.createCashbackPromo(
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
                        .addCashbackRule(RuleType.FIRST_ORDER_CUTTING_RULE,
                                RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                                true)
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
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

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(6));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(150)));
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(150))));

        // для мультистейдж промки событие отправляем по факту начисления
        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(150))),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("email", is(nullValue())),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(promo.getPromoKey())),
                hasProperty("clientDeviceType", is(nullValue()))
        )));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(6));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));
    }

}
