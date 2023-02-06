package ru.yandex.travel.orders.services.payments.model;

import java.math.BigDecimal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TrustCompositeOrderPaymentMarkupTest {
    @Test
    public void testNoZeros() {
        TrustCompositeOrderPaymentMarkup value = TrustCompositeOrderPaymentMarkup.builder()
                .card(BigDecimal.ONE)
                .yandexAccount(BigDecimal.ZERO)
                .build();
        TrustCompositeOrderPaymentMarkup valueNoZero = TrustCompositeOrderPaymentMarkup.builder()
                .card(BigDecimal.ONE)
                .build();

        assertThat(value.withoutZeros()).isEqualTo(valueNoZero);
    }
}
