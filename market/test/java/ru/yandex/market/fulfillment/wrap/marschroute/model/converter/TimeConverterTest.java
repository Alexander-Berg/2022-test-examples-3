package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConverterTest extends BaseIntegrationTest {

    private static final ZoneOffset OFFSET = ZoneOffset.ofHours(3);
    private final TimeConverter converter = new TimeConverter();

    @MethodSource("data")
    @ParameterizedTest
    void test(OffsetTime initialTime, MarschrouteTime expected) {
        MarschrouteTime actual = converter.convert(initialTime);

        assertThat(actual).isEqualTo(expected);
    }

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(OffsetTime.of(LocalTime.of(10, 30), OFFSET), MarschrouteTime.create("10:30")),
            Arguments.of(OffsetTime.of(LocalTime.of(23, 59), OFFSET), MarschrouteTime.create("23:59")),
            Arguments.of(OffsetTime.of(LocalTime.of(0, 0), OFFSET), MarschrouteTime.create("00:00"))
        );
    }
}
