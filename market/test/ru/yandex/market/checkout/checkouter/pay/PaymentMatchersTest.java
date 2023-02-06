package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentMatchersTest {

    @Test
    public void requiresUserPayment() {
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.YANDEX));
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.CREDIT));
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.INSTALLMENT));
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.APPLE_PAY));
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.GOOGLE_PAY));
        assertTrue(PaymentMatchers.requiresUserPayment(PaymentMethod.SBP));
    }
}
