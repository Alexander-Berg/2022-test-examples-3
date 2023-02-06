package ru.yandex.market.hrms.tms.manager;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

class AssignmentSyncManagerProcessDuplicatedOebsNumbersTest extends AbstractTmsTest {

    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @Autowired
    private AssignmentSyncManager assignmentSyncManager;

    @Test
    @DbUnitDataSet(
            before = "AssignmentSyncManagerTest.shouldIgnoreDuplicatedOebsNumbers.before.csv",
            after = "AssignmentSyncManagerTest.shouldIgnoreDuplicatedOebsNumbers.after.csv")
    void shouldIgnoreDuplicatedOebsNumbers() {
        mockClock(LocalDate.of(2021, 10, 12));

        String resource = "/results/oebs/get_employee_schedules_with_duplicated_oebs_numbers.json";
        oebsApiConfigurer.mockGetEmployeeSchedules(resource);

        assignmentSyncManager.syncEmployeeSchedules();
    }
}

