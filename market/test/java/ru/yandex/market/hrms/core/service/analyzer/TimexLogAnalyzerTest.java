package ru.yandex.market.hrms.core.service.analyzer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.function.Function5V;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.domain.domain.repo.DomainRepo;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarService;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeAssignmentEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeAssignmentRepo;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.exception.HrmsNotFoundException;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.GroupedActionsStat;
import ru.yandex.market.hrms.core.service.analyzer.timex.TimexLogAnalyzerFactory;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataSet(before = "TimexLogAnalyzerTest.before.csv")
public class TimexLogAnalyzerTest extends AbstractCoreTest {

    private static final String STAFF_LOGIN = "gjmrd";

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private TimexLogAnalyzerFactory timexLogAnalyzerFactory;

    @Autowired
    private DomainRepo domainRepo;

    @Autowired
    private EmployeeAssignmentRepo employeeAssignmentRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Test
    public void shouldReturnCorrectStats() {
        actAndAssert((domain, interval, employee, assignemnt, analyzer) -> {
            var from = Instant.parse("2021-10-01T06:00:00Z");
            var to = Instant.parse("2021-10-01T18:00:00Z");
            GroupedActionsStat stats = analyzer.getStats(employee, from, to);
            assertEquals(Instant.parse("2021-10-01T05:35:00Z"), stats.firstActionTime());
            assertEquals(Instant.parse("2021-10-01T18:00:00Z"), stats.lastActionTime());
            assertEquals(4, stats.actionsCount());
        });
    }

    @Test
    @DbUnitDataSet(before = "TimexLogAnalyzerTest.positions.before.csv")
    public void shouldUseAllLogs() {
        actAndAssert((domain, interval, employee, assignemnt, analyzer) -> {
            var from = Instant.parse("2021-10-01T06:00:00Z");
            var to = Instant.parse("2021-10-01T18:00:00Z");
            GroupedActionsStat stats = analyzer.getStats(employee, from, to);
            assertEquals(Instant.parse("2021-10-01T05:31:00Z"), stats.firstActionTime());
            assertEquals(Instant.parse("2021-10-01T18:00:00Z"), stats.lastActionTime());
            assertEquals(6, stats.actionsCount());
        });
    }

    @Test
    public void shouldReturnCorrectLastActionTime() {
        actAndAssert((domain, interval, employee, assignemnt, analyzer) -> {
            var from = Instant.parse("2021-10-01T06:00:00Z");
            var to = Instant.parse("2021-10-01T18:00:00Z");
            Instant lastActionTime = analyzer.getLastActionTime(employee, from, to);
            assertEquals(Instant.parse("2021-10-01T18:00:00Z"), lastActionTime);
        });
    }

    @Test
    public void shouldReturnCorrectHasActions() {
        actAndAssert((domain, interval, employee, assignemnt, analyzer) -> {
            var from = Instant.parse("2021-10-01T06:00:00Z");
            var to = Instant.parse("2021-10-01T18:00:00Z");
            boolean hasActions = analyzer.hasActions(employee, from, to);
            boolean hasWorkEndScan = analyzer.hasWorkEndScan(employee, from, to);
            assertEquals(true, hasActions);
            assertEquals(true, hasWorkEndScan);
        });
    }

    @Test
    public void shouldReturnCorrectIsPerformanceLow() {
        actAndAssert((domain, interval, employee, assignemnt, analyzer) -> {
            var from = Instant.parse("2021-10-01T06:00:00Z");
            var to = Instant.parse("2021-10-01T18:00:00Z");
            boolean isPerformanceLow = analyzer.isPerformanceLow(employee, from, to, 10000);
            assertEquals(false, isPerformanceLow);
            var secondFrom = Instant.parse("2021-10-02T06:00:00Z");
            var secondTo = Instant.parse("2021-10-02T18:00:00Z");

            boolean secondIsPerformanceLow = analyzer.isPerformanceLow(employee, secondFrom, secondTo, 1);
            assertEquals( true, secondIsPerformanceLow);
        });
    }

    private void actAndAssert(Function5V<Domain, LocalDateInterval, EmployeeEntity,
                                  EmployeeAssignmentEntity, EmployeePerformanceAnalyzer> func) {

        Domain domain = domainRepo.findById(1L).orElseThrow(() -> new HrmsNotFoundException("Не найден домен"));
        LocalDate from = LocalDate.of(2021, 10, 1);
        LocalDate to = LocalDate.of(2021, 11, 1);
        LocalDateInterval interval = new LocalDateInterval(from, to);

        EmployeeAssignmentEntity assignemnt = employeeAssignmentRepo.findById(2L)
                .orElseThrow(() -> new HrmsNotFoundException("Не найлено назначение"));

        EmployeeEntity employee = employeeRepo.findById(123L)
                .orElseThrow(() -> new HrmsNotFoundException("Не найден сотрудник"));

        EmployeePerformanceAnalyzer analyzer = timexLogAnalyzerFactory.createEmployeePerformanceAnalyzer(
                domain, interval, Set.of(STAFF_LOGIN)
        );

        func.apply(domain, interval, employee, assignemnt, analyzer);
    }
}
