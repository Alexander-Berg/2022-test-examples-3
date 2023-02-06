package ru.yandex.market.tpl.core.domain.util;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;

class PreciseIntervalCalculatorTest {

    private final PreciseIntervalCalculator preciseIntervalCalculator = new PreciseIntervalCalculator();

    private final Clock clock = Clock.fixed(
            ClockUtil.defaultDateTime().toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
            DateTimeUtil.DEFAULT_ZONE_ID);

    static class TestDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    // 2-x часовые интервалы
                    Arguments.of(
                            LocalTime.of(9, 15),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("09:00-11:00")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 15),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("09:20-11:20")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 33),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("09:30-11:30")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 23),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTimeInterval.valueOf("09:20-11:20")
                    ),
                    Arguments.of(
                            LocalTime.of(14, 23),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("13:20-15:20")
                    ),
                    Arguments.of(
                            LocalTime.of(13, 50),
                            Duration.ofHours(2),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("12:00-14:00")
                    ),
                    // часовые интервалы
                    Arguments.of(
                            LocalTime.of(9, 15),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("09:00-10:00")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 15),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("09:50-10:50")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 33),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("10:00-11:00")
                    ),
                    Arguments.of(
                            LocalTime.of(10, 23),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("14:00-18:00"),
                            LocalTimeInterval.valueOf("09:50-10:50")
                    ),
                    Arguments.of(
                            LocalTime.of(14, 23),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("13:50-14:50")
                    ),
                    Arguments.of(
                            LocalTime.of(13, 50),
                            Duration.ofHours(1),
                            LocalTimeInterval.valueOf("09:00-14:00"),
                            LocalTimeInterval.valueOf("13:00-14:00")
                    )
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestDataProvider.class)
    void calc(
            LocalTime expectedDeliveryTime,
            Duration preciseDeliveryIntervalDuration,
            LocalTimeInterval initialInterval,
            LocalTimeInterval expectedInterval
    ) {

        Interval actualPreciseInterval = preciseIntervalCalculator.calc(
                DateTimeUtil.todayAt(expectedDeliveryTime, clock),
                preciseDeliveryIntervalDuration,
                initialInterval.toInterval(LocalDate.now(clock), clock.getZone())
        );

        assertThat(actualPreciseInterval).isEqualTo(expectedInterval.toInterval(LocalDate.now(clock), clock.getZone()));
    }
}
