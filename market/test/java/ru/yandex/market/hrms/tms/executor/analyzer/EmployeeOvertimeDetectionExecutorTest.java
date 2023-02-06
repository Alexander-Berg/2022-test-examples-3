package ru.yandex.market.hrms.tms.executor.analyzer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.overtime.repo.EmployeeOvertimeRepo;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

public class EmployeeOvertimeDetectionExecutorTest extends AbstractTmsTest {

    @Autowired
    private EmployeeOvertimeDetectionExecutor executor;

    @Test
    @DbUnitDataSet(
            before = "EmployeeOvertimeDetectionExecutorTest.hrms3869.before.csv",
            after = "EmployeeOvertimeDetectionExecutorTest.hrms3869.after.csv")
    public void hrms3869() {
        mockClock(LocalDateTime.parse("2021-11-10T15:00:00", DateTimeFormatter.ISO_DATE_TIME));
        executor.executeJob(null);
    }

    @Test
    @DisplayName("Обнаружение и регистрация подработок")
    @DbUnitDataSet(before = "EmployeeOvertimeDetectionExecutorTest.before.csv")
    @DbUnitDataSet(
            before = "EmployeeOvertimeDetectionTestShouldCreate.before.csv",
            after = "EmployeeOvertimeDetectionExecutorTestShouldntCreate.after.csv")
    void shouldNotCreateOvertimeWhenShiftIsNotOver() {
        mockClock(LocalDateTime.of(2021, 3, 19, 10, 0));
        executor.doRealJob(null);
    }

    @Test
    @DisplayName("Обнаружение и регистрация подработок")
    @DbUnitDataSet(before = "EmployeeOvertimeDetectionExecutorTest.before.csv")
    @DbUnitDataSet(
            before = "EmployeeOvertimeDetectionTestShouldCreate.before.csv",
            after = "EmployeeOvertimeDetectionExecutorTest.after.csv")
    void shouldCreateOvertime() {
        mockClock(LocalDateTime.parse("2021-03-20T10:00:01", DateTimeFormatter.ISO_DATE_TIME));
        executor.doRealJob(null);
    }

    @Test
    @DisplayName("Обнаружение и регистрация подработок")
    @DbUnitDataSet(before = "EmployeeOvertimeDetectionExecutorTest.before.csv")
    @DbUnitDataSet(
            before = "EmployeeOvertimeDetectionTestNpo.before.csv",
            after = "EmployeeOvertimeDetectionExecutorTest.after.csv")
    void shouldCreateOvertimeIfThereIsNpo() {
        mockClock(LocalDateTime.parse("2021-03-20T10:00:01", DateTimeFormatter.ISO_DATE_TIME));
        executor.doRealJob(null);
    }

    @Test
    @DisplayName("Обнаружение подработок запрещенных по ТК РФ")
    @DbUnitDataSet(before = "EmployeeOvertimeDetectionExecutorTest.before.csv")
    @DbUnitDataSet(
            before = "EmployeeOvertimeDetectionTestShouldntCreate.before.csv",
            after = "EmployeeOvertimeDetectionExecutorTestShouldntCreate.after.csv")
    void shouldNotCreateOvertimeWhenThereIsWorkshift() {
        mockClock(LocalDateTime.of(2021, 3, 19, 10, 0));
        executor.doRealJob(null);
    }
}
