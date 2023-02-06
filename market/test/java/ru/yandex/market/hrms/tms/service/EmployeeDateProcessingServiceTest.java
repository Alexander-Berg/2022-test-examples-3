package ru.yandex.market.hrms.tms.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.service.employee.EmployeeDateProcessingService;
import ru.yandex.market.hrms.core.domain.employee.EmployeeDay;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@Slf4j
@DbUnitDataSet(before = "EmployeeDateProcessingServiceTest.csv")
public class EmployeeDateProcessingServiceTest extends AbstractTmsTest {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private EmployeeDateProcessingService employeeDateProcessingService;

    private record EmployeeDateProcessingFields(
            LocalDate date,
            Long employeeId,
            Long employeeAssignmentId
    ) {
    }

    @Test
    public void filteredByAssignment() {
        EmployeeEntity employee = employeeRepo.findByIdOrThrow(16144L);
        LocalDate startDate = LocalDate.of(2021, 7, 10);
        LocalDate endDate = LocalDate.of(2021, 7, 14);

        Set<EmployeeDay> employeeDays = employeeDateProcessingService
                .loadUnprocessed(List.of(employee), new LocalDateInterval(startDate, endDate));

        List<EmployeeDateProcessingFields> actual = StreamEx.of(employeeDays)
                .map(x -> new EmployeeDateProcessingFields(x.day(), x.employee().getId(), x.assignment().getId()))
                .sorted(Comparator.comparing(EmployeeDateProcessingFields::date)
                        .thenComparing(EmployeeDateProcessingFields::employeeAssignmentId))
                .toList();
        List<EmployeeDateProcessingFields> expected = List.of(
                new EmployeeDateProcessingFields(startDate.plusDays(1), 16144L, 18656L),
                new EmployeeDateProcessingFields(startDate.plusDays(1), 16144L, 18657L),
                new EmployeeDateProcessingFields(startDate.plusDays(2), 16144L, 18656L),
                new EmployeeDateProcessingFields(startDate.plusDays(3), 16144L, 18657L)
        );
        Assertions.assertIterableEquals(expected, actual);
    }

    @Test
    public void emptyWhenNoAssignments() {
        EmployeeEntity employee = employeeRepo.findByIdOrThrow(16145L);
        LocalDate startDate = LocalDate.of(2021, 7, 11);
        LocalDate endDate = LocalDate.of(2021, 7, 14);

        Set<EmployeeDay> employeeDays = employeeDateProcessingService
                .loadUnprocessed(List.of(employee), new LocalDateInterval(startDate, endDate));

        Assertions.assertIterableEquals(Set.of(), employeeDays);
    }
}
