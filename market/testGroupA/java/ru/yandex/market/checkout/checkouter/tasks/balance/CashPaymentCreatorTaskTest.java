package ru.yandex.market.checkout.checkouter.tasks.balance;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.checkers.CreateBalanceOrderParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.DEFAULT_SUPPLIER_INN;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.UID_HEADER;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCashbackBalanceCalls;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;


public class CashPaymentCreatorTaskTest extends AbstractPaymentTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private PaymentWritingDao paymentWritingDao;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private PaymentReadingDao paymentReadingDao;
    @Autowired
    private PaymentService paymentService;

    private Order order;

    private Order createOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(true);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return orderService.getOrder(orderCreateHelper.createOrder(parameters).getId());
    }

    @BeforeEach
    public void prepareOrder() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        order = createOrder();
        setupShop(order.getShopId());
        setupShop(FulfilmentProvider.FF_SHOP_ID);
        orderStatusHelper.proceedOrderToStatusWithoutTask(order, OrderStatus.DELIVERED);

    }

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                paymentTestHelper.updateFiscalAgentTypeEnabled(false, Set.of()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldCreateCashPayment(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        Payment payment = getCreatedPayment();
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();

        checkCashbackBalanceCalls(trustMockConfigurer.eventsGatewayIterator());
        checkCreateServiceProductAndOrder(callIter);

        checkCreateBasket(callIter, payment, isFiscalAgentTypeEnabled);
        assertThat(payment.getStatus(), equalTo(PaymentStatus.IN_PROGRESS));

        checkReceipts(payment);
        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        Assertions.assertTrue(events.getItems().stream().anyMatch(ohe ->
                HistoryEventType.NEW_CASH_PAYMENT == ohe.getType()), "Has NEW_CASH_PAYMENT event");

        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkResumePaymentWithoutExistingPayment(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        bindServiceOrderIdForAllItems();
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());
        Payment payment = getCreatedPayment();
        //Нет вызова CreateServiceProduct и CreateServiceOrder
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();
        checkCashbackBalanceCalls(trustMockConfigurer.eventsGatewayIterator());
        checkCreateBasket(callIter, payment, isFiscalAgentTypeEnabled);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkResumePaymentWithNullTrustId(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        bindServiceOrderIdForAllItems();
        Payment oldPayment = insertBadPayment();

        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());
        Payment payment = getCreatedPayment();
        assertThat(payment.getId(), equalTo(oldPayment.getId()));
        assertThat(payment.getUid(), equalTo(oldPayment.getUid()));
        //Специально заинсертил пустой uid в платеж, чтобы убедиться в том что мы игнорим тот что в заказе.
        assertThat(order.getBuyer().getUid(), not(equalTo(payment.getUid())));
        //Нет вызова CreateServiceProduct и CreateServiceOrder
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();
        checkCashbackBalanceCalls(trustMockConfigurer.eventsGatewayIterator());
        //checkCreateBasket(callIter, payment, isFiscalAgentTypeEnabled);
        ////////
        CreateBasketParams basketParams = createBasket()
                .withBackUrl(equalTo("/payments/" + payment.getId() + "/notify-basket"))
                .withPayMethodId("cash-0")
                .withCurrency(Currency.RUR)
                .withYandexUid(BuyerProvider.YANDEX_UID)
                .withUid(payment.getUid())
                .withFiscalAgentType(isFiscalAgentTypeEnabled ? "none_agent" : null)
                .withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
        // перечитываем, т.к. некоторые поля не возвращаются
        orderService.getOrder(order.getId()).getItems().forEach(item ->
                basketParams.withOrder(
                        item.getBalanceOrderId(),
                        item.getQuantityIfExistsOrCount(),
                        item.getQuantPriceIfExistsOrBuyerPrice(),
                        item.getOfferName(),
                        item.getVat() == null ? null : item.getVat().getTrustId(),
                        DEFAULT_SUPPLIER_INN, // inn с магазина, ордер создается с бесплатной доставкой
                        isFiscalAgentTypeEnabled ?
                                (Objects.requireNonNullElse(item.getSupplierType(), SupplierType.FIRST_PARTY)
                                        == SupplierType.FIRST_PARTY ? "none_agent" : "agent") :
                                null
                        )
                        .withPassParams(notNullValue(String.class))
        );

        ServeEvent event = callIter.next();

        assertEquals(CREATE_BASKET_STUB, event.getStubMapping().getName());

        assertNull(event.getRequest().getHeader(UID_HEADER));

        checkPayBasketCall(callIter, payment.getUid(), payment.getBasketKey());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkPaymentRetryAfterBalanceFail(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        trustMockConfigurer.mockCreateBasket(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        try {
            queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());
            fail("Expected exception");
        } catch (Exception e) {
            log.info("error as expected", e);
        }
        Payment failedPayment = getCreatedPayment();
        assertThat(failedPayment.getBasketId(), nullValue());

        trustMockConfigurer.mockCreateBasket();
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());
        Payment newPayment = getCreatedPayment();
        assertThat(newPayment.getBasketId(), notNullValue());

        assertThat(newPayment.getStatus(), equalTo(PaymentStatus.IN_PROGRESS));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void loadExpiredPostpayPaymentsTest(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        Payment payment = getCreatedPayment();
        assertThat(payment.getStatus(), equalTo(PaymentStatus.IN_PROGRESS));
        setFixedTime(payment.getStatusExpiryDate().toInstant().plus(1, MINUTES));
        Collection<Payment> payments = paymentService.loadPaymentsWithExpiredStatus(PaymentGoal.ORDER_POSTPAY,
                new PaymentStatus[]{PaymentStatus.IN_PROGRESS});
        assertThat(payments, not(empty()));
    }

    private void setupShop(Long shopId) {
        ShopMetaData shopMetaData = ShopSettingsHelper.createCustomNewPrepayMeta(shopId.intValue());
        shopService.updateMeta(shopId, shopMetaData);
    }

    private void checkCreateServiceProductAndOrder(OneElementBackIterator<ServeEvent> callIter) throws Exception {
        List<CreateBalanceOrderParams> createBalanceOrderParams = new ArrayList<>();
        order.getItems().forEach(item -> {
            try {
                checkLoadPartnerCall(callIter, 667L);
                checkOptionalCreateServiceProductCall(callIter, product(667L, "667-667", "667_667"));

                createBalanceOrderParams.add(new CreateBalanceOrderParams(
                        0,
                        "667_667",
                        "100500",
                        notNullValue(PassParams.class),
                        order.getId() + "-item-" + item.getId(),
                        order.getCreationDate()
                ));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        checkBatchServiceOrderCreationCall(callIter, BuyerProvider.UID, createBalanceOrderParams);
    }

    private void checkCreateBasket(Iterator<ServeEvent> callIter, Payment payment, boolean isFiscalAgentTypeEnabled) {
        CreateBasketParams basketParams = createBasket()
                .withBackUrl(equalTo("/payments/" + payment.getId() + "/notify-basket"))
                .withPayMethodId("cash-0")
                .withCurrency(Currency.RUR)
                .withYandexUid(BuyerProvider.YANDEX_UID)
                .withUid(payment.getUid())
                .withFiscalAgentType(isFiscalAgentTypeEnabled ? "none_agent" : null)
                .withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
        // перечитываем, т.к. некоторые поля не возвращаются
        orderService.getOrder(order.getId()).getItems().forEach(item ->
                basketParams.withOrder(
                                item.getBalanceOrderId(),
                                item.getQuantityIfExistsOrCount(),
                                item.getQuantPriceIfExistsOrBuyerPrice(),
                                item.getOfferName(),
                                item.getVat() == null ? null : item.getVat().getTrustId(),
                                DEFAULT_SUPPLIER_INN, // inn с магазина, ордер создается с бесплатной доставкой
                                isFiscalAgentTypeEnabled ?
                                        (Objects.requireNonNullElse(item.getSupplierType(), SupplierType.FIRST_PARTY)
                                                == SupplierType.FIRST_PARTY ? "none_agent" : "agent") :
                                        null
                        )
                        .withPassParams(notNullValue(String.class))
        );

        checkCreateBasketCall(callIter, basketParams);

        checkPayBasketCall(callIter, payment.getUid(), payment.getBasketKey());
    }

    private void checkReceipts(Payment payment) {
        Assertions.assertTrue(receiptService.paymentHasReceipt(payment));
        List<Receipt> receipts = receiptService.findByPayment(payment);
        assertThat(receipts, Matchers.hasSize(1));

        Receipt receipt = receipts.get(0);

        Assertions.assertEquals(ReceiptType.INCOME, receipt.getType());
        Assertions.assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());
    }

    private Payment getCreatedPayment() throws Exception {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(order.getId(), PaymentGoal.ORDER_POSTPAY);
        assertThat(payments, hasSize(1));
        return Iterables.getOnlyElement(payments);
    }

    private void bindServiceOrderIdForAllItems() {
        transactionTemplate.execute(ts -> {
            order.getItems().forEach(item -> item.setBalanceOrderId(item.getId() + "balblablalbal"));
            orderWritingDao.bindItemBalanceOrderIdAndCommissionForItems(order);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldCreateCashPaymentFor2Items(boolean isFiscalAgentTypeEnabled) throws Exception {
        if (isFiscalAgentTypeEnabled) {
            checkouterFeatureWriter.writeValue(FISCAL_AGENT_TYPE_ENABLED,
                    paymentTestHelper.updateFiscalAgentTypeEnabled(true, Set.of()));
        }
        Parameters parameters = CashParametersProvider.createOrderWithTwoItems(true);
        parameters.getBuiltMultiCart().setPromoCode(PROMO_CODE);
        parameters.getReportParameters().setShopSupportsSubsidies(true);
        parameters.setMockLoyalty(true);

        Order order = orderCreateHelper.createOrder(parameters);

        setupShop(order.getShopId());
        setupShop(FulfilmentProvider.FF_SHOP_ID);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        PagedPayments payments = paymentTestHelper.getPagedPayments(order.getId(), PaymentGoal.ORDER_POSTPAY);

        Collection<Payment> paymentList = payments.getItems();
        assertThat(paymentList, hasSize(1));

        Payment payment = Iterables.getOnlyElement(paymentList);

        Assertions.assertTrue(receiptService.paymentHasReceipt(payment));
        List<Receipt> receipts = receiptService.findByPayment(payment);
        assertThat(receipts, Matchers.hasSize(1));

        Receipt receipt = receipts.get(0);

        Assertions.assertEquals(ReceiptType.INCOME, receipt.getType());
        Assertions.assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        Assertions.assertTrue(events.getItems().stream().anyMatch(ohe ->
                HistoryEventType.NEW_CASH_PAYMENT == ohe.getType()), "Has NEW_CASH_PAYMENT event");

        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));

        final Order changedOrder = orderService.getOrder(order.getId());
        assertTrue(changedOrder.getItems().stream()
                .filter(i -> TEST_SHOP_SKU.equals(i.getShopSku()))
                .allMatch(i -> i.getAgencyCommission() != null), "AgencyCommission on order items had to be updated");
    }

    private Payment insertBadPayment() {
        Date now = Date.from(getClock().instant().minus(1, ChronoUnit.DAYS));
        long newPaymentId = paymentWritingDao.getPaymentSequences().getNextPaymentId();
        Payment payment = new Payment();
        payment.setId(newPaymentId);
        payment.setStatus(PaymentStatus.INIT);
        payment.setType(PaymentGoal.ORDER_POSTPAY);
        payment.setPrepayType(PrepayType.YANDEX_MARKET);
        payment.setOrderId(order.getId());
        payment.setCurrency(Currency.RUR);
        payment.setTotalAmount(BigDecimal.TEN);
        payment.setCreationDate(now);
        payment.setStatusUpdateDate(now);
        payment.setUpdateDate(now);
        payment.setShopId(order.getShopId());
        payment.setFake(false);
        transactionTemplate.execute(ts -> {
            paymentWritingDao.insertPayment(ClientInfo.SYSTEM, payment);
            return null;
        });
        return payment;
    }
}
