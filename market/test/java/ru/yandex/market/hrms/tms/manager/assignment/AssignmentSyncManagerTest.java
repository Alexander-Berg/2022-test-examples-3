package ru.yandex.market.hrms.tms.manager.assignment;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.AssignmentSyncManager;

@DbUnitDataSet(before = "AssignmentSyncManagerTest.before.csv")
class AssignmentSyncManagerTest extends AbstractTmsTest {
    @Autowired
    private AssignmentSyncManager assignmentSyncManager;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @BeforeEach
    void setUp() {
        mockClock(LocalDate.of(2021, 2, 16));
    }

    @DbUnitDataSet(after = "AssignmentSyncManagerTest.after.csv")
    @Test
    void shouldRunSync() {
        assignmentSyncManager.syncEmployeeSchedules();
    }

    @DbUnitDataSet(after = "AssignmentSyncManagerTest.after.csv")
    @Test
    void shouldNotCreateDuplicates() {
        assignmentSyncManager.syncEmployeeSchedules();
        assignmentSyncManager.syncEmployeeSchedules();
    }

    @DbUnitDataSet(after = "AssignmentSyncManagerTest.new_month.after.csv")
    @Test
    void shouldNotFailOnNewMonth() {
        assignmentSyncManager.syncEmployeeSchedules();

        mockClock(LocalDate.of(2021, 3, 14));
        assignmentSyncManager.syncEmployeeSchedules();
    }

    @DbUnitDataSet(after = "AssignmentSyncManagerTest.after_delete.csv")
    @Test
    void shouldDeleteOutdatedSchedule() {
        assignmentSyncManager.syncEmployeeSchedules();
        oebsApiConfigurer.mockGetEmployeeSchedules("/results/oebs/empty_response.json");

        assignmentSyncManager.syncEmployeeSchedules();
    }

    @DbUnitDataSet(after = "AssignmentSyncManagerTest.shouldProcessOrgChanges.after.csv")
    @Test
    void shouldProcessOrgChanges() {
        oebsApiConfigurer.mockGetEmployeeSchedules("/results/oebs/get_employee_schedules_org_changed.json");

        assignmentSyncManager.syncEmployeeSchedules();
    }

    @Disabled("logic will be moved to own job")
    @DbUnitDataSet(after = "AssignmentSyncManagerTest.shouldProcessShiftChanges.after.csv")
    @Test
    void shouldProcessShiftChanges() {
        oebsApiConfigurer.mockGetEmployeeSchedules("/results/oebs/get_employee_schedules_shift_changed.json");

        assignmentSyncManager.syncEmployeeSchedules();
    }
}
