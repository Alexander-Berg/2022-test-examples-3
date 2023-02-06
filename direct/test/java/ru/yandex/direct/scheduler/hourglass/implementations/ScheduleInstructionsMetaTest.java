package ru.yandex.direct.scheduler.hourglass.implementations;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.scheduler.Hourglass;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.ScheduleInfoConverterImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.ScheduleInfoImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.ModifierDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTimeData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.ScheduleDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.ScheduleCronData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.SchedulePeriodicData;
import ru.yandex.direct.scheduler.hourglass.schedule.ScheduleInfo;
import ru.yandex.direct.scheduler.hourglass.schedule.ScheduleInfoConverter;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.ModifierType;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType;
import ru.yandex.direct.scheduler.support.DirectJob;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.CRON;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.PERIODIC;

public class ScheduleInstructionsMetaTest {

    @Test
    public void metaString_PeriodicStrategy() {

        List<ScheduleDataWithTypeImpl> scheduleDataWithTypes = List.of(
                new ScheduleDataWithTypeImpl(PERIODIC, new SchedulePeriodicData(2, SECONDS)));

        ScheduleInfo scheduleInfo =
                new ScheduleInfoImpl(scheduleDataWithTypes, List.of());

        ScheduleInfoConverter scheduleInfoConverter = new ScheduleInfoConverterImpl();
        assertEquals("{\"strategies\":[{\"type\":\"PERIODIC\",\"data\":{\"interval\":2}}],\"modifiers\":[]}",
                scheduleInfoConverter.serializeSchedule(scheduleInfo));
    }

    @Test
    public void metaString_FarFutureStrategy() {

        List<ScheduleDataWithTypeImpl> scheduleDataWithTypes =
                List.of(new ScheduleDataWithTypeImpl(ScheduleType.FAR_FUTURE, null));

        ScheduleInfo scheduleInfo =
                new ScheduleInfoImpl(scheduleDataWithTypes, List.of());

        ScheduleInfoConverter scheduleInfoConverter = new ScheduleInfoConverterImpl();
        assertEquals("{\"strategies\":[{\"type\":\"FAR_FUTURE\",\"data\":null}],\"modifiers\":[]}",
                scheduleInfoConverter.serializeSchedule(scheduleInfo));
    }

    @Test
    public void metaString_CronStrategy() {

        List<ScheduleDataWithTypeImpl> scheduleDataWithTypes = List.of(
                new ScheduleDataWithTypeImpl(CRON, new ScheduleCronData("0 20 10,21 * * ?"))
        );

        ScheduleInfo scheduleInfo =
                new ScheduleInfoImpl(scheduleDataWithTypes, List.of());

        ScheduleInfoConverter scheduleInfoConverter = new ScheduleInfoConverterImpl();
        assertEquals("{\"strategies\":[{\"type\":\"CRON\",\"data\":{\"expression\":\"0 20 10,21 * * ?\"}}]," +
                        "\"modifiers\":[]}",
                scheduleInfoConverter.serializeSchedule(scheduleInfo));
    }

    @Test
    public void metaString_TwoStrategies() {
        ScheduleInfo scheduleInfo = new ScheduleInfoImpl(
                List.of(new ScheduleDataWithTypeImpl(CRON, new ScheduleCronData("0 20 10,21 * * ?")),
                        new ScheduleDataWithTypeImpl(PERIODIC, new SchedulePeriodicData(2, SECONDS))),
                List.of()
        );
        ScheduleInfoConverter scheduleInfoConverter = new ScheduleInfoConverterImpl();
        assertEquals("{\"strategies\":[{\"type\":\"CRON\",\"data\":{\"expression\":\"0 20 10,21 * * ?\"}}," +
                        "{\"type\":\"PERIODIC\",\"data\":{\"interval\":2}}],\"modifiers\":[]}",
                scheduleInfoConverter.serializeSchedule(scheduleInfo));
    }

    @Test
    public void metaString_CronStrategyWithStretching() {

        List<ScheduleDataWithTypeImpl> scheduleDataWithTypes = List.of(
                new ScheduleDataWithTypeImpl(CRON, new ScheduleCronData("0 20 10,21 * * ?"))
        );

        List<ModifierDataWithTypeImpl> nextRunModifiers =
                List.of(new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME,
                        new RandomStartTimeData(362)));

        ScheduleInfo scheduleInfo =
                new ScheduleInfoImpl(scheduleDataWithTypes, nextRunModifiers);
        ScheduleInfoConverter scheduleInfoConverter = new ScheduleInfoConverterImpl();
        assertEquals("{\"strategies\":[{\"type\":\"CRON\",\"data\":{\"expression\":\"0 20 10,21 * * ?\"}}]," +
                        "\"modifiers\":[{\"type\":\"RANDOM_START_TIME\",\"data\":{\"randomDelta\":362}}]}",
                scheduleInfoConverter.serializeSchedule(scheduleInfo));
    }

    @Hourglass
    private class TestDirectJob extends DirectJob {
        @Override
        public void execute() {
        }

        @Override
        public void onShutdown() {
        }
    }
}
