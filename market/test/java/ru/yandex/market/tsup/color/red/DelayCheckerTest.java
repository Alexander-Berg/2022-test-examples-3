package ru.yandex.market.tsup.color.red;

import java.time.Duration;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tsup.service.data_provider.primitive.external.tpl_carrier.model.PartnerRunDriverLog;
import ru.yandex.market.tsup.service.rating.TripData;
import ru.yandex.market.tsup.service.rating.checker.red.DelayChecker;

public class DelayCheckerTest {

    private final DelayChecker delayChecker = new DelayChecker();

    @Test
    void staticMargin() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var actual = LocalDateTime.of(2021, 10, 21, 12, 14, 59);
        Assertions.assertThat(delayChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                null,
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void staticMarginExceeded() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var actual = LocalDateTime.of(2021, 10, 21, 12, 16, 59);
        Assertions.assertThat(delayChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                null,
                null,
                null
            )
        )).isTrue();
    }

    @Test
    void dynamicMargin() {
        // 5% от 36 часов пути = 108 минут
        // (ru.yandex.market.tsup.service.rating.ArrivalTimeComparingService.MARGIN_OF_TOLERANCE_PERCENTAGE)
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var started = predicted.minus(Duration.ofHours(36));
        var actual = predicted.plus(Duration.ofMinutes(107));
        Assertions.assertThat(delayChecker.isApplicable(
            new TripData(
                1,
                started,
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                null,
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void dynamicMarginExceeded() {
        // 5% от 36 часов пути = 108 минут
        // (ru.yandex.market.tsup.service.rating.ArrivalTimeComparingService.MARGIN_OF_TOLERANCE_PERCENTAGE)
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var started = predicted.minus(Duration.ofHours(36));
        var actual = predicted.plus(Duration.ofMinutes(110));
        Assertions.assertThat(delayChecker.isApplicable(
            new TripData(
                1,
                started,
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                null,
                null,
                null
            )
        )).isTrue();
    }

}
