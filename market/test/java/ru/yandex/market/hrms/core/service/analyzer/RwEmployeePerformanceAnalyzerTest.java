package ru.yandex.market.hrms.core.service.analyzer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.model.domain.DomainType;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.exception.HrmsNotFoundException;
import ru.yandex.market.hrms.core.domain.strategy.StrategyResolver;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.GroupedActionsStat;
import ru.yandex.market.hrms.core.service.analyzer.sc.ScActionStatAnalyzerImpl;
import ru.yandex.market.hrms.core.service.analyzer.wms.WmsOperationsAnalyzerImpl;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "RwEmployeePerformanceAnalyzerTest.before.csv")
public class RwEmployeePerformanceAnalyzerTest extends AbstractCoreTest {

    private StrategyResolver<DomainType, EmployeePerformanceAnalyzerFactory> strategyResolver;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Test
    public void wmsAndScAnalyzersShouldBePresent() {
        var analyzer = getAnalyzer();

        var analyzers = ((CombinedEmployeePerformanceAnalyzer) analyzer)
                .getAllAnalyzers();

        var scAnalyzer = analyzers.stream()
                .filter(x -> x instanceof WmsOperationsAnalyzerImpl).findFirst();

        var wmsAnalyzer = analyzers.stream()
                .filter(x -> x instanceof ScActionStatAnalyzerImpl).findFirst();

        assertThat(scAnalyzer.isPresent(), is(true));
        assertThat(wmsAnalyzer.isPresent(), is(true));
    }

    @Test
    public void wmsAndScLogsShouldBePresentInStats() {
        var analyzer = getAnalyzer();

        var employee = employeeRepo.findById(5581L)
                .orElseThrow(() -> new HrmsNotFoundException(EmployeeEntity.class));

        var from = Instant.parse("2021-08-30T06:00:00Z");
        var to = Instant.parse("2021-08-30T18:00:00Z");

        var statMap = analyzer.getStatMap(employee, from, to);
        var wmsStats = statMap.get(ActivityLogSource.WMS);
        var scStats = statMap.get(ActivityLogSource.SC);

        assertThat(wmsStats, not(GroupedActionsStat.ZERO));
        assertThat(scStats, not(GroupedActionsStat.ZERO));
    }

    private EmployeePerformanceAnalyzer getAnalyzer() {
        var employees = employeeRepo.findById(5581L)
                .map(List::of)
                .orElseThrow(() -> new HrmsNotFoundException(EmployeeEntity.class));

        var from = LocalDate.of(2021, 8, 30);
        var to = LocalDate.of(2021, 8, 31);
        var interval = new LocalDateInterval(from, to);

        return strategyResolver.resolve(DomainType.RW)
                .create(1L, employees, interval);
    }
}
