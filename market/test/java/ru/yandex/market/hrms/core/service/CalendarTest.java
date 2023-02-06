package ru.yandex.market.hrms.core.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarDateHelper;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarService;
import ru.yandex.market.hrms.core.domain.employee.calendar.WorkShift;
import ru.yandex.market.hrms.core.domain.employee.calendar.calendar.Calendar;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "CalendarTest.beforeEach.csv")
public class CalendarTest extends AbstractCoreTest {
    @Autowired
    private EmployeeRepo employeeRepo;
    @Autowired
    private CalendarService calendarService;
    @Autowired
    private CalendarDateHelper calendarDateHelper;

    @Test
    public void getWorkShiftsWithOffset() {
        LocalDate date = LocalDate.of(2021, 7, 18);
        LocalDateInterval interval = new LocalDateInterval(date, Period.ofDays(2));

        List<EmployeeEntity> employees = employeeRepo.findAll();
        List<Long> employeeIds = StreamEx.of(employees)
                .map(EmployeeEntity::getId)
                .toList();

        List<WorkShift> expected = List.of(
                new WorkShift(1,
                        DateTimeUtil.atDefaultZone(date.atTime(8, 15)),
                        DateTimeUtil.atDefaultZone(date.atTime(20, 15))),
                new WorkShift(1,
                        DateTimeUtil.atDefaultZone(date.plusDays(1).atTime(20, 15)),
                        DateTimeUtil.atDefaultZone(date.plusDays(2).atTime(8, 15)))
        );

        Calendar calendar = calendarService
                .getCalendar(employeeIds, interval, true);
        calendar.getRows().values()
                .forEach(calendarDates -> {
                    List<WorkShift> actual = StreamEx.of(calendarDates.values())
                            .map(calendarDateHelper::getWorkShift)
                            .flatMap(Collection::stream)
                            .sortedBy(WorkShift::startedAt)
                            .toList();
                    Assertions.assertIterableEquals(expected, actual);
                });
    }
}
