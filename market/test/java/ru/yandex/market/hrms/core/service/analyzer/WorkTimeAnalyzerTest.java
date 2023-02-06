package ru.yandex.market.hrms.core.service.analyzer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

@DbUnitDataSet(before = {
        "WorkTimeAnalyzerTest.before.csv",
})
class WorkTimeAnalyzerTest extends AbstractCoreTest {
    public static final LocalDate START_FROM = LocalDate.of(2021, 1, 1);

    @Autowired
    private WorkTimeAnalyzer workTimeAnalyzer;

    @BeforeEach
    public void beforeEach() {
        mockClock(LocalDateTime.parse("2021-09-24T22:00:00"));
    }

    @Test
    @DbUnitDataSet(after = "WorkTimeAnalyzerTest.absencesWhenNoOperations.after.csv")
    public void absencesWhenNoOperations() {
        workTimeAnalyzer.analyzeDomain(1L, START_FROM);
    }

    @Test
    @DbUnitDataSet(
            before = "WorkTimeAnalyzerTest.withOperations.csv",
            after = "WorkTimeAnalyzerTest.withOperations.after.csv")
    public void presencesWhenOperationsExist() {
        workTimeAnalyzer.analyzeDomain(1L, START_FROM);
    }

    @Test
    @DbUnitDataSet(after = "WorkTimeAnalyzerTest.withoutOperatingZones.after.csv")
    public void analyzeWithOperatingZones() {
        workTimeAnalyzer.analyzeDomain(1L, START_FROM);
    }

    @Test
    @DisplayName("СЦ. Сохранить желтую НН, если смена закончилась 10 минут назад и нет логов после конца смены")
    @DbUnitDataSet(after = "WorkTimeAnalyzerTest.closedShiftNoScLogs.after.csv")
    public void yellowNnWhenNoScLogsShiftClosed10Minutes() {
        mockClock(LocalDateTime.parse("2021-09-24T20:36:00.00"));
        workTimeAnalyzer.analyzeDomain(43L, START_FROM);
    }

    @Test
    @DisplayName("СЦ. Проставить желтые НН после окончания смены в таймзоне домена")
    @DbUnitDataSet(after = "WorkTimeAnalyzerTest.closedShift.sc_nsk.after.csv")
    public void yellowNnWhenNoScLogsShiftClosedInDomainTz() {
        // смена заканчивается в 17:15 по НСК
        mockClock(LocalDateTime.parse("2021-09-24T16:36:00.00"));
        workTimeAnalyzer.analyzeDomain(4L, START_FROM);
    }

    @Test
    @DisplayName("ФФЦ. Проставить желтые НН после окончания смены в таймзоне домена")
    @DbUnitDataSet(after = "WorkTimeAnalyzerTest.closedShift.ffc_nsk.after.csv")
    public void yellowNnWhenNoFfcLogsShiftClosedInDomainTz() {
        // смена заканчивается в 17:15 по НСК
        mockClock(LocalDateTime.parse("2021-09-24T16:36:00.00")); // по Москве
        workTimeAnalyzer.analyzeDomain(5L, START_FROM);
    }
}
