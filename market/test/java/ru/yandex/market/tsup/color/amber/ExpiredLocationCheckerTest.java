package ru.yandex.market.tsup.color.amber;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tsup.service.data_provider.primitive.external.tpl_carrier.model.PartnerRunDriverLog;
import ru.yandex.market.tsup.service.rating.TripData;
import ru.yandex.market.tsup.service.rating.checker.amber.ExpiredLocationChecker;

public class ExpiredLocationCheckerTest {

    private final TestableClock clock = new TestableClock();

    private final ExpiredLocationChecker expiredLocationChecker = new ExpiredLocationChecker(clock);

    @Test
    void freshLocation() {
        String lastMsgTime = "2021-09-15T18:10:00.00Z";
        String currentTime = "2021-09-15T19:09:00.00Z";

        clock.setFixed(Instant.parse(currentTime), ZoneId.systemDefault());
        Assertions.assertThat(expiredLocationChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder().build(),
                systemLocalDateTime(lastMsgTime),
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void expiredLocation() {
        String lastMsgTime = "2021-09-15T18:10:00.00Z";
        String currentTime = "2021-09-15T19:11:00.00Z";

        clock.setFixed(Instant.parse(currentTime), ZoneId.systemDefault());
        Assertions.assertThat(expiredLocationChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder().build(),
                systemLocalDateTime(lastMsgTime),
                null,
                null
            )
        )).isTrue();
    }

    @Test
    void expiredLocationAndIgnored() {
        String lastMsgTime = "2021-09-15T18:10:00.00Z";
        String ignoredTime = "2021-09-15T20:10:00.00Z";
        String currentTime = "2021-09-15T19:11:00.00Z";

        clock.setFixed(Instant.parse(currentTime), ZoneId.systemDefault());
        Assertions.assertThat(expiredLocationChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder().build(),
                systemLocalDateTime(lastMsgTime),
                Instant.parse(ignoredTime).atZone(ZoneId.systemDefault()).toInstant(),
                null
            )
        )).isFalse();
    }

    @Test
    void expiredLocationAndIgnoredPassed() {
        String lastMsgTime = "2021-09-15T18:10:00.00Z";
        String ignoredTime = "2021-09-15T20:10:00.00Z";
        String currentTime = "2021-09-15T20:11:00.00Z";

        clock.setFixed(Instant.parse(currentTime), ZoneId.systemDefault());
        Assertions.assertThat(expiredLocationChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder().build(),
                systemLocalDateTime(lastMsgTime),
                Instant.parse(ignoredTime).atZone(ZoneId.systemDefault()).toInstant(),
                null
            )
        )).isTrue();
    }

    private LocalDateTime systemLocalDateTime(String time) {
        return Instant.parse(time).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
