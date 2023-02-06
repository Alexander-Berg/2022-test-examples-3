package ru.yandex.direct.scheduler.hourglass.implementations.schedule;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.ModifierDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTimeData;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.scheduler.hourglass.schedule.modifiers.ModifierType.RANDOM_START_TIME;

public class ScheduleInfoConverterAllModifiersTest {
    private ScheduleInfoConverterImpl scheduleInfoConverter;

    @Before
    public void before() {
        scheduleInfoConverter = new ScheduleInfoConverterImpl();
    }

    @Test
    public void modifiersTest() {
        var modifierType = RANDOM_START_TIME;
        var modifierData = new RandomStartTimeData(123);
        var scheduleInfo = new ScheduleInfoImpl(
                List.of(),
                List.of(new ModifierDataWithTypeImpl(modifierType, modifierData))
        );
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);

        var deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }
}
