package ru.yandex.direct.scheduler.hourglass.implementations.schedule;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.ModifierDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTimeData;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.ScheduleDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.SchedulePeriodicData;
import ru.yandex.direct.scheduler.hourglass.schedule.ScheduleInfo;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.ModifierType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.FAR_FUTURE;
import static ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType.PERIODIC;

public class ScheduleInfoConverterImplTest {
    private ScheduleInfoConverterImpl scheduleInfoConverter;

    @Before
    public void before() {
        scheduleInfoConverter = new ScheduleInfoConverterImpl();
    }

    @Test
    public void test() {
        var scheduleInfo = new ScheduleInfoImpl(
                List.of(
                        new ScheduleDataWithTypeImpl(PERIODIC, new SchedulePeriodicData(5)),
                        new ScheduleDataWithTypeImpl(FAR_FUTURE, null)),
                List.of(
                        new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME, new RandomStartTimeData(6)),
                        new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME, new RandomStartTimeData(8)))
        );
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);
        var deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }

    @Test
    public void onlyModifiersTest() {
        ScheduleInfo scheduleInfo = new ScheduleInfoImpl(
                List.of(),
                List.of(new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME, new RandomStartTimeData(6)))
        );
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);
        ScheduleInfo deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }

    @Test
    public void onlyStrategyTest() {
        var scheduleInfo = new ScheduleInfoImpl(
                List.of(
                        new ScheduleDataWithTypeImpl(FAR_FUTURE, null)),
                List.of()
        );
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);

        var deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }

    @Test
    public void invalidStringTest() {
        var serializedSchedule = "invalid schedule info";
        assertThatThrownBy(() -> scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void emptyScheduleInfo() {
        var scheduleInfo = new ScheduleInfoImpl(List.of(), List.of());
        var serializedSchedule = scheduleInfoConverter.serializeAndEncodeSchedule(scheduleInfo);
        var deserializeSchedule = scheduleInfoConverter.getScheduleInfoFromEncodedString(serializedSchedule);
        assertThat(deserializeSchedule).isEqualToComparingFieldByFieldRecursively(scheduleInfo);
    }

    @Test
    public void invalidModifierTypeTest() {
        var scheduleInfoConverter = new ScheduleInfoConverterImpl(new IdentityCodec());
        var scheduleInfoString = "{\"strategies\":[],\"modifiers\":[{\"type\":\"INVALID_TYPE\"," +
                "\"data\":{\"randomDelta\":6}}]}";
        assertThatIllegalArgumentException().isThrownBy(() -> scheduleInfoConverter.getScheduleInfoFromEncodedString(scheduleInfoString));
    }

    @Test
    public void invalidStrategyTypeTest() {
        var scheduleInfoConverter = new ScheduleInfoConverterImpl(new IdentityCodec());
        var scheduleInfoString = "{\"strategies\":[{\"type\":\"INVALID_TYPE\",\"data\":null}],\"modifiers\":[]}";
        assertThatIllegalArgumentException().isThrownBy(() -> scheduleInfoConverter.getScheduleInfoFromEncodedString(scheduleInfoString));
    }

    @Test
    public void typeFromAnotherStrategy() {
        var scheduleInfoConverter = new ScheduleInfoConverterImpl(new IdentityCodec());
        var scheduleInfoString = "{\"strategies\":[{\"type\":\"FAR_FUTURE\",\"data\":3}],\"modifiers\":[]}";
        assertThatIllegalArgumentException().isThrownBy(() -> scheduleInfoConverter.getScheduleInfoFromEncodedString(scheduleInfoString));
    }

    private class IdentityCodec implements Codec {
        @Override
        public String decode(String encodedValue) {
            return encodedValue;
        }

        @Override
        public String encode(String value) {
            return value;
        }
    }
}
