package ru.yandex.market.hrms.core.domain.employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.employee.EmployeePresenceService;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;


class EmployeePresenceServiceTest extends AbstractCoreTest {

    @Autowired
    private EmployeePresenceService employeePresenceService;

    @Test
    @DbUnitDataSet(before = "EmployeePresenceServiceTest.before.csv")
    public void testReturnUniquePresenceWithCorrectAssignment() {
        //given
        mockClock(LocalDate.of(2021, 10, 14));

        List<Long> employeeIds = List.of(18960L);
        LocalDate start = LocalDate.of(2021, 10, 14);
        LocalDateInterval interval = new LocalDateInterval(start, start.plusDays(1));
        //when
        Set<EmployeeDay> employeeDays = employeePresenceService.loadPresences(employeeIds, interval);

        //then
        Assertions.assertFalse(employeeDays.isEmpty());
        Assertions.assertFalse(employeeDays.size() > 1);

        EmployeeDay result = employeeDays.stream().findFirst().get();
        Assertions.assertEquals(37433L, result.assignment().getId());
    }

}
