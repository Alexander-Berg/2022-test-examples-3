package ru.yandex.market.hrms.core.service.analyzer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeePresence;
import ru.yandex.market.hrms.core.service.employee.EmployeeAssignmentService;
import ru.yandex.market.hrms.core.domain.employee.EmployeeDay;
import ru.yandex.market.hrms.core.service.employee.EmployeeService;
import ru.yandex.market.hrms.core.domain.employee.absence.repo.EmployeeAbsence;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarService;
import ru.yandex.market.hrms.core.domain.employee.calendar.calendar.CalendarDate;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeAssignmentEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeGap;
import ru.yandex.market.hrms.core.domain.employee.sick_leave.repo.EmployeeSickLeaveEntity;
import ru.yandex.market.hrms.core.domain.overtime.repo.EmployeeOvertime;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.GroupedActionsStat;
import ru.yandex.market.hrms.core.service.util.HrmsDateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@DbUnitDataSet(before = "EmployeePerformanceAnalyzerTest.before.csv")
public class EmployeePerformanceAnalyzerTest extends AbstractCoreTest {

    @Autowired
    private FfcPerformanceAnalyzerFactory employeePerformanceAnalyzerFactory;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private EmployeeAssignmentService employeeAssignmentService;

    @Test
    public void shouldReturnLastActionTime() {
        mockClock(LocalDateTime.of(2021, 8, 30, 0, 0));
        var employeeIds = List.of(5581L);
        var employees = employeeService.findByIds(employeeIds);
        var interval = new LocalDateInterval(
                LocalDate.of(2021, 8, 30),
                LocalDate.of(2021, 8, 31));

        var analyzer = employeePerformanceAnalyzerFactory.create(
                1L,
                employees.values(),
                interval
        );


        var from = LocalDate.of(2021, 8, 30);
        var fromInstant = HrmsDateTimeUtil.toInstant(from);

        var to = LocalDate.of(2021, 8, 31);
        var toInstant = HrmsDateTimeUtil.toInstant(to);

        var lastActionTime = analyzer.getLastActionTime(employees.get(5581L), fromInstant, toInstant);

        MatcherAssert.assertThat(lastActionTime, is(Instant.parse("2021-08-30T13:08:00Z")));
    }

    @Test
    @DbUnitDataSet(before = "EmployeePerformanceAnalyzerTestDomainProperty.before.csv")
    public void disabledPositionShouldBeIgnored() {
        mockClock(LocalDateTime.of(2021, 8, 30, 0, 0));
        var employeeIds = List.of(5581L);
        var employees = employeeService.findByIds(employeeIds);
        var interval = new LocalDateInterval(
                LocalDate.of(2021, 8, 30),
                LocalDate.of(2021, 8, 31));
        var analyzer = employeePerformanceAnalyzerFactory.create(
                1L,
                employees.values(),
                interval
        );
        var from = LocalDate.of(2021, 8, 30);
        var fromInstant = HrmsDateTimeUtil.toInstant(from);

        var to = LocalDate.of(2021, 8, 31);
        var toInstant = HrmsDateTimeUtil.toInstant(to);

        var lastActionTime = analyzer.getLastActionTime(employees.get(5581L), fromInstant, toInstant);

        MatcherAssert.assertThat(lastActionTime, nullValue());
    }

    @Test
    public void combinedAnalyzerShouldReturnCorrectStats() {
        var analyzer1 = new EmployeePerformanceAnalyzer() {
            @Override
            public boolean isPerformanceLow(EmployeeEntity employee, Instant from, Instant to, long threshold) {
                return false;
            }

            @Override
            public GroupedActionsStat getStats(EmployeeEntity employee, Instant since, Instant till) {
                return null;
            }

            @Override
            public Map<ActivityLogSource, GroupedActionsStat> getStatMap(EmployeeEntity employee, Instant since,
                                                                         Instant till) {
                return Map.of();
            }

            @Override
            public boolean hasActions(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @javax.annotation.Nullable
            @Override
            public Instant getLastActionTime(EmployeeEntity employee, Instant from, Instant to) {
                return null;
            }

            @Override
            public boolean hasWorkEndScan(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @Override
            public Optional<Instant> getLastActivityStart(EmployeeEntity employee, Instant since, Instant till) {
                return Optional.empty();
            }
        };

        var analyzer2 = new EmployeePerformanceAnalyzer() {

            @Override
            public boolean isPerformanceLow(EmployeeEntity employee, Instant from, Instant to, long threshold) {
                return false;
            }

            @Override
            public GroupedActionsStat getStats(EmployeeEntity employee, Instant since, Instant till) {
                return new GroupedActionsStat(
                        100L,
                        Instant.ofEpochSecond(100000),
                        Instant.ofEpochSecond(200000));
            }

            @Override
            public Map<ActivityLogSource, GroupedActionsStat> getStatMap(EmployeeEntity employee, Instant since,
                                                                         Instant till) {
                return Map.of();
            }

            @Override
            public boolean hasActions(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @javax.annotation.Nullable
            @Override
            public Instant getLastActionTime(EmployeeEntity employee, Instant from, Instant to) {
                return null;
            }

            @Override
            public boolean hasWorkEndScan(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @Override
            public Optional<Instant> getLastActivityStart(EmployeeEntity employee, Instant since, Instant till) {
                return Optional.empty();
            }
        };

        var combinedAnalyzer = new CombinedEmployeePerformanceAnalyzer(analyzer1, analyzer2);

        var stat = combinedAnalyzer.getStats(null, Instant.now(), Instant.now());

        MatcherAssert.assertThat(stat, is(new GroupedActionsStat(
                100L,
                Instant.ofEpochSecond(100000),
                Instant.ofEpochSecond(200000))));
    }

    @Test
    @DisplayName(value = "Независимо от того сколько анализаторов в цепочке" +
            " в случае если по одному из них найдена активность isLowPerformance всегда будет false")
    public void combinedAnalyzerShouldReturnCorrectIsLowPerformance() {
        var analyzer1 = new EmployeePerformanceAnalyzer() {

            @Override
            public boolean isPerformanceLow(EmployeeEntity employee, Instant from, Instant to, long threshold) {
                return false;
            }

            @Override
            public GroupedActionsStat getStats(EmployeeEntity employee, Instant since, Instant till) {
                return GroupedActionsStat.ZERO;
            }

            @Override
            public Map<ActivityLogSource, GroupedActionsStat> getStatMap(EmployeeEntity employee, Instant since,
                                                                         Instant till) {
                return Map.of();
            }

            @Override
            public boolean hasActions(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @javax.annotation.Nullable
            @Override
            public Instant getLastActionTime(EmployeeEntity employee, Instant from, Instant to) {
                return null;
            }

            @Override
            public boolean hasWorkEndScan(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @Override
            public Optional<Instant> getLastActivityStart(EmployeeEntity employee, Instant since, Instant till) {
                return Optional.empty();
            }
        };

        var analyzer2 = new EmployeePerformanceAnalyzer() {

            @Override
            public boolean isPerformanceLow(EmployeeEntity employee, Instant from, Instant to, long threshold) {
                return false;
            }

            @Override
            public GroupedActionsStat getStats(EmployeeEntity employee, Instant since, Instant till) {
                return GroupedActionsStat.ZERO;
            }

            @Override
            public Map<ActivityLogSource, GroupedActionsStat> getStatMap(EmployeeEntity employee, Instant since,
                                                                         Instant till) {
                return Map.of();
            }

            @Override
            public boolean hasActions(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @javax.annotation.Nullable
            @Override
            public Instant getLastActionTime(EmployeeEntity employee, Instant from, Instant to) {
                return null;
            }

            @Override
            public boolean hasWorkEndScan(EmployeeEntity employee, Instant from, Instant to) {
                return false;
            }

            @Override
            public Optional<Instant> getLastActivityStart(EmployeeEntity employee, Instant since, Instant till) {
                return Optional.empty();
            }
        };

        var combinedAnalyzer = new CombinedEmployeePerformanceAnalyzer(analyzer1, analyzer2);

        EmployeeWorkShiftInfo shift = mockEmployeeShift();

        Assertions.assertFalse(combinedAnalyzer.isPerformanceLow(shift.employeeDay().employee(),
                Instant.now(), Instant.now(), -1L));

    }

    private EmployeeWorkShiftInfo mockEmployeeShift() {
        return new EmployeeWorkShiftInfo(
                new EmployeeDay(EmployeeAssignmentEntity.builder().build(),
                        EmployeeEntity.builder().build(),
                        LocalDate.of(2021, 12, 02)),
                new CalendarDate(Collections.emptyList(), Collections.emptyList(),
                        EmployeeGap.builder().build(),
                        EmployeePresence.builder().build(),
                        EmployeeAbsence.builder().build(),
                        EmployeeOvertime.builder().build(),
                        EmployeeSickLeaveEntity.builder().build()
                        )
        );
    }
}
