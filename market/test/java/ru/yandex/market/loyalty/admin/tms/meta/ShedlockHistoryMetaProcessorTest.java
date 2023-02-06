package ru.yandex.market.loyalty.admin.tms.meta;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.loyalty.admin.monitoring.AdminMonitorType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockHistory;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockHistoryRepository;
import ru.yandex.market.loyalty.core.model.tms.TmsJobStatus;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.admin.tms.meta.ScheduledCron.SHEDLOCK_HISTORY_CLEANER_CRON;
import static ru.yandex.market.loyalty.admin.utils.CronUtils.getMillisecondsIntervalFromCronWithStartTime;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 09.02.2021
 */

@ActiveProfiles({"monitor-mock-test"})
@TestFor(ShedlockHistoryMetaProcessor.class)
public class ShedlockHistoryMetaProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String WATCHER_JOB_NAME = "shedlockHistoryWatcher";

    @Autowired
    private ShedlockHistoryRepository repository;
    @Autowired
    private ShedlockHistoryMetaProcessor processor;
    @Autowired
    private PushMonitor monitor;

    @Test
    public void shouldCleanLastMonthRows() {
        var young = generateWithStartFinishTime(
                clock.instant().minusMillis(Duration.ofDays(5).toMillis()),
                clock.instant()
        );
        var oldWithFinishTime = generateWithStartFinishTime(
                clock.instant().minusMillis(Duration.ofDays(90).toMillis()),
                clock.instant().minusMillis(Duration.ofDays(89).toMillis())
        );
        var oldWithoutFinishTime = generateWithStartFinishTime(
                clock.instant().minusMillis(Duration.ofDays(90).toMillis()),
                null
        );

        repository.saveAll(List.of(young, oldWithFinishTime, oldWithoutFinishTime));

        processor.deleteOldRecords();

        var result = IteratorUtils.toList(repository.findAll().iterator());

        assertEquals(1, result.size());
        assertTrue(result.contains(young));
        assertFalse(result.contains(oldWithFinishTime));
        assertFalse(result.contains(oldWithoutFinishTime));
    }

    @Test
    public void shouldDeleteAllRowsViaBatch() {
        repository.saveAll(generateListRows(10));

        List<ShedlockHistory> find = IteratorUtils.toList(repository.findAll().iterator());
        var sublist = find.subList(0, 9).stream()
                .map(ShedlockHistory::getId)
                .collect(Collectors.toSet());

        assertEquals(10, find.size());

        repository.deleteAllWithIds(sublist);
        find = IteratorUtils.toList(repository.findAll().iterator());

        assertEquals(1, find.size());
    }

    @Test
    public void shouldFireMonitoringForNotStartedJobs() {
        var oldRun = generateWithStartFinishTime(
                WATCHER_JOB_NAME,
                TmsJobStatus.DONE,
                clock.instant().minusMillis(Duration.ofDays(25).toMillis()),
                clock.instant().minusMillis(Duration.ofDays(24).toMillis())
        );

        repository.save(oldRun);

        processor.watchForIdleJobs();

        verify(monitor, times(1))
                .addTemporaryCritical(
                        eq(AdminMonitorType.SHEDLOCK_HISTORY_WATCHER_NO_RUNS),
                        contains(WATCHER_JOB_NAME),
                        eq(10L),
                        eq(TimeUnit.MINUTES)
                );
    }

    @Test
    public void shouldFireMonitoringForExceededRuntimeJobs() {
        var exceeded = generateWithStartFinishTime(
                WATCHER_JOB_NAME,
                TmsJobStatus.IN_PROGRESS,
                clock.instant().minusMillis(getMillisecondsIntervalFromCronWithStartTime(
                        clock.instant(),
                        SHEDLOCK_HISTORY_CLEANER_CRON).get() * 2
                ),
                null
        );
        var forDurationPercentile = List.of(
                generateWithStartFinishTime(
                        WATCHER_JOB_NAME,
                        TmsJobStatus.DONE,
                        clock.instant().minusMillis(Duration.ofMinutes(30).toMillis()),
                        clock.instant().minusMillis(Duration.ofMinutes(29).toMillis())
                ),
                generateWithStartFinishTime(
                        WATCHER_JOB_NAME,
                        TmsJobStatus.DONE,
                        clock.instant().minusMillis(Duration.ofMinutes(28).toMillis()),
                        clock.instant().minusMillis(Duration.ofMinutes(27).toMillis())
                ),
                generateWithStartFinishTime(
                        WATCHER_JOB_NAME,
                        TmsJobStatus.DONE,
                        clock.instant().minusMillis(Duration.ofMinutes(26).toMillis()),
                        clock.instant().minusMillis(Duration.ofMinutes(25).toMillis())
                )
        );

        repository.saveAll(forDurationPercentile);
        repository.save(exceeded);

        processor.watchForIdleJobs();

        verify(monitor, times(1))
                .addTemporaryCritical(
                        eq(AdminMonitorType.SHEDLOCK_HISTORY_WATCHER_LONG_DURATION),
                        anyString(),
                        eq(10L),
                        eq(TimeUnit.MINUTES)
                );
    }

    ShedlockHistory generateWithStartFinishTime(Instant start, Instant finish) {
        return generateWithStartFinishTime(
                "test-name",
                finish == null ? TmsJobStatus.IN_PROGRESS : TmsJobStatus.DONE,
                start,
                finish
        );
    }

    ShedlockHistory generateWithStartFinishTime(String name, TmsJobStatus status, Instant start, Instant finish) {
        return ShedlockHistory.ShedlockHistoryBuilder.builder()
                .withStartTime(start)
                .withFinishTime(finish)
                .withName(name)
                .withStatus(status.getCode())
                .withLockedBy("host")
                .withTraceId("traceId")
                .build();
    }

    List<ShedlockHistory> generateListRows(int count) {
        List<ShedlockHistory> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(ShedlockHistory.ShedlockHistoryBuilder.builder()
                    .withName("test" + i)
                    .build());
        }
        return result;
    }

}
