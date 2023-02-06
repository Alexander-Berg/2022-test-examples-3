package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

class MarschrouteDateTimeToDateTimeConverterTest extends BaseIntegrationTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "01.01.1970 00:00:00",
                MarschrouteDateTime.create("01.01.1970 00:00:00"),
                new DateTime("1970-01-01T00:00:00")
            ),
            Arguments.of(
                "01.01.2017 23:59:59",
                MarschrouteDateTime.create("01.01.2017 23:59:59"),
                new DateTime("2017-01-01T23:59:59")
            ),
            Arguments.of(
                "01.01.2017 23:59",
                MarschrouteDateTime.create("01.01.2017 23:59"),
                new DateTime("2017-01-01T23:59+03:00")
            )
        );
    }

    private MarschrouteDateTimeToDateTimeConverter converter = new MarschrouteDateTimeToDateTimeConverter();

    @MethodSource("data")
    @ParameterizedTest(name = " [" + INDEX_PLACEHOLDER + "] {0}")
    void testConversion(String name, MarschrouteDateTime marschrouteDateTime, DateTime expected) throws Exception {
        DateTime actual = converter.convert(marschrouteDateTime);

        softly.assertThat(actual)
            .as("Asserting converter DateTime is equal to expected value")
            .isEqualTo(expected);
    }
}
