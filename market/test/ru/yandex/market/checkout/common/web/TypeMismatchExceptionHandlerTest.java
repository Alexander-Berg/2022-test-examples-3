package ru.yandex.market.checkout.common.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;

public class TypeMismatchExceptionHandlerTest {

    @Test
    public void testReplaceMessage() {
        TypeMismatchException noValidPayment = new TypeMismatchException("NO_VALID_PAYMENT", PaymentType.class);

        String replaceMessage = TypeMismatchExceptionHandler.replaceMessage(noValidPayment.getMessage());

        Assertions.assertEquals("Failed to convert value of type [java.lang.String] " +
                "to required type [ru.yandex.market.checkout.checkouter.pay.PaymentType]", replaceMessage);
    }

}
