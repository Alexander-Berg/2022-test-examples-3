package ru.yandex.market.mbo.monitoring.executor;

import com.google.common.collect.Streams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.12.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class TmsExecutorMonitoringTest {

    private static final String EXECUTOR_NAME = "processGoodsExecutor";
    private static final String OK = "OK";

    @Mock
    private QuartzLogRepository quartzLogRepository;
    private TmsExecutorMonitoring monitoring;
    private LocalDateTime now;

    @Before
    public void before() {
        monitoring = new TmsExecutorMonitoring();
        monitoring.setQuartzLogRepository(quartzLogRepository);
        monitoring.setExecutorName(EXECUTOR_NAME);
        monitoring.setCheckName("process-goods-executor");

        now = LocalDateTime.of(2015, Month.JULY, 3, 4, 13);
    }

    @Test
    public void failsOfLastRunsOk() {
        List<QuartzLogEntry> entries = Streams.concat(
            entries(OK, 1),
            entries("failMessage", 2),
            entries(OK, 1))
            .collect(Collectors.toList());

        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt())).thenReturn(entries);
        monitoring.setFailedOfLastRunsToWarning(3, 10);

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo(MonitoringStatus.OK.getGolemName());
    }

    @Test
    public void failsOfLastRunsFailsWarning() {
        List<QuartzLogEntry> entries = Streams.concat(
            entries(OK, 1),
            entries("failMessage", 3),
            entries(OK, 1))
            .collect(Collectors.toList());

        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt())).thenReturn(entries);
        monitoring.setFailedOfLastRunsToWarning(3, 10);

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage())
            .isEqualTo("processGoodsExecutor 3 of last 10 executions are not ok. Last: OK");
    }

    @Test
    public void moreThanDayAgoOk() {
        QuartzLogEntry entry = new QuartzLogEntry();
        entry.setJobFinishedTime(LocalDateTime.now().minus(Duration.ofMinutes(40)));
        when(quartzLogRepository.getLastSuccessful(EXECUTOR_NAME)).thenReturn(entry);
        monitoring.setLastRunAgo(Duration.ofDays(1));

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo(MonitoringStatus.OK.getGolemName());
    }

    @Test
    public void moreThanDayAgoNoOne() {
        when(quartzLogRepository.getLastSuccessful(EXECUTOR_NAME)).thenReturn(null);
        monitoring.setLastRunAgo(Duration.ofDays(1));

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        assertThat(result.getMessage()).isEqualTo("processGoodsExecutor has never been successfully completed");
    }

    @Test
    public void moreThanDayAgoButLastWasTwo() {
        QuartzLogEntry entry = new QuartzLogEntry();
        LocalDateTime jobFinishedTime = LocalDateTime.now().minus(Duration.ofDays(2));
        entry.setJobFinishedTime(jobFinishedTime);
        when(quartzLogRepository.getLastSuccessful(EXECUTOR_NAME)).thenReturn(entry);
        monitoring.setLastRunAgo(Duration.ofDays(1));

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        assertThat(result.getMessage())
            .matches("processGoodsExecutor last successful execution was .* deadline .*");
    }

    @Test
    public void sequentialExecutorOk() {
        List<QuartzLogEntry> entries = Streams.concat(entries(OK, 1), entries("failMessage", 3), entries(OK, 1))
            .collect(Collectors.toList());

        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt())).thenReturn(entries);
        monitoring.setSequentialFailsToCritical(5);

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo(MonitoringStatus.OK.getGolemName());
    }

    @Test
    public void sequentialExecutorWarning() {
        List<QuartzLogEntry> entries = Streams.concat(
            entries("failMessage", 1),
            entries("FAIL", 3))
            .collect(Collectors.toList());

        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt())).thenReturn(entries);
        monitoring.setSequentialFailsToWarning(2);
        monitoring.setSequentialFailsToCritical(5);

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage())
            .isEqualTo("processGoodsExecutor at least 4 last executions are not ok. Last: failMessage");
    }

    @Test
    public void sequentialExecutorCritical() {
        List<QuartzLogEntry> entries = Streams.concat(
            entries("failMessage", 1),
            entries("FAIL", 4))
            .collect(Collectors.toList());

        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt())).thenReturn(entries);
        monitoring.setSequentialFailsToCritical(5);

        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        assertThat(result.getMessage())
            .isEqualTo("processGoodsExecutor at least 5 last executions are not ok. Last: failMessage");
    }

    @Test
    public void checkSortingFromLatestIsOk() {
        monitoring.setSequentialFailsToCritical(2);
        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt()))
            .thenReturn(
                Arrays.asList(
                    okEntry(now),
                    okEntry(now.minusMinutes(2)),
                    okEntry(now.minusMinutes(2)),
                    okEntry(now.minusMinutes(7))
                )
            );
        monitoring.check();
    }

    @Test
    public void checkSortingHaveNullIsError() {
        monitoring.setSequentialFailsToCritical(2);
        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt()))
            .thenReturn(
                Arrays.asList(
                    failEntry(now),
                    okEntry(null),
                    okEntry(now.minusMinutes(4)),
                    okEntry(now.minusMinutes(7))
                )
            );

        assertThatThrownBy(monitoring::check)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("execution doesn't have trigger fire time. Probably, quartz log is broken");
    }

    @Test
    public void checkSortingFail() {
        monitoring.setSequentialFailsToCritical(4);
        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt()))
            .thenReturn(
                Arrays.asList(
                    failEntry(now),
                    failEntry(now.minusMinutes(5)),
                    failEntry(now.minusMinutes(4)),
                    failEntry(now.minusMinutes(7))
                )
            );

        assertThatThrownBy(monitoring::check).isInstanceOf(RuntimeException.class)
            .hasMessage("executions aren't sorted by trigger fire time." +
                " 2015-07-03T04:09 should be less or equal to 2015-07-03T04:08");
    }

    @Test
    public void skipOnProcessing() {
        monitoring.setSequentialFailsToCritical(2);
        when(quartzLogRepository.getLastNotRunningExecutions(eq(EXECUTOR_NAME), anyInt()))
            .thenReturn(entries(null, 3).collect(Collectors.toList()));


        ComplexMonitoring.Result result = monitoring.check();
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    private static QuartzLogEntry okEntry(LocalDateTime triggerFireTime) {
        return getQuartzLogEntry(triggerFireTime, OK);
    }

    private static QuartzLogEntry failEntry(LocalDateTime triggerFireTime) {
        return getQuartzLogEntry(triggerFireTime, "failed");
    }

    private static QuartzLogEntry getQuartzLogEntry(LocalDateTime triggerFireTime, String status) {
        QuartzLogEntry entry = new QuartzLogEntry();
        entry.setJobStatus(status);
        entry.setTriggerFireTime(triggerFireTime);
        entry.setJobFinishedTime(triggerFireTime);
        return entry;
    }

    private Stream<QuartzLogEntry> entries(String status, int count) {
        return Stream.generate(() -> {
            QuartzLogEntry entry = new QuartzLogEntry();
            entry.setJobStatus(status);
            entry.setTriggerFireTime(now);
            return entry;
        }).limit(count);
    }
}
