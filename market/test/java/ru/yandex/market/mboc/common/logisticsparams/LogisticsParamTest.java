package ru.yandex.market.mboc.common.logisticsparams;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class LogisticsParamTest {

    private static final int[] EMPTY_ARRAY = new int[]{};

    @Test
    public void scheduleToListTest() {
        int[] daysInt = LogisticsParam.scheduleToArray("пн,вт,ср");
        assertThat(daysInt).isEqualTo(new int[]{1, 2, 3});
    }

    @Test
    public void scheduleFromArrayTest() {
        String daysStr = LogisticsParam.scheduleFromArray(new int[]{1, 2, 3});
        assertThat(daysStr).isEqualTo("пн,вт,ср");
    }

    @Test
    public void emptyScheduleArrayTest() {
        String dayStr = LogisticsParam.scheduleFromArray(EMPTY_ARRAY);
        assertThat(dayStr).isEqualTo("");
    }

    @Test
    public void emptyScheduleStrTest() {
        int[] dayArray = LogisticsParam.scheduleToArray("");
        assertThat(dayArray).isEqualTo(EMPTY_ARRAY);
    }
}
