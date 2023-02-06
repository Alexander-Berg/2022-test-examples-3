package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

class DateTimeToMarschrouteDateConverterTest {

    private final DateTimeToMarschrouteDateConverter converter = new DateTimeToMarschrouteDateConverter();

    @MethodSource("data")
    @ParameterizedTest(name = " [" + INDEX_PLACEHOLDER + "] {0}")
    void test(String name, DateTime initialDateTime, MarschrouteDate expected) {
        MarschrouteDate actual = converter.convert(initialDateTime);

        assertThat(actual).isEqualTo(expected);
    }

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "2017-09-10",
                new DateTime("2017-09-10"),
                MarschrouteDate.create("10.09.2017")
            ),
            Arguments.of(
                "2017-09-10T10:30",
                new DateTime("2017-09-10T10:30"),
                MarschrouteDate.create("10.09.2017")
            ),
            Arguments.of(
                "2017-09-10T05:56:10",
                new DateTime("2017-09-10T05:56:10"),
                MarschrouteDate.create("10.09.2017")
            ),
            Arguments.of(
                "2017-09-10T05:56:10",
                new DateTime("2017-09-10T05:56:10"),
                MarschrouteDate.create("10.09.2017")
            ),
            Arguments.of(
                "2017-09-10T23:59:59",
                new DateTime("2017-09-10T23:59:59"),
                MarschrouteDate.create("10.09.2017")
            ),
            Arguments.of(
                "2017-09-10T00:00:00",
                new DateTime("2017-09-10T00:00:00"),
                MarschrouteDate.create("10.09.2017")
            )
        );
    }
}
