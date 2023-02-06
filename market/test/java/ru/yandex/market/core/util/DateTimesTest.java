package ru.yandex.market.core.util;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Date: 29.04.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@SuppressWarnings("unused")
class DateTimesTest {

    @Nonnull
    private static Stream<Arguments> getTimestampInstantArgs() {
        return Stream.of(
                Arguments.of("empty",
                        Timestamp.newBuilder()
                                .build(),
                        Instant.ofEpochSecond(0L)),
                Arguments.of("with nanos",
                        Timestamp.newBuilder()
                                .setSeconds(230L)
                                .setNanos(432)
                                .build(),
                        Instant.ofEpochSecond(230L, 432L)
                ),
                Arguments.of("without nanos",
                        Timestamp.newBuilder()
                                .setSeconds(230L)
                                .build(),
                        Instant.ofEpochSecond(230L)
                )
        );
    }

    @DisplayName("Преобразование даты из Instant в протобуф формат.")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getTimestampInstantArgs")
    void toTimestamp_notNull_correctMapping(String name, Timestamp timestamp, Instant instant) {
        Assertions.assertThat(DateTimes.toTimestamp(instant))
                .isEqualTo(timestamp);
    }

    @DisplayName("Преобразование даты из протобуф формата в Instant.")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getTimestampInstantArgs")
    void toInstant_notNull_correctMapping(String name, Timestamp timestamp, Instant instant) {
        Assertions.assertThat(DateTimes.toInstant(timestamp))
                .isEqualTo(instant);
    }
}
