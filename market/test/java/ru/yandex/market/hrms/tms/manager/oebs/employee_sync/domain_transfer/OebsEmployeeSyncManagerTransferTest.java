package ru.yandex.market.hrms.tms.manager.oebs.employee_sync.domain_transfer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

@DbUnitDataSet(before = "OebsEmployeeSyncManagerTransferTest.before.csv")
public class OebsEmployeeSyncManagerTransferTest extends AbstractTmsTest {

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.TransferInside.before.csv",
            after = "OebsEmployeeSyncManagerTest.TransferInside.after.csv")
    @Test
    void shouldChangeDomainAndGroupEmployee() {
        mockClock(LocalDateTime.of(2021, 11, 2, 11, 12, 13));
        employeeSyncManager.syncOebsEmployees();
    }

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.NotTransferButChangeType.before.csv",
            after = "OebsEmployeeSyncManagerTest.NotTransferButChangeType.after.csv")
    @Test
    void shouldNotChangeDomainAndGroupIfChangedType() {
        mockClock(LocalDateTime.of(2021, 11, 2, 11, 12, 13));
        employeeSyncManager.syncOebsEmployees();
    }

}
