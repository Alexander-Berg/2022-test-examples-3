package ru.yandex.market.checkout.checkouter.b2b;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.BuyerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class B2bOrderStatusUpdateValidatorTest extends AbstractWebTestBase {
    @Autowired
    private StatusUpdateValidator statusUpdateValidator;
    @Autowired
    private CheckouterFeatureWriter featureWriter;

    private static Order prepareOrderWithPayment(OrderStatus orderStatus, PaymentStatus paymentStatus) {
        Payment payment = spy(new Payment());
        payment.setStatus(paymentStatus);

        Order order = mock(Order.class);
        Buyer buyer = mock(Buyer.class);
        when(order.getPayment()).thenReturn(payment);
        when(order.getBuyer()).thenReturn(buyer);
        when(order.getStatus()).thenReturn(orderStatus);
        when(buyer.getType()).thenReturn(BuyerType.BUSINESS);
        return order;
    }

    private static Order prepareOrderWithoutPayment(OrderStatus orderStatus) {
        Order order = mock(Order.class);
        Buyer buyer = mock(Buyer.class);
        when(order.getPayment()).thenReturn(null);
        when(order.getBuyer()).thenReturn(buyer);
        when(order.getStatus()).thenReturn(orderStatus);
        when(buyer.getType()).thenReturn(BuyerType.BUSINESS);
        return order;
    }

    @Test
    public void testFromUnpaidToPendingWithoutPaymentToggleOff() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.OFF);

        Order order = prepareOrderWithoutPayment(OrderStatus.UNPAID);
        statusUpdateValidator.validateStatusUpdate(order,
                OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM);
    }

    @Test
    public void testFromUnpaidToPendingWithoutPaymentToggleLogging() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        Order order = prepareOrderWithoutPayment(OrderStatus.UNPAID);
        statusUpdateValidator.validateStatusUpdate(order,
                OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM);
    }

    @Test
    public void testFromUnpaidToPendingWithoutPaymentToggleOnFail() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        Order order = prepareOrderWithoutPayment(OrderStatus.UNPAID);
        assertThrows(IllegalStateException.class, () ->
                statusUpdateValidator.validateStatusUpdate(order,
                        OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM));
    }

    @Test
    public void testFromUnpaidToPendingToggleOn() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.CLEARED);
        statusUpdateValidator.validateStatusUpdate(order,
                OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM);
    }

    @Test
    public void testFromUnpaidToPendingToggleLogging() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.CLEARED);
        statusUpdateValidator.validateStatusUpdate(order,
                OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM);
    }

    @Test
    public void testFromUnpaidToPendingToggleOnFail() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.IN_PROGRESS);
        assertThrows(OrderStatusNotAllowedException.class, () ->
                statusUpdateValidator.validateStatusUpdate(order,
                        OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM));
    }

    @Test
    public void testFromUnpaidToPendingToggleLoggingFail() {
        featureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.LOGGING);

        Order order = prepareOrderWithPayment(OrderStatus.UNPAID, PaymentStatus.IN_PROGRESS);
        assertThrows(OrderStatusNotAllowedException.class, () ->
                statusUpdateValidator.validateStatusUpdate(order,
                        OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION, ClientInfo.SYSTEM));
    }
}
