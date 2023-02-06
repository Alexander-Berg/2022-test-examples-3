package ru.yandex.direct.scheduler.hourglass.implementations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.direct.hourglass.TaskProcessingResult;
import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.scheduler.hourglass.HourglassJob;
import ru.yandex.direct.scheduler.hourglass.ParamDescription;
import ru.yandex.direct.scheduler.hourglass.TaskDescription;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.ScheduleInfoImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.ModifierDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.NextRunModifierFactoryImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomDeltaCalculator;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTime;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTimeData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.NextRunStrategiesFactoryImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.PeriodicStrategy;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.ScheduleDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.SchedulePeriodicData;
import ru.yandex.direct.scheduler.hourglass.schedule.ScheduleInfo;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.ModifierType;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.NextRunModifier;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.NextRunCalcStrategy;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.NextRunStrategiesFactory;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StartTimeStretchingTest {

    private final static long DEFAULT_TIMESTAMP = 1568901681L;
    private int period = 3600;

    private Clock clock = Clock.fixed(Instant.ofEpochSecond(DEFAULT_TIMESTAMP), ZoneOffset.UTC);
    private ScheduleInfoProcessor scheduleInfoProcessor = getScheduleInfoProcessor(clock);
    private RandomDeltaCalculator randomDeltaCalculator = new RandomDeltaCalculator();

    private TaskProcessingResult taskProcessingResult(long startSeconds, long finishSeconds) {
        return TaskProcessingResultImpl
                .builder()
                .withLastFinishTime(Instant.ofEpochSecond(finishSeconds))
                .withLastStartTime(Instant.ofEpochSecond(startSeconds))
                .build();
    }

    private ScheduleInfo getScheduleInfo(TaskDescription taskDescription, ParamDescription paramDescription,
                                         int period) {

        var randomDelta = randomDeltaCalculator.calculateDelta(period, taskDescription, paramDescription);


        return new ScheduleInfoImpl(
                List.of(new ScheduleDataWithTypeImpl(ScheduleType.PERIODIC, new SchedulePeriodicData(period,
                        TimeUnit.SECONDS))),
                List.of(new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME,
                        new RandomStartTimeData(randomDelta)))
        );
    }

    @Test
    public void jobsHaveDifferentStartTime() {
        var taskDescription1 = TaskDescriptionImpl.builder().setTaskClass(TestJob.class).build();
        var param = new ParamDescriptionImpl(1, 1, TaskParametersMap.of());
        var scheduleInfo1 = getScheduleInfo(taskDescription1, param, period);
        var taskDescription2 = TaskDescriptionImpl.builder().setTaskClass(AnotherJob.class).build();
        var scheduleInfo2 = getScheduleInfo(taskDescription2, param, period);
        var taskProcessingResult = taskProcessingResult(DEFAULT_TIMESTAMP - 3 * period - 123,
                DEFAULT_TIMESTAMP - 2 * period - 97);

        long nextRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo1, taskDescription1);
        long anotherRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo2, taskDescription2);

        assertTrue(nextRunSeconds != anotherRunSeconds);
    }

    @Test
    public void jobsFromTheSameGroupHaveDifferentStartTime() {
        var taskDescription1 = TaskDescriptionImpl.builder().setTaskClass(TestJob.class).build();
        var param1 = new ParamDescriptionImpl(1, 2, TaskParametersMap.of());
        var scheduleInfo1 = getScheduleInfo(taskDescription1, param1, period);

        var taskDescription2 = TaskDescriptionImpl.builder().setTaskClass(AnotherJob.class).build();
        var param2 = new ParamDescriptionImpl(2, 2, TaskParametersMap.of());
        var scheduleInfo2 = getScheduleInfo(taskDescription2, param2, period);
        TaskProcessingResult taskProcessingResult = taskProcessingResult(DEFAULT_TIMESTAMP - 3 * period - 123,
                DEFAULT_TIMESTAMP - 2 * period - 97);
        long nextRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo1, taskDescription1);
        long anotherRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo2, taskDescription2);

        assertTrue(nextRunSeconds != anotherRunSeconds);
    }

    @Test
    public void jobsFromTheSameGroupHaveSameStartTime() {
        var taskDescription = TaskDescriptionImpl.builder().setTaskClass(TestJob.class).build();
        var param1 = new ParamDescriptionImpl(1, 2, TaskParametersMap.of());
        var scheduleInfo1 = getScheduleInfo(taskDescription, param1, 0);

        var param2 = new ParamDescriptionImpl(2, 2, TaskParametersMap.of());
        var scheduleInfo2 = getScheduleInfo(taskDescription, param2, 0);


        TaskProcessingResult taskProcessingResult = taskProcessingResult(DEFAULT_TIMESTAMP - 3 * period - 123,
                DEFAULT_TIMESTAMP - 2 * period - 97);

        long nextRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo1, taskDescription);
        long anotherRunSeconds = getNextRunSeconds(taskProcessingResult, scheduleInfo2, taskDescription);

        assertEquals(nextRunSeconds, anotherRunSeconds);
    }

    private long getNextRunSeconds(TaskProcessingResult taskProcessingResult,
                                   ScheduleInfo scheduleInfo, TaskDescription taskDescription) {

        var nextRunCalculator = scheduleInfoProcessor.getNextRunCalculator(scheduleInfo,
                taskDescription.getScheduleReducer());

        return nextRunCalculator.calculateNextRun(taskProcessingResult)
                .getEpochSecond();
    }

    private ScheduleInfoProcessor getScheduleInfoProcessor(Clock clock) {
        return new ScheduleInfoProcessor(nextRunStrategiesFactory(clock), nextRunModifiersFactory());
    }

    private NextRunStrategiesFactory nextRunStrategiesFactory(Clock clock) {
        List<NextRunCalcStrategy> strategies = List.of(
                new PeriodicStrategy(clock)
        );
        return new NextRunStrategiesFactoryImpl(strategies);
    }

    private NextRunModifierFactoryImpl nextRunModifiersFactory() {
        List<NextRunModifier> modifiers = List.of(
                new RandomStartTime()
        );
        return new NextRunModifierFactoryImpl(modifiers);
    }

    private static class TestJob implements HourglassJob {
        @Override
        public void execute(TaskParametersMap parametersMap) {
        }

        @Override
        public void onShutdown() {
        }
    }

    private static class AnotherJob implements HourglassJob {
        @Override
        public void execute(TaskParametersMap parametersMap) {
        }

        @Override
        public void onShutdown() {
        }
    }
}
