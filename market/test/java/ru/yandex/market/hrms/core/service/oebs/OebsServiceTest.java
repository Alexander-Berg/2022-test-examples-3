package ru.yandex.market.hrms.core.service.oebs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import one.util.streamex.StreamEx;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.oebs.model.EmployeeAssignment;
import ru.yandex.market.hrms.core.service.oebs.model.EmployeeAssignments;
import ru.yandex.market.hrms.core.service.oebs.model.HROperation;

public class OebsServiceTest extends AbstractCoreTest {
    @Autowired
    private OebsService oebsService;

    @Test
    void shouldGetSchedule() {
        List<EmployeeAssignments> employeesSchedule = oebsService.getEmployeesSchedule(List.of("timursha"),
                YearMonth.of(2021, 2));

        MatcherAssert.assertThat(StreamEx.of(employeesSchedule).filterBy(EmployeeAssignments::getLogin, "timursha").toList(), Matchers.hasSize(1));

        EmployeeAssignments employeeAssignments = employeesSchedule.get(0);
        MatcherAssert.assertThat(employeeAssignments.getLogin(), CoreMatchers.is("timursha"));

        List<EmployeeAssignment> assignments = employeeAssignments.getAssignments();
        MatcherAssert.assertThat(assignments, Matchers.hasSize(1));

        EmployeeAssignment assignment = assignments.get(0);
        MatcherAssert.assertThat(assignment.getNumber(), CoreMatchers.is("123/45-67-89"));
        MatcherAssert.assertThat(assignment.getScheduleStartDate(), CoreMatchers.is(LocalDate.of(2021, 2, 1)));
    }

    @Test
    void shouldLoadHrOperations() {
        LocalDateTime dateFrom = LocalDate.of(2021, 2, 1).atStartOfDay();
        LocalDateTime dateTo = LocalDate.of(2021, 3, 1).atStartOfDay();
        List<HROperation> hrOperations = oebsService.getEmployeesHROperations(
                List.of("timursha"), dateFrom, dateTo
        );

        MatcherAssert.assertThat(StreamEx.of(hrOperations)
                .filterBy(HROperation::login, "timursha").toList(), Matchers.hasSize(35));
    }
}
