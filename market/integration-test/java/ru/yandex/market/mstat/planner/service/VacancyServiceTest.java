package ru.yandex.market.mstat.planner.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.Employee;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.parseDate;
import static ru.yandex.market.mstat.planner.util.RestUtil.toSqlDate;

public class VacancyServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private EmployeeService employeeService;

    @Test
    public void vacationCreateTest() {

        String login = "JOB-91100000000";
        String startDate = "10.10.2010";

        Employee employee = new Employee();
        employee.setLogin(login);
        employee.setDepartment_id(data.departmentId);
        employee.setName(login);

        Employee emp = vacancyService.createVacancy(employee, startDate, AuthInfoService.PLANNER);
        emp = employeeService.getEmployee(emp.getLogin());

        assertEquals(emp.getVacancy_start_date(), toSqlDate(parseDate(startDate)));
        assertTrue(emp.isVacancy());
        assertEquals(emp.getLogin(), login);
        assertEquals(emp.getDepartment_id().longValue(), data.departmentId);
    }

}
