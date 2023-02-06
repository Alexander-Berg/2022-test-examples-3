package ru.yandex.market.hrms.core.service.analyzer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.employee.EmployeeService;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.GroupedActionsStat;
import ru.yandex.market.hrms.core.service.analyzer.wms.WmsLogAnalyzerFactory;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "WmsLogAnalyzerTest.before.csv")
public class WmsLogAnalyzerTest extends AbstractCoreTest {

    @Autowired
    private EmployeeService employeeRepo;

    @Autowired
    private WmsLogAnalyzerFactory wmsLogAnalyzerFactory;

    @Test
    public void shouldReturnCorrectStats() {
        var employee = employeeRepo.getEmployee(123L);
        var employees = Set.of(employee);

        var interval = new LocalDateInterval(LocalDate.of(2022, 1, 26), LocalDate.of(2022, 1, 26));

        var analyzer = wmsLogAnalyzerFactory.createOperationsAnalyzer(1L, employees, interval);

        var from = Instant.parse("2022-01-26T09:00:00Z");
        var to = Instant.parse("2022-01-26T18:00:00Z");
        var stats = analyzer.getStats(employee, from, to);

        assertEquals(Instant.parse("2022-01-26T09:00:00Z"), stats.firstActionTime());
        assertEquals(Instant.parse("2022-01-26T18:30:00Z"), stats.lastActionTime());
    }

    @Test
    public void shouldHasWorkEndScan() {
        var employee = employeeRepo.getEmployee(123L);
        var employees = Set.of(employee);

        var interval = new LocalDateInterval(LocalDate.of(2022, 1, 26), LocalDate.of(2022, 1, 26));

        var analyzer = wmsLogAnalyzerFactory.createOperationsAnalyzer(1L, employees, interval);

        var from = Instant.parse("2022-01-26T09:00:00Z");
        var to = Instant.parse("2022-01-26T18:00:00Z");
        assertTrue(analyzer.hasWorkEndScan(employee, from, to));

        var secondFrom = Instant.parse("2022-02-26T09:00:00Z");
        var secondTo = Instant.parse("2022-02-26T18:00:00Z");
        assertFalse(analyzer.hasWorkEndScan(employee, secondFrom, secondTo));
    }

    @Test
    public void shouldHasActions() {
        var employee = employeeRepo.getEmployee(123L);
        var employees = Set.of(employee);

        var interval = new LocalDateInterval(LocalDate.of(2022, 1, 26), LocalDate.of(2022, 1, 26));

        var analyzer = wmsLogAnalyzerFactory.createOperationsAnalyzer(1L, employees, interval);

        var from = Instant.parse("2022-01-26T09:00:00Z");
        var to = Instant.parse("2022-01-26T18:00:00Z");

        assertTrue(analyzer.hasActions(employee, from, to));

        var secondFrom = Instant.parse("2022-02-26T18:00:00Z");
        var secondTo = Instant.parse("2022-02-26T21:00:00Z");

        assertFalse(analyzer.hasActions(employee, secondFrom, secondTo));
    }

    @Test
    @DbUnitDataSet(before = "WmsLogAnalyzerTest.positions.before.csv")
    public void shouldIgnoreEmpoyeeStats() {
        var employee = employeeRepo.getEmployee(123L);
        var employees = Set.of(employee);

        var interval = new LocalDateInterval(LocalDate.of(2022, 1, 26), LocalDate.of(2022, 1, 26));

        var analyzer = wmsLogAnalyzerFactory.createOperationsAnalyzer(1L, employees, interval);

        var from = Instant.parse("2022-01-26T09:00:00Z");
        var to = Instant.parse("2022-01-26T18:00:00Z");
        var stats = analyzer.getStats(employee, from, to);

        assertEquals(GroupedActionsStat.ZERO, stats);
    }
}
