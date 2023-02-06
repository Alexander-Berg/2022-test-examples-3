package ru.yandex.market.hrms.tms.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

public class HrOperationSyncManagerRemoveDuplicatesTest extends AbstractTmsTest {
    @Autowired
    private HrOperationSyncManager hrOperationSyncManager;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(
            before = "HROperationSyncManagerRemoveDuplicatesTest.before.csv",
            after = "HROperationSyncManagerRemoveDuplicatesTest.after.csv"
    )
    @Test
    void shouldRemoveDuplicates() {
        oebsApiConfigurer.mockGetHrOperations("/results/oebs/get_hr_operations_duplicates.json");

        hrOperationSyncManager.synchronizeEmployeeHrOperations();
    }

    @DbUnitDataSet(
            before = "HROperationSyncManagerRemoveDuplicatesTest.before.csv",
            after = "HROperationSyncManagerRemoveDuplicatesTest.after.csv"
    )
    @Test
    void shouldHandleOrgChange() {
        oebsApiConfigurer.mockGetHrOperations("/results/oebs/get_hr_operations_org_changed.json");

        hrOperationSyncManager.synchronizeEmployeeHrOperations();
    }
}
