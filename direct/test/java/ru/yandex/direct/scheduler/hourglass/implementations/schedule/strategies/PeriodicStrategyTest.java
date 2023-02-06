package ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.direct.hourglass.TaskProcessingResult;
import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.SchedulePeriodicData;

import static org.junit.Assert.assertTrue;

public class PeriodicStrategyTest {

    private final static long DEFAULT_TIMESTAMP = 1568901681L;
    private int period = 3600;

    private Clock clock = Clock.fixed(Instant.ofEpochSecond(DEFAULT_TIMESTAMP), ZoneOffset.UTC);

    private long currentPeriod = (DEFAULT_TIMESTAMP / period) * period;

    private PeriodicStrategy periodicStrategy = new PeriodicStrategy(clock);
    private SchedulePeriodicData periodicData = new SchedulePeriodicData(period, TimeUnit.SECONDS);

    private TaskProcessingResult taskProcessingResult(long startSeconds, long finishSeconds) {
        return TaskProcessingResultImpl.builder()
                .withException(null)
                .withLastStartTime(Instant.ofEpochSecond(startSeconds))
                .withLastFinishTime(Instant.ofEpochSecond(finishSeconds))
                .build();
    }

    @Test
    public void getNextDateForJobFinishedInPast() {

        TaskProcessingResult taskProcessingResult = taskProcessingResult(DEFAULT_TIMESTAMP - 3 * period - 123,
                DEFAULT_TIMESTAMP - 3 * period - 97);

        Instant nextRun = periodicStrategy.getNextDate(taskProcessingResult, periodicData);

        long nextRunSeconds = nextRun.getEpochSecond();

        assertTrue(nextRunSeconds >= currentPeriod && nextRunSeconds < currentPeriod + period);
    }

    @Test
    public void getNextDateForJobFinishedInCurrentPeriod() {

        TaskProcessingResult taskProcessingResult = taskProcessingResult(currentPeriod - period - 23,
                currentPeriod + 97);

        Instant nextRun = periodicStrategy.getNextDate(taskProcessingResult, periodicData);

        long nextRunSeconds = nextRun.getEpochSecond();

        assertTrue(nextRunSeconds >= currentPeriod && nextRunSeconds < currentPeriod + period);
    }

    @Test
    public void getNextDateForJobStartedInCurrentPeriod() {

        TaskProcessingResult taskProcessingResult = taskProcessingResult(currentPeriod,
                currentPeriod + 25);

        Instant nextRun = periodicStrategy.getNextDate(taskProcessingResult, periodicData);

        long nextRunSeconds = nextRun.getEpochSecond();

        assertTrue(nextRunSeconds >= currentPeriod + period && nextRunSeconds < currentPeriod + 2 * period);
    }

    @Test
    public void differentJobsHavedifferentRunTimeInSamePeriod() {

        TaskProcessingResult taskProcessingResult = taskProcessingResult(currentPeriod,
                currentPeriod + 25);

        Instant nextRun = periodicStrategy.getNextDate(taskProcessingResult, periodicData);

        long nextRunSeconds = nextRun.getEpochSecond();

        assertTrue(nextRunSeconds >= currentPeriod + period && nextRunSeconds < currentPeriod + 2 * period);
    }
}
