package ru.yandex.market.checkout.checkouter.order.paymentinfo.actualize;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PaymentInfoValidator;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID;

public class PaymentInfoValidatorTest {

    @Test
    public void paymentTypeIsNull() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentMethod(PaymentMethod.YANDEX);
        var orders = List.of(order);
        var exceptionMessage = assertThrows(IllegalArgumentException.class, () ->
                validator.validatePaymentMethods(orders)).getMessage();
        assertEquals("Order have invalid payment method", exceptionMessage);
    }

    @Test
    public void paymentTypeIsUnknown() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentMethod(PaymentMethod.YANDEX);
        order.setPaymentType(PaymentType.UNKNOWN);
        var orders = List.of(order);
        var exceptionMessage = assertThrows(IllegalArgumentException.class, () ->
                validator.validatePaymentMethods(orders)).getMessage();
        assertEquals("Order have invalid payment method", exceptionMessage);

    }

    @Test
    public void paymentMethodIsNull() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PREPAID);
        var orders = List.of(order);
        var exceptionMessage = assertThrows(IllegalArgumentException.class, () ->
                validator.validatePaymentMethods(orders)).getMessage();
        assertEquals("Order have invalid payment method", exceptionMessage);
    }

    @Test
    public void paymentMethodIsUnknown() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PREPAID);
        order.setPaymentMethod(PaymentMethod.UNKNOWN);
        var orders = List.of(order);
        var exceptionMessage = assertThrows(IllegalArgumentException.class, () ->
                validator.validatePaymentMethods(orders)).getMessage();
        assertEquals("Order have invalid payment method", exceptionMessage);
    }

    @Test
    public void paymentMethodAndTypeIsValid() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PREPAID);
        order.setPaymentMethod(PaymentMethod.YANDEX);
        var orders = List.of(order);
        assertDoesNotThrow(() -> validator.validatePaymentMethods(orders));
    }

    @Test
    public void paymentMethodIsNotAllowed() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentMethod(PaymentMethod.YANDEX);
        var paymentOptions = Set.of(PaymentMethod.APPLE_PAY, PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentOptions(paymentOptions);
        var orders = List.of(order);
        var exceptionMessage = assertThrows(IllegalArgumentException.class, () ->
                validator.validateAllowedPaymentMethods(orders)).getMessage();
        assertEquals("Not allowed payment method", exceptionMessage);
    }

    @Test
    public void paymentMethodIsAllowed() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentMethod(PaymentMethod.YANDEX);
        var paymentOptions = Set.of(PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX);
        order.setPaymentOptions(paymentOptions);
        var orders = List.of(order);
        assertDoesNotThrow(() -> validator.validateAllowedPaymentMethods(orders));
    }

    @Test
    public void orderSubStatusIsNull() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PaymentType.PREPAID);
        order.setStatus(OrderStatus.UNPAID);
        var orders = List.of(order);

        var exceptionMessage = assertThrows(IllegalStateException.class, () ->
                validator.validateOrdersStatus(orders)).getMessage();
        assertEquals("Invalid order status", exceptionMessage);
    }

    @Test
    public void orderIsPaid() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PaymentType.PREPAID);
        order.setSubstatus(OrderSubstatus.STARTED);
        order.setStatus(OrderStatus.PROCESSING);
        var orders = List.of(order);

        var exceptionMessage = assertThrows(IllegalStateException.class, () ->
                validator.validateOrdersStatus(orders)).getMessage();
        assertEquals("Invalid order status", exceptionMessage);
    }

    @Test
    public void paymentIsHold() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PaymentType.PREPAID);
        order.setSubstatus(OrderSubstatus.AWAIT_PAYMENT);
        order.setStatus(OrderStatus.UNPAID);
        var orders = List.of(order);

        var exceptionMessage = assertThrows(IllegalStateException.class, () ->
                validator.validateOrdersStatus(orders)).getMessage();
        assertEquals("Invalid order status", exceptionMessage);
    }

    @Test
    public void paymentIsUnpaid() {
        var validator = new PaymentInfoValidator();
        var order = new Order();
        order.setPaymentType(PaymentType.PREPAID);
        order.setSubstatus(OrderSubstatus.WAITING_USER_DELIVERY_INPUT);
        order.setStatus(OrderStatus.UNPAID);
        var orders = List.of(order);

        assertDoesNotThrow(() -> validator.validateOrdersStatus(orders));
    }
}
