package ru.yandex.direct.libs.timetarget;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimeTargetCalcCoefTest {

    private static final LocalDateTime MONDAY_JAN_FIRST_MIDNIGHT = LocalDateTime.of(2017, 1, 2, 0, 0);
    private static final LocalDate MONDAY_JAN_FIRST = MONDAY_JAN_FIRST_MIDNIGHT.toLocalDate();

    private ProductionCalendar productionCalendar;

    @Before
    public void setUp() throws Exception {
        productionCalendar = mock(ProductionCalendar.class);
        when(productionCalendar.getWeekdayType(any())).thenReturn(WeekdayType.MONDAY);
    }

    @Test
    public void calcTimeTargetCoef_returnZero_whenEmpty() {
        TimeTarget emptyTimeTarget = new TimeTarget();

        double actual = emptyTimeTarget.calcTimeTargetCoef(MONDAY_JAN_FIRST_MIDNIGHT, productionCalendar);

        assertThat(actual).isEqualTo(0.0);
    }

    @Test
    public void calcTimeTargetCoef_callsProductionCalendar() {
        TimeTarget emptyTimeTarget = new TimeTarget();

        emptyTimeTarget.calcTimeTargetCoef(MONDAY_JAN_FIRST_MIDNIGHT, productionCalendar);

        verify(productionCalendar).getWeekdayType(MONDAY_JAN_FIRST);
    }

    @Test
    public void calcTimeTargetCoef_callsGetHoursCoef() {
        TimeTarget emptyTimeTarget = spy(new TimeTarget());

        emptyTimeTarget.calcTimeTargetCoef(MONDAY_JAN_FIRST_MIDNIGHT, productionCalendar);

        verify(emptyTimeTarget).getHoursCoef(MONDAY_JAN_FIRST, productionCalendar);
    }

    /**
     * Проверка, что расчёт коэффициента базируется на том {@link WeekdayType},
     * который возвращается из {@link ProductionCalendar}
     */
    @Test
    public void calcTimeTargetCoef_coefDependsOnWeekendTypeFromCalendar() {
        TimeTarget emptyTimeTarget = new TimeTarget()
                .withWeekdayCoefMap(ImmutableMap.of(
                        WeekdayType.FRIDAY, new HoursCoef().withHourCoef(0, 100)
                ));

        LocalDateTime mondayJanFirst = LocalDateTime.of(2017, 1, 2, 0, 0);
        ProductionCalendar productionCalendarReturnsMonday = (date) -> WeekdayType.FRIDAY;
        double actual = emptyTimeTarget.calcTimeTargetCoef(mondayJanFirst, productionCalendarReturnsMonday);

        assertThat(actual).isEqualTo(1.0);
    }

    @Test
    public void getHoursCoefForHoliday() {
        HoursCoef hoursCoefForHoliday = new HoursCoef().withHourCoef(0, 113);
        TimeTarget timeTarget = new TimeTarget()
                .withWeekdayCoefMap(ImmutableMap.of(WeekdayType.HOLIDAY, hoursCoefForHoliday));
        ProductionCalendar productionCalendarReturnsHoliday = (date) -> WeekdayType.HOLIDAY;

        HoursCoef hoursCoef = timeTarget.getHoursCoef(MONDAY_JAN_FIRST, productionCalendarReturnsHoliday);
        assertThat(hoursCoef).isEqualTo(hoursCoefForHoliday);
    }

    @Test
    public void getHoursCoefForHoliday_whenHolidayHoursCoefIsEmpty() {
        HoursCoef hoursCoefForMonday = new HoursCoef().withHourCoef(0, 114);
        TimeTarget timeTarget = new TimeTarget()
                .withWeekdayCoefMap(ImmutableMap.of(WeekdayType.MONDAY, hoursCoefForMonday));
        ProductionCalendar productionCalendarReturnsHoliday = (date) -> WeekdayType.HOLIDAY;

        HoursCoef hoursCoef = timeTarget.getHoursCoef(MONDAY_JAN_FIRST, productionCalendarReturnsHoliday);
        assertThat(hoursCoef).isEqualTo(hoursCoefForMonday);
    }

}
