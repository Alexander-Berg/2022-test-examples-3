package ru.yandex.market.hrms.tms.manager.gaps;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.StaffConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

@DbUnitDataSet(before = "GapSyncManagerTest.before.csv")
public class GapsSyncManagerTest extends AbstractTmsTest {

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @Autowired
    private StaffConfigurer staffConfigurer;

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTestAddNewGaps.before.csv",
            after = "EmployeeSyncManagerTestAddNewGaps.after.csv")
    void shouldAddNewGaps() {
        mockClock(LocalDateTime.of(2021, 2, 25, 13, 18));
        staffConfigurer.mockPostGap("/results/gap_api_page1.json", 0);
        staffConfigurer.mockPostGap("/results/gap_api_page2.json", 1);
        employeeSyncManager.synchronizeEmployeesGaps();
    }

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTestUpdateGaps.before.csv",
            after = "EmployeeSyncManagerTestUpdateGaps.after.csv")
    void shouldUpdateExistingEmployeeGaps() {
        mockClock(LocalDateTime.of(2021, 2, 25, 13, 18));
        staffConfigurer.mockPostGap("/results/gap_api2.json", 0);
        employeeSyncManager.synchronizeEmployeesGaps();
    }

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTestCancelGaps.before.csv",
            after = "EmployeeSyncManagerTestCancelGaps.after.csv")
    void shouldCancelDeletedGaps() {
        mockClock(LocalDateTime.of(2021, 2, 25, 13, 18));
        staffConfigurer.mockPostGap("/results/gap_api3.json", 0);
        employeeSyncManager.synchronizeEmployeesGaps();
    }

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTestCancelFutureGaps.before.csv",
            after = "EmployeeSyncManagerTestCancelFutureGaps.after.csv")
    void shouldCancelFutureGapsForFiredEmployees() {
        mockClock(LocalDateTime.of(2021, 2, 25, 13, 18));
        staffConfigurer.mockPostGap("/results/gap_api2.json", 0);
        employeeSyncManager.synchronizeEmployeesGaps();
    }
}

