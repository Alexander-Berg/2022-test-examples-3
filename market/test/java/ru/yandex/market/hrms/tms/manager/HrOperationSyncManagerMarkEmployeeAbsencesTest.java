package ru.yandex.market.hrms.tms.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@DbUnitDataSet(before = "HROperationSyncManagerMarkEmployeeAbsencesTest.before.csv")
public class HrOperationSyncManagerMarkEmployeeAbsencesTest extends AbstractTmsTest {

    @Autowired
    private HrOperationSyncManager hrOperationSyncManager;

    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(after = "HROperationSyncManagerMarkEmployeeAbsencesTest.after.csv")
    @Test
    void shouldMarkAbsencesAsReceivedFromOebs() {
        oebsApiConfigurer.mockGetHrOperations("/results/oebs/get_hr_operations_absences.json");

        hrOperationSyncManager.synchronizeEmployeeHrOperations();
    }

}
