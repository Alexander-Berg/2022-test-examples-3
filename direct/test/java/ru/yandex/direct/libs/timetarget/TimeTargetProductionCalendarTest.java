package ru.yandex.direct.libs.timetarget;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.libs.timetarget.WeekdayType.FRIDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.HOLIDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.MONDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.SATURDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.TUESDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.WEDNESDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.WORKING_WEEKEND;

@RunWith(Parameterized.class)
public class TimeTargetProductionCalendarTest {

    private static final double ZERO_COEF = 0.0;

    public LocalDateTime dateTime;

    @Parameterized.Parameter()
    public TimeTarget timeTarget;
    @Parameterized.Parameter(1)
    public WeekdayType productionCalendarResponse;
    @Parameterized.Parameter(2)
    public double expectedCoef;
    @Parameterized.Parameter(3)
    public String description;

    private ProductionCalendar productionCalendar;

    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                        {setDefaultTimeTargetForDays(MONDAY), MONDAY, 0.1,
                                "1.1. обычный день / есть в расписании показов"},
                        {setDefaultTimeTargetForDays(MONDAY, WEDNESDAY), TUESDAY, ZERO_COEF,
                                "1.2. обычный день / отсутствует в расписании показов"},
                        {setDefaultTimeTargetForDays(MONDAY), HOLIDAY, 0.1,
                                "2.1. праздничный день / есть в расписании, но нет расписания для праздников"},
                        {setDefaultTimeTargetForDays(MONDAY, HOLIDAY), HOLIDAY, 0.8,
                                "2.2. праздничный день /есть расписание для текущего дня и для праздников"},
                        {setDefaultTimeTargetForDays(MONDAY, HOLIDAY), HOLIDAY, 0.8,
                                "2.3. праздничный день /есть расписание для текущего дня и для праздников"},
                        {setDefaultTimeTargetForDays(MONDAY, FRIDAY), WeekdayType.WORKING_WEEKEND, ZERO_COEF,
                                "3.1. рабочий выходной / нет в расписании, учет рабочих выходных выключен"},
                        {setDefaultTimeTargetForDays(MONDAY, FRIDAY, WORKING_WEEKEND), WeekdayType.WORKING_WEEKEND, 0.1,
                                "3.2. рабочий выходной / нет в расписании, учет рабочих выходных включен (ожидаем коэффициент для понедельника"},
                        {setDefaultTimeTargetForDays(FRIDAY, WORKING_WEEKEND), WeekdayType.WORKING_WEEKEND, ZERO_COEF,
                                "3.3. рабочий выходной / нет в расписании, учет рабочих выходных включен, но нет расписания для понедельника (ожидаем 0)"},
                        {setDefaultTimeTargetForDays(SATURDAY), WeekdayType.WORKING_WEEKEND, 0.6,
                                "3.4. рабочий выходной / есть в расписании, учет рабочих выходных выключен"},
                        {setDefaultTimeTargetForDays(MONDAY, SATURDAY, WORKING_WEEKEND), WeekdayType.SATURDAY, 0.6,
                                "3.5. рабочий выходной / есть в расписании, учет рабочих выходных включен (ожидаем коэф для субботы)"},
                }
        );
    }

    /**
     * Подгоняет тестовую дату под тестовый день производственного календаря.
     * Используем предновогоднюю неделю 2018г.
     */
    private static LocalDateTime getTestDateTimeFor(WeekdayType weekdayType) {
        int dayOfMonth = 24; //24.12.2018 - понедельник
        if (weekdayType.getInternalNum() <= WeekdayType.HOLIDAY.getInternalNum()) {
            dayOfMonth = dayOfMonth + weekdayType.ordinal();
        } else if (weekdayType.equals(WeekdayType.WORKING_WEEKEND)) {
            dayOfMonth = 29; //29.12.2018 - рабочий выходной
        }
        return LocalDateTime.of(2018, 12, dayOfMonth, 20, 0);
    }

    /**
     * Заполняет тестовое расписание показов для указанных дней
     * Для каждого WeekdayType (кроме рабочего выходного) выставляет свои коэффициенты в зависимости от порядкового номера:
     * MONDAY - 10, TUESDAY - 20, ... , HOLIDAY - 80
     */
    private static TimeTarget setDefaultTimeTargetForDays(WeekdayType... weekdays) {
        Map<WeekdayType, HoursCoef> timeTargetCoefs = new HashMap<>();
        for (WeekdayType weekdayType : weekdays) {
            if (weekdayType.equals(WORKING_WEEKEND)) {
                timeTargetCoefs.put(weekdayType, new HoursCoef());
            } else {
                timeTargetCoefs.put(weekdayType, getDefaultHoursCoefs(weekdayType.getInternalNum() * 10));
            }
        }
        return new TimeTarget()
                .withPreset(TimeTarget.Preset.OTHER)
                .withWeekdayCoefMap(timeTargetCoefs);
    }

    @Before
    public void setUp() throws Exception {
        dateTime = getTestDateTimeFor(productionCalendarResponse);

        productionCalendar = (date) -> productionCalendarResponse;
    }

    /**
     * Создаёт тестовый {@link HoursCoef} с рабочими часами с 8 до 20
     */
    private static HoursCoef getDefaultHoursCoefs(int coef) {
        HoursCoef hoursCoef = new HoursCoef();
        IntStream.range(8, 21)
                .forEach(hour -> hoursCoef.setCoef(hour, coef));
        return hoursCoef;
    }

    /**
     * Проверка, что для указанного часа, возвращается заданный в {@code timeTarget} коэффициент.
     */
    @Test
    public void checkCoefCorrectWhenSet() {
        double actualCoef = timeTarget.calcTimeTargetCoef(dateTime, productionCalendar);
        assertThat(actualCoef).isEqualTo(expectedCoef);
    }
}
