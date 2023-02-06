package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationNoMultistageDao;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderTerminationEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculationNoMultistage;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.blackbox.UserInfoResponse;
import ru.yandex.market.loyalty.core.service.perks.StaticPerkService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.multistage.MultiStageStaticPerkRuleTest.DISALLOWED_STATIC_PERK;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStageOnDeliveryPromoTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private PromoManager promoManager;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private OrderCashbackCalculationNoMultistageDao orderCashbackCalculationNoMultistageDao;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private OrderPaidDataDao orderPaidDataDao;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private MultiStageTestUtils multiStageTestUtils;
    @Autowired
    private StaticPerkService staticPerkService;
    @Autowired
    private TriggerEventDao triggerEventDao;


    private AtomicLong orderSequence = new AtomicLong(1);

    @Before
    public void setUp() {
        multiStageTestUtils.setUpCashback();
    }

    @Test
    public void testSingleOrderDirectFlow() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, RuleParameterName.PERK_TYPE,
                                PerkType.YANDEX_PLUS)
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
        assertThat(calculations.size(), equalTo(0));
        List<OrderCashbackCalculationNoMultistage> calculations2 = orderCashbackCalculationNoMultistageDao.findAll();
        assertThat(calculations2, hasSize(1));

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
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testSingleOrderCancelled() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
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
        assertThat(calculations.size(), equalTo(0));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.CANCELLED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
    }

    @Test
    public void shouldCorrectlyApplyTwoPromos() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket2")
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
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
        assertThat(calculations.size(), equalTo(1));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(2));
        assertThat(walletTransactions, everyItem(
                hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)))
        );
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(1));
    }

    @Test
    public void shouldCorrectlyCancelAfterPaid() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket2")
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
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
        assertThat(calculations.size(), equalTo(1));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));

        //Отправляем событие отмены
        processEvent(
                buildOrder(OrderStatus.CANCELLED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions, everyItem(
                hasProperty("status", equalTo(YandexWalletTransactionStatus.CANCELLED)))
        );
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(1));
        assertThat(calculations.get(0), hasProperty("result", equalTo(ResolvingState.CANCELLED)));
    }

    @Test
    public void shouldNotThrowOnEmptyCashbackOption() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
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
        assertThat(calculations.size(), equalTo(0));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1,
                        DeliveryType.DELIVERY, null, DEFAULT_UID).build(),
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
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1,
                        DeliveryType.DELIVERY, null, DEFAULT_UID).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testMultiOrderOrderDirectFlow() {
        final Long ORDER_ID_1 = orderSequence.getAndIncrement();
        final Long ORDER_ID_2 = orderSequence.getAndIncrement();
        final String MULTI_ORDER_ID = UUID.randomUUID().toString();
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
        );
        reloadPromoCache();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID_1.toString())
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
                        .build(),
                orderRequestWithBundlesBuilder()
                        .withOrderId(ORDER_ID_2.toString())
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
                        .build()
        )
                .withMultiOrderId(MULTI_ORDER_ID)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(0));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_2, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testSingleOrderDirectFlowAndNotRejectedByOnCreationRule() {
        final Long ORDER_ID = orderSequence.getAndIncrement();
        Promo promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_CREATION_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK))
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

        staticPerkService.providePerkToUser(DEFAULT_UID, DISALLOWED_STATIC_PERK);

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
        assertThat(calculations, hasSize(0));
    }

    @Test
    public void testFailTriggerEventOnPerkTimeout() {
        //Включаем перк YANDEX_PLUS для DEFAULT_UID + 1
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + (DEFAULT_UID + 1)))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenReturn(ResponseEntity.ok(BlackboxUtils.mockBlackboxResponse(true, PerkType.YANDEX_PLUS)));

        final Long ORDER_ID = orderSequence.getAndIncrement();
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1000))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, RuleParameterName.PERK_TYPE,
                                PerkType.YANDEX_PLUS)
        );
        reloadPromoCache();

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
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID + 1))
                .build());


        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(0));
        List<OrderCashbackCalculationNoMultistage> calculations2 = orderCashbackCalculationNoMultistageDao.findAll();
        assertThat(calculations2, hasSize(1));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1, DEFAULT_UID + 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));

        //Эмулируем задержку в ответе BLACKBOX
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + (DEFAULT_UID + 1)))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).then(invocation -> {
            Thread.sleep(2000);
            return ResponseEntity.ok(BlackboxUtils.mockBlackboxResponse(true, PerkType.YANDEX_PLUS));
        });

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1, DEFAULT_UID + 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        Optional<TriggerEvent> terminationEventOpt = triggerEventDao.getAll().stream()
                .filter(triggerEvent -> triggerEvent instanceof OrderTerminationEvent)
                .findFirst();
        assertThat(terminationEventOpt.isPresent(), equalTo(Boolean.TRUE));
        assertThat(terminationEventOpt.get(), hasProperty("processedResult",
                equalTo(TriggerEventProcessedResult.ERROR)));
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(0));
    }
}
