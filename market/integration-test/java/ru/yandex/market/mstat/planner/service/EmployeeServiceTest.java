package ru.yandex.market.mstat.planner.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.Employee;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestStatus;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.today;

public class EmployeeServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Test
    public void testDeleteVacancy() {

        String job = "JOB-123456";
        Employee employeeVacancy = new Employee();
        employeeVacancy.setLogin(job);
        employeeVacancy.setDepartment_id(data.departmentId);
        employeeVacancy.setName(job);
        employeeVacancy.setDismissed(false);
        vacancyService.createVacancy(employeeVacancy, null, AuthInfoService.PLANNER);

        ProjectWithRequest pwrDefault = projectWithRequestService.getOrCreateCurrentRelocation(job);
        ProjectWithRequest pwrFact = projectWithRequestService.createFact(
                ImmutableMap.<String, BigDecimal>builder().put("b", BigDecimal.ONE).build(),
                ImmutableMap.<String, BigDecimal>builder().put("11", BigDecimal.ONE).build(), job);
        long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder().put("11", BigDecimal.ONE).build(), null);
        Request plan = requestService.createNewRequest(
                data.createPlanTemplate(1, data.departmentId, job, "6d",
                java.sql.Date.valueOf(today().toLocalDate().minusDays(2)),
                java.sql.Date.valueOf(today().toLocalDate().plusDays(4)),
                null, projectId)
        );
        requestService.acceptRequest(plan.getRequest_id(), plan);

        requestService.closeVacation(job);

        Request rDefault = requestService.getRequestById(pwrDefault.getRequest().getRequest_id());
        Request rFact = requestService.getRequestById(pwrFact.getRequest().getRequest_id());
        Request rPlan = requestService.getRequestById(plan.getRequest_id());

        assertTrue(employeeService.getEmployee(job).getDismissed());
        assertEquals(today(), rDefault.getDate_end());
        assertEquals(today(), rFact.getDate_end());
        assertEquals(today(), rPlan.getDate_end());
        assertEquals(RequestStatus.accepted.name(), rPlan.getStatus());
        assertEquals(RequestStatus.accepted.name(), rFact.getStatus());
        assertEquals(RequestStatus.accepted.name(), rDefault.getStatus());

        List<Request> requests = requestService.getRequestsByProjectId(projectId).stream()
                .filter(r -> r.getStatus().equals(RequestStatus.pending.name()))
                .collect(Collectors.toList());
        assertEquals(requests.size(), 1);
        Request newPlanRequest = requests.get(0);
        assertEquals(plan.getDate_end(), newPlanRequest.getDate_end());
        assertEquals(today(), newPlanRequest.getDate_start());
        assertNull(newPlanRequest.getEmployee());
    }

}
