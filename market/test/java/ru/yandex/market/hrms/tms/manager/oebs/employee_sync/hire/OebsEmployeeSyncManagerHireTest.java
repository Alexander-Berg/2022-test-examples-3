package ru.yandex.market.hrms.tms.manager.oebs.employee_sync.hire;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

@DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.before.csv")
public class OebsEmployeeSyncManagerHireTest extends AbstractTmsTest {

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @DbUnitDataSet(before = "OebsEmployeeSyncManagerTest.HireNew.before.csv",
            after = "OebsEmployeeSyncManagerTest.HireNew.after.csv")
    @Test
    void shouldUpdateOldEmployeeAndHireNewOne() {
        mockClock(LocalDate.of(2021, 11, 2));
        employeeSyncManager.syncOebsEmployees();
    }

}
