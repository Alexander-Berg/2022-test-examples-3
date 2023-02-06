package ru.yandex.market.hrms.tms.executor.analyzer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;


class AnalyzeWorkTimeExecutorTest extends AbstractTmsTest {

    @Autowired
    private AnalyzeWorkTimeExecutor analyzeWorkTimeExecutor;

    @Test
    @DisplayName("Анализ рабочих смен сотрудников ФФЦ")
    @DbUnitDataSet(
            before = "AnalyzeWorkTimeExecutorTest.before.csv",
            after = "AnalyzeWorkTimeExecutorTest.after.csv")
    @DbUnitDataSet(before = "AnalyzeWorkTimeExecutorTest.positions.before.csv")
    void analyzeForFfc() {
        mockClock(LocalDateTime.of(2021, 3, 20, 13, 0));
        analyzeWorkTimeExecutor.doRealJob(null);
    }

    @Test
    @DisplayName("Анализ рабочих смен сотрудников СЦ")
    @DbUnitDataSet(
            before = "AnalyzeWorkTimeExecutorTestForSc.before.csv",
            after = "AnalyzeWorkTimeExecutorTestForSc.after.csv")
    @DbUnitDataSet(before = "AnalyzeWorkTimeExecutorTest.positions.before.csv")
    void analyzeForSc() {
        mockClock(LocalDateTime.of(2021, 3, 20, 13, 0));
        analyzeWorkTimeExecutor.doRealJob(null);
    }

    @Test
    @Disabled
    @DisplayName("Анализ рабочих смен сотрудников")
    @DbUnitDataSet(
            before = "AnalyzeWorkTimeTestWhenNoWms.before.csv",
            after = "AnalyzeWorkTimeTestWhenNoWms.after.csv")
    @DbUnitDataSet(before = "AnalyzeWorkTimeExecutorTest.positions.before.csv")
    void shouldCreateAbsenceWhenNoWmsActions() {
        mockClock(LocalDateTime.of(2021, 3, 20, 13, 0));
        analyzeWorkTimeExecutor.doRealJob(null);
    }

    @Test
    @DisplayName("Анализ рабочих смен сотрудников ФФЦ")
    @DbUnitDataSet(
            before = "AnalyzeWorkTimeExecutorTest.before.csv",
            after = "AnalyzeWorkTimeExecutorTest.noposition.after.csv")
    @DbUnitDataSet(before = "AnalyzeWorkTimeExecutorTest.noposition.before.csv")
    void analyzeWhenPositions() {
        mockClock(LocalDateTime.of(2021, 3, 20, 13, 0));
        analyzeWorkTimeExecutor.doRealJob(null);
    }
}
