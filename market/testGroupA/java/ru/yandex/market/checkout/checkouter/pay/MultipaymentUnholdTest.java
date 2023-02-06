package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * @author : poluektov
 * date: 2021-11-10.
 */
public class MultipaymentUnholdTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @Test
    public void testPartialUnholdReceiptForMultiorder() {
        //Создаем мультиордер с платежом в холде
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Parameters anotherParams = defaultBlueOrderParameters();
        anotherParams.setPaymentMethod(PaymentMethod.YANDEX);

        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(anotherParams);
        assertThat(order1.getStatus(), equalTo(OrderStatus.UNPAID));

        Payment payment = payHelper.payForOrders(List.of(order1, order2));
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getStatus(), PaymentStatus.HOLD);
        //Отменяем один из заказов и пытаемся завершить платеж.
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.CANCELLED);
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(order1, order2)));
        paymentService.clearOrCancelHeldPayment(payment.getId(), Set.of(order1.getId(), order2.getId()));

        //Проверяем что в чеке анхолда есть строчка с доставкой.
        Receipt receipt = receiptService.findByPayment(payment, ReceiptType.INCOME_RETURN).get(0);
        assertTrue(receipt.getItems().stream().anyMatch(receiptItem -> receiptItem.getDeliveryId() != null));
    }

    @Test
    public void testPartialUnholdReceiptForMultiorderWithFreeDelivery() {
        //Создаем мультиордер с платежом в холде
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        Parameters anotherParams = defaultBlueOrderParameters();
        anotherParams.setPaymentMethod(PaymentMethod.YANDEX);

        zeroDelivery(parameters);
        zeroDelivery(anotherParams);

        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(anotherParams);
        assertThat(order1.getStatus(), equalTo(OrderStatus.UNPAID));
        Payment payment = payHelper.payForOrders(List.of(order1, order2));
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getStatus(), PaymentStatus.HOLD);
        //Отменяем один из заказов и пытаемся завершить платеж.
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.CANCELLED);
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(order1, order2)));
        paymentService.clearOrCancelHeldPayment(payment.getId(), Set.of(order1.getId(), order2.getId()));

        //Проверяем что в чеке анхолда нет строчки с бесплатной доставкой.
        Receipt receipt = receiptService.findByPayment(payment, ReceiptType.INCOME_RETURN).get(0);
        assertTrue(receipt.getItems().stream().noneMatch(ReceiptItem::isDelivery));
    }

    private void zeroDelivery(Parameters parameters) {
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults().stream()
                .flatMap(actualDeliveryResult -> actualDeliveryResult.getDelivery().stream())
                .forEach(deliveryOption -> deliveryOption.setPrice(BigDecimal.ZERO));
    }

    // MARKETCHECKOUT-24548
    @Test
    void testPartialUnholdReceiptForMultiorderWithThreeOrders() {
        // Создаем мультиордер состоящий из трех ордеров и с платежом в холде.
        Order order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order3 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertThat(order1.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(order2.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(order3.getStatus(), equalTo(OrderStatus.UNPAID));
        Payment payment = orderPayHelper.payForOrders(List.of(order1, order2, order3));
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getStatus(), PaymentStatus.HOLD);

        // Отменяем два из трех заказов и пытаемся завершить платеж.
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(order1.getId()), OrderStatus.DELIVERED);
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(order3.getId()), OrderStatus.CANCELLED);
        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());
        order3 = orderService.getOrder(order3.getId());
        assertThat(order1.getStatus(), equalTo(OrderStatus.DELIVERED));
        assertThat(order2.getStatus(), equalTo(OrderStatus.CANCELLED));
        assertThat(order3.getStatus(), equalTo(OrderStatus.CANCELLED));
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(order1, order2, order3)));
        paymentService.clearOrCancelHeldPayment(payment.getId(), Set.of(order1.getId(), order2.getId(),
                order3.getId()));

        // Проверяем, что в возвратном чеке две строчки с доставкой.
        List<Receipt> receipts = receiptService.findByPayment(payment, ReceiptType.INCOME_RETURN);
        assertEquals(1, receipts.size());
        Receipt receipt = receipts.get(0);
        assertEquals(2, receipt.getItems().stream().filter(ReceiptItem::isDelivery).count());
    }
}
