package ru.yandex.market.tsum.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.trace.TskvRecordBuilder;
import ru.yandex.market.tsum.analytics.model.JobLaunchMetrics;
import ru.yandex.market.tsum.analytics.model.JobMetrics;
import ru.yandex.market.tsum.analytics.model.PipelineMetrics;
import ru.yandex.market.tsum.analytics.model.pipeline.PipelineHistoryEvent;
import ru.yandex.market.tsum.analytics.model.pipeline.PipelineJob;
import ru.yandex.market.tsum.clients.calendar.Holidays;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 26.01.18
 */
public class PipelineMetricsCalculatorTest {
    Instant firstQueued = Instant.parse("2018-01-01T00:00:00Z");
    Instant firstRunning = Instant.parse("2018-01-01T00:00:01Z");
    Instant firstSuccessful = Instant.parse("2018-01-02T21:00:05Z");
    Instant secondQueued = Instant.parse("2018-01-04T10:00:00Z");
    Instant secondRunning = Instant.parse("2018-01-04T10:00:10Z");
    Instant thirdQueued = Instant.parse("2018-01-04T11:00:00Z");
    Instant thirdRunning = Instant.parse("2018-01-04T11:00:15Z");
    Instant thirdSuccessful = Instant.parse("2018-01-04T11:01:00Z");
    Instant secondSuccessful = Instant.parse("2018-01-04T11:01:30Z");
    Instant fourthWaitingForStage = Instant.parse("2018-01-05T01:00:00Z");
    Instant fourthQueued = Instant.parse("2018-01-05T01:01:00Z");
    Instant fourthRunning = Instant.parse("2018-01-05T01:01:05Z");
    Instant fourthFailed = Instant.parse("2018-01-05T22:02:00Z");
    Instant fourthQueuedAgain = Instant.parse("2018-01-07T01:02:10Z");
    Instant fourthRunningAgain = Instant.parse("2018-01-07T01:02:15Z");
    Instant fourthSuccessful = Instant.parse("2018-01-07T01:03:00Z");
    Instant fifthWaitingForSchedule = Instant.parse("2018-01-07T01:04:01Z");
    Instant fifthQueued = Instant.parse("2018-01-09T12:00:00Z");
    Instant fifthRunning = Instant.parse("2018-01-09T01:00:06Z");
    Instant fifthSuccessful = Instant.parse("2018-01-09T01:04:00Z");
    Instant sixthQueued = Instant.parse("2018-01-10T01:04:01Z");

    @Test
    public void calculatesMetrics() {
        List<PipelineHistoryEvent> history = createTestPipelineHistory();

        Holidays holidays = new Holidays();
        holidays.setHolidays(Arrays.asList(
            new Holidays.Holiday(LocalDate.of(2018, 1, 3), "weekend"),
            new Holidays.Holiday(LocalDate.of(2018, 1, 6), "weekend"),
            new Holidays.Holiday(LocalDate.of(2018, 1, 8), "weekend")
        ));

        PipelineMetrics metrics = PipelineMetricsCalculator.parseHistory(
            "test-project", "test-pipeline", "1", history, false, holidays
        );

        Assert.assertEquals("test-project", metrics.getProjectId());
        Assert.assertEquals("test-pipeline", metrics.getPipelineId());
        Assert.assertEquals("1", metrics.getReleaseId());
        Assert.assertEquals(firstQueued, metrics.getStartDate());
        Assert.assertEquals(fifthSuccessful.getEpochSecond() - firstQueued.getEpochSecond(), metrics.getTotalSeconds());

        Assert.assertEquals(5, metrics.getJobs().size());

        JobMetrics firstJob = findJob(metrics, "first");
        JobMetrics secondJob = findJob(metrics, "second");
        JobMetrics fourthJob = findJob(metrics, "fourth");
        JobMetrics fifthJob = findJob(metrics, "fifth");

        JobLaunchMetrics expected = JobLaunchMetrics.builder()
            .withLaunchId(1)
            .withStart(firstQueued)
            .withEnd(firstSuccessful)
            .withExclusiveExecutionTimeMillis(firstSuccessful.toEpochMilli() - firstQueued.toEpochMilli())
            .withExclusiveExecutionTimeNoHolidaysMillis(
                firstSuccessful.toEpochMilli() - firstQueued.toEpochMilli() - TimeUnit.SECONDS.toMillis(5)
            )
            .withExecutionTimeSeconds(firstSuccessful.getEpochSecond() - firstQueued.getEpochSecond())
            .withExecutionTimeNoHolidaysSeconds(
                firstSuccessful.getEpochSecond() - firstQueued.getEpochSecond() - 5
            )
            .withWaitingForLaunchSeconds(0)
            .withWaitingForLaunchNoHolidaysSeconds(0)
            .withWaitingForRelaunchSeconds(0)
            .withWaitingForRelaunchNoHolidaysSeconds(0)
            .withWaitingForStageSeconds(0)
            .withWaitingForStageNoHolidaysSeconds(0)
            .withWaitingForScheduleSeconds(0)
            .withWaitingForScheduleNoHolidaysSeconds(0)
            .build();

        Assert.assertEquals(expected, firstJob.getLaunchMetrics().get(0));

        long secondSuccessfulTs = secondSuccessful.toEpochMilli();
        long thirdSuccessfulTs = thirdSuccessful.toEpochMilli();
        long thirdQueuedTs = thirdQueued.toEpochMilli();
        long secondQueuedTs = secondQueued.toEpochMilli();

        long expectedExclusiveExecutionTime =
            secondSuccessfulTs - (thirdSuccessfulTs - thirdQueuedTs) - secondQueuedTs;

        expected = JobLaunchMetrics.builder()
            .withLaunchId(1)
            .withStart(secondQueued)
            .withEnd(secondSuccessful)
            .withExclusiveExecutionTimeMillis(
                expectedExclusiveExecutionTime
            )
            .withExclusiveExecutionTimeNoHolidaysMillis(
                expectedExclusiveExecutionTime
            )
            .withExecutionTimeSeconds(secondSuccessful.getEpochSecond() - secondQueued.getEpochSecond())
            .withExecutionTimeNoHolidaysSeconds(secondSuccessful.getEpochSecond() - secondQueued.getEpochSecond())
            .withWaitingForLaunchSeconds(secondQueued.getEpochSecond() - firstSuccessful.getEpochSecond())
            .withWaitingForLaunchNoHolidaysSeconds(secondQueued.getEpochSecond() - firstSuccessful.getEpochSecond() -
                (TimeUnit.HOURS.toSeconds(23) + TimeUnit.MINUTES.toSeconds(59) + TimeUnit.SECONDS.toSeconds(55))
            )
            .withWaitingForRelaunchSeconds(0)
            .withWaitingForStageSeconds(0)
            .withWaitingForScheduleSeconds(0)
            .withWaitingForScheduleNoHolidaysSeconds(0)
            .build();

        Assert.assertEquals(expected, secondJob.getLaunchMetrics().get(0));

        expected = JobLaunchMetrics.builder()
            .withLaunchId(1)
            .withStart(fourthQueued)
            .withEnd(fourthFailed)
            .withExclusiveExecutionTimeMillis(fourthFailed.toEpochMilli() - fourthQueued.toEpochMilli())
            .withExclusiveExecutionTimeNoHolidaysMillis(fourthFailed.toEpochMilli() - fourthQueued.toEpochMilli() -
                (TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(2)))
            .withExecutionTimeSeconds(fourthFailed.getEpochSecond() - fourthQueued.getEpochSecond())
            .withExecutionTimeNoHolidaysSeconds(fourthFailed.getEpochSecond() - fourthQueued.getEpochSecond() -
                (TimeUnit.HOURS.toSeconds(1) + TimeUnit.MINUTES.toSeconds(2)))
            .withWaitingForLaunchSeconds(fourthWaitingForStage.getEpochSecond() - secondSuccessful.getEpochSecond())
            .withWaitingForLaunchNoHolidaysSeconds(fourthWaitingForStage.getEpochSecond() -
                secondSuccessful.getEpochSecond())
            .withWaitingForRelaunchSeconds(0)
            .withWaitingForRelaunchNoHolidaysSeconds(0)
            .withWaitingForStageSeconds(fourthQueued.getEpochSecond() - fourthWaitingForStage.getEpochSecond())
            .withWaitingForStageNoHolidaysSeconds(fourthQueued.getEpochSecond() -
                fourthWaitingForStage.getEpochSecond())
            .withWaitingForScheduleSeconds(0)
            .withWaitingForScheduleNoHolidaysSeconds(0)
            .build();

        Assert.assertEquals(expected, fourthJob.getLaunchMetrics().get(0));

        expected = JobLaunchMetrics.builder()
            .withLaunchId(2)
            .withStart(fourthQueuedAgain)
            .withEnd(fourthSuccessful)
            .withExclusiveExecutionTimeMillis(fourthSuccessful.toEpochMilli() - fourthQueuedAgain.toEpochMilli())
            .withExclusiveExecutionTimeNoHolidaysMillis(fourthSuccessful.toEpochMilli() -
                fourthQueuedAgain.toEpochMilli())
            .withExecutionTimeSeconds(fourthSuccessful.getEpochSecond() - fourthQueuedAgain.getEpochSecond())
            .withExecutionTimeNoHolidaysSeconds(fourthSuccessful.getEpochSecond() - fourthQueuedAgain.getEpochSecond())
            .withWaitingForLaunchSeconds(0)
            .withWaitingForLaunchNoHolidaysSeconds(0)
            .withWaitingForRelaunchSeconds(fourthQueuedAgain.getEpochSecond() - fourthFailed.getEpochSecond())
            .withWaitingForRelaunchNoHolidaysSeconds(fourthQueuedAgain.getEpochSecond() -
                fourthFailed.getEpochSecond() - (TimeUnit.HOURS.toSeconds(22) + TimeUnit.MINUTES.toSeconds(58)))
            .withWaitingForStageSeconds(0)
            .withWaitingForStageNoHolidaysSeconds(0)
            .withWaitingForScheduleSeconds(0)
            .withWaitingForScheduleNoHolidaysSeconds(0)
            .build();

        Assert.assertEquals(expected, fourthJob.getLaunchMetrics().get(1));

        expected = JobLaunchMetrics.builder()
            .withLaunchId(1)
            .withStart(fifthQueued)
            .withEnd(fifthSuccessful)
            .withExclusiveExecutionTimeMillis(fifthSuccessful.toEpochMilli() - fifthQueued.toEpochMilli())
            .withExclusiveExecutionTimeNoHolidaysMillis(fifthSuccessful.toEpochMilli() - fifthQueued.toEpochMilli())
            .withExecutionTimeSeconds(fifthSuccessful.getEpochSecond() - fifthQueued.getEpochSecond())
            .withExecutionTimeNoHolidaysSeconds(fifthSuccessful.getEpochSecond() - fifthQueued.getEpochSecond())
            .withWaitingForLaunchSeconds(fifthWaitingForSchedule.getEpochSecond() - fourthSuccessful.getEpochSecond())
            .withWaitingForLaunchNoHolidaysSeconds(fifthWaitingForSchedule.getEpochSecond() -
                fourthSuccessful.getEpochSecond())
            .withWaitingForRelaunchSeconds(0)
            .withWaitingForRelaunchNoHolidaysSeconds(0)
            .withWaitingForScheduleSeconds(fifthQueued.getEpochSecond() - fifthWaitingForSchedule.getEpochSecond())
            .withWaitingForScheduleNoHolidaysSeconds(fifthQueued.getEpochSecond() -
                fifthWaitingForSchedule.getEpochSecond()
                - (TimeUnit.HOURS.toSeconds(23) + TimeUnit.MINUTES.toSeconds(59) + TimeUnit.SECONDS.toSeconds(55)))
            .build();

        Assert.assertEquals(expected, fifthJob.getLaunchMetrics().get(0));
    }

    @Test
    @Ignore
    public void exportsToTskv() {
        List<PipelineHistoryEvent> history = createTestPipelineHistory();
        PipelineMetrics metrics = PipelineMetricsCalculator.parseHistory(
            "test-project", "test-pipeline", "1", history, false, null
        );

        TskvRecordBuilder tskv = new TskvRecordBuilder();
        metrics.addToTskv(tskv);
        System.out.println(tskv.build());
    }

    private JobMetrics findJob(PipelineMetrics metrics, String id) {
        return metrics.getJobs().stream()
            .filter(x -> x.getJobId().equals(id))
            .findFirst().orElseThrow(IllegalStateException::new);
    }

    private List<PipelineHistoryEvent> createTestPipelineHistory() {
        // ---first---second--\
        // -|-third --------------|-fourth------------|-fifth

        PipelineJob first = new PipelineJob("first", "first");
        PipelineJob second = new PipelineJob("second", "second");
        PipelineJob third = new PipelineJob("third", "third");
        PipelineJob fourth = new PipelineJob("fourth", "fourth");
        PipelineJob fifth = new PipelineJob("fifth", "fifth");
        PipelineJob sixth = new PipelineJob("sixth", "sixth");

        second.setUpstreams(Collections.singletonList(first));
        fourth.setUpstreams(Arrays.asList(second, third));
        fifth.setUpstreams(Collections.singletonList(fourth));

        return Arrays.asList(
            createHistoryRecord(firstQueued, first, StatusChangeType.QUEUED, 1),
            createHistoryRecord(firstRunning, first, StatusChangeType.RUNNING, 1),
            createHistoryRecord(firstSuccessful, first, StatusChangeType.SUCCESSFUL, 1),
            createHistoryRecord(secondQueued, second, StatusChangeType.QUEUED, 1),
            createHistoryRecord(secondRunning, second, StatusChangeType.RUNNING, 1),
            createHistoryRecord(thirdQueued, third, StatusChangeType.QUEUED, 1),
            createHistoryRecord(thirdRunning, third, StatusChangeType.RUNNING, 1),
            createHistoryRecord(thirdSuccessful, third, StatusChangeType.SUCCESSFUL, 1),
            createHistoryRecord(secondSuccessful, second, StatusChangeType.SUCCESSFUL, 1),
            createHistoryRecord(fourthWaitingForStage, fourth, StatusChangeType.WAITING_FOR_STAGE, 1),
            createHistoryRecord(fourthQueued, fourth, StatusChangeType.QUEUED, 1),
            createHistoryRecord(fourthRunning, fourth, StatusChangeType.RUNNING, 1),
            createHistoryRecord(fourthFailed, fourth, StatusChangeType.FAILED, 1),
            createHistoryRecord(fourthQueuedAgain, fourth, StatusChangeType.QUEUED, 2),
            createHistoryRecord(fourthRunningAgain, fourth, StatusChangeType.RUNNING, 2),
            createHistoryRecord(fourthSuccessful, fourth, StatusChangeType.SUCCESSFUL, 2),
            createHistoryRecord(fifthWaitingForSchedule, fifth, StatusChangeType.WAITING_FOR_SCHEDULE, 1),
            createHistoryRecord(fifthQueued, fifth, StatusChangeType.QUEUED, 1),
            createHistoryRecord(fifthRunning, fifth, StatusChangeType.RUNNING, 1),
            createHistoryRecord(fifthSuccessful, fifth, StatusChangeType.SUCCESSFUL, 1),
            createHistoryRecord(sixthQueued, sixth, StatusChangeType.QUEUED, 2)
        );
    }

    private PipelineHistoryEvent createHistoryRecord(Instant instant, PipelineJob start,
                                                     StatusChangeType queued, int launchNumber) {
        return new PipelineHistoryEvent(instant, queued, start, launchNumber);
    }
}
