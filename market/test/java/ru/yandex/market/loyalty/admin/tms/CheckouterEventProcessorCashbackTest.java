package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.MinOrderTotalCuttingRule;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.OrderCashbackCalculationService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author Vladimir Sitnikov
 * fonar101@yandex-team.ru
 */
@TestFor({CheckouterEventRestProcessor.class, TriggerEventTmsProcessor.class})
public class CheckouterEventProcessorCashbackTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final long ORDER_ID = 4524543L;
    private static final long ANOTHER_ORDER_ID = ORDER_ID + 1;
    public static final String DEFAULT_MULTI_ORDER_ID = "multiOrder";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private OrderCashbackCalculationService orderCashbackCalculationService;


    @Test
    public void shouldSetTransactionInQueueForSingleOrder() {
        Promo promo = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ORDER));

        enqueueTransaction(promo, ORDER_ID, null, BigDecimal.valueOf(100), DEFAULT_UID);

        processMultiOrderEvent(PROCESSING, ORDER_ID, null, 1);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(DELIVERED, ORDER_ID, null, 1);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 10), hasSize(1));
    }

    @Test
    public void shouldSetTransactionInQueueForMultiOrder() {
        configurationService.enable(YANDEX_CASHBACK_ENABLED);
        mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        Promo promo = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ORDER)
                .setBillingSchema(BillingSchema.SOLID)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(699)));

        saveOrderCashbackCalculation(promo, MIN_ORDER_TOTAL_CUTTING_RULE, DEFAULT_MULTI_ORDER_ID, null,
                BigDecimal.valueOf(100));

        processMultiOrderEvent(PROCESSING, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        processMultiOrderEvent(PROCESSING, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.anotherOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.PENDING, 10), hasSize(1));

        List<OrderCashbackCalculation> calculationsAfterPaid =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(calculationsAfterPaid, hasSize(1));
        assertThat(calculationsAfterPaid, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", is(nullValue())),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));

        processMultiOrderEvent(DELIVERED, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        processMultiOrderEvent(DELIVERED, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.anotherOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 10), hasSize(1));
        List<OrderCashbackCalculation> calculationsAfterTerminate =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(calculationsAfterTerminate, hasSize(1));
        assertThat(calculationsAfterTerminate, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", equalTo(ResolvingState.FINAL)),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));
    }

    @Test
    public void shouldSetTransactionCancelledForMultiOrder() {
        configurationService.enable(YANDEX_CASHBACK_ENABLED);
        mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        Promo promo = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ORDER)
                .setBillingSchema(BillingSchema.SOLID)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(699)));

        saveOrderCashbackCalculation(promo, MIN_ORDER_TOTAL_CUTTING_RULE, DEFAULT_MULTI_ORDER_ID, null,
                BigDecimal.valueOf(100));

        processMultiOrderEvent(PROCESSING, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        processMultiOrderEvent(PROCESSING, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.anotherOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.PENDING, 10), hasSize(1));

        List<OrderCashbackCalculation> calculationsAfterPaid =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(calculationsAfterPaid, hasSize(1));
        assertThat(calculationsAfterPaid, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", is(nullValue())),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));

        processMultiOrderEvent(CANCELLED, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        processMultiOrderEvent(CANCELLED, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<OrderCashbackCalculation> calculationsAfterTerminate =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.CANCELLED, 10), hasSize(1));
        assertThat(calculationsAfterTerminate, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", equalTo(ResolvingState.CANCELLED)),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));
    }

    @Test
    public void shouldSetTransactionInQueueAndRecalcAmountForMultiOrder() {
        configurationService.enable(YANDEX_CASHBACK_ENABLED);
        mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        Promo promo = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ORDER)
                .setBillingSchema(BillingSchema.SOLID)
                .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(699)));

        saveOrderCashbackCalculation(promo, MIN_ORDER_TOTAL_CUTTING_RULE, DEFAULT_MULTI_ORDER_ID, null,
                BigDecimal.valueOf(100));

        processMultiOrderEvent(PROCESSING, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        processMultiOrderEvent(PROCESSING, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.anotherOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.PENDING, 10), hasSize(1));

        List<OrderCashbackCalculation> calculationsAfterPaid =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(calculationsAfterPaid, hasSize(1));
        assertThat(calculationsAfterPaid, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", is(nullValue())),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));


        processMultiOrderEvent(CANCELLED, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.defaultOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        processMultiOrderEvent(DELIVERED, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, false,
                List.of(CheckouterUtils.anotherOrderItem()
                        .setPrice(BigDecimal.valueOf(5000))
                        .setCount(BigDecimal.valueOf(1))
                        .build()));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<OrderCashbackCalculation> calculationsAfterTerminate =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);
        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 10), hasSize(1));
        assertThat(yandexWalletTransactionDao.query(YandexWalletTransactionStatus.IN_QUEUE, 10), contains(allOf(
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(50)))
        )));
        assertThat(calculationsAfterTerminate, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", equalTo(ResolvingState.FINAL)),
                hasProperty("orderTerminationResult", equalTo(ResolvingState.FINAL)),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("finalCashbackAmount", comparesEqualTo(BigDecimal.valueOf(50)))
        )));
    }

    private void saveOrderCashbackCalculation(Promo promo, RuleType<MinOrderTotalCuttingRule> ruleType,
                                              String multiOrderId, Long orderId, BigDecimal initialCashbackAmount) {
        orderCashbackCalculationService.save(OrderCashbackCalculation.builder()
                .setUid(DEFAULT_UID)
                .setCashbackPropsId(promo.getCashbackPropsId())
                .setInitialResult(ResolvingState.INTERMEDIATE)
                .setResult(ResolvingState.INTERMEDIATE)
                .setInitialCashbackAmount(initialCashbackAmount)
                .setMultiOrderId(multiOrderId)
                .setOrderId(orderId)
                .setPromoId(promo.getId())
                .setRuleBeanName(ruleType.getBeanName())
                .build());
    }

    private void enqueueTransaction(Promo promo, Long orderId, String multiOrderId, BigDecimal amount, long uid) {
        yandexWalletTransactionDao.enqueueTransactions(
                null,
                "testCampaign",
                List.of(new YandexWalletNewTransaction(
                        uid,
                        amount,
                        "accrualForOrder" + orderId,
                        null,
                        "CASHBACK_EMIT",
                        null
                )),
                null,
                null,
                promo.getId(),
                YandexWalletTransactionStatus.PENDING,
                YandexWalletTransactionPriority.HIGH,
                orderId,
                multiOrderId
        );
    }

    private void processMultiOrderEvent(OrderStatus status, long orderId, String multiOrder, int count) {
        processMultiOrderEvent(status, orderId, multiOrder, count, false);
    }

    private void processMultiOrderEvent(OrderStatus status, long orderId, String multiOrder, int count,
                                        boolean isClickAndCollect) {
        processMultiOrderEvent(status, orderId, multiOrder, count, isClickAndCollect, Collections.emptyList());
    }

    private void processMultiOrderEvent(
            OrderStatus status, long orderId, String multiOrder, int count, boolean isClickAndCollect,
            List<OrderItem> items
    ) {
        processMultiOrderEvent(status, orderId, multiOrder, count, isClickAndCollect, items,
                HistoryEventType.ORDER_STATUS_UPDATED);
    }

    private void processMultiOrderEvent(
            OrderStatus status, long orderId, String multiOrder, int count, boolean isClickAndCollect,
            List<OrderItem> items, HistoryEventType historyEventType
    ) {
        Order order = CheckouterUtils.defaultOrder(status)
                .setOrderId(orderId)
                .setMultiOrderId(multiOrder)
                .setPaymentType(PaymentType.PREPAID)
                .setProperty(OrderPropertyType.MULTI_ORDER_SIZE, count)
                .setDeliveryPartnerType(isClickAndCollect ? DeliveryPartnerType.SHOP :
                        DeliveryPartnerType.YANDEX_MARKET)
                .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "MasterCard")
                .addItems(items)
                .build();
        processEvent(order, historyEventType);
    }
}
