package ru.yandex.market.logistics.tarifficator.converter;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;

@DisplayName("Unit-тест конвертации даты-времени из строковых форматов YT в Instant и обратно")
class YtDateTimeConverterTest extends AbstractUnitTest {

    private static final YtDateTimeConverter YT_DATE_TIME_CONVERTER = new YtDateTimeConverter();

    @DisplayName("Из строкового формата YT в Instant")
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("ytDatetimes")
    void fromYtDatetime(String ytDatetime, Instant expectedDatetime) {
        softly.assertThat(YT_DATE_TIME_CONVERTER.fromYtDatetime(ytDatetime))
            .isEqualTo(expectedDatetime);
    }

    private static Stream<Arguments> ytDatetimes() {
        return Stream.of(
            Arguments.of("2020-03-09 07:12:20.260921", Instant.parse("2020-03-09T04:12:20.260921Z")),
            Arguments.of("2020-03-09 07:12:20.260", Instant.parse("2020-03-09T04:12:20.260Z")),
            Arguments.of("2020-03-09T07:12:20.260921999Z", Instant.parse("2020-03-09T04:12:20.260921999Z")),
            Arguments.of("2020-03-09T07:12:20Z", Instant.parse("2020-03-09T04:12:20Z")),
            Arguments.of("2020-03-09T07:12:20", Instant.parse("2020-03-09T04:12:20Z"))
        );
    }

    @DisplayName("Из Instant в строковый формат YT")
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("datetimes")
    void toYtDatetime(Instant datetime, String ytDatetime) {
        softly.assertThat(YT_DATE_TIME_CONVERTER.toYtDatetime(datetime))
            .isEqualTo(ytDatetime);
    }

    private static Stream<Arguments> datetimes() {
        return Stream.of(
            Arguments.of(Instant.parse("2020-03-09T04:12:20.260921Z"), "2020-03-09T07:12:20.260921"),
            Arguments.of(Instant.parse("2020-03-09T04:12:20Z"), "2020-03-09T07:12:20")
        );
    }

}
