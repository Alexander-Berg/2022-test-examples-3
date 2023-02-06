package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentProperties;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.admin.tms.YandexWalletTopUpProcessor;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.PaymentInfo;
import ru.yandex.market.loyalty.core.config.qualifier.BankCashbackCoreApi;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderPaidData;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bank.cashback.core.model.SaveDeliveredAmountRequest;
import ru.yandex.market.loyalty.core.utils.BankTestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.prepareOrderItems;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.preparePagedReturns;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.api.model.discount.PaymentFeature.YA_BANK;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_BANK_CASHBACK;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PAYMENT_FEATURE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_CANCELLED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_CONFIRMED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.FAKE_PENDING;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PAYMENT_FEATURES_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PERKS_ALLOWED_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_PROMO_KEY;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_BANK_CASHBACK_REARR;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.RearrUtils.REARR_FACTORS_HEADER;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStageExternalCashbackTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final String PAYMENT_METHOD = "YANDEX";
    private static final long PAYMENT_ID = 146311503;
    private static final long ANOTHER_PAYMENT_ID = 146311504;
    private static final String RRN = "test_rrn";
    private static final String ANOTHER_RRN = "another_rrn";
    private static final String AUTH_CODE = "test_auth_code";
    private static final String REARR = "yandex_bank_perk_on";

    @Autowired
    private MultiStageTestUtils multiStageTestUtils;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private BankTestUtils bankTestUtils;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private OrderPaidDataDao orderPaidDataDao;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private YandexWalletTopUpProcessor ywTopUpProcessor;
    @Autowired
    @BankCashbackCoreApi
    private RestTemplate coreRestTemplate;

    private Promo promo;

    @Before
    public void setUp() {
        bankTestUtils.mockCalculatorWithDefaultResponse();
        bankTestUtils.mockCoreWithSuccess();
        mockCheckouterClient();

        promo = promoManager.createExternalCashbackPromo(
                PromoUtils.ExternalCashback.defaultBank()
                        .addCashbackRule(PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_BANK_CASHBACK)
                        .addCashbackRule(MAX_ORDER_TOTAL_CUTTING_RULE, MAX_ORDER_TOTAL, BigDecimal.valueOf(15000))
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.ONE)
                        .addCashbackRule(PAYMENT_FEATURES_CUTTING_RULE, PAYMENT_FEATURE, YA_BANK)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE)
        );

        reloadPromoCache();

        multiStageTestUtils.setUpCashback();
        configurationService.enable(YANDEX_BANK_CASHBACK_ENABLED);
        configurationService.set(YANDEX_BANK_CASHBACK_PROMO_KEY, promo.getPromoKey());
    }

    @Test
    public void testSingleOrderDirectFlow() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, false)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(150, false);

        //Отправляем событие оплаты
        processPaid(true, false);

        checkPaidData(1);
        checkYwt(150, false, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(150);


        //Отправляем событие доставки
        processTermination(false);

        checkYwt(150, false, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(1500), 0);

        checkYwt(150, false, FAKE_CONFIRMED);
    }

    @Test
    public void testSingleOrderNoPaymentInfoOnSpend() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(false, false)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(0, false);

        //Отправляем событие оплаты
        processPaid(true, false);

        checkPaidData(1);
        checkYwt(150, false, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(150);

        //Отправляем событие доставки
        processTermination(false);

        checkYwt(150, false, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(1500), 0);

        checkYwt(150, false, FAKE_CONFIRMED);
    }

    @Test
    public void testSingleOrderNoPaymentInfoOnPaid() {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, false)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(150, false);

        //Отправляем событие оплаты
        processPaid(false, false);

        checkPaidData(1);
        checkNoYwt();

        // для мультистейдж промки событие отправляем по факту начисления
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        processTermination(false);
        checkNoYwt();
    }

    @Test
    public void testMultiOrderDirectFlow() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaid(true, true);

        checkPaidData(2);
        checkYwt(300, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(300);

        //Отправляем событие доставки
        processTermination(true);
        checkYwt(300, true, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(3000), 0);
        checkYwt(300, true, FAKE_CONFIRMED);
    }

    @Test
    public void testMultiOrderHalfPaymentInfoOnPaid() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaidWithHalfPaymentIno();

        checkPaidData(2);
        checkYwt(150, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(150);

        //Отправляем событие доставки
        processTermination(true);
        checkYwt(150, true, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(1500), 0);

        checkYwt(150, true, FAKE_CONFIRMED);
    }

    @Test
    public void testMultiOrderHalfCancelledOnTermination() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaid(true, true);

        checkPaidData(2);
        checkYwt(300, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(300);

        //Отправляем событие доставки
        processTerminationWithCancellation(true, false);
        checkYwt(150, true, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(1500), 1);

        checkYwt(150, true, FAKE_CONFIRMED);
    }

    @Test
    public void testMultiOrderCancelledOnTermination() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaid(true, true);

        checkPaidData(2);
        checkYwt(300, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(300);

        //Отправляем событие доставки
        processTerminationWithCancellation(true, true);
        checkYwt(300, true, FAKE_CANCELLED);

        // Не отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkNoBankRequests();

        checkYwt(300, true, FAKE_CANCELLED);
    }

    @Test
    public void testMultiOrderWithTwoDifferentPaymentsThenRefund() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaidWithTwoPaymentInfo();

        checkPaidData(2);
        checkYwt(300, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(300);

        //Отправляем событие доставки
        processTermination(true);
        checkYwt(300, true, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequests();

        checkYwt(300, true, FAKE_CONFIRMED);


        clearInvocations(coreRestTemplate);
        // Отправляем событие возврата в Я.Банк
        processRefund(true);
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        ywTopUpProcessor.yandexWalletRefundFakeTransactions(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequestsRefund(true);

        checkYwtRefund(true);
    }

    @Test
    public void testMultiOrderWithTwoDifferentPaymentsThenHalfRefund() throws InterruptedException {
        var discountResponse = multiStageTestUtils.spendRequest(
                createRequest(true, true)
        );

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
        checkCalculations(300, true);

        //Отправляем событие оплаты
        processPaidWithTwoPaymentInfo();

        checkPaidData(2);
        checkYwt(300, true, FAKE_PENDING);

        // для мультистейдж промки событие отправляем по факту начисления
        checkLogBrokerEvent(300);

        //Отправляем событие доставки
        processTermination(true);
        checkYwt(300, true, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequests();

        checkYwt(300, true, FAKE_CONFIRMED);


        clearInvocations(coreRestTemplate);
        // Отправляем событие возврата в Я.Банк
        processRefund(false);
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        ywTopUpProcessor.yandexWalletRefundFakeTransactions(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequestsRefund(false);

        checkYwtRefund(false);
    }

    @Test
    public void testWithRearrFactorEnabled() throws InterruptedException {
        configurationService.set(YANDEX_BANK_CASHBACK_REARR, REARR);

        final var requestMock = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));
        Mockito.when(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(eq(REARR_FACTORS_HEADER))).thenReturn(REARR);

        multiStageTestUtils.spendRequest(createRequest(true, false));

        Mockito.reset(requestMock);

        checkCalculations(150, false);

        //Отправляем событие оплаты
        processPaid(true, false);

        checkPaidData(1);
        checkYwt(150, false, FAKE_PENDING);

        //Отправляем событие доставки
        processTermination(false);

        checkYwt(150, false, FAKE_IN_QUEUE);

        // Отправляем событие в ЯБанк
        ywTopUpProcessor.yandexWalletFakeTransactionsProcess(Duration.of(5, ChronoUnit.MINUTES), 500);
        checkBankRequest(BigDecimal.valueOf(1500), 0);

        checkYwt(150, false, FAKE_CONFIRMED);
    }


    private void mockCheckouterClient() {
        when(checkouterClient.payments().getPayment(
                any(RequestClientInfo.class), argThat(hasProperty("paymentId", equalTo(PAYMENT_ID))))
        ).thenReturn(createPayment(RRN));
        when(checkouterClient.payments().getPayment(
                any(RequestClientInfo.class), argThat(hasProperty("paymentId", equalTo(ANOTHER_PAYMENT_ID))))
        ).thenReturn(createPayment(ANOTHER_RRN));
    }

    private static Payment createPayment(String rrn) {
        var payment = new Payment();
        var paymentProps = new PaymentProperties();
        paymentProps.setYaCard(true);
        paymentProps.setRRN(rrn);
        paymentProps.setApprovalCode(AUTH_CODE);
        paymentProps.setPaymentMethodId(PAYMENT_METHOD);
        payment.setProperties(paymentProps);
        return payment;
    }

    private static MultiCartWithBundlesDiscountRequest createRequest(boolean withYaCard, boolean isMultiOrder) {
        var orders = new ArrayList<OrderWithBundlesRequest>();
        orders.add(orderRequestWithBundlesBuilder()
                .withOrderId(DEFAULT_ORDER_ID)
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
                .build());

        if (isMultiOrder) {
            orders.add(orderRequestWithBundlesBuilder()
                    .withOrderId(ANOTHER_ORDER_ID)
                    .withOrderItem(
                            warehouse(MARKET_WAREHOUSE_ID),
                            itemKey(ANOTHER_ITEM_KEY),
                            price(500),
                            quantity(3)
                    )
                    .withDeliveries(courierDelivery(
                            withPrice(BigDecimal.valueOf(350)),
                            builder -> builder.setSelected(true)
                    ))
                    .build());
        }

        return DiscountRequestWithBundlesBuilder.builder(orders)
                .withMultiOrderId(isMultiOrder ? DEFAULT_MULTI_ORDER_ID : null)
                .withPaymentInfo(withYaCard ? new PaymentInfo(PAYMENT_METHOD, Collections.singleton(YA_BANK)) : null)
                .build();
    }

    private void checkCalculations(long amount, boolean isMultiOrder) {
        List<OrderCashbackCalculation> calculations = orderCashbackCalculationDao.findAll();
        assertThat(calculations.size(), equalTo(2));
        assertThat(calculations, everyItem(allOf(
                hasProperty("orderId", isMultiOrder ? nullValue() : equalTo(Long.valueOf(DEFAULT_ORDER_ID))),
                hasProperty("multiOrderId", isMultiOrder ? equalTo(DEFAULT_MULTI_ORDER_ID) : nullValue()),
                hasProperty("result", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("initialCashbackAmount", equalTo(BigDecimal.valueOf(amount)))
        )));
    }

    private void processPaid(boolean withYaCard, boolean isMultiOrder) {
        processPaidEvent(DEFAULT_ORDER_ID, DEFAULT_ITEM_KEY, withYaCard ? PAYMENT_ID : null, isMultiOrder);
        if (isMultiOrder) {
            processPaidEvent(ANOTHER_ORDER_ID, ANOTHER_ITEM_KEY, withYaCard ? PAYMENT_ID : null, isMultiOrder);
        }
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void processPaidWithHalfPaymentIno() {
        processPaidEvent(DEFAULT_ORDER_ID, DEFAULT_ITEM_KEY, PAYMENT_ID, true);
        processPaidEvent(ANOTHER_ORDER_ID, ANOTHER_ITEM_KEY, null, true);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void processPaidWithTwoPaymentInfo() {
        processPaidEvent(DEFAULT_ORDER_ID, DEFAULT_ITEM_KEY, PAYMENT_ID, true);
        processPaidEvent(ANOTHER_ORDER_ID, ANOTHER_ITEM_KEY, ANOTHER_PAYMENT_ID, true);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void processPaidEvent(String orderId, ItemKey itemKey, Long paymentId, boolean isMultiOrder) {
        Order order = buildOrder(
                OrderStatus.PROCESSING, Long.valueOf(orderId),
                isMultiOrder ? DEFAULT_MULTI_ORDER_ID : null,
                itemKey, BigDecimal.valueOf(500), BigDecimal.valueOf(3), isMultiOrder ? 2 : 1
        )
                .build();
        order.setProperty("isYaCard", "true");
        order.setPaymentId(paymentId);
        processEvent(order, HistoryEventType.ORDER_STATUS_UPDATED);
    }

    private void checkPaidData(int size) {
        List<OrderPaidData> paidDataDaoAll = orderPaidDataDao.findAll();
        assertThat(paidDataDaoAll, hasSize(size));
    }

    private void checkYwt(long amount, boolean isMultiOrder, YandexWalletTransactionStatus status) {
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(1));
        assertThat(walletTransactions.get(0), allOf(
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(amount))),
                hasProperty("status", equalTo(status)),
                hasProperty("orderId", isMultiOrder ? nullValue() : equalTo(Long.valueOf(DEFAULT_ORDER_ID))),
                hasProperty("multiOrderId", isMultiOrder ? equalTo(DEFAULT_MULTI_ORDER_ID) : nullValue())
        ));
    }

    private void checkNoYwt() {
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, hasSize(0));
    }

    private void checkYwtRefund(boolean fullRefund) {
        List<YandexWalletTransaction> walletTransactions = yandexWalletTransactionDao.findAll();
        assertThat(walletTransactions, allOf(
                hasSize(2),
                containsInAnyOrder(
                        allOf(
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(300))),
                                hasProperty("status", equalTo(FAKE_CONFIRMED)),
                                hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.CONFIRMED)),
                                hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID))
                        ),
                        allOf(
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(150))),
                                hasProperty("status", equalTo(FAKE_CONFIRMED)),
                                hasProperty("refundStatus", equalTo(fullRefund
                                        ? YandexWalletRefundTransactionStatus.CONFIRMED
                                        : YandexWalletRefundTransactionStatus.NOT_QUEUED)),
                                hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID))
                        )
                )
        ));
    }

    private void checkLogBrokerEvent(long amount) {
        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(amount))),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("email", is(nullValue())),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(promo.getPromoKey())),
                hasProperty("clientDeviceType", is(nullValue()))
        )));
    }

    private void processTermination(boolean isMultiOrder) {
        processEvent(
                buildOrder(
                        OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID),
                        isMultiOrder ? DEFAULT_MULTI_ORDER_ID : null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), isMultiOrder ? 2 : 1
                )
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        if (isMultiOrder) {
            processEvent(
                    buildOrder(
                            OrderStatus.DELIVERED, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                            ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2
                    )
                            .build(),
                    HistoryEventType.ORDER_STATUS_UPDATED
            );
        }

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void processTerminationWithCancellation(boolean isMultiOrder, Boolean cancelAll) {
        processEvent(
                buildOrder(
                        OrderStatus.CANCELLED, Long.valueOf(DEFAULT_ORDER_ID),
                        isMultiOrder ? DEFAULT_MULTI_ORDER_ID : null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), isMultiOrder ? 2 : 1
                )
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );

        if (isMultiOrder) {
            processEvent(
                    buildOrder(
                            cancelAll ? OrderStatus.CANCELLED : OrderStatus.DELIVERED, Long.valueOf(ANOTHER_ORDER_ID),
                            DEFAULT_MULTI_ORDER_ID,
                            ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2
                    )
                            .build(),
                    HistoryEventType.ORDER_STATUS_UPDATED
            );
        }

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void processRefund(boolean fullRefund) {
        var firstOrder = buildOrder(
                OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID),
                DEFAULT_MULTI_ORDER_ID,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), fullRefund ? 2 : 1
        ).build();

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(firstOrder, 3));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(prepareOrderItems(firstOrder));

        processEvent(firstOrder,HistoryEventType.REFUND);

        if (fullRefund) {
            var secondOrder = buildOrder(
                    OrderStatus.DELIVERED, Long.valueOf(ANOTHER_ORDER_ID),
                    DEFAULT_MULTI_ORDER_ID,
                    DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2
            ).build();

            when(checkouterClient.returns().getOrderReturns(any(), any()))
                    .thenReturn(preparePagedReturns(secondOrder, 3));
            when(checkouterClient.getOrderItems(any(), any()))
                    .thenReturn(prepareOrderItems(secondOrder));

            processEvent(secondOrder, HistoryEventType.REFUND);
        }

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    @SuppressWarnings("unchecked")
    private void checkBankRequest(BigDecimal amount, int version) {
        var expected = buildRequest(RRN, amount, version);
        verify(coreRestTemplate, only()).exchange(
                argThat(hasProperty("body", equalTo(expected))),
                any(Class.class)
        );
    }

    @SuppressWarnings("unchecked")
    private void checkBankRequests() {
        var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(coreRestTemplate, times(2)).exchange(captor.capture(), any(Class.class));
        assertThat(captor.getAllValues().stream()
                        .map(RequestEntity::getBody)
                        .collect(Collectors.toList()),
                containsInRelativeOrder(
                        buildRequest(RRN, BigDecimal.valueOf(1500), 0),
                        buildRequest(ANOTHER_RRN, BigDecimal.valueOf(1500), 0)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private void checkBankRequestsRefund(boolean fullRefund) {
        var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(coreRestTemplate, times(2)).exchange(captor.capture(), any(Class.class));
        assertThat(
                captor.getAllValues().stream()
                        .map(RequestEntity::getBody)
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        buildRequest(RRN, BigDecimal.ZERO, fullRefund ? 2 : 1),
                        buildRequest(
                                ANOTHER_RRN,
                                fullRefund ? BigDecimal.ZERO : BigDecimal.valueOf(1500),
                                fullRefund ? 2 : 1)
                )
        );
    }

    private void checkNoBankRequests() {
        verify(coreRestTemplate, never()).exchange(
                any(RequestEntity.class),
                any(Class.class)
        );
    }

    private SaveDeliveredAmountRequest buildRequest(String rrn, BigDecimal amount, int version) {
        return SaveDeliveredAmountRequest.builder()
                .setRrn(rrn)
                .setAuthCode(AUTH_CODE)
                .setUid(String.valueOf(DEFAULT_UID))
                .setPaymentMethodId(PAYMENT_METHOD)
                .setAmount(amount)
                .setCurrency("RUB")
                .setVersion(version)
                .build();
    }
}
