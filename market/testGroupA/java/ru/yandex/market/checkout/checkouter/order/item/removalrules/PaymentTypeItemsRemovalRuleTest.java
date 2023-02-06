package ru.yandex.market.checkout.checkouter.order.item.removalrules;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

import static ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder.NOT_ALLOWED_PAYMENT_TYPE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.APPLE_PAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.checkouter.pay.PaymentType.POSTPAID;
import static ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID;

class PaymentTypeItemsRemovalRuleTest extends AbstractRemovalRuleTest {

    private PaymentTypeItemsRemovalRule paymentRule;

    @Test
    @DisplayName("Валидный способ оплаты")
    void checkValidPayment() {
        paymentRule = new PaymentTypeItemsRemovalRule(true);
        Order order = initOrder(PREPAID, YANDEX);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertAllowedOrderPermission(order, response);
    }

    @Test
    @DisplayName("Валидный способ оплаты, но предоплата запрещена")
    void checkValidPaymentWithDisabledPrepaid() {
        paymentRule = new PaymentTypeItemsRemovalRule(false);
        Order order = initOrder(PREPAID, APPLE_PAY);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertDisabledOrderPermission(order, response, NOT_ALLOWED_PAYMENT_TYPE);
    }

    @Test
    @DisplayName("Невалидный способ оплаты")
    void checkInvalidPayment() {
        paymentRule = new PaymentTypeItemsRemovalRule(true);
        Order order = initOrder(PREPAID, PaymentMethod.B2B_ACCOUNT_PREPAYMENT);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertDisabledOrderPermission(order, response, NOT_ALLOWED_PAYMENT_TYPE);
    }

    @Test
    @DisplayName("C постоплатой не важно какой метод оплаты")
    void postpaidPayment() {
        paymentRule = new PaymentTypeItemsRemovalRule(true);
        Order order = initOrder(POSTPAID, null);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertAllowedOrderPermission(order, response);
    }

    @Test
    @DisplayName("На постоплату не распространяется флаг отмены предоплаты")
    void postpaidPaymentWithDisabledPrepaid() {
        paymentRule = new PaymentTypeItemsRemovalRule(false);
        Order order = initOrder(POSTPAID, null);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertAllowedOrderPermission(order, response);
    }

    @Test
    @DisplayName("Удалять нельзя, если заказ был оплачен при помощи сертификатов")
    void disableWhenPaymentWithCertificate() {
        paymentRule = new PaymentTypeItemsRemovalRule(false);
        Order order = initOrder(POSTPAID, null);
        order.setExternalCertificateId(666L);

        OrderItemsRemovalPermissionResponse response = paymentRule.apply(order);

        assertDisabledOrderPermission(order, response, NOT_ALLOWED_PAYMENT_TYPE);
    }

    private Order initOrder(PaymentType paymentType, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.setId(16L);
        order.setItems(Collections.emptySet());
        order.setPaymentType(paymentType);
        order.setPaymentMethod(paymentMethod);
        return order;
    }
}
