package ru.yandex.market.stat.dicts.bazinga;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.commune.bazinga.scheduler.schedule.LastJobInfo;
import ru.yandex.commune.bazinga.scheduler.schedule.Schedule;

import static org.junit.Assert.assertTrue;

@Slf4j
public class BazingaHelperTest {

    @Test
    public void testCronHalfHourly() {
        Schedule cron = BazingaHelper.cronHalfHourly();
        LastJobInfo lj = new LastJobInfo();
        Instant now = Instant.now();
        Instant nextRun = cron.nextScheduleTime(lj, now).get();
        log.info("Schedule: {}", cron.toPrettyString());
        log.info("Now: {}", now.toString());
        log.info("Next run: {}", nextRun.toString());
        assertTrue("", nextRun.isAfter(now));
        assertTrue("", nextRun.isBefore(now.plus(Duration.standardMinutes(30)).plus(Duration.standardSeconds(10))));
    }
}
