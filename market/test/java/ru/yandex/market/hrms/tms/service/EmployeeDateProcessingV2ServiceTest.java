package ru.yandex.market.hrms.tms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.EmployeeDay;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.service.employee.EmployeeDateProcessingV2Service;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

public class EmployeeDateProcessingV2ServiceTest extends AbstractTmsTest {

    @Autowired
    private EmployeeRepo employeeRepo;
    @Autowired
    private EmployeeDateProcessingV2Service employeeDateProcessingService;


    @Test
    @DbUnitDataSet(before = "EmployeeDateProcessingV2ServiceTest.csv",
            after = "EmployeeDateProcessingV2ServiceTest.csv")
    public void ignoreDeletedAssignmentAfterFireDate() {
        List<EmployeeEntity> all = employeeRepo.findAll();
        LocalDate startDate = LocalDate.of(2021, 12, 4);
        LocalDate endDate = LocalDate.of(2021, 12, 10);

        Set<EmployeeDay> employeeDays = employeeDateProcessingService
                .loadUnprocessed(all, new LocalDateInterval(startDate, endDate), 1L);

        Assertions.assertEquals(9, employeeDays.size());
    }

    @Test
    @DbUnitDataSet(before = "EmployeeDateProcessingV2ServiceTest.csv")
    public void emptyWhenNoAssignments() {
        List<EmployeeEntity> all = employeeRepo.findAll();
        LocalDate startDate = LocalDate.of(2021, 7, 11);
        LocalDate endDate = LocalDate.of(2021, 7, 14);

        Set<EmployeeDay> employeeDays = employeeDateProcessingService
                .loadUnprocessed(all, new LocalDateInterval(startDate, endDate), 1L);

        Assertions.assertIterableEquals(Set.of(), employeeDays);
    }

    @Test
    @DbUnitDataSet(before = "EmployeeDateProcessingV2Transferred.before.csv")
    public void testShouldCorrectWorkWithTransferred() {
        List<EmployeeEntity> all = employeeRepo.findAll();
        LocalDate startDate = LocalDate.of(2021, 12, 4);
        LocalDate endDate = LocalDate.of(2021, 12, 10);

        Set<EmployeeDay> employeeDays = employeeDateProcessingService
                .loadUnprocessed(all, new LocalDateInterval(startDate, endDate), 1L);
        Set<EmployeeDay> employeeDays2 = employeeDateProcessingService
                .loadUnprocessed(all, new LocalDateInterval(startDate, endDate), 2L);

        Assertions.assertEquals(6, employeeDays.size());
        Assertions.assertEquals(4, employeeDays2.size());
    }
}
