package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.grid.model.GdTime;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdWorkTime;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class ToGdWorkTimesConverterTest {

    @Test
    public void toGdWorkTimesConverterTest() {
        String workTime = "0#4#08#00#20#00";
        List<GdWorkTime> gdWorkTimes = WorkTimeConverter.toGdWorkTimes(workTime);

        List<GdWorkTime> expectedGdAddWorkTimes = Collections.singletonList(
                new GdWorkTime()
                        .withDaysOfWeek(List.of(0, 1, 2, 3, 4))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(20).withMinute(0))
        );

        assertThat(gdWorkTimes)
                .is(matchedBy(beanDiffer(expectedGdAddWorkTimes)));
    }

    @Test
    public void toGdWorkTimesGluedConverterTest() {
        String workTime = "0#1#08#00#17#15;3#3#08#00#17#15;5#6#08#00#17#15";
        List<GdWorkTime> gdWorkTimes = WorkTimeConverter.toGdWorkTimes(workTime);

        List<GdWorkTime> expectedGdAddWorkTimes = Collections.singletonList(
                new GdWorkTime()
                        .withDaysOfWeek(List.of(0, 1, 3, 5, 6))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(17).withMinute(15))
        );

        assertThat(gdWorkTimes)
                .is(matchedBy(beanDiffer(expectedGdAddWorkTimes)));
    }

    @Test
    public void toGdWorkTimesConverter_WhenListWithManyItemsTest() {
        String workTime = "0#1#08#00#17#15;3#3#10#15#18#30;4#4#08#00#17#30;5#5#08#00#17#15";
        List<GdWorkTime> gdWorkTimes = WorkTimeConverter.toGdWorkTimes(workTime);

        List<GdWorkTime> expectedGdAddWorkTimes = List.of(
                new GdWorkTime()
                        .withDaysOfWeek(List.of(0, 1, 5))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(17).withMinute(15)),
                new GdWorkTime()
                        .withDaysOfWeek(List.of(3))
                        .withStartTime(new GdTime().withHour(10).withMinute(15))
                        .withEndTime(new GdTime().withHour(18).withMinute(30)),
                new GdWorkTime()
                        .withDaysOfWeek(List.of(4))
                        .withStartTime(new GdTime().withHour(8).withMinute(0))
                        .withEndTime(new GdTime().withHour(17).withMinute(30))
        );

        assertThat(gdWorkTimes)
                .is(matchedBy(beanDiffer(expectedGdAddWorkTimes)));
    }

}
