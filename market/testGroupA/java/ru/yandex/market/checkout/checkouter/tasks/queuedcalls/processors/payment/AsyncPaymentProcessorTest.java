package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.AsyncPaymentCardsCacheService;
import ru.yandex.market.checkout.checkouter.balance.service.TrustService;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.BindingItem;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.BindingsResponse;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.pay.cashier.CashierService;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.Color.ALL_COLORS;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest.builder;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.PAYMENT_METHODS_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkListPaymentMethodsCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.findEvents;

public class AsyncPaymentProcessorTest extends AbstractWebTestBase {

    @Autowired
    private CashierService cashierService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private Clock clock;
    @Autowired
    private OrderEditService orderEditService;
    @Autowired
    private PaymentWritingDao paymentWritingDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PaymentReadingDao paymentReadingDao;
    private TrustService trustService;
    private AsyncPaymentCardsCacheService asyncPaymentCardsCacheService;
    private ObjectMapper objectMapper;

    private AsyncPaymentProcessor asyncPaymentProcessor;

    @BeforeEach
    public void setUp() {
        trustService = mock(TrustService.class);
        asyncPaymentCardsCacheService = mock(AsyncPaymentCardsCacheService.class);
        asyncPaymentProcessor = new AsyncPaymentProcessor(
                cashierService,
                orderService,
                clock,
                orderEditService,
                trustService,
                asyncPaymentCardsCacheService,
                checkouterProperties,
                orderUpdateService,
                50,
                300);
        trustMockConfigurer.mockWholeTrust();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.resetAll();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void whenMaxDurationReached_shouldCancelOrders(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(
                orders,
                null,
                clock.instant().minus(5 * 60 + 1, ChronoUnit.MINUTES));

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertOrdersCancelled(executionResult, orders);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onFirstExecution_shouldCreatePayment(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(orders);
        Long uid = orders.get(0).getBuyer().getUid();
        var paymentMethods = mockPaymentMethodsCache(uid);

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertPaymentCreated(executionResult, orders, paymentMethods.get(0).getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onFirstExecution_withExplicitCardId_shouldCreatePayment(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        String cardId = "card-" + UUID.randomUUID().toString();
        var queuedCallExecution = createQueuedCallExecution(orders, cardId);

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertPaymentCreated(executionResult, orders, cardId);
    }

    @ParameterizedTest
    @CsvSource({
            "true,HOLD",
            "true,CLEARED",
            "false,HOLD",
            "false,CLEARED",
    })
    public void onSecondExecution_paymentSuccessful_shouldSuccessQueuedCall(boolean isMultiOrder,
                                                                            PaymentStatus paymentStatus) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(orders);
        Long uid = orders.get(0).getBuyer().getUid();
        mockPaymentMethodsCache(uid);

        asyncPaymentProcessor.process(queuedCallExecution);
        changePaymentStatus(orders, paymentStatus);

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertNotNull(executionResult);
        assertTrue(executionResult.isSuccess());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onSecondExecution_paymentProcessing_shouldDelayQueuedCall(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(orders);
        Long uid = orders.get(0).getBuyer().getUid();
        mockPaymentMethodsCache(uid);

        asyncPaymentProcessor.process(queuedCallExecution);

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertNotNull(executionResult);
        assertTrue(executionResult.isDelayExecution());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onSecondExecution_paymentCancelled_otherPayMethodExists_shouldDelayQueuedCall(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(orders);
        Long uid = orders.get(0).getBuyer().getUid();
        var paymentMethods = mockPaymentMethodsCache(uid);

        asyncPaymentProcessor.process(queuedCallExecution);
        changePaymentStatus(orders, PaymentStatus.CANCELLED);
        var cancelledPayment = orderService.getOrder(orders.get(0).getId()).getPayment();
        mockUsedPaymentMethods(orders, paymentMethods.get(0));

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertPaymentCreated(
                executionResult,
                orders,
                paymentMethods.get(1).getId(),
                paymentMethods.get(0).getId(),
                cancelledPayment.getBasketKey());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onSecondExecution_withExplicitCardId_paymentCancelled_shouldWaitUserInput(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        String cardId = "card-" + UUID.randomUUID().toString();
        var queuedCallExecution = createQueuedCallExecution(orders, cardId);

        asyncPaymentProcessor.process(queuedCallExecution);
        changePaymentStatus(orders, PaymentStatus.CANCELLED);

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertOrdersWaitingUserInput(executionResult, orders);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onSecondExecution_orderCancelled_shouldSuccessQueuedCall(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        String cardId = "card-" + UUID.randomUUID().toString();
        var queuedCallExecution = createQueuedCallExecution(orders, cardId);

        asyncPaymentProcessor.process(queuedCallExecution);

        var request = new OrderEditRequest();
        request.setCancellationRequest(builder()
                .substatus(OrderSubstatus.USER_NOT_PAID)
                .build());
        for (var order : orders) {
            orderEditService.editOrder(order.getId(), ClientInfo.SYSTEM, ALL_COLORS, request);
        }

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertEquals(ExecutionResult.SUCCESS, executionResult);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onThirdExecution_paymentCancelled_otherPayMethodNotExists_shouldCancelOrders(boolean isMultiOrder) {
        var orders = createOrders(isMultiOrder);
        var queuedCallExecution = createQueuedCallExecution(orders);
        Long uid = orders.get(0).getBuyer().getUid();
        var paymentMethods = mockPaymentMethodsCache(uid);

        asyncPaymentProcessor.process(queuedCallExecution);
        changePaymentStatus(orders, PaymentStatus.CANCELLED);
        mockUsedPaymentMethods(orders, paymentMethods.get(0), paymentMethods.get(1));

        var executionResult = asyncPaymentProcessor.process(queuedCallExecution);

        assertOrdersCancelled(executionResult, orders);
    }

    private void assertOrdersCancelled(ExecutionResult executionResult, List<Order> orders) {
        assertNotNull(executionResult);
        assertTrue(executionResult.isSuccess());
        for (var order : orders) {
            var actualOrder = orderService.getOrder(order.getId());
            assertEquals(OrderStatus.CANCELLED, actualOrder.getStatus());
            assertEquals(OrderSubstatus.USER_NOT_PAID, actualOrder.getSubstatus());
        }
    }

    private void assertOrdersWaitingUserInput(ExecutionResult executionResult, List<Order> orders) {
        assertNotNull(executionResult);
        assertTrue(executionResult.isSuccess());
        for (var order : orders) {
            var actualOrder = orderService.getOrder(order.getId());
            assertEquals(OrderStatus.UNPAID, actualOrder.getStatus());
            assertEquals(OrderSubstatus.WAITING_USER_INPUT, actualOrder.getSubstatus());
        }
    }

    private void assertPaymentCreated(ExecutionResult executionResult,
                                      List<Order> orders,
                                      String cardId) {
        assertPaymentCreated(executionResult, orders, cardId, null, null);
    }

    private void assertPaymentCreated(
            ExecutionResult executionResult,
            List<Order> orders,
            String cardId,
            String cancelledCardId,
            TrustBasketKey cancelledBasketKey) {
        assertNotNull(executionResult);
        assertTrue(executionResult.isDelayExecution());
        var actualOrders = new ArrayList<>(orderService
                .getOrders(orders.stream().map(BasicOrder::getId).collect(Collectors.toList()))
                .values());
        Long uid = actualOrders.get(0).getBuyer().getUid();
        var payment = paymentReadingDao.loadPayment(actualOrders.get(0).getPaymentId());
        assertNotNull(payment);
        assertTrue(actualOrders
                .stream()
                .allMatch(o -> o.getPayment() != null && o.getPayment().getId().equals(payment.getId())));
        assertNotNull(payment.getProperties());
        assertNotNull(payment.getProperties().getAsync());
        assertTrue(payment.getProperties().getAsync());
        var iterator = assertTrustCommonCalls(trustMockConfigurer.servedEvents(), orders, uid);
        if (cancelledCardId != null && cancelledBasketKey != null) {
            assertTrustBasketCalls(
                    iterator,
                    actualOrders,
                    cancelledCardId,
                    uid,
                    cancelledBasketKey);
        }
        assertTrustBasketCalls(
                iterator,
                actualOrders,
                cardId,
                uid,
                payment.getBasketKey());
    }

    private OneElementBackIterator<ServeEvent> assertTrustCommonCalls(
            List<ServeEvent> events, List<Order> orders, Long uid) {
        ShopMetaData shop = shopService.getMeta(orders.get(0).getShopId());
        var paymentsMethodIterator = findEvents(events, PAYMENT_METHODS_STUB);
        while (paymentsMethodIterator.hasNext()) {
            //если не заходим то сработал кеш
            //ru.yandex.market.checkout.checkouter.balance.service.PaymentMethodsFallbackCacheService.getPaymentMethods
            checkListPaymentMethodsCall(paymentsMethodIterator, uid.toString(), null, null);
        }
        var iterator = new OneElementBackIterator<>(events.stream()
                .filter(e -> !PAYMENT_METHODS_STUB.equals(e.getStubMapping().getName()))
                .iterator());
        checkOptionalLoadPartnerCall(iterator, shop.getClientId());
        checkOptionalCreateServiceProductCall(iterator, null);
        checkOptionalCreateServiceProductCall(iterator, null);
        checkOptionalLoadPartnerCall(iterator, shop.getClientId());
        checkBatchServiceOrderCreationCall(iterator, uid);
        return iterator;
    }

    private void assertTrustBasketCalls(OneElementBackIterator<ServeEvent> iterator, List<Order> orders, String cardId,
                                        Long uid, TrustBasketKey basketKey) {
        var createBasketParams = CreateBasketParams.createBasket()
                .withPayMethodId(cardId)
                .withUid(uid)
                .withCurrency(Currency.RUR)
                .withPassParams(notNullValue(String.class))
                .withYandexUid("test-yandex-uid");
        for (var order : orders) {
            createBasketParams.withOrdersByItemsAndDelivery(order, false);
            createBasketParams.withSpasiboOrderMap(order
                    .getItems()
                    .stream()
                    .collect(Collectors.toMap(OrderItem::getBalanceOrderId, OrderItem::getShopSku)));
        }
        createBasketParams.withSpasiboOrderMap(orders
                .stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.toMap(OrderItem::getBalanceOrderId, OrderItem::getShopSku)));
        checkCreateBasketCall(iterator, createBasketParams);
        checkPayBasketCall(iterator, uid, basketKey);
    }

    private List<Order> createOrders(boolean isMultiOrder) {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        if (isMultiOrder) {
            parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
            parameters.setPaymentMethod(ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX);
            var multiOrder = orderCreateHelper.createMultiOrder(parameters);
            return multiOrder.getOrders();
        }

        var order = orderCreateHelper.createOrder(parameters);
        return List.of(order);
    }

    private void changePaymentStatus(List<Order> orders, PaymentStatus targetStatus) {
        var payment = orderService.getOrder(orders.get(0).getId()).getPayment();
        payment.setStatus(targetStatus);
        transactionTemplate.execute(txStatus -> {
            paymentWritingDao.updateStatus(payment, ClientInfo.SYSTEM);
            return null;
        });
    }

    private List<BindingItem> mockPaymentMethodsCache(Long uid) {
        var bindings = new BindingsResponse(List.of(
                new BindingItem("card_" + UUID.randomUUID(), "card", false, new BigDecimal(222)),
                new BindingItem("card_" + UUID.randomUUID(), "card", false, new BigDecimal(111))
        ));
        when(trustService.getBindings(uid.toString(), null, null, BLUE, true)).thenReturn(bindings);
        return bindings.getBindingItems();
    }

    private void mockUsedPaymentMethods(List<Order> orders, BindingItem... usedMethods) {
        var minOrderId = orders.stream().min(Comparator.comparingLong(BasicOrder::getId)).orElseThrow().getId();
        var usedCardSet = Arrays.stream(usedMethods).map(BindingItem::getId).collect(Collectors.toSet());
        when(asyncPaymentCardsCacheService.getUsedCardIds(minOrderId)).thenReturn(usedCardSet);
    }

    private QueuedCallProcessor.QueuedCallExecution createQueuedCallExecution(List<Order> orders) {
        return createQueuedCallExecution(orders, null, clock.instant());
    }

    private QueuedCallProcessor.QueuedCallExecution createQueuedCallExecution(List<Order> orders, String cardId) {
        return createQueuedCallExecution(orders, cardId, clock.instant());
    }

    private QueuedCallProcessor.QueuedCallExecution createQueuedCallExecution(
            List<Order> orders,
            String cardId,
            Instant createdAt) {
        var payload = new AsyncPaymentCallPayload(
                orders.stream().map(Order::getId).collect(Collectors.toList()),
                cardId,
                "login_id");
        try {
            return new QueuedCallProcessor.QueuedCallExecution(
                    orders.stream().findFirst().orElseThrow().getId(),
                    objectMapper.writeValueAsString(payload),
                    0,
                    createdAt,
                    null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
