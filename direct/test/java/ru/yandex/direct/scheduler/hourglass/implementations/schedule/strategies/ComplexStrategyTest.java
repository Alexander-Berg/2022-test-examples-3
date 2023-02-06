package ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Test;

import ru.yandex.direct.hourglass.TaskProcessingResult;
import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.ScheduleInfoProcessor;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.ScheduleInfoImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.NextRunModifierFactoryImpl;
import ru.yandex.direct.scheduler.hourglass.schedule.NextRunCalculator;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.NextRunCalcStrategy;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.NextRunStrategiesFactory;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleData;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class ComplexStrategyTest {

    private final ScheduleInfoProcessor scheduleInfoProcessor = getSchedulerInfoProcessor();

    @Test
    public void getNextDateTest() {
        AtomicReference<Instant> nextRunHolder1 =
                new AtomicReference<>(LocalDateTime.of(2019, 1, 1, 12, 3).toInstant(ZoneOffset.UTC));
        AtomicReference<Instant> nextRunHolder2 =
                new AtomicReference<>(LocalDateTime.of(2019, 1, 1, 12, 16).toInstant(ZoneOffset.UTC));

        var scheduleData1 = new TestScheduleData(nextRunHolder1::get);
        var scheduleData2 = new TestScheduleData(nextRunHolder2::get);

        NextRunCalculator nextRunCalculator = complexNextRunCalculator((a, b) -> (a.compareTo(b) < 0 ? a : b),
                List.of(scheduleData1,
                        scheduleData2));

        var gotNextDate1 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate1, nextRunHolder1.get());

        nextRunHolder1.set(LocalDateTime.of(2019, 1, 1, 21, 3).toInstant(ZoneOffset.UTC));
        nextRunHolder2.set(LocalDateTime.of(2019, 1, 1, 21, 0).toInstant(ZoneOffset.UTC));

        var gotNextDate2 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate2, nextRunHolder2.get());

        nextRunHolder1.set(LocalDateTime.of(2019, 1, 1, 21, 10).toInstant(ZoneOffset.UTC));
        nextRunHolder2.set(LocalDateTime.of(2019, 1, 1, 21, 10).toInstant(ZoneOffset.UTC));

        var gotNextDate3 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate3, nextRunHolder2.get());
    }

    NextRunCalculator complexNextRunCalculator(BinaryOperator<Instant> nextRunReducer,
                                               List<TestScheduleData> testScheduleDataList) {
        List<ScheduleDataWithTypeImpl> scheduleDataWithTypeList = testScheduleDataList.stream()
                .map(testScheduleData -> new ScheduleDataWithTypeImpl(ScheduleType.FAR_FUTURE, testScheduleData))
                .collect(Collectors.toList());
        return scheduleInfoProcessor.getNextRunCalculator(new ScheduleInfoImpl(scheduleDataWithTypeList, List.of()),
                nextRunReducer);
    }

    //TODO overflow check test


    @Test
    public void getNextDateWithAnotherReducerTest() {
        AtomicReference<Instant> nextRunHolder1 =
                new AtomicReference<>(LocalDateTime.of(2019, 1, 1, 12, 3).toInstant(ZoneOffset.UTC));
        AtomicReference<Instant> nextRunHolder2 =
                new AtomicReference<>(LocalDateTime.of(2019, 1, 1, 12, 16).toInstant(ZoneOffset.UTC));

        var scheduleData1 = new TestScheduleData(nextRunHolder1::get);
        var scheduleData2 = new TestScheduleData(nextRunHolder2::get);

        NextRunCalculator nextRunCalculator = complexNextRunCalculator((a, b) -> a.compareTo(b) < 0 ? b : a,
                List.of(scheduleData1, scheduleData2));

        var gotNextDate1 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate1, nextRunHolder2.get());

        nextRunHolder1.set(LocalDateTime.of(2019, 1, 1, 21, 3).toInstant(ZoneOffset.UTC));
        nextRunHolder2.set(LocalDateTime.of(2019, 1, 1, 21, 0).toInstant(ZoneOffset.UTC));

        var gotNextDate2 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate2, nextRunHolder1.get());

        nextRunHolder1.set(LocalDateTime.of(2019, 1, 1, 21, 10).toInstant(ZoneOffset.UTC));
        nextRunHolder2.set(LocalDateTime.of(2019, 1, 1, 21, 10).toInstant(ZoneOffset.UTC));

        var gotNextDate3 = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        assertEquals(gotNextDate3, nextRunHolder1.get());
    }

    @Test
    public void getNextDateTest_EmptyStrategiesList() {

        var now = Instant.now();

        var nextRunCalculator = complexNextRunCalculator((a, b) -> a.compareTo(b) < 0 ? a : b, List.of());

        var gotNextDate = nextRunCalculator.calculateNextRun(TaskProcessingResultImpl.builder().build());
        var offset = new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES);
        assertThat(gotNextDate).isCloseTo(now, offset);
    }

    private ScheduleInfoProcessor getSchedulerInfoProcessor() {
        return new ScheduleInfoProcessor(nextRunStrategiesFactory(), new NextRunModifierFactoryImpl(List.of()));
    }

    public NextRunStrategiesFactory nextRunStrategiesFactory() {
        return new NextRunStrategiesFactoryImpl(List.of(new TestNextRunStrategy()));
    }

    private class TestNextRunStrategy implements NextRunCalcStrategy {

        @Override
        public Instant getNextDate(@Nonnull TaskProcessingResult taskProcessingResult, ScheduleData scheduleData) {
            return ((TestScheduleData) scheduleData).getNextRunGetter().get();
        }


        // тип не имеет значения для теста
        @Override
        public ScheduleType getType() {
            return ScheduleType.FAR_FUTURE;
        }
    }

    private class TestScheduleData implements ScheduleData {
        private Supplier<Instant> nextRunGetter;

        public TestScheduleData(Supplier<Instant> nextRunGetter) {
            this.nextRunGetter = nextRunGetter;
        }

        public Supplier<Instant> getNextRunGetter() {
            return nextRunGetter;
        }
    }
}
