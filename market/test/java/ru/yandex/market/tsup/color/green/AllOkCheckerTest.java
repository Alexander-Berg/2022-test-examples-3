package ru.yandex.market.tsup.color.green;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tsup.service.data_provider.primitive.external.tpl_carrier.model.PartnerRunDriverLog;
import ru.yandex.market.tsup.service.rating.TripData;
import ru.yandex.market.tsup.service.rating.checker.green.AllOkChecker;

public class AllOkCheckerTest {

    private final AllOkChecker allOkChecker = new AllOkChecker();

    @Test
    void allOk() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var actual = LocalDateTime.of(2021, 10, 21, 12, 14, 59);
        Assertions.assertThat(allOkChecker.isApplicable(
            new TripData(
                1,
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                null,
                null
            )
        )).isTrue();
    }

    @Test
    void noPredicted() {
        var actual = LocalDateTime.of(2021, 10, 21, 12, 14, 59);
        Assertions.assertThat(allOkChecker.isApplicable(
            new TripData(
                1,
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                PartnerRunDriverLog.builder()
                    .estimatedTimeOfArrival(actual)
                    .build(),
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void noActual() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        Assertions.assertThat(allOkChecker.isApplicable(
            new TripData(
                1,
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .build(),
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void noStartTime() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var actual = LocalDateTime.of(2021, 10, 21, 12, 14, 59);
        Assertions.assertThat(allOkChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .plannedTimeOfArrival(predicted)
                    .estimatedTimeOfArrival(actual)
                    .build(),
                LocalDateTime.of(2021, 10, 21, 12, 0, 0),
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void noLocationUpdate() {
        var predicted = LocalDateTime.of(2021, 10, 21, 12, 0, 0);
        var actual = LocalDateTime.of(2021, 10, 21, 12, 14, 59);
        Assertions.assertThat(allOkChecker.isApplicable(
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
}
