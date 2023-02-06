package ru.yandex.market.mstat.planner.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.Project;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestStatus;
import ru.yandex.market.mstat.planner.model.Specialization;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RequestService requestService;

    @Test
    public void testProjectServiceByDepartment() {

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);
        LocalDate todayPlusFourDays = today.plusDays(4);

        Specialization spec = data.createSpecialization(data.departmentId, "SPEC", data.login);
        data.createEmployee(data.departmentId, "test");
        Specialization badSpec = data.createSpecialization(data.departmentId, "SPEC2", "test");

        long dep2 = data.createDepartment(118L, "Важнейший департамент", data.parentDepartmentId);

        Map<String, BigDecimal> contours1 = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(data.contourId), BigDecimal.ONE)
                .build();

        long contourId2 = data.createTestContour(data.groupId, "Новый контур");
        Map<String, BigDecimal> contours2 = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(contourId2), BigDecimal.ONE)
                .build();

        Long projectId1 = data.createProject(contours1, null);
        Long projectId2 = data.createProject(contours2, null);

        Request r1 = data.createPlanTemplate(1, data.departmentId, data.login, "2d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(today), spec.getSpecialization_id(), projectId1);

        Request r2 = data.createPlanTemplate(1, dep2, data.login, "3d",
                java.sql.Date.valueOf(todayPlusOneDay), java.sql.Date.valueOf(todayPlusFourDays), null, projectId2);

        Request plan1 = requestService.createNewRequest(r1);
        Request plan2 = requestService.createNewRequest(r2);

        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        Map<Long, Project> projectMap;

        projectMap = projectService.byDepartment(data.departmentId, twoDaysAgo, today, "mstattest",
                RequestStatus.accepted, spec.getSpecialization_id(),
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build());
        assertEquals(1, projectMap.keySet().size());
        assertTrue(projectMap.containsKey(projectId1));

        projectMap = projectService.byDepartment(data.departmentId, twoDaysAgo, today, "mstattest",
                RequestStatus.accepted, null,
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build());
        assertEquals(1, projectMap.keySet().size());
        assertTrue(projectMap.containsKey(projectId1));

        projectMap = projectService.byDepartment(dep2, todayPlusOneDay, todayPlusFourDays, "mstattest",
                RequestStatus.pending, null,
                ImmutableList.<String>builder().add(String.valueOf(contourId2)).build());
        assertEquals(1, projectMap.keySet().size());
        assertTrue(projectMap.containsKey(projectId2));

        projectMap = projectService.byDepartment(dep2, todayPlusOneDay, todayPlusFourDays, "mstattest",
                RequestStatus.accepted, null,
                ImmutableList.<String>builder().add(String.valueOf(contourId2)).build());
        assertEquals(0, projectMap.keySet().size());

        projectMap = projectService.byDepartment(data.departmentId, todayPlusOneDay, todayPlusFourDays, "mstattest",
                RequestStatus.accepted, spec.getSpecialization_id(),
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build());
        assertEquals(0, projectMap.keySet().size());

        projectMap = projectService.byDepartment(dep2, todayPlusOneDay, todayPlusFourDays, "mstattest",
                RequestStatus.pending, null,
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build());
        assertEquals(0, projectMap.keySet().size());

        projectMap = projectService.byDepartment(data.departmentId, twoDaysAgo, today, "mstattest",
                RequestStatus.accepted, badSpec.getSpecialization_id(),
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build());
        assertEquals(0, projectMap.keySet().size());
    }

    @Test
    public void testProjectsCurrentUser() {

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);
        LocalDate todayPlusFourDays = today.plusDays(4);
        LocalDate todayPlusFiveDays = today.plusDays(5);
        LocalDate todayPlusEightDays = today.plusDays(8);

        Specialization spec = data.createSpecialization(data.departmentId, "SPEC", data.login);

        Map<String, BigDecimal> contours1 = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(data.contourId), BigDecimal.ONE)
                .build();

        long contourId2 = data.createTestContour(data.groupId, "Новый контур");
        Map<String, BigDecimal> contours2 = ImmutableMap.<String, BigDecimal>builder()
                .put(String.valueOf(contourId2), BigDecimal.ONE)
                .build();

        Long projectId1 = data.createProject(contours1, null);
        Long projectId2 = data.createProject(contours2, null);

        Request r1 = data.createPlanTemplate(1, data.departmentId, data.login, "2d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(today), spec.getSpecialization_id(), projectId1);

        Request r2 = data.createPlanTemplate(1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(todayPlusOneDay), java.sql.Date.valueOf(todayPlusFourDays), null, projectId2);

        Request r3 = data.createPlanTemplate(1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(todayPlusFiveDays), java.sql.Date.valueOf(todayPlusEightDays), null, projectId2);

        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);
        Request plan2 = requestService.createNewRequest(r2);
        Request plan3 = requestService.createNewRequest(r3);
        requestService.acceptRequest(plan3.getRequest_id(), plan3);

        SortedMap<Long, Project> resultProjects;

        resultProjects = projectService.projectsCurrentDepartment(data.departmentId, twoDaysAgo, todayPlusEightDays,
                "mstattest", null, null, null);
        assertEquals(2, resultProjects.entrySet().size());
        assertTrue(resultProjects.containsKey(projectId1));
        assertTrue(resultProjects.containsKey(projectId2));
        List<Long> requestProject2 = resultProjects.get(projectId2).getRequests()
                .stream().map(Request::getRequest_id).collect(Collectors.toList());
        assertTrue(requestProject2.contains(r2.getRequest_id()));
        assertTrue(requestProject2.contains(r3.getRequest_id()));
        assertTrue(resultProjects.get(projectId1).getRequests()
                .stream().map(Request::getRequest_id).collect(Collectors.toList())
                .contains(r1.getRequest_id())
        );

        resultProjects = projectService.projectsCurrentDepartment(data.departmentId, twoDaysAgo, todayPlusFourDays,
                "mstattest", null, null, null);
        requestProject2 = resultProjects.get(projectId2).getRequests()
                .stream().map(Request::getRequest_id).collect(Collectors.toList());
        assertTrue(requestProject2.contains(r2.getRequest_id()));
        assertFalse(requestProject2.contains(r3.getRequest_id()));

        resultProjects = projectService.projectsCurrentDepartment(data.departmentId, twoDaysAgo, todayPlusEightDays,
                "mstattest", null, spec.getSpecialization_id(),
                ImmutableList.<String>builder().add(String.valueOf(data.contourId)).build()
        );
        assertEquals(1, resultProjects.entrySet().size());
        assertTrue(resultProjects.containsKey(projectId1));
        assertFalse(resultProjects.containsKey(projectId2));

        resultProjects = projectService.projectsCurrentDepartment(data.departmentId, twoDaysAgo, todayPlusEightDays,
                "mstattest", RequestStatus.accepted, null, null);
        assertEquals(2, resultProjects.entrySet().size());
        assertTrue(resultProjects.containsKey(projectId1));
        assertTrue(resultProjects.containsKey(projectId2));
        requestProject2 = resultProjects.get(projectId2).getRequests()
                .stream().map(Request::getRequest_id).collect(Collectors.toList());
        assertTrue(requestProject2.contains(r3.getRequest_id()));
        assertTrue(resultProjects.get(projectId1).getRequests()
                .stream().map(Request::getRequest_id).collect(Collectors.toList()).contains(r1.getRequest_id()));

    }

}
