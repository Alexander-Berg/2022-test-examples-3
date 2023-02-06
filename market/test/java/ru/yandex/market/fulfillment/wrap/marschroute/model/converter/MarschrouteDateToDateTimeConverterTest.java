package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

class MarschrouteDateToDateTimeConverterTest extends BaseIntegrationTest {

    private final MarschrouteDateToDateTimeConverter converter = new MarschrouteDateToDateTimeConverter();

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "01.01.1970",
                MarschrouteDate.create("01.01.1970"),
                new DateTime("1970-01-01")
            ),
            Arguments.of(
                "01.01.2017",
                MarschrouteDate.create("01.01.2017"),
                new DateTime("2017-01-01")
            ),
            Arguments.of(
                "01.01.2017",
                MarschrouteDate.create("01.01.2017"),
                new DateTime("2017-01-01")
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest(name = " [" + INDEX_PLACEHOLDER + "] {0}")
    void testConversion(String name, MarschrouteDate marschrouteDate, DateTime expected) {
        DateTime actual = converter.convert(marschrouteDate);

        softly.assertThat(actual)
            .as("Asserting converter DateTime is equal to expected value")
            .isEqualTo(expected);
    }
}
