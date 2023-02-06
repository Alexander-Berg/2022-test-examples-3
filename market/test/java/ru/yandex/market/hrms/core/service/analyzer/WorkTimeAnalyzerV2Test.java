package ru.yandex.market.hrms.core.service.analyzer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.absence.EmployeeAbsenceRecalculationService;

@DbUnitDataSet(before = {"WorkTimeAnalyzerV2.schedules.csv", "WorkTimeAnalyzerV2.before.csv"})
class WorkTimeAnalyzerV2Test extends AbstractCoreTest {
    public static final LocalDate START_FROM = LocalDate.of(2021, 1, 1);

    @Autowired
    private WorkTimeAnalyzer workTimeAnalyzer;

    @Autowired
    private EmployeeAbsenceRecalculationService employeeAbsenceRecalculationService;

    @Test
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv",
            after = "WorkTimeAnalyzerV2.absenceWhenNoOperations.after.csv")
    public void absencesWhenNoOperations() {
        mockClock(LocalDateTime.parse("2021-12-24T22:00:00"));
        workTimeAnalyzer.analyzeDomainV2(1L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(41L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(52L, START_FROM, false);
    }

    @Test
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv",
            after = "WorkTimeAnalzerV2.ignoreDelete.before.csv")
    @DbUnitDataSet(after = "WorkTimeAnalyzerV2.absence.after.csv")
    public void ignoreDeletedPreprocessed() {
        mockClock(LocalDateTime.parse("2021-12-26T22:00:00"));
        workTimeAnalyzer.analyzeDomainV2(1L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(41L, START_FROM, false);
    }

    @Test
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv",
            after = "WorkTimeAnalzerV2.ignoreDelete.before.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.environment.before.csv")
    @DbUnitDataSet(after = "WorkTimeAnalyzerV2.draftAbsence.after.csv")
    public void ignoreDeletedPreprocessedDraft() {
        mockClock(LocalDateTime.parse("2021-12-26T22:00:00"));
        workTimeAnalyzer.analyzeDomainV2(1L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(41L, START_FROM, false);
    }

    @Test
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.environment.before.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.draftAbsence.after.csv")
    @DbUnitDataSet(after = "WorkTimeAnalyzerV2.recalculatedDraftAbsence.after.csv")
    public void recalculateDraftAbsence() {
        mockClock(LocalDateTime.parse("2021-12-26T22:00:00"));
        employeeAbsenceRecalculationService.recalculateDraftAbsences(41L, LocalDate.of(2021, 12, 26));
    }

    @Test
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.environment.before.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.draftAbsence.after.csv")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2.operations.before.csv")
    @DbUnitDataSet(after = "WorkTimeAnalyzerV2.cancelledAbsence.after.csv")
    public void recalculateWithOperations() {
        mockClock(LocalDateTime.parse("2021-12-26T22:00:00"));
        employeeAbsenceRecalculationService.recalculateDraftAbsences(41L, LocalDate.of(2021, 12, 26));
    }

    @Test
    @DbUnitDataSet(
            before = {"WorkTimeAnalyzerV2Test.withOperations.before.csv", "WorkTimeAnalyzerV2Test.employee.before.csv"},
            after = "WorkTimeAnalyzerV2Test.withOperations.after.csv")
    public void presencesWhenOperationsExist() {
        mockClock(LocalDateTime.parse("2021-12-24T22:00:00"));
        workTimeAnalyzer.analyzeDomainV2(1L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(41L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(52L, START_FROM, false);
    }

    @Test
    @DisplayName("СЦ. Проставить желтые НН после окончания смены в таймзоне домена")
    @DbUnitDataSet(before = "WorkTimeAnalyzerV2Test.employee.before.csv",
            after = "WorkTimeAnalyzerV2Test.AbsenceAtShiftEndByLocalTime.after.csv")
    public void yellowNnWhenNoScLogsShiftClosedInDomainTz() {
        // смена заканчивается в 21:00 по НСК
        mockClock(LocalDateTime.parse("2021-12-22T17:00:00"));
        workTimeAnalyzer.analyzeDomainV2(41L, START_FROM, false);
    }

    @Test
    @DbUnitDataSet(
            before = "WorkTimeAnalyzerV2Test.employeeTransferred.before.csv",
            after = "WorkTimeAnalyzerV2Test.employeeTransferred.after.csv")
    public void test1() {
        mockClock(LocalDateTime.parse("2021-12-25T20:00:00"));
        workTimeAnalyzer.analyzeDomainV2(1L, START_FROM, false);
        workTimeAnalyzer.analyzeDomainV2(2L, START_FROM, false);
    }

}
