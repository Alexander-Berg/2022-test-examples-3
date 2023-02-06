package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.request.PaymentRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.mediabilling.MediabillingMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;

/**
 * @author apollin
 */
public class PlusPaymentTest extends AbstractReturnTestBase {

    public static final long SHOP_ID = 11317159L;

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Autowired
    private MediabillingMockConfigurer mediabillingMockConfigurer;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private RefundHelper refundHelper;

    @Test
    public void createPlusStationPayment() throws IOException {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);

        assertEquals("https://testing.payment-widget.ott.yandex.ru?productIds=some_product_id&" +
                "target=market-station&marketPaymentId=1", paymentResponse.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.YA_PLUS_SUBSCRIPTION, payment.getType());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
    }

    @Test
    public void whenPaymentRetry_createSecondPaymentAndBindOrderToNew() throws IOException {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse1 = orderPayHelper.payWithRealResponse(order);
        CreatePaymentResponse paymentResponse2 = orderPayHelper.payWithRealResponse(order);
        assertEquals(
                paymentResponse2.getId(),
                orderService.getOrder(order.getId()).getPayment().getId()
        );
        assertEquals("https://testing.payment-widget.ott.yandex.ru?productIds=some_product_id&" +
                "target=market-station&marketPaymentId=1", paymentResponse2.getPaymentUrl());

        Payment payment = paymentService.getPayment(paymentResponse2.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.YA_PLUS_SUBSCRIPTION, payment.getType());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
    }

    @Test
    public void givenInitializedPayment_whenGetPayment_updateStatusToCleared() throws IOException {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        mediabillingMockConfigurer.mockOrderStatus();

        var payment = client.payments()
                .getPayment(
                        new RequestClientInfo(ClientRole.SYSTEM, 0L),
                        PaymentRequest.builder(paymentResponse.getId()).withForceTrustSync(true).build());

        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        var orders = orderService.getOrdersByPayment(payment.getId(),
                new ClientInfo(ClientRole.SYSTEM, 0L));
        assertEquals(1, orders.size());
        assertEquals(OrderStatus.PROCESSING, List.copyOf(orders).get(0).getStatus());
    }


    @Test
    public void givenProcessingOrder_whenRefund_successfulRefund() throws Exception {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        mediabillingMockConfigurer.mockOrderStatus();
        orderPayHelper.notifyPlusPayment(payment.getId(), "cleared", "trust-payment-id", "pt");

        mediabillingMockConfigurer.mockRefund();
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_REFUND, order.getId());
        refundHelper.proceedAsyncRefunds(order.getId());
        Collection<Refund> refunds = refundService.getRefunds(order.getId());

        assertThat(refunds, hasSize(1));
        Refund firstRefund = refunds.iterator().next();
        assertThat(firstRefund.getStatus(), equalTo(RefundStatus.ACCEPTED));
        assertThat(firstRefund.getTrustRefundId(), equalTo("some_id"));
        assertThat(firstRefund.getStatusExpiryDate(), notNullValue());
    }

    @Test
    public void givenOrderInDeliveryStatus_whenRefund_successfulRefund() throws Exception {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        mediabillingMockConfigurer.mockOrderStatus();
        orderPayHelper.notifyPlusPayment(payment.getId(), "cleared", "trust-payment-id", "pt");
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        mediabillingMockConfigurer.mockRefund();
        //перегружаем чтобы получить нормальный платеж
        RefundableItems refundableItems = refundService.getRefundableItems(orderService.getOrder(order.getId()));
        List<RefundItem> refundItemsList = refundableItems.getItems().stream()
                .map(ri -> new RefundItem(null, ri.getFeedId(), ri.getOfferId(), ri.getCount(),
                        ri.getQuantityIfExistsOrCount(), false, null))
                .collect(Collectors.toList());

        RefundItems refundItems = new RefundItems(refundItemsList);
        var refund = refundService.createRefund(
                order.getId(),
                null,
                "Возврат денег за станционный заказ",
                new ClientInfo(ClientRole.REFEREE, 135135L),
                RefundReason.ORDER_CANCELLED,
                PaymentGoal.YA_PLUS_SUBSCRIPTION,
                false,
                refundItems,
                false,
                null,
                false
        );
        refundHelper.proceedAsyncRefunds(refund);

        Collection<Refund> refunds = refundService.getRefunds(order.getId());

        assertThat(refunds, hasSize(1));
        Refund firstRefund = refunds.iterator().next();
        assertThat(firstRefund.getTrustRefundId(), equalTo("some_id"));
        assertThat(firstRefund.getStatus(), equalTo(RefundStatus.ACCEPTED));
        assertThat(firstRefund.getStatusExpiryDate(), notNullValue());
    }

    @Test
    public void givenStationOrder_whenReturn_forbidReturn() throws Exception {
        Order order = createMbPlusOrder();
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);
        mediabillingMockConfigurer.mockOrderStatus();
        orderPayHelper.notifyPlusPayment(payment.getId(), "cleared", "trust-payment-id", "pt");
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        mediabillingMockConfigurer.mockRefund();

        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            returnHelper.initReturn(order.getId(), request);
        });
    }


    private Order createMbPlusOrder() throws IOException {
        Parameters parameters = prepaidBlueOrderParameters();
        mediabillingMockConfigurer.mockInvoiceCreation();
        parameters.getBuiltMultiCart().getCarts().forEach(o ->
                o.setPaymentSubmethod(PaymentSubmethod.STATION_SUBSCRIPTION));
        parameters.getOrders().forEach(item -> item.setPaymentSubmethod(PaymentSubmethod.STATION_SUBSCRIPTION));
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getItems().forEach(o -> o.setSupplierId(SHOP_ID));
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        return orderCreateHelper.createOrder(parameters);
    }
}
