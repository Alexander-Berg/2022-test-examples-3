package ru.yandex.direct.scheduler.hourglass.implementations.schedule;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.ScheduleDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.ScheduleCronData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.ScheduleDaemonData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.SchedulePeriodicData;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleData;
import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.FAR_FUTURE;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.PERIODIC;

@RunWith(Parameterized.class)
public class ScheduleInfoConverterAllStrategiesTest {
    private ScheduleInfoConverterImpl scheduleInfoConverter;

    @Parameterized.Parameter
    public ScheduleData scheduleData;

    @Parameterized.Parameter(1)
    public ScheduleType scheduleType;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        return List.of(
                new Object[]{new ScheduleCronData("0 0 5 * * ?"), ScheduleType.CRON},
                new Object[]{new SchedulePeriodicData(3), PERIODIC},
                new Object[]{new ScheduleDaemonData(5), ScheduleType.DAEMON},
                new Object[]{null, FAR_FUTURE}
        );
    }
    @Before
    public void before() {
        scheduleInfoConverter = new ScheduleInfoConverterImpl();
    }

    @Test
    public void onlyStrategyTest() {
        var scheduleInfo = new ScheduleInfoImpl(
                List.of(
                        new ScheduleDataWithTypeImpl(scheduleType, scheduleData)),
                List.of()
        );
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);

        var deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }
}
