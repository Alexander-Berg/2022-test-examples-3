package ru.yandex.direct.libs.timetarget;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class TimeTargetCalcCoefParameterizedTest {

    private static final double ZERO_COEF = 0.0;

    @Parameterized.Parameter(0)
    public LocalDateTime dateTime;
    @Parameterized.Parameter(1)
    public WeekdayType productionCalendarResponse;
    @Parameterized.Parameter(2)
    public double expectedCoef;

    private TimeTarget timeTarget;
    private ProductionCalendar productionCalendar;

    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                        {midnightFor(2017, 1, 2), WeekdayType.MONDAY, 0.1},
                        {midnightFor(2017, 1, 3), WeekdayType.TUESDAY, 0.2},
                        {midnightFor(2017, 1, 4), WeekdayType.WEDNESDAY, 0.3},
                        {midnightFor(2017, 1, 5), WeekdayType.THURSDAY, 0.4},
                        {midnightFor(2017, 1, 6), WeekdayType.FRIDAY, 0.5},
                        {midnightFor(2017, 1, 7), WeekdayType.SATURDAY, 0.6},
                        {midnightFor(2017, 1, 8), WeekdayType.SUNDAY, 0.7},
                        {midnightFor(2017, 1, 9), WeekdayType.HOLIDAY, 0.8},
                        {midnightFor(2017, 1, 10), WeekdayType.WORKING_WEEKEND, 0.2},
                }
        );
    }

    private static LocalDateTime midnightFor(int year, int month, int dayOfMonth) {
        return LocalDateTime.of(year, month, dayOfMonth, 0, 0);
    }

    @Before
    public void setUp() throws Exception {
        timeTarget = new TimeTarget()
                .withPreset(TimeTarget.Preset.WORKTIME)
                .withWeekdayCoefMap(new HashMap<WeekdayType, HoursCoef>() {{
                    put(WeekdayType.MONDAY, buildHoursWithMidnightCoef(10));
                    put(WeekdayType.TUESDAY, buildHoursWithMidnightCoef(20));
                    put(WeekdayType.WEDNESDAY, buildHoursWithMidnightCoef(30));
                    put(WeekdayType.THURSDAY, buildHoursWithMidnightCoef(40));
                    put(WeekdayType.FRIDAY, buildHoursWithMidnightCoef(50));
                    put(WeekdayType.SATURDAY, buildHoursWithMidnightCoef(60));
                    put(WeekdayType.SUNDAY, buildHoursWithMidnightCoef(70));
                    put(WeekdayType.HOLIDAY, buildHoursWithMidnightCoef(80));
                    put(WeekdayType.WORKING_WEEKEND, new HoursCoef());
                }});
        productionCalendar = (date) -> productionCalendarResponse;
    }

    /**
     * Создаёт {@link HoursCoef} с коэффициентом {@code coef} для часа {@code 0}
     */
    private HoursCoef buildHoursWithMidnightCoef(int coef) {
        return new HoursCoef().withHourCoef(0, coef);
    }

    /**
     * Проверка, что для указанного часа, возвращается заданный в {@code timeTarget} коэффициент.
     */
    @Test
    public void checkCoefCorrectWhenSet() {
        double actualCoef = timeTarget.calcTimeTargetCoef(dateTime, productionCalendar);
        assertThat(actualCoef).isEqualTo(expectedCoef);
    }

    /**
     * Проверка, что для соседнего часа, для которого коэффициент не задан, возвращается {@value ZERO_COEF}.
     */
    @Test
    public void checkCoefCorrectWhenNotSet() {
        double actualCoef = timeTarget.calcTimeTargetCoef(dateTime.plus(1, ChronoUnit.HOURS), productionCalendar);
        assertThat(actualCoef).isEqualTo(ZERO_COEF);
    }

}
