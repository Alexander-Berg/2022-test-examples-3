package ru.yandex.direct.libs.timetarget;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.libs.timetarget.TimeTarget.Preset.ALL;
import static ru.yandex.direct.libs.timetarget.TimeTarget.Preset.OTHER;

@RunWith(Parameterized.class)
public class TimeTargetEqualsShowHoursTest {

    @Parameterized.Parameter(0)
    public TimeTarget timeTarget1;
    @Parameterized.Parameter(1)
    public TimeTarget timeTarget2;
    @Parameterized.Parameter(2)
    public boolean result;

    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        var init = new TimeTarget()
                .withPreset(OTHER)
                .withWeekdayCoefMap(
                        Map.of(
                                WeekdayType.MONDAY, new HoursCoef().withHourCoef(3, 100).withHourCoef(5, 150),
                                WeekdayType.FRIDAY, new HoursCoef(),
                                WeekdayType.WEDNESDAY, new HoursCoef().withHourCoef(7, 30)
                        )
                );
        return asList(new Object[][]{
                        {init, init.copy(), true},
                        {init, init.copy().withPreset(null), true},
                        {init, init.copy().withPreset(ALL), true},
                        {init, init.copy().withHourCoef(WeekdayType.FRIDAY, 1, 50), false},
                        {init, init.copy().withHourCoef(WeekdayType.MONDAY, 1, 50), false},
                        {init, init.copy().withHourCoef(WeekdayType.MONDAY, 5, 50), true},
                }
        );
    }


    @Test
    public void checkCoefCorrectWhenSet() {
        assertThat(timeTarget1.equalsShowHours(timeTarget2)).isEqualTo(result);
    }
}
