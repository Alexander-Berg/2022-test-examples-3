package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.notifications.NotificationType;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.dao.ydb.NotificationDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoCodeGeneratorType;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.cashback.CashbackService;
import ru.yandex.market.loyalty.core.service.discount.DiscountAntifraudService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.perks.PerkService;
import ru.yandex.market.loyalty.core.stub.NotificationDaoStub;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.ORDER_HISTORY_EVENT_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStageReferralPromoTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    private final static String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promo";
    private final static String DEFAULT_COUPON_PROMO_KEY = "coupon_promo";
    private final static long DEFAULT_PROMOCODE_USER_UID = 0L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PerkService perkService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CashbackService cashbackService;
    @Autowired
    private DiscountAntifraudService discountAntifraudService;
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
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private UserReferralPromocodeDao userReferralPromocodeDao;
    @Autowired
    private DiscountServiceTestingUtils discountServiceTestingUtils;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private TriggerEventDao triggerEventDao;

    @Before
    public void setUp() throws Exception {
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
    }

    @Test
    public void testReferralPromocodeDirectFlow() {
        final Long ORDER_ID = 1L;
        preparePromos();

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(6000))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(1L).buildOperationContext())
                .build();
        discountServiceTestingUtils.spendDiscount(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat("Error in calculations: " + calculations, calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(ORDER_ID)),
                        hasProperty("uid", equalTo(DEFAULT_PROMOCODE_USER_UID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(300)))
                )
        ));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        // не создано транзакций до оплаты заказа
        assertThat(walletTransactions, hasSize(0));
        Promo promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        // нет траты бюджета до оплаты заказа
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(0)));
        // скидка по промокоду списана с бюджета
        Promo couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(500)));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));
        promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        // списание с бюджета после оплаты
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(300)));
        couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(500)));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat("Error in transactions: " + walletTransactions, walletTransactions.get(0),
                hasProperty("status", equalTo(YandexWalletTransactionStatus.IN_QUEUE)));

        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(3));
        assertThat("Error in calculations: " + calculations, calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.FINAL))
        ));

        promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        // двойного списания нет
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(300)));
        couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(500)));
    }

    @Test
    public void testReferralPromocodeSingleOrderCancelled() {
        final Long ORDER_ID = 1L;
        preparePromos();
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(6000))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(1L).buildOperationContext())
                .build();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountServiceTestingUtils.spendDiscount(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId", equalTo(ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(300)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));

        //Отправляем событие отмены
        processEvent(
                buildOrder(OrderStatus.CANCELLED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), hasProperty("status", equalTo(YandexWalletTransactionStatus.CANCELLED)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(3));
        assertThat(calculations, everyItem(
                hasProperty("result", equalTo(ResolvingState.CANCELLED))
        ));
        assertThat(triggerEventDao.getAll(), everyItem(
                hasProperty("params", hasEntry(is(ORDER_HISTORY_EVENT_ID), notNullValue()))));
        // бюджеты возвращены
        Promo promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.ZERO));
        Promo couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void testReferralPromocodeDirectFlowMulti() {
        final Long ORDER_ID_1 = 81L;
        final Long ORDER_ID_2 = 82L;
        final String MULTI_ORDER_ID = UUID.randomUUID().toString();
        preparePromos();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                                .withOrderId(ORDER_ID_1.toString())
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(3000),
                                        quantity(1)
                                )
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
                                        price(3000),
                                        quantity(1)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                .withCoupon("REFERRAL_CODE")
                .withMultiOrderId(MULTI_ORDER_ID)
                .withOperationContext(OperationContextFactory.withUidBuilder(101101L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                spendRequest(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("multiOrderId", equalTo(MULTI_ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(300)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_2, MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
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
        // бюджеты списано верно
        Promo promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(300)));
        Promo couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(500)));
    }

    @Test
    public void testReferralPromocodePartCancelledMulti() {
        final Long ORDER_ID_1 = 1L;
        final Long ORDER_ID_2 = 2L;
        final String MULTI_ORDER_ID = UUID.randomUUID().toString();
        preparePromos();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                                .withOrderId(ORDER_ID_1.toString())
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(3000),
                                        quantity(1)
                                )
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
                                        price(3000),
                                        quantity(1)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                .withCoupon("REFERRAL_CODE")
                .withMultiOrderId(MULTI_ORDER_ID)
                .withOperationContext(OperationContextFactory.withUidBuilder(1L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = spendRequest(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("multiOrderId", equalTo(MULTI_ORDER_ID)),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(300)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID_2, MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(2));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID_1, MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.CANCELLED, ORDER_ID_2, MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(3000), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0),
                hasProperty("status", equalTo(YandexWalletTransactionStatus.CANCELLED)));
        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(3));
        assertThat(calculations, hasItem(
                hasProperty("result", equalTo(ResolvingState.CANCELLED))
        ));
        // бюджеты возвращены
        Promo promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.ZERO)); // MIN_ORDER_TOTAL
        Promo couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(250))); // половина мультизаказа
    }

    @Test
    public void testReferralPromocodeAntifraudReject() {
        final Long ORDER_ID = 1L;
        preparePromos();
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(6000))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(1L).buildOperationContext())
                .build();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountServiceTestingUtils.spendDiscount(request);

        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(3));
        assertThat(calculations, everyItem(
                allOf(
                        hasProperty("orderId",
                                equalTo(Long.valueOf(discountResponse.getOrders().get(0).getOrderId()))),
                        hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                        hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(300)))
                )
        ));

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(1));
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0).getAmount(), comparesEqualTo(BigDecimal.valueOf(300)));

        //Мокируем антифрод, чтобы правило FirstOrder упало
        antiFraudMockUtil.previousOrders(100, 100);

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, ORDER_ID, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(6000), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0),
                hasProperty("status", equalTo(YandexWalletTransactionStatus.CANCELLED)));

        calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations, hasSize(3));
        assertThat(calculations, hasItem(
                hasProperty("result", equalTo(ResolvingState.CANCELLED))
        ));

        //Нотификация о фроде
        var notifications = ((NotificationDaoStub) notificationDao).getNotifications();
        assertThat(notifications, hasSize(1));
        assertThat(notifications.get(0),
                hasProperty("type", equalTo(NotificationType.REFERRAL_NOT_FIRST_ORDER_FOR_FRIEND)));
        // бюджет кешбэка возвращен
        Promo promo = promoService.getPromoByPromoKey(DEFAULT_ACCRUAL_PROMO_KEY);
        assertThat(promo.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.ZERO));
        // скидка использована
        Promo couponPromo = promoService.getPromoByPromoKey(DEFAULT_COUPON_PROMO_KEY);
        assertThat(couponPromo.getSpentBudget(), comparesEqualTo(BigDecimal.valueOf(500)));
    }

    private MultiCartWithBundlesDiscountResponse spendRequest(MultiCartWithBundlesDiscountRequest discountRequest) {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        return discountService.spendDiscount(discountRequest, applicabilityPolicy, "");
    }

    private void preparePromos() {
        Promo accrualPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(300))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(5000))
                .addCashbackRule(
                        RuleType.FIRST_ORDER_CUTTING_RULE,
                        RuleParameterName.ANTIFRAUD_CHECK_REQUIRED,
                        true)
                .addCashbackRule(
                        RuleType.MAX_PROMO_ACCRUALS_CUTTING_RULE,
                        RuleParameterName.MAX_LOYALTY_ACCRUALS_BY_PROMO,
                        BigDecimal.valueOf(10))
                .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, RuleParameterName.PERK_TYPE, PerkType.BERU_PLUS)
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
                .setCampaignName("referral_accrual")
                .setEmissionBudget(BigDecimal.valueOf(100000))
                .setBudget(BigDecimal.valueOf(100000))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, accrualPromo.getPromoKey());
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPromoKey(DEFAULT_COUPON_PROMO_KEY)
                        .setCouponCode("REFERRAL_CODE")
                        .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                        .setBudget(BigDecimal.valueOf(10000))
                        .setEmissionBudget(BigDecimal.valueOf(10000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        promoService.setPromoParam(couponPromo.getPromoId().getId(), PromoParameterName.GENERATOR_TYPE,
                PromoCodeGeneratorType.REFERRAL);
        deferredMetaTransactionService.consumeBatchOfTransactions(10);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 5000);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(DEFAULT_PROMOCODE_USER_UID)
                .setPromocode("REFERRAL_CODE")
                .setAssignTime(clock.instant())
                .setExpireTime(accrualPromo.getEndDate().toInstant())
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        reloadPromoCache();
    }
}
