package ru.yandex.market.checkout.util.items;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.UpdateBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.math.BigDecimal.ROUND_HALF_UP;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.BasketLineState;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildReversalConfig;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBasketClearCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkUpdateBasketCalls;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.skipCheckBasketCalls;
import static ru.yandex.market.checkout.util.balance.checkers.UpdateBasketParams.updateBasket;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

@TestComponent
public class ChangeItemsPaymentChecker {

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private TrustMockConfigurer trustMockConfigurer;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private ReceiptService receiptService;

    private static BigDecimal moneyAmount(OrderItem item) {
        return item.getBuyerPrice().multiply(new BigDecimal(item.getCount()));
    }

    public void mockBalanceIfPrepaid(Order order) {
        if (order.getPaymentType() == PREPAID) {
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_CANCEL);
            trustMockConfigurer.resetRequests();

            CheckBasketParams config = buildReversalConfig();
            if (order.getBalanceOrderId() != null) {
                config.setLines(Collections.singleton(
                        new BasketLineState(order.getBalanceOrderId(), 1, order.getBuyerTotal())
                ));
            } else {
                Collection<BasketLineState> lines = new ArrayList<>();
                order.getItems().forEach(
                        i -> lines.add(new BasketLineState(
                                i.getBalanceOrderId(),
                                i.getCount(),
                                i.getBuyerPrice().multiply(new BigDecimal(i.getCount()))
                        )));
                if (!order.getDelivery().isFree()) {
                    lines.add(new BasketLineState(
                            order.getDelivery().getBalanceOrderId(),
                            1,
                            order.getDelivery().getBuyerPrice()));
                }
                config.setLines(lines);
            }
            trustMockConfigurer.mockCheckBasket(config);
            trustMockConfigurer.mockStatusBasket(config, null);
        }
    }

    public void checkPaymentAfterChange(Order order, Order orderBeforeChange, ChangeItemsContext context) {
        Payment payment = paymentService.getPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        if (!payment.isCleared()) {
            assertThat(payment.getTotalAmount(), numberEqualsTo(order.getBuyerTotal()));

            checkUpdateBasket(order, payment, orderBeforeChange);

            if (payment.requiresPrintableReceipt()) {
                List<Receipt> receipts = receiptService.findByPayment(payment, ReceiptType.INCOME_RETURN);
                assertThat(receipts, hasSize(1));
                Receipt receipt = receipts.get(0);
                assertThat(receipt.getStatus(), is(ReceiptStatus.WAITING_FOR_CLEARANCE));
                List<ReceiptItem> receiptItems = receipt.getItems();
                assertThat(receiptItems, hasSize(context.getAccumulatedChanges().size()));
                for (ReceiptItem item : receiptItems) {
                    OrderItem origItem = context.getOrigOrder().getItem(item.getItemId());
                    assertThat(origItem, notNullValue());
                    Integer newCount = context.getAccumulatedChanges().get(origItem.getOfferItemKey());
                    assertThat(newCount, notNullValue());
                    assertThat(item.getCount(), is(origItem.getCount() - newCount));
                }
            }
            assertThat(refundService.getRefunds(order.getId()), empty());
        } else {
            assertThat(payment.getTotalAmount(), numberEqualsTo(orderBeforeChange.getPayment().getTotalAmount()));

            List<Refund> refunds = refundService.getRefunds(order.getId()).stream()
                    .filter(r -> r.getPaymentId().equals(payment.getId()))
                    .filter(r -> !context.getRefundsBefore().contains(r.getId())).collect(toList());
            refunds.forEach(r -> context.getRefundsBefore().add(r.getId()));

            BigDecimal remainderBeforeChange = orderBeforeChange.getPayment().getRemainder(orderBeforeChange);
            BigDecimal expectedRefundAmount = orderBeforeChange.getBuyerTotal().subtract(order.getBuyerTotal());
            expectedRefundAmount = expectedRefundAmount.min(remainderBeforeChange);

            if (expectedRefundAmount.signum() > 0) {
                assertThat(refunds, hasSize(1));
                Refund refund = refunds.get(0);
                assertThat(refund.getAmount(), numberEqualsTo(expectedRefundAmount));
                assertThat(refund.getOrderRemainder(), numberEqualsTo(
                        payment.getTotalAmount()
                                .subtract(avoidNull(order.getRefundActual(), ZERO))
                                .subtract(avoidNull(order.getRefundPlanned(), ZERO))));
                assertThat(refund.wereOrderItemsChanged(), is(true));
                assertThat(refund.getReason(), is(RefundReason.ORDER_CHANGED));
            } else {
                assertThat(refunds, empty());
            }
        }
    }

    public void checkPaymentClearance(Order order) {
        Payment payment = order.getPayment();

        Iterator<ServeEvent> callIter = skipCheckBasketCalls(trustMockConfigurer.servedEvents());
        checkBasketClearCall(callIter, order.getBuyer().getUid(), payment.getBasketKey());

        if (payment.requiresPrintableReceipt()) {
            receiptRepairHelper.repairReceipts();

            List<Receipt> receipts = receiptService.findByPayment(payment, ReceiptType.INCOME_RETURN);
            assertThat(receipts, hasSize(1));
            Receipt receipt = receipts.get(0);
            assertThat(receipt.getStatus(), is(ReceiptStatus.PRINTED));
        }
    }

    private void checkUpdateBasket(Order order, Payment payment, Order orderBeforeChange) {
        try {
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_PARTIAL_UNHOLD);
            Iterator<ServeEvent> callIter = skipCheckBasketCalls(trustMockConfigurer.servedEvents());

            UpdateBasketParams updateBasketParams = updateBasket(payment.getBasketKey())
                    .withUid(order.getBuyer().getUid())
                    .withUserIp("127.0.0.1");
            addUpdateBasketLines(order, updateBasketParams, orderBeforeChange);

            checkUpdateBasketCalls(callIter, updateBasketParams);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void addUpdateBasketLines(Order order, UpdateBasketParams updateBasketParams, Order orderBeforeChange) {
        Map<Long, OrderItem> actualItemsById = order.getItemsMapById();
        //убираем неизмененные строки
        order.getItems().stream()
                .filter(item -> actualItemsById.containsKey(item.getId()))
                .filter(item -> moneyAmount(item).compareTo(moneyAmount(actualItemsById.get(item.getId()))) != 0)
                .forEach(i ->
                        updateBasketParams.withLineToUpdate(
                                i.getBalanceOrderId(),
                                i.getCount(),
                                i.getBuyerPrice().multiply(new BigDecimal(i.getCount())).setScale(2, ROUND_HALF_UP)
                        )
                );

        //удаленные строки
        orderBeforeChange.getItems().stream().filter(i -> !actualItemsById.containsKey(i.getId()))
                .forEach(i -> updateBasketParams.withLineToRemove(i.getBalanceOrderId()));
        //строку в корзине по доставке не меняем
    }
}
