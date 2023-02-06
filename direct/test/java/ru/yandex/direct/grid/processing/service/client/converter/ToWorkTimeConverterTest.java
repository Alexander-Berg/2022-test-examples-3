package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.grid.model.GdTime;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddWorkTime;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class ToWorkTimeConverterTest {

    @Test
    public void toWorkTimeConverterTest() {
        List<GdAddWorkTime> gdAddWorkTimes = Collections.singletonList(
                new GdAddWorkTime()
                        .withDaysOfWeek(ImmutableSet.of(0, 1, 2, 3, 4))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(20).withMinute(0))
        );

        String workTime = WorkTimeConverter.toWorkTime(gdAddWorkTimes);

        String expectedEncodedString = "0#4#08#00#20#00";
        assertThat(workTime)
                .isEqualTo(expectedEncodedString);
    }

    @Test
    public void toWorkTimeConverter_GlueDailyScheduleTest() {
        List<GdAddWorkTime> gdAddWorkTimes = Collections.singletonList(
                new GdAddWorkTime()
                        .withDaysOfWeek(ImmutableSet.of(0, 1, 3, 5, 6))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(17).withMinute(15))
        );

        String workTime = WorkTimeConverter.toWorkTime(gdAddWorkTimes);

        String expectedEncodedString = "0#1#08#00#17#15;3#3#08#00#17#15;5#6#08#00#17#15";
        assertThat(workTime)
                .isEqualTo(expectedEncodedString);
    }

    @Test
    public void toWorkTimeConverter_FromListWithTwoItemsTest() {
        List<GdAddWorkTime> gdAddWorkTimes = ImmutableList.of(
                new GdAddWorkTime()
                        .withDaysOfWeek(ImmutableSet.of(0, 1, 5))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(17).withMinute(15)),
                new GdAddWorkTime()
                        .withDaysOfWeek(ImmutableSet.of(3))
                        .withStartTime(new GdTime().withHour(10).withMinute(15))
                        .withEndTime(new GdTime().withHour(18).withMinute(30))
        );

        String workTime = WorkTimeConverter.toWorkTime(gdAddWorkTimes);

        String expectedEncodedString = "0#1#08#00#17#15;3#3#10#15#18#30;5#5#08#00#17#15";
        assertThat(workTime)
                .isEqualTo(expectedEncodedString);
    }

}
