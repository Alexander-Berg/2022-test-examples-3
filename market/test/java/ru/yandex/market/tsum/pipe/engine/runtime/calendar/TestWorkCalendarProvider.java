package ru.yandex.market.tsum.pipe.engine.runtime.calendar;

import ru.yandex.market.tsum.pipe.engine.definition.common.TypeOfSchedulerConstraint;

import java.time.LocalDate;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 25.03.2019
 */
public class TestWorkCalendarProvider implements WorkCalendarProvider {

    @Override
    public TypeOfSchedulerConstraint getTypeOfDay(LocalDate day) {
        switch (day.getDayOfWeek()) {
            case FRIDAY:
                return TypeOfSchedulerConstraint.PRE_HOLIDAY;
            case SATURDAY:
            case SUNDAY:
                return TypeOfSchedulerConstraint.HOLIDAY;
            default:
                return TypeOfSchedulerConstraint.WORK;
        }
    }
}
