package ru.yandex.market.mstat.planner.service;


import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mstat.planner.dao.HeadcountDao;
import ru.yandex.market.mstat.planner.dto.HeadcountDto;
import ru.yandex.market.mstat.planner.dto.HeadcountHistoryDto;
import ru.yandex.market.mstat.planner.model.Headcount;
import ru.yandex.market.mstat.planner.model.HeadcountVacancy;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestType;
import ru.yandex.market.mstat.planner.util.RestUtil;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HeadcountServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private HeadcountService headcountService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Autowired
    private HeadcountDao headcountDao;

    @Test
    public void testGetHeadcountsInDepartment_LoginOrNameInclusionFilter() {
        LocalDate today = RestUtil.todayLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        final ImmutableMap<String, BigDecimal> projectContours = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(data.contourId), BigDecimal.ONE)
                .build();

        String employeeLogin1 = "qwerty1";
        data.createEmployee(data.departmentId, employeeLogin1, "Иван Иванов");

        final Headcount qwerty1Headcount = data.createHeadcount(1,
                100L,
                data.departmentId,
                employeeLogin1,
                Date.valueOf(twoDaysAgo),
                Date.valueOf(todayPlusOneDay));

        projectWithRequestService.createRelocation(projectContours, null, employeeLogin1, data.departmentId, data.login, "some desc");

        String employeeLogin2 = "asdfg2";
        data.createEmployee(data.departmentId, employeeLogin2, "Петр Петров");

        final Headcount asdfg2Headount = data.createHeadcount(1,
                200L,
                data.departmentId,
                employeeLogin2,
                Date.valueOf(twoDaysAgo),
                Date.valueOf(todayPlusOneDay));

        projectWithRequestService.createRelocation(projectContours, null, employeeLogin2, data.departmentId, data.login, "some desc");

        // login inclusion
        final List<HeadcountDto> loginRes = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                null,
                "eRt");

        assertEquals(1, loginRes.size());
        assertEquals(qwerty1Headcount.getEmployee(), loginRes.get(0).getEmployee());

        // name inclusion
        final List<HeadcountDto> res2 = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                null,
                "ВАН");

        assertEquals(1, res2.size());
        assertEquals(qwerty1Headcount.getEmployee(), res2.get(0).getEmployee());

        // no such employee
        final List<HeadcountDto> res4 = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                null,
                "qwerty2");

        assertEquals(0, res4.size());
    }

    @Test
    public void testGetHeadcountsInDepartment_HeadcountFilter() {
        LocalDate today = RestUtil.todayLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        final ImmutableMap<String, BigDecimal> projectContours = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(data.contourId), BigDecimal.ONE)
                .build();

        String employeeLogin = "qwerty1";

        final Headcount headcount1 = data.createHeadcount(1,
                100L,
                data.departmentId,
                employeeLogin,
                Date.valueOf(twoDaysAgo),
                Date.valueOf(todayPlusOneDay));

        final Headcount headcount2 = data.createHeadcount(0,
                200L,
                data.departmentId,
                employeeLogin,
                Date.valueOf(twoDaysAgo),
                Date.valueOf(todayPlusOneDay));

        projectWithRequestService.createRelocation(projectContours, null, employeeLogin, data.departmentId, data.login, "some desc");

        // without filter
        final List<HeadcountDto> res1 = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                null,
                null);

        assertEquals(2, res1.size());

        // with filter
        int expectedHeadcount = 1;
        final List<HeadcountDto> res2 = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                expectedHeadcount,
                null,
                null);

        assertEquals(1, res2.size());
        assertEquals(Integer.valueOf(expectedHeadcount), res2.get(0).getHeadcount());

    }

    @Test
    public void testHeadcountHistory() {
        LocalDate today = RestUtil.todayLocalDate();

        String emp1 = "employee1";
        data.createEmployee(data.departmentId, emp1);
        String emp2 = "employee2";
        data.createEmployee(data.departmentId, emp2);
        String emp3 = "employee3";
        data.createEmployee(data.departmentId, emp3);

        final Headcount headcount1 = data.createHeadcount(1,
                100L,
                data.departmentId,
                emp1,
                Date.valueOf(today.minusDays(10)),
                Date.valueOf(today.minusDays(1)));

        final Headcount headcount1Last = data.createHeadcount(1,
                100L,
                data.departmentId,
                emp3,
                Date.valueOf(today),
                Date.valueOf(today.plusDays(10)));

        final Headcount headcount2 = data.createHeadcount(1,
                200L,
                data.departmentId,
                emp2,
                Date.valueOf(today.minusDays(10)),
                Date.valueOf(today.minusDays(5)));

        final Headcount headcount2Last = data.createHeadcount(1,
                200L,
                data.departmentId,
                emp2,
                Date.valueOf(today.minusDays(4)),
                Date.valueOf(today.minusDays(1)));

        ArrayDeque<HeadcountHistoryDto> history1 = headcountService.getHeadcountHistory(100L);
        ArrayDeque<HeadcountHistoryDto> history2 = headcountService.getHeadcountHistory(200L);

        assertEquals(2, history1.size());
        assertEquals(emp3, history1.getFirst().getEmployee());
        assertEquals(emp1, history1.getLast().getEmployee());

        assertEquals(3, history2.size());
        assertEquals("CLOSED", history2.getFirst().getStatus());

    }

    @Test
    public void testGetHeadcountsInDepartment_RequestTypeFilter() {
        LocalDate today = RestUtil.todayLocalDate();

        final LocalDate queryDateStart = today.minusDays(10);
        final LocalDate queryDateEnd = today.plusDays(10);

        final LocalDate req1StartDate = today.minusDays(5);
        final LocalDate req1EndDate = today.plusDays(2);

        // This start date is closer to current date
        final LocalDate req2StartDate = today.minusDays(3);
        final LocalDate req2EndDate = today.plusDays(2);

        // Second req interval closer to zero, but start date after today (req3StartDate) has higher priority
        final LocalDate req3StartDate = today;
        final LocalDate req3EndDate = today.plusDays(9);

        String emp1 = "employee1";
        data.createEmployee(data.departmentId, emp1);
        String emp2 = "employee2";
        data.createEmployee(data.departmentId, emp2);
        String emp3 = "employee3";
        data.createEmployee(data.departmentId, emp3);

        final Headcount headcount1 = data.createHeadcount(1,
                100L,
                data.departmentId,
                emp1,
                Date.valueOf(queryDateStart),
                Date.valueOf(queryDateEnd));

        final Headcount headcount2 = data.createHeadcount(1,
                200L,
                data.departmentId,
                emp2,
                Date.valueOf(queryDateStart),
                Date.valueOf(queryDateEnd));

        final Headcount headcount3 = data.createHeadcount(1,
                300L,
                data.departmentId,
                emp3,
                Date.valueOf(queryDateStart),
                Date.valueOf(queryDateEnd));

        final ImmutableMap<String, BigDecimal> projectContours = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(data.contourId), BigDecimal.ONE)
                .build();

        Long projectId = data.createProject(projectContours, null);

        ////////////////
        // Employee 1 //
        ////////////////

        // Plan request
        Request r1 = data.createPlanTemplate(0.4, data.departmentId, emp1, "3d",
                java.sql.Date.valueOf(req1StartDate), java.sql.Date.valueOf(req1EndDate), null, projectId);
        Request emp1PlanRequest = requestService.createNewRequest(r1);
        requestService.acceptRequest(emp1PlanRequest.getRequest_id(), emp1PlanRequest);

        // Plan request closest
        Request r11 = data.createPlanTemplate(0.6, data.departmentId, emp1, "3d",
                java.sql.Date.valueOf(req2StartDate), java.sql.Date.valueOf(req2EndDate), null, projectId);
        Request emp11PlanRequest = requestService.createNewRequest(r11);
        requestService.acceptRequest(emp11PlanRequest.getRequest_id(), emp11PlanRequest);

        // Fact request
        final ProjectWithRequest emp1Fact = projectWithRequestService.createFact(projectContours, null, emp1);

        ////////////////
        // Employee 2 //
        ////////////////

        // Default request
        final ProjectWithRequest emp2Default = projectWithRequestService.createRelocation(projectContours, null, emp2, data.departmentId, data.login, "some desc");

        // Plan request
        Request r4 = data.createPlanTemplate(0.4, data.departmentId, emp2, "3d",
                java.sql.Date.valueOf(req2StartDate), java.sql.Date.valueOf(req2EndDate), null, projectId);
        Request emp2PlanRequest = requestService.createNewRequest(r4);
        requestService.acceptRequest(emp2PlanRequest.getRequest_id(), emp2PlanRequest);

        // Plan request closest (already started requests has lower priority)
        Request r41 = data.createPlanTemplate(0.6, data.departmentId, emp2, "3d",
                java.sql.Date.valueOf(req3StartDate), java.sql.Date.valueOf(req3EndDate), null, projectId);
        Request emp21PlanRequest = requestService.createNewRequest(r41);
        requestService.acceptRequest(emp21PlanRequest.getRequest_id(), emp21PlanRequest);

        ////////////////
        // Employee 3 //
        ////////////////

        // Emp 3 Default request
        final ProjectWithRequest emp3Default = projectWithRequestService.createRelocation(projectContours, null, emp3, data.departmentId, data.login, "some desc");

        // without filter
        final List<HeadcountDto> withoutFilterRes = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                null,
                null);

        assertEquals(3, withoutFilterRes.size());

        /////////////////////
        // has fact filter //
        /////////////////////

        final List<HeadcountDto> hasFactFilter = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                true,
                null);

        // Only first employee has fact
        assertEquals(1, hasFactFilter.size());
        assertEquals(emp1, hasFactFilter.get(0).getEmployee());

        final Map<RequestType, ProjectWithRequest> projectWithRequests = hasFactFilter.get(0).getProjectWithRequest();
        // should be closest start date
        assertEquals(emp11PlanRequest.getRequest_id(), projectWithRequests.get(RequestType.plan).getRequest().getRequest_id());
        assertEquals(emp1Fact.getRequest().getRequest_id(), projectWithRequests.get(RequestType.fact).getRequest().getRequest_id());
        assertNull(projectWithRequests.get(RequestType.employee_relocation));

        //////////////////////////////
        // doesn't have fact filter //
        //////////////////////////////

        final List<HeadcountDto> noFactFilter = headcountService.getHeadcountsInDepartment(data.departmentId,
                null,
                null,
                false,
                null);

        assertEquals(2, noFactFilter.size());

        final Map<String, HeadcountDto> emplToHeadcountMap = noFactFilter.stream().collect(Collectors.toMap(HeadcountDto::getEmployee, v -> v));

        final Set<String> resEmployees = emplToHeadcountMap.keySet();
        // First employee has fact, so should not be presented in result
        assertFalse(resEmployees.contains(emp1));
        assertTrue(resEmployees.contains(emp2));
        assertTrue(resEmployees.contains(emp3));

        final HeadcountDto headcountDtoEmp2 = emplToHeadcountMap.get(emp2);
        final Map<RequestType, ProjectWithRequest> emp2ProjectWithRequest = headcountDtoEmp2.getProjectWithRequest();
        assertEquals(emp21PlanRequest.getRequest_id(), emp2ProjectWithRequest.get(RequestType.plan).getRequest().getRequest_id());
        assertEquals(emp2Default.getRequest().getRequest_id(), emp2ProjectWithRequest.get(RequestType.employee_relocation).getRequest().getRequest_id());
        assertNull(emp2ProjectWithRequest.get(RequestType.fact));

        final HeadcountDto headcountDtoEmp3 = emplToHeadcountMap.get(emp3);
        final Map<RequestType, ProjectWithRequest> emp3ProjectWithRequest = headcountDtoEmp3.getProjectWithRequest();
        assertEquals(emp3Default.getRequest().getRequest_id(), emp3ProjectWithRequest.get(RequestType.employee_relocation).getRequest().getRequest_id());
        assertNull(emp3ProjectWithRequest.get(RequestType.fact));
        assertNull(emp3ProjectWithRequest.get(RequestType.plan));
    }

    @Test
    public void testGetJobTicketByLogin() {
        LocalDate today = RestUtil.todayLocalDate();
        String login = "Login1";
        final HeadcountVacancy headcountVacancy = data.createHeadcountVacancy(
                1234L,
                "JOB-4321",
                Date.valueOf(today.minusDays(100)),
                Date.valueOf(today.plusDays(1)),
                "offer_accepted");
        final Headcount headcount = data.createHeadcount(
                1,
                headcountVacancy.headcountId,
                data.departmentId,
                login,
                Date.valueOf(today.minusDays(10)),
                Date.valueOf(today.minusDays(1)));

        Long id = headcountDao.getHeadcountIdByLogin(login).orElse(null);
        assertEquals(Long.valueOf(1234L), id);
        String jobTicket = headcountDao.getJobTicketByHeadcountId(id).orElse(null);
        assertEquals("JOB-4321", jobTicket);

    }

}
