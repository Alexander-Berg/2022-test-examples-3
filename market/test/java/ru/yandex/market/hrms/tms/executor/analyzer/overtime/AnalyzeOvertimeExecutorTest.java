package ru.yandex.market.hrms.tms.executor.analyzer.overtime;

import java.time.Instant;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.overtime.repo.EmployeeOvertime;
import ru.yandex.market.hrms.core.domain.overtime.repo.EmployeeOvertimeRepo;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.executor.analyzer.AnalyzeOvertimeExecutor;

class AnalyzeOvertimeExecutorTest extends AbstractTmsTest {

    //fixme: добавить тесты на reject несогласованных
    
    @Autowired
    private AnalyzeOvertimeExecutor executor;

    @Test
    @DbUnitDataSet(
            before = "AnalyzeOvertimeExecutorTest.happyPath.before.csv",
            after = "AnalyzeOvertimeExecutorTest.happyPath.after.csv"
    )
    @DbUnitDataSet(before = "AnalyzeOvertimeExecutorTest.flags.analyzeOvertimesHourly.csv")
    public void happyPath() {
        Instant instant = Instant.parse("2021-12-12T21:00:00+03:00");
        mockClock(instant);

        executor.executeJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "AnalyzeOvertimeExecutorTest.happyPath.before.csv",
            after = "AnalyzeOvertimeExecutorTest.rejectedWhenNoStats.after.csv"
    )
    @DbUnitDataSet(before = "AnalyzeOvertimeExecutorTest.flags.analyzeOvertimesHourly.csv")
    @DbUnitDataSet(before = "AnalyzeOvertimeExecutorTest.flags.rejectOvertimeWhenNoStats.csv")
    public void shouldRejectOvertimeWhenSlotCompletedAndNoStats() {
        Instant instant = Instant.parse("2021-12-13T08:20:00+03:00");
        mockClock(instant);

        executor.executeJob(null);
    }
}
