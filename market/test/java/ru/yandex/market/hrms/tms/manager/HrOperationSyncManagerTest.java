package ru.yandex.market.hrms.tms.manager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeHROperation;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeHROperationRepo;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@DbUnitDataSet(before = "HROperationSyncManagerTest.before.csv")
public class HrOperationSyncManagerTest extends AbstractTmsTest {
    @Autowired
    private HrOperationSyncManager hrOperationSyncManager;
    @Autowired
    private EmployeeHROperationRepo employeeHROperationRepo;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(after = "HROperationSyncManagerTest.after.csv")
    @Test
    void shouldSyncHrOperations() {
        hrOperationSyncManager.synchronizeEmployeeHrOperations();

        List<EmployeeHROperation> operations = employeeHROperationRepo.findAll();

        MatcherAssert.assertThat(operations, Matchers.hasSize(3));
    }

    @DbUnitDataSet(after = "HROperationSyncManagerTest.after.csv")
    @Test
    void shouldNotCreateDuplicates() {
        hrOperationSyncManager.synchronizeEmployeeHrOperations();
        hrOperationSyncManager.synchronizeEmployeeHrOperations();

        List<EmployeeHROperation> all = employeeHROperationRepo.findAll();

        MatcherAssert.assertThat(all, Matchers.hasSize(3));
    }

    @Test
    void shouldDeleteIfOebsNotReturn() {
        LocalDateTime now = LocalDateTime.now();

        mockClock(now);
        hrOperationSyncManager.synchronizeEmployeeHrOperations();

        mockClock(now.plusHours(2));
        oebsApiConfigurer.mockGetHrOperations("/results/oebs/empty_response.json");
        hrOperationSyncManager.synchronizeEmployeeHrOperations();

        List<EmployeeHROperation> all = employeeHROperationRepo.findAll();
        MatcherAssert.assertThat(all, Matchers.everyItem(
                Matchers.hasProperty("deleted", Matchers.is(true))
        ));
    }

    @Test
    @DbUnitDataSet(after = "HROperationSyncManagerTest.after.csv")
    void shouldNotFailIfNoDetails() {
        EmployeeHROperation emptyEho = new EmployeeHROperation(
                1L,
                EmployeeEntity.builder().id(1L).build(),
                null,
                "123",
                Instant.parse("2020-02-01T00:00:00Z"),
                null,
                false,
                Collections.emptySet()
        );
        employeeHROperationRepo.save(emptyEho);

        hrOperationSyncManager.synchronizeEmployeeHrOperations();

        List<EmployeeHROperation> all = employeeHROperationRepo.findAll();
        MatcherAssert.assertThat(all, Matchers.hasSize(3));
    }
}
