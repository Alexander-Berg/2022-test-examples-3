package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteVasCode;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

class VasCodeConverterTest extends BaseIntegrationTest {

    private final VasCodeConverter converter = new VasCodeConverter();

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                ServiceType.VERIFY_ITEM,
                MarschrouteVasCode.VERIFY_ITEM
            ),
            Arguments.of(
                ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY,
                MarschrouteVasCode.STORE_DEFECTIVE_ITEMS_SEPARATELY
            ),
            Arguments.of(
                ServiceType.UNKNOWN,
                null
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testConversion(ServiceType serviceType, MarschrouteVasCode expected) {
        MarschrouteVasCode actual = converter.convert(serviceType);

        softly.assertThat(actual)
            .as("Asserting that converted VAS code is equal to expected value")
            .isEqualTo(expected);
    }
}
