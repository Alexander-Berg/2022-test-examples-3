package ru.yandex.market.hrms.tms.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

public class HrOperationSyncManagerWithAssignmentsTest extends AbstractTmsTest {
    @Autowired
    private HrOperationSyncManager hrOperationSyncManager;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @Test
    @DbUnitDataSet(before = "HROperationSyncManagerWithAssignmentsTest.before.csv")
    void shouldSyncHrOperations() {
        oebsApiConfigurer.mockGetHrOperations("/results/oebs/get_hr_operations_1.json");
        hrOperationSyncManager.synchronizeEmployeeHrOperations();
    }
}
