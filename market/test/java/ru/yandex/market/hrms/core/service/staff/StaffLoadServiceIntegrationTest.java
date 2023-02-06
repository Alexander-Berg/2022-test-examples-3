package ru.yandex.market.hrms.core.service.staff;

import java.util.List;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.staff.StaffEmployeeGroupDto;
import ru.yandex.market.hrms.test.configurer.StaffConfigurer;

public class StaffLoadServiceIntegrationTest extends AbstractCoreTest {

    private static final long DOMAIN_ID = 1L;
    private static final Set<Long> STAFF_GROUP_IDS = Set.of(1234L, 1235L);

    @Autowired
    private StaffLoadService staffLoadService;

    @Autowired
    private StaffConfigurer staffConfigurer;

    @Test
    @DbUnitDataSet(before = "StaffLoadServiceIntegrationTest.before.csv")
    void shouldLoadEmployeesFromStaff() {
        STAFF_GROUP_IDS.forEach(staffGroupId -> {
            staffConfigurer.mockGetPersons("/results/staff_api.json", staffGroupId, 1);
        });

        List<StaffEmployeeGroupDto> employees = staffLoadService.loadEmployeesFromStaff(DOMAIN_ID);

        MatcherAssert.assertThat(employees, CoreMatchers.not(Matchers.empty()));
    }
}
