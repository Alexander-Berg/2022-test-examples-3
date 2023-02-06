package ru.yandex.market.olap2.model;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class LoadTaskMetricsTest {
    @Test
    public void testSecLen() {
        assertThat(LoadTaskMetrics.secLen(
                ZonedDateTime.of(2020, 11, 1, 14, 0, 0, 0,
                        ZoneId.of("UTC")),
                ZonedDateTime.of(2020, 11, 1, 14, 1, 0, 0,
                        ZoneId.of("UTC"))
                ), Matchers.is(60L));
    }

    @Test
    public void testK() {
        assertThat(LoadTaskMetrics.k("sh{k}it", "ame_on_"), Matchers.is("shame_on_it"));
    }

    @Test
    public void testTZShift() {
        LoadTaskMetrics m = new LoadTaskMetrics();
        m.setTmStartTime(ZonedDateTime.of(2020, 11, 9, 10, 0, 0, 0,
                ZoneId.of("UTC")));
        m.setTmYtPrepOpFinishTime(ZonedDateTime.of(2020, 11, 9, 13, 0, 0, 0,
                ZoneId.of("Europe/Moscow")));


        LocalDateTime utcLocal = LocalDateTime.of(2020, 11, 9, 10, 0);
        ZonedDateTime utc = utcLocal.atZone(ZoneId.of("Etc/UTC"));
        LocalDateTime mskLocal = LocalDateTime.of(2020, 11, 9, 13, 0);
        ZonedDateTime msk = mskLocal.atZone(ZoneId.of("Europe/Moscow"));

        assertThat(ChronoUnit.SECONDS.between(utc, msk), Matchers.is(0L));
        //assertThat(ChronoUnit.SECONDS.between(utc, msk), Matchers.is(0L));
    }
}
