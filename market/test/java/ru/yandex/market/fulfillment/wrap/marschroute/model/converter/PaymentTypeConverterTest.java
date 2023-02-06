package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

class PaymentTypeConverterTest extends BaseIntegrationTest {

    private final PaymentTypeConverter converter = new PaymentTypeConverter();

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                PaymentType.CASH,
                MarschroutePaymentType.CASH
            ),
            Arguments.of(
                PaymentType.CARD,
                MarschroutePaymentType.CASH
            ),
            Arguments.of(
                PaymentType.PREPAID,
                MarschroutePaymentType.PREPAID
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testConversion(PaymentType paymentMethod, MarschroutePaymentType expected) {
        MarschroutePaymentType actual = converter.convert(paymentMethod);

        softly.assertThat(actual)
            .as("Asserting that converted MarschroutePaymentType is equal to expected value")
            .isEqualTo(expected);
    }
}
