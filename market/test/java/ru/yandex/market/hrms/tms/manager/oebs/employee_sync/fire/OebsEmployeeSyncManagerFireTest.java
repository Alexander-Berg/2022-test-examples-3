package ru.yandex.market.hrms.tms.manager.oebs.employee_sync.fire;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

@DbUnitDataSet(before = "OebsEmployeeSyncManagerFireTest.before.csv")
public class OebsEmployeeSyncManagerFireTest extends AbstractTmsTest {

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerFireTest.NotFireByFlag.before.csv",
            after = "OebsEmployeeSyncManagerFireTest.NotFireByFlag.after.csv")
    @Test
    void shouldNotFireEmployeeWhenFlagTurnedOff() {
        mockClock(LocalDateTime.of(2021, 11, 2, 11, 12, 13));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.SimpleFire.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.SimpleFire.after.csv")
    @Test
    void shouldFireEmployeeWithTodayFireDate() {
        mockClock(LocalDateTime.of(2021, 11, 2, 11, 12, 13));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.CloseGroupsForMarkedFired.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.CloseGroupsForMarkedFired.after.csv")
    @Test
    void shouldFireEmployeeMarkedAsFireButWithNotClosedGroups() {
        mockClock(LocalDateTime.of(2021, 11, 2, 11, 12, 13));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.SimpleFire.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.SimpleFire.after.csv")
    @Test
    void shouldFireEmployeeWithFireDateInFuture() {
        mockClock(LocalDate.of(2021, 10, 28));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.FireInPast.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.FireInPast.after.csv")
    @Test
    void shouldFireEmployeeWithFireDateInPast() {
        mockClock(LocalDate.of(2021, 10, 28));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.TransferDate.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.TransferDate.after.csv")
    @Test
    void shouldFireEmployeeWithTransferDate() {
        mockClock(LocalDate.of(2021, 10, 28));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.SimpleRehire.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.SimpleRehire.after.csv")
    @Test
    void shouldRehireEmployee() {
        mockClock(LocalDateTime.of(2021, 11, 27, 12, 0, 0));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = { "OebsEmployeeSyncManagerFireTest.NotFireIgnored.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.ignored.csv" },
            after = "OebsEmployeeSyncManagerFireTest.NotFireIgnored.before.csv")
    @Test
    void shouldNotFireIgnoredEmployee() {
        mockClock(LocalDate.of(2021, 10, 28));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = {"OebsEmployeeSyncManagerFireTest.CancelHiring.before.csv",
            "OebsEmployeeSyncManagerFireTest.environment.csv" },
            after = "OebsEmployeeSyncManagerFireTest.CancelHiring.after.csv")
    @Test
    void shouldFireEmployeeWithCancelHiring() {
        mockClock(LocalDate.of(2021, 10, 28));
        employeeSyncManager.syncOebsEmployees();
    }
}
