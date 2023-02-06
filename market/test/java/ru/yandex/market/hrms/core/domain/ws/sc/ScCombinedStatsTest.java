package ru.yandex.market.hrms.core.domain.ws.sc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.employee.EmployeeService;
import ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource;
import ru.yandex.market.hrms.core.service.analyzer.sc.ScActionStatAnalyzerFactory;
import ru.yandex.market.hrms.core.service.outstaff.shift.Context;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutstaffShiftCalculationService;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScCombinedStatsTest extends AbstractCoreTest {

    @Autowired
    private OutstaffShiftCalculationService outstaffShiftCalculationService;

    @Autowired
    private ScActionStatAnalyzerFactory scActionStatAnalyzerFactory;

    @Autowired
    private EmployeeService employeeService;

    @Test
    @DbUnitDataSet(before = "ScCombinedStatTest.staff.before.csv")
    public void shouldSeeBothUsersStats() {
        var employeeMap = employeeService.findByIds(List.of(1L));
        var from = LocalDate.of(2021, 11, 25);
        var to = LocalDate.of(2021, 11, 26);
        var interval = new LocalDateInterval(from, to);
        var analyzer = scActionStatAnalyzerFactory
                .create(1L, employeeMap.values(), interval);

        var employee = employeeMap.get(1L);

        var fromInstant = Instant.parse("2021-11-25T06:00:00Z");
        var toInstant = Instant.parse("2021-11-25T18:00:00Z");

        var stats = analyzer.getStatMap(employee, fromInstant, toInstant);
        var scStats = stats.get(ActivityLogSource.SC);
        assertThat(scStats.firstActionTime(), is(Instant.parse("2021-11-25T11:00:00Z")));
        assertThat(scStats.lastActionTime(), is(Instant.parse("2021-11-25T17:00:00Z")));
    }

    @Test
    @DbUnitDataSet(before = "ScCombinedStatTest.outstaff.before.csv")
    public void shouldSeeBothUsersStatsOutstaff() {
        var from = LocalDate.of(2021, 11, 25);
        var to = LocalDate.of(2021, 11, 26);
        var interval = new LocalDateInterval(from, to);
        var context = Context.builder()
                .outstaffIds(List.of(1L))
                .domainId(1L)
                .interval(interval)
                .build();

        var shift = outstaffShiftCalculationService.calculate(context).getShifts().get(0);
        assertThat(shift.getFirstActivityTs(), is(Instant.parse("2021-11-25T11:00:00Z")));
        assertThat(shift.getLastActivityTs(), is(Instant.parse("2021-11-25T17:00:00Z")));
    }
}
