package ru.yandex.market.checkout.checkouter.promo.bundles.returns;

import java.time.Instant;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CLEARED;
import static ru.yandex.market.checkout.checkouter.pay.RefundReason.ORDER_CANCELLED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.ACCEPTED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.helpers.RefundHelper.assertRefund;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftsCancelOrderTest extends AbstractReturnTestBase {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private RefundHelper refundHelper;

    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void shouldCancelProcessingPayedOrder() throws Exception {
        Instant currentTime = Instant.now();

        setFixedTime(currentTime);

        Order typicalOrder = createTypicalOrderWithBundles();

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, PROCESSING);

        setFixedTime(currentTime.plus(5, DAYS));

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        typicalOrder = orderService.getOrder(typicalOrder.getId());

        assertThat(typicalOrder.getStatus(), is(PROCESSING));
        assertThat(typicalOrder.getPayment().getStatus(), is(CLEARED));

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_CASH_PAYMENT);

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, CANCELLED);

        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND_SUBSIDY_PAYMENT);

        Refund refund = refundHelper.anyRefundFor(typicalOrder, PaymentGoal.ORDER_PREPAY);
        refund = refundHelper.proceedAsyncRefund(refund);
        assertRefund(refund, ACCEPTED, ORDER_CANCELLED);

        refund = refundHelper.processToSuccess(refund, typicalOrder);
        assertRefund(refund, SUCCESS, ORDER_CANCELLED);

        typicalOrder = orderService.getOrder(typicalOrder.getId());

        assertThat(typicalOrder.getStatus(), is(CANCELLED));
        assertThat(refund.getAmount(), comparesEqualTo(typicalOrder.getTotal()));

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(4));
    }

    @Test
    public void shouldCancelDeliveringOrder() throws Exception {
        Order typicalOrder = createTypicalOrderWithBundles();

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, DELIVERY);

        assertThat(typicalOrder.getStatus(), is(DELIVERY));
        assertThat(typicalOrder.getPayment().getStatus(), is(CLEARED));

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_CASH_PAYMENT);

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, CANCELLED);

        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND_SUBSIDY_PAYMENT);

        Refund refund = refundHelper.anyRefundFor(typicalOrder, PaymentGoal.ORDER_PREPAY);
        refund = refundHelper.proceedAsyncRefund(refund);
        assertRefund(refund, ACCEPTED, ORDER_CANCELLED);

        refund = refundHelper.processToSuccess(refund, typicalOrder);
        assertRefund(refund, SUCCESS, ORDER_CANCELLED);

        typicalOrder = orderService.getOrder(typicalOrder.getId());

        assertThat(typicalOrder.getStatus(), is(CANCELLED));
        assertThat(refund.getAmount(), comparesEqualTo(typicalOrder.getTotal()));

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(4));
    }

    private Order createTypicalOrderWithBundles() {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000);

        OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .emptyId()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .price(2000);

        Order typicalOrder = orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondaryOffer)
        ), PROMO_KEY, config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(true),
                                itemResponseFor(secondaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(false),
                                itemResponseFor(primaryOffer)
                        )));


        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)), null);

        return typicalOrder;
    }
}
