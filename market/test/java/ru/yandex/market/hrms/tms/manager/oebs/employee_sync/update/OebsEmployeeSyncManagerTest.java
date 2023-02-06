package ru.yandex.market.hrms.tms.manager.oebs.employee_sync.update;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

@DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.before.csv")
public class OebsEmployeeSyncManagerTest extends AbstractTmsTest {

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.JustUpdate.before.csv",
            after = "OebsEmployeeSyncManagerTest.JustUpdate.after.csv")
    @Test
    void shouldUpdateEmployeeForToday() {
        mockClock(LocalDate.of(2021, 11, 2));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.TransferDisappeared.before.csv",
            after = "OebsEmployeeSyncManagerTest.TransferDisappeared.after.csv")
    @Test
    void shouldDeleteWithTransferEmployeeWhichHaveNotInfoForToday() {
        mockClock(LocalDate.of(2021, 11, 2));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.IgnoreNotPrimary.before.csv",
            after = "OebsEmployeeSyncManagerTest.IgnoreNotPrimary.after.csv")
    @Test
    void shouldIgnoreNotPrimaryAssignmentInfoIfHadPrimaryOne() {
        mockClock(LocalDate.of(2021, 11, 2));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.UseNotPrimary.before.csv",
            after = "OebsEmployeeSyncManagerTest.UseNotPrimary.after.csv")
    @Test
    void shouldUseNotPrimaryAssignmentInfoIfHadNotPrimary() {
        mockClock(LocalDate.of(2021, 11, 2));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.DoNotTransfer.before.csv",
            after = "OebsEmployeeSyncManagerTest.DoNotTransfer.after.csv")
    @Test
    void shouldNotDeleteWithTransferEmployeeIfThereIsNoLogsForToday() {
        mockClock(LocalDate.of(2021, 11, 3));
        employeeSyncManager.syncOebsEmployees();
    }

}
