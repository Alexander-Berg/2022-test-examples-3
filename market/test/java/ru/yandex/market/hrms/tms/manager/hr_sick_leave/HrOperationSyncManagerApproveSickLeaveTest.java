package ru.yandex.market.hrms.tms.manager.hr_sick_leave;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.HrOperationSyncManager;

@DbUnitDataSet(before = "HrOperationSyncManagerApproveSickLeaveTest.before.csv")
public class HrOperationSyncManagerApproveSickLeaveTest extends AbstractTmsTest {

    @Autowired
    private HrOperationSyncManager hrOperationSyncManager;

    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(after = "HrOperationSyncManagerApproveSickLeaveTest.after.csv")
    @Test
    void shouldMarkSickLeavesAsReceivedFromOebs() {
        mockClock(LocalDate.of(2021, 3, 10));

        oebsApiConfigurer.mockGetHrOperations("/results/oebs/get_hr_operations_sick_leaves.json");

        hrOperationSyncManager.synchronizeEmployeeHrOperations();
    }
}
