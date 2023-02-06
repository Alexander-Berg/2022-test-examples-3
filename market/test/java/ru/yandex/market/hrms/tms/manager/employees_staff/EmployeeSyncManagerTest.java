package ru.yandex.market.hrms.tms.manager.employees_staff;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeGroup;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeGroupRepo;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.test.configurer.StaffConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeSyncManager;

import static org.hamcrest.Matchers.hasSize;

@DbUnitDataSet(before = "EmployeeSyncManagerTest.before.csv")
public class EmployeeSyncManagerTest extends AbstractTmsTest {
    private static final long STAFF_ROOT_GROUP_ID = 12347;

    private static final long CHILD_EMPLOYEE_GROUP_ID = 22346L;

    private static final String YANDEX_LOGIN = "yukondrashov";

    @Autowired
    private EmployeeSyncManager employeeSyncManager;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private StaffConfigurer staffConfigurer;

    @Autowired
    private EmployeeGroupRepo employeeGroupRepo;

    @Test
    @DbUnitDataSet(after = "EmployeeSyncManagerTest.shouldHireNewEmployees.after.csv")
    void shouldHireNewEmployees() {
        mockClock(LocalDate.of(2021, 9, 1));
        mockStaffApiConfig("/results/staff_api.json", "/results/staff_api_group_12346.json");
        employeeSyncManager.synchronizeEmployeesWithStaff();
    }

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTestTransferred.before.csv")
    void shouldNotFailOnTransferredEmployee() {
        mockStaffApiConfig("/results/staff_api.json", "/results/staff_api_group_12346.json");
        employeeSyncManager.synchronizeEmployeesWithStaff();
    }

    @Test
    @DbUnitDataSet(
            before = "EmployeeSyncManagerTest.shouldNotFireSpecialEmployees.csv",
            after = "EmployeeSyncManagerTest.shouldNotFireSpecialEmployees.csv"
    )
    void shouldNotFireSpecialEmployees() {
        mockStaffApiConfig("/results/staff_api_empty.json", "/results/staff_api_empty.json");
        employeeSyncManager.synchronizeEmployeesWithStaff();
    }

    @Test
    void shouldFireNewEmployees() {
        mockStaffApiConfig("/results/staff_api.json", "/results/staff_api_group_12346.json");
        employeeSyncManager.synchronizeEmployeesWithStaff();

        staffConfigurer.mockGetPersonsRecursively("/results/staff_api2.json", STAFF_ROOT_GROUP_ID, 1);
        staffConfigurer.mockGetPersonsRecursively("/results/staff_api2_page2.json", STAFF_ROOT_GROUP_ID, 2);
        staffConfigurer.mockGetPersons("/results/staff_api_group_12346.json", 12346, 1);
        employeeSyncManager.synchronizeEmployeesWithStaff();

        List<EmployeeEntity> allEmployees = employeeRepo.findAll();
        MatcherAssert.assertThat(allEmployees, hasSize(12));

        List<EmployeeGroup> childEmployeeGroup = employeeGroupRepo.findAllByGroupId(CHILD_EMPLOYEE_GROUP_ID);
        MatcherAssert.assertThat(childEmployeeGroup, hasSize(10));

        MatcherAssert.assertThat(childEmployeeGroup, CoreMatchers.hasItem(
                Matchers.allOf(
                        Matchers.hasProperty("removedAt", CoreMatchers.nullValue()),
                        Matchers.hasProperty("employee", Matchers.hasProperty("staffLogin",
                                CoreMatchers.is(YANDEX_LOGIN)))
                )
        ));
    }

    @Test
    @DbUnitDataSet(before = "EmployeeSyncManagerTest.shouldUpdateEmployees.before.csv",
            after = "EmployeeSyncManagerTest.shouldUpdateEmployees.after.csv")
    void shouldUpdateExistingEmployees() {
        mockClock(LocalDate.of(2021, 11, 8));
        mockStaffApiConfig("/results/staff_api_group_12345_update.json", "/results/staff_api_group_12346.json");

        employeeSyncManager.synchronizeEmployeesWithStaff();
    }

    private void mockStaffApiConfig(String rootGroupJsonPath, String childGroupJsonPath) {
        staffConfigurer.mockGetPersonsRecursively(rootGroupJsonPath, STAFF_ROOT_GROUP_ID, 1);
        staffConfigurer.mockGetPersons(childGroupJsonPath, 12346, 1);
        staffConfigurer.mockGetGroups("/results/staff_groups_12347.json", STAFF_ROOT_GROUP_ID, 1);
    }
}
