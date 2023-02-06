package ru.yandex.market.hrms.core.domain.employee;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EmployeeGroupLinkTest extends AbstractCoreTest {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Test
    @DbUnitDataSet(before = "EmployeeGroupLinkTest.before.csv")
    public void shouldReturnLatestGroup() {
        var employee = employeeRepo.findAllByIdsFetchEmployeeGroups(List.of(16440L)).get(0);
        var link = employee.findLastGroupLink().orElseThrow();
        assertThat(link.getGroup().getId(), is(274L));
    }
}
