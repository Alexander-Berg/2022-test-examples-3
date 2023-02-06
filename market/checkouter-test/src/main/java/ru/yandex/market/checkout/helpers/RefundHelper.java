package ru.yandex.market.checkout.helpers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.JsonPath;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.hamcrest.core.Is;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.ExpectedRefund;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundableItem;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.pay.TrustRefundKey;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ARCHIVED;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestHelper.isNewPrepayType;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundRequest;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalWithRefundConfig;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkDoRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.skipCheckBasketCalls;

@WebTestHelper
public class RefundHelper {

    private final MockRequestHelper requestHelper;
    private final TrustMockConfigurer trustMockConfigurer;
    private final ReceiptService receiptService;
    private final RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    public RefundHelper(MockRequestHelper requestHelper, TrustMockConfigurer trustMockConfigurer,
                        ReceiptService receiptService, RefundService refundService) {
        this.requestHelper = requestHelper;
        this.trustMockConfigurer = trustMockConfigurer;
        this.receiptService = receiptService;
        this.refundService = refundService;
    }

    @Nonnull
    public RefundableItems getRefundableItemsFor(Order order) {
        try {
            return requestHelper.get(RefundableItems.class, order,
                    "/orders/{orderId}/refundable-items", order.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public Refund refund(Order order, RefundReason reason,
                         int expectedResponseStatus) throws Exception {
        trustMockConfigurer.resetRequests();
        return refund(getRefundableItemsFor(order), order, reason, expectedResponseStatus);
    }

    @Nonnull
    public Refund refund(
            RefundableItems itemsToRefund,
            Order order,
            RefundReason reason,
            int expectedResponseStatus
    ) throws Exception {
        return this.refund(itemsToRefund, order, reason, expectedResponseStatus, false);
    }

    @Nonnull
    public Refund refund(
            RefundableItems itemsToRefund,
            Order order,
            RefundReason reason,
            int expectedResponseStatus,
            boolean skipValidation
    ) throws Exception {
        trustMockConfigurer.resetRequests();
        Refund refund = requestHelper.request(refundRequest(itemsToRefund, BigDecimal.ZERO, order, reason, false),
                Refund.class,
                status().is(expectedResponseStatus));
        Refund expectedRefund;
        Long refundId = refund.getId();
        if (isAsyncRefundStrategyEnabled(resolvePaymentGoal(refund))) {
            refund = proceedAsyncRefunds(order.getId()).stream()
                    .filter(it -> Objects.equals(it.getId(), refundId))
                    .findAny()
                    .orElseThrow();
            expectedRefund = new ExpectedRefund();
            expectedRefund.setId(refundId);
            expectedRefund.setPayment(order.getPayment());
            expectedRefund.setTrustRefundKey(refund.getTrustRefundKey());
            expectedRefund.setStatus(refund.getStatus());
            expectedRefund.setReason(refund.getReason());
            expectedRefund.setCreatedByRole(refund.getCreatedByRole());
            expectedRefund.setCreatedBy(refund.getCreatedBy());
            expectedRefund.setShopManagerId(null);
            expectedRefund.setAmount(refund.getAmount());
        } else {
            expectedRefund = refund;
        }
        if (!skipValidation) {
            checkBalanceCallsAfterRefund(expectedRefund, itemsToRefund, order);
        }
        for (Refund it : refundService.getRefunds(order.getId())) {
            processToSuccess(it, order);
        }
        return refundService.getRefund(refundId);
    }

    public void notifyRefund(Order order, Refund refund, BasketStatus basketStatus, CheckBasketParams mockConfig)
            throws Exception {
        trustMockConfigurer.mockCheckBasket(mockConfig);
        trustMockConfigurer.mockStatusBasket(mockConfig, null);
        requestHelper.post(order, null,
                "/payments/{paymentId}/notify?status={status}&trust_refund_id={refundId}&mode={mode}",
                order.getPaymentId(), basketStatus, refund.getTrustRefundId(), NotificationMode.refund_result
        );
    }

    private void notifyAndCheckRefund(Order order, Refund refund, BasketStatus basketStatus) throws Exception {
        notifyRefund(order, refund, basketStatus, buildReversalWithRefundConfig(refund.getTrustRefundId()));
    }

    private TrustRefundKey checkBalanceCallsAfterRefund(Refund expectedRefund,
                                                        RefundableItems refundableItems, Order order) {
        Iterator<ServeEvent> callIter = skipCheckBasketCalls(trustMockConfigurer.servedEvents());

        CreateRefundParams refundCheckerConfig = CreateRefundParams.refund(order.getPayment().getBasketKey())
                .withUserIp("127.0.0.1")
                .withReason(expectedRefund.getComment() != null ?
                        expectedRefund.getComment() : "Reason: " + expectedRefund.getReason().name()
                );

        if (isNewPrepayType(order.getPayment()) &&
                receiptService.paymentHasReceipt(order.getPayment())) {
            refundableItems.getItems().forEach(i ->
                    refundCheckerConfig.withRefundLine(
                            resolveItemBy(order, i).getBalanceOrderId(),
                            i.getRefundableQuantityIfExistsOrRefundableCount(),
                            i.getQuantPriceIfExistsOrBuyerPrice()
                                    .multiply(i.getRefundableQuantityIfExistsOrRefundableCount())
                    ));
            refundableItems.getItemServices().forEach(s ->
                    refundCheckerConfig.withRefundLine(
                            order.getItemServicesMapById().get(s.getId()).getBalanceOrderId(),
                            BigDecimal.valueOf(s.getRefundableCount()),
                            s.getPrice().multiply(BigDecimal.valueOf(s.getRefundableCount()))
                    ));

            if (refundableItems.getDelivery() != null
                    && refundableItems.getDelivery().isRefundable()) {
                refundCheckerConfig.withRefundLine(
                        order.getDelivery().getBalanceOrderId(),
                        BigDecimal.ONE,
                        refundableItems.getDelivery().getBuyerPrice());
            }
        } else {
            if (expectedRefund.getAmount() == null && refundableItems != null) {
                expectedRefund.setAmount(refundableItems.getRefundableAmount());
            }
            refundCheckerConfig.withRefundLine(order.getBalanceOrderId(), BigDecimal.ZERO, expectedRefund.getAmount());
        }

        Long uid = expectedRefund.getPayment().getUid();
        String trustRefundId = checkCreateRefundCall(callIter, uid, refundCheckerConfig);
        checkDoRefundCall(callIter, trustRefundId, uid);

        assertThat("Detected unexpected calls to Balance", callIter.hasNext(), Is.is(false));

        return new TrustRefundKey(trustRefundId);
    }

    private OrderItem resolveItemBy(Order order, RefundableItem refundableItem) {
        if (refundableItem.getId() != null) {
            return order.getItem(refundableItem.getId());
        } else {
            return order.firstItemFor(new FeedOfferId(refundableItem.getOfferId(),
                    refundableItem.getFeedId()));
        }
    }

    @Nonnull
    public Refund refund(BigDecimal amount, Order order, RefundReason reason) throws Exception {
        trustMockConfigurer.resetRequests();
        return requestHelper.request(refundRequest(null, amount, order, reason, false), Refund.class);
    }

    public static void assertRefund(Refund refund, RefundStatus status, RefundReason refundReason) {
        assertThat(refund, notNullValue());
        assertThat(refund.getId(), notNullValue());
        assertThat(refund.getStatus(), notNullValue());
        assertThat(refund.getReason(), notNullValue());
        assertThat(refund.getAmount(), notNullValue());
        assertThat(refund.getStatus(), is(status));
        assertThat(refund.getReason(), is(refundReason));
    }

    private Multimap<PaymentGoal, Refund> anyRefundsFor(Order typicalOrder) {
        return refundService.getRefunds(typicalOrder.getId()).stream()
                .collect(toMultimap(ref -> ref.getPayment().getType(), Function.identity(), HashMultimap::create));
    }

    public Refund anyRefundFor(Order typicalOrder, PaymentGoal paymentGoal) {
        return anyRefundsFor(typicalOrder).get(paymentGoal).stream().findFirst().orElse(null);
    }

    public Refund processToSuccess(Refund refund, Order order) throws Exception {
        notifyAndCheckRefund(order, refund, BasketStatus.success);
        return refundService.getRefund(refund.getId());
    }

    public Refund getRefund(ResultActions result) {
        long refundId = RefundHelper.getRefundIdFromResponse(result);
        return refundService.getRefund(refundId);
    }

    public void checkReceiptOfPartialRefund(final Refund refund, final BigDecimal amount) {
        final List<Receipt> receipts = receiptService.findByRefund(refund, ReceiptType.INCOME_RETURN);
        assertEquals(1, receipts.size());
        final List<ReceiptItem> items = receipts.get(0).getItems();
        assertEquals(1, items.size());
        final ReceiptItem receiptItem = items.get(0);
        assertEquals(0, receiptItem.getCount());
        assertThat(receiptItem.getAmount(), comparesEqualTo(amount));
    }

    public void checkTrustCallsForPartialRefund(@Nonnull final Order order,
                                                @Nonnull final OrderItem orderItem,
                                                @Nonnull final BigDecimal amount,
                                                final int expectedEventsCount) {
        checkTrustCallsForPartialRefund(order, orderItem.getBalanceOrderId(), amount, expectedEventsCount);
    }

    public void checkTrustCallsForPartialRefund(@Nonnull final Order order,
                                                @Nonnull final BigDecimal amount,
                                                final int expectedEventsCount) {
        checkTrustCallsForPartialRefund(order, order.getBalanceOrderId(), amount, expectedEventsCount);
    }

    private void checkTrustCallsForPartialRefund(@Nonnull final Order order,
                                                 @Nonnull final String balanceOrderId,
                                                 @Nonnull final BigDecimal amount,
                                                 final int expectedEventsCount) {
        List<ServeEvent> createRefundEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(TrustMockConfigurer.CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        // Событий может быть несколько,
        assertThat(createRefundEvents, hasSize(expectedEventsCount));
        // но проверяем всегда последнее
        ServeEvent refundEvent = createRefundEvents.get(expectedEventsCount - 1);
        checkCreateRefundCall(refundEvent, order.getUid(), CreateRefundParams.refund(order.getPayment().getBasketKey())
                .withUserIp("127.0.0.1")
                .withReason("Reason: ORDER_CHANGED")
                .withRefundLine(
                        balanceOrderId,
                        BigDecimal.ZERO,
                        amount
                )
        );
    }

    private static long getRefundIdFromResponse(ResultActions result) {
        String responseBody;
        try {
            responseBody = result.andReturn().getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        return (((Number) JsonPath.read(responseBody, "$.id")).longValue());
    }

    public void checkRefundAmount(@Nonnull final Refund refund,
                                  @Nonnull final BigDecimal expectedAmount,
                                  @Nonnull final BigDecimal expectedReminder) {
        assertThat(refund.getAmount(), comparesEqualTo(expectedAmount));
        assertThat(refund.getOrderRemainder(), comparesEqualTo(expectedReminder));
    }

    public ResultActions getOrderRefundsForActions(long orderId, ClientInfo clientInfo, boolean archived)
            throws Exception {
        return requestHelper.mockMvc.perform((get("/orders/{orderId}/refunds", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(ARCHIVED, String.valueOf(archived))));
    }

    public boolean isAsyncRefundStrategyEnabled(PaymentGoal paymentGoal) {
        return refundService.chooseStrategy(paymentGoal).asyncProcessingEnabled();
    }

    public Collection<Refund> proceedAsyncRefunds(Collection<Refund> refunds) {
        return refunds
                .stream()
                .map(this::proceedAsyncRefund)
                .collect(toList());
    }

    public Refund proceedAsyncRefund(Refund refund) {
        if (!isAsyncRefundStrategyEnabled(resolvePaymentGoal(refund)) || refund.getStatus() == RefundStatus.SUCCESS) {
            return refund;
        }
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, refund.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PROCESS_REFUND, refund.getId());
        return refundService.getRefund(refund.getId());
    }

    private PaymentGoal resolvePaymentGoal(Refund refund) {
        return Optional.of(refund)
                .map(Refund::getPayment)
                .map(Payment::getType)
                .orElseGet(() -> Optional.of(refund)
                        .map(Refund::getPaymentId)
                        .map(paymentId -> paymentService.getPayment(paymentId, ClientInfo.SYSTEM))
                        .map(Payment::getType)
                        .orElse(null));
    }

    public Collection<Refund> proceedAsyncRefunds(Return ret) {
        return proceedAsyncRefunds(refundService.getReturnRefunds(ret));
    }

    public Collection<Refund> proceedAsyncRefunds(Long orderId) {
        return proceedAsyncRefunds(refundService.getRefunds(orderId));
    }

    public Collection<Refund> proceedAsyncRefunds(Long... orderIds) {
        return Arrays.stream(orderIds)
                .map(this::proceedAsyncRefunds)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    public boolean isAsyncRefundStrategyEnabled(Refund refund) {
        return isAsyncRefundStrategyEnabled(resolvePaymentGoal(refund));
    }
}
