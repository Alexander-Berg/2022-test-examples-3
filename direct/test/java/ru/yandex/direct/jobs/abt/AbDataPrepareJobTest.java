package ru.yandex.direct.jobs.abt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.jobs.abt.check.TableExistsChecker;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbDataPrepareJobTest {

    @Test
    void getCalculateDatesRangeTest() {
        var clock = Clock.fixed(Instant.ofEpochSecond(1586345827L), ZoneOffset.UTC); // 2020-04-08
        var abDataPrepareJob = new AbDataPrepareJob(mock(YtProvider.class), mock(TableExistsChecker.class),
                mock(PpcPropertiesSupport.class), mock(AbDataPrepareParametersSource.class), "", "", "", clock);

        var lastCalculateDate = 1586000227L; // 2020-04-04

        var calcPeriod = abDataPrepareJob.getCalculateDatesRange(lastCalculateDate);
        assertThat(calcPeriod.getLeft()).isEqualTo(Instant.ofEpochSecond(1586044800L)); // 2020-04-05
        assertThat(calcPeriod.getRight()).isEqualTo(Instant.ofEpochSecond(1586217600L)); // 2020-04-07

    }

    @Test
    void getCalculateDatesRange_NullPreviousDateTest() {
        var clock = Clock.fixed(Instant.ofEpochSecond(1586345827L), ZoneOffset.UTC); // 2020-04-08
        var abDataPrepareJob = new AbDataPrepareJob(mock(YtProvider.class), mock(TableExistsChecker.class),
                mock(PpcPropertiesSupport.class), mock(AbDataPrepareParametersSource.class), "", "", "", clock);

        Long lastCalculateDate = null;

        var calcPeriod = abDataPrepareJob.getCalculateDatesRange(lastCalculateDate);
        assertThat(calcPeriod.getLeft()).isEqualTo(Instant.ofEpochSecond(1586217600L)); // 2020-04-07
        assertThat(calcPeriod.getRight()).isEqualTo(Instant.ofEpochSecond(1586217600L)); // 2020-04-07

    }
}
