package ru.yandex.market.mstat.planner.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.controller.rest.exceptions.OverloadPlanException;
import ru.yandex.market.mstat.planner.dto.request.stat.ContourRequestsStatDto;
import ru.yandex.market.mstat.planner.dto.request.stat.DepartmentRequestsStatDto;
import ru.yandex.market.mstat.planner.dto.request.stat.RequestsStatDto;
import ru.yandex.market.mstat.planner.dto.request.stat.SpecializationRequestsStatDto;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestStatus;
import ru.yandex.market.mstat.planner.model.Specialization;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.dateToStr;

public class RequestServiceTest extends AbstractDbIntegrationTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Autowired
    private RequestService requestService;

    @Test
    public void testValidationPlan() {

        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        Request r2 = data.createPlanTemplate(0.3, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        Request rPlanForAccept = data.createPlanTemplate(0.2, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request planForAccept = requestService.createNewRequest(rPlanForAccept);

        OverloadPlanException thrown = assertThrows(OverloadPlanException.class,
                () -> requestService.acceptRequest(planForAccept.getRequest_id(), planForAccept)
        );

        assertTrue(thrown.getRequestIds().containsAll(ImmutableList.<Long>builder()
                .add(plan1.getRequest_id())
                .add(plan2.getRequest_id())
                .build())
        );

        Request rPlan = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan = requestService.createNewRequest(rPlan);
        requestService.acceptRequest(plan.getRequest_id(), plan);

        // Отклоняю подтвержденный реквест.
        // Подтверждаю заново, но с загрузкой 1.
        // Ожидаю что будет ошибка валидации.
        // Это проверка на то, что валидируется приходящее с фронта значение, а не то которое уже есть в базе
        Request acceptedRequest = requestService.getRequestById(plan.getRequest_id());
        requestService.rejectRequest(acceptedRequest.getRequest_id(), acceptedRequest);
        acceptedRequest.setJob_load(BigDecimal.ONE);
        OverloadPlanException thrownAcceptTwice = assertThrows(OverloadPlanException.class,
                () -> requestService.acceptRequest(acceptedRequest.getRequest_id(), acceptedRequest)
        );
        assertTrue(thrownAcceptTwice.getRequestIds().containsAll(ImmutableList.<Long>builder()
                .add(plan1.getRequest_id())
                .add(plan2.getRequest_id())
                .build())
        );


    }


    @Test
    public void testCloseActiveRequests() {

        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        requestService.closeRequestToday(plan1, AuthInfoService.PLANNER);

        assertEquals(RequestStatus.accepted.name(), requestService.getRequestById(plan1.getRequest_id()).getStatus());
        assertEquals(requestService.getRequestById(plan1.getRequest_id()).getDate_end(), java.sql.Date.valueOf(today));

    }

    @Test
    public void testGetInRangeByDepAnyEmployee() {

        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        // создаем заявку на любого сотрудника
        Request r1 = data.createPlanTemplate(0.6, data.departmentId, "", "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);

        List<Request> requests = requestService.getInRangeByDep(data.departmentId, twoDaysAgo, todayPlusOneDay, null, null, null, null);

        assertTrue(requests.stream().map(Request::getRequest_id).collect(Collectors.toList()).contains(plan1.getRequest_id()));
        assertEquals(1, requests.size());
        assertNull(requests.get(0).getEmployee());
    }

    @Test
    public void testGetInRangeByDepWithAuthorFilter() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 5).atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        String filterEmployee = "new_employee";
        data.createEmployee(data.departmentId, filterEmployee);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, "", "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        r1.setRequest_author(filterEmployee);
        Request plan1 = requestService.createNewRequest(r1);

        // shouldn't be in result
        Request r2 = data.createPlanTemplate(0.6, data.departmentId, "", "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        r2.setRequest_author(data.login);
        Request plan2 = requestService.createNewRequest(r2);

        List<Request> requests = requestService.getInRangeByDep(
                data.departmentId,
                twoDaysAgo,
                todayPlusOneDay,
                filterEmployee,
                null,
                null,
                null);

        assertEquals(1, requests.size());
        assertEquals(plan1.getRequest_id(), requests.get(0).getRequest_id());
    }

    @Test
    public void testGetInRangeByDepWithStatusFilter() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 5).atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // shouldn't be in result
        Request r2 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);

        List<Request> requests = requestService.getInRangeByDep(
                data.departmentId,
                twoDaysAgo,
                todayPlusOneDay,
                "",
                RequestStatus.accepted,
                null,
                null);

        assertEquals(1, requests.size());
        assertEquals(plan1.getRequest_id(), requests.get(0).getRequest_id());
    }

    @Test
    public void testGetInRangeByDepWithSpecFilter() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 5).atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        final Specialization spec1 = data.createSpecialization(data.departmentId, "SPEC1", data.login);
        final Specialization spec2 = data.createSpecialization(data.departmentId, "SPEC2", data.login);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), spec1.getSpecialization_id(), projectId);
        Request plan1 = requestService.createNewRequest(r1);

        // shouldn't be in result
        Request r2 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), spec2.getSpecialization_id(), projectId);
        Request plan2 = requestService.createNewRequest(r2);

        List<Request> requests = requestService.getInRangeByDep(
                data.departmentId,
                twoDaysAgo,
                todayPlusOneDay,
                "",
                null,
                spec1.getSpecialization_id(),
                null);

        assertEquals(1, requests.size());
        assertEquals(plan1.getRequest_id(), requests.get(0).getRequest_id());
    }

    @Test
    public void testGetInRangeByDepWithContoursFilter() {

        final long contour1 = data.createTestContour(data.groupId, "contour1");
        final long contour2 = data.createTestContour(data.groupId, "contour2");

        Long project1 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour1), BigDecimal.ONE)
                        .build(),
                null);
        Long project2 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour1), BigDecimal.valueOf(0.5))
                        .put(String.valueOf(contour2), BigDecimal.valueOf(0.5))
                        .build(),
                null);
        Long project3 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour2), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 5).atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, project1);
        Request plan1 = requestService.createNewRequest(r1);

        Request r2 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, project2);
        Request plan2 = requestService.createNewRequest(r2);

        // shouldn't be in result
        Request r3 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, project3);
        Request plan3 = requestService.createNewRequest(r3);

        List<Request> requests = requestService.getInRangeByDep(
                data.departmentId,
                twoDaysAgo,
                todayPlusOneDay,
                "",
                null,
                null,
                Collections.singletonList(String.valueOf(contour1)));

        assertEquals(2, requests.size());
        assertEquals(plan1.getRequest_id(), requests.get(0).getRequest_id());
        assertEquals(plan2.getRequest_id(), requests.get(1).getRequest_id());
    }

    @Test
    public void testStatisticsBySpecializations() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 2).atStartOfDay().toLocalDate();
        LocalDate requestStart = today.minusDays(9);
        LocalDate requestEnd = today.plusDays(1);

        // target dep
        final long dep1 = data.createDepartment(1L, "", data.departmentId);
        final Specialization spec1 = data.createSpecialization(dep1, "SPEC1", data.login);

        // sub dep of target dep
        final long dep1sub = data.createDepartment(3L, "", dep1);
        final Specialization spec1Sub = data.createSpecialization(dep1sub, "SPEC1SUB", data.login);

        // out of request
        final long dep2 = data.createDepartment(2L, "", data.departmentId);
        final Specialization spec2 = data.createSpecialization(dep2, "SPEC2", data.login);

        double load1 = 0.6;
        double load2 = 0.35;
        double load22 = 0.7;
        double load3 = 0.5;

        double load4 = 1;
        double load5 = 0.7;
        double load6 = 0.8;

        ////////////////////
        // SPEC1 requests //
        ////////////////////

        // 1 day intersection, accepted
        Request r1 = data.createPlanTemplate(load1,
                spec1.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestStart.minusDays(5)),
                java.sql.Date.valueOf(requestStart),
                spec1.getSpecialization_id(),
                projectId);
        final Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // full intersection, accepted
        Request r2 = data.createPlanTemplate(load2,
                spec1.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                spec1.getSpecialization_id(),
                projectId);
        final Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // intersection, accepted
        Request r22 = data.createPlanTemplate(load22,
                spec1.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd.plusDays(3)),
                spec1.getSpecialization_id(),
                projectId);
        final Request plan22 = requestService.createNewRequest(r22);
        requestService.acceptRequest(plan22.getRequest_id(), plan22);

        // out of period, pending, shouldn't be in result
        Request r3 = data.createPlanTemplate(load3,
                spec1.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                spec1.getSpecialization_id(),
                projectId);
        final Request plan3 = requestService.createNewRequest(r3);

        ///////////////////////
        // SPEC1SUB requests //
        ///////////////////////

        // 1 day intersection at the end, rejected
        Request r4 = data.createPlanTemplate(load4,
                spec1Sub.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestEnd),
                java.sql.Date.valueOf(requestEnd.plusDays(2)),
                spec1Sub.getSpecialization_id(),
                projectId);
        final Request plan4 = requestService.createNewRequest(r4);
        requestService.rejectRequest(plan4.getRequest_id(), plan4);

        // full intersection, pending
        Request r5 = data.createPlanTemplate(load5,
                spec1Sub.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                spec1Sub.getSpecialization_id(),
                projectId);
        final Request plan5 = requestService.createNewRequest(r5);

        // intersection, deleted, shouldn't be in result
        Request r6 = data.createPlanTemplate(load6,
                spec1Sub.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestEnd.minusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                spec1Sub.getSpecialization_id(),
                projectId);
        final Request plan6 = requestService.createNewRequest(r6);
        requestService.markDeleteRequest(plan6.getRequest_id());

        ////////////////////
        // SPEC2 requests //
        ////////////////////
        // Shouldn't be in result

        // full intersection
        Request r7 = data.createPlanTemplate(load2,
                spec2.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                spec2.getSpecialization_id(),
                projectId);
        final Request plan7 = requestService.createNewRequest(r7);

        // out of period
        Request r8 = data.createPlanTemplate(load3,
                spec2.getSpecialization_department_id(),
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                spec2.getSpecialization_id(),
                projectId);
        final Request plan8 = requestService.createNewRequest(r8);

        ////////////////
        // Assertions //
        ////////////////

        final List<SpecializationRequestsStatDto> statList = requestService.getStatisticsBySpecializations(dep1, dateToStr(requestStart), dateToStr(requestEnd));

        assertEquals(2, statList.size());

        ////////////
        // Spec 1 //
        ////////////

        final SpecializationRequestsStatDto spec1Stat = statList.get(0);
        assertEquals(spec1.getSpecialization_id(), spec1Stat.getSpecId());
        assertEquals(spec1.getSpecialization_short(), spec1Stat.getSpecName());

        // Total
        assertEquals(Long.valueOf(3), spec1Stat.getRequestsStat().getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                spec1Stat.getRequestsStat().getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(3), spec1Stat.getRequestsStat().getAcceptedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                spec1Stat.getRequestsStat().getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(0), spec1Stat.getRequestsStat().getPendingCnt());
        assertEquals(
                0d,
                spec1Stat.getRequestsStat().getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(0), spec1Stat.getRequestsStat().getRejectedCnt());
        assertEquals(
                0d,
                spec1Stat.getRequestsStat().getRejectedFte(), 1E-6);

        ////////////////
        // Spec 1 sub //
        ////////////////

        final SpecializationRequestsStatDto spec1SubStat = statList.get(1);
        assertEquals(spec1Sub.getSpecialization_id(), spec1SubStat.getSpecId());
        assertEquals(spec1Sub.getSpecialization_short(), spec1SubStat.getSpecName());

        // Total
        assertEquals(Long.valueOf(2), spec1SubStat.getRequestsStat().getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4, plan5),
                spec1SubStat.getRequestsStat().getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(0), spec1SubStat.getRequestsStat().getAcceptedCnt());
        assertEquals(
                0d,
                spec1SubStat.getRequestsStat().getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(1), spec1SubStat.getRequestsStat().getPendingCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan5),
                spec1SubStat.getRequestsStat().getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(1), spec1SubStat.getRequestsStat().getRejectedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4),
                spec1SubStat.getRequestsStat().getRejectedFte(), 1E-6);
    }

    @Test
    public void testStatisticsByContours() {

        final long contour1 = data.createTestContour(data.groupId, "contour1");
        final long contour2 = data.createTestContour(data.groupId, "contour2");
        final long contour3 = data.createTestContour(data.groupId, "contour3");

        Long project1 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour1), BigDecimal.valueOf(0.5))
                        .put(String.valueOf(contour2), BigDecimal.valueOf(0.5))
                        .build(),
                null);

        Long project2 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour2), BigDecimal.ONE)
                        .build(),
                null);

        Long project3 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(contour2), BigDecimal.valueOf(0.5))
                        .put(String.valueOf(contour3), BigDecimal.valueOf(0.5))
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 2).atStartOfDay().toLocalDate();
        LocalDate requestStart = today.minusDays(9);
        LocalDate requestEnd = today.plusDays(1);

        // target dep
        final long dep1 = data.createDepartment(1L, "", data.departmentId);

        // sub dep of target dep
        final long dep1sub = data.createDepartment(3L, "", dep1);

        // out of request
        final long dep2 = data.createDepartment(2L, "", data.departmentId);

        double load1 = 0.6;
        double load2 = 0.35;
        double load22 = 0.7;
        double load3 = 0.5;

        double load4 = 1;
        double load5 = 0.7;
        double load6 = 0.8;

        ///////////////////
        // DEP1 requests //
        ///////////////////

        // 1 day intersection, accepted, CONTOUR 1,2
        Request r1 = data.createPlanTemplate(load1,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart.minusDays(5)),
                java.sql.Date.valueOf(requestStart),
                0L,
                project1);
        final Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // full intersection, accepted, CONTOUR 1,2
        Request r2 = data.createPlanTemplate(load2,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // intersection, accepted, CONTOUR 2
        Request r22 = data.createPlanTemplate(load22,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd.plusDays(3)),
                0L,
                project2);
        final Request plan22 = requestService.createNewRequest(r22);
        requestService.acceptRequest(plan22.getRequest_id(), plan22);

        // out of period, pending, shouldn't be in result
        Request r3 = data.createPlanTemplate(load3,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                0L,
                project2);
        final Request plan3 = requestService.createNewRequest(r3);

        //////////////////////
        // DEP1SUB requests //
        //////////////////////

        // 1 day intersection at the end, rejected, CONTOUR 2,3
        Request r4 = data.createPlanTemplate(load4,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd),
                java.sql.Date.valueOf(requestEnd.plusDays(2)),
                0L,
                project3);
        final Request plan4 = requestService.createNewRequest(r4);
        requestService.rejectRequest(plan4.getRequest_id(), plan4);

        // full intersection, pending, CONTOUR 2,3
        Request r5 = data.createPlanTemplate(load5,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project3);
        final Request plan5 = requestService.createNewRequest(r5);

        // intersection, deleted, shouldn't be in result
        Request r6 = data.createPlanTemplate(load6,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.minusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                0L,
                project1);
        final Request plan6 = requestService.createNewRequest(r6);
        requestService.markDeleteRequest(plan6.getRequest_id());

        ///////////////////
        // DEP2 requests //
        ///////////////////
        // Shouldn't be in result

        // full intersection
        Request r7 = data.createPlanTemplate(load2,
                dep2,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan7 = requestService.createNewRequest(r7);

        // out of period
        Request r8 = data.createPlanTemplate(load3,
                dep2,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                0L,
                project1);
        final Request plan8 = requestService.createNewRequest(r8);

        ////////////////
        // Assertions //
        ////////////////

        final List<ContourRequestsStatDto> statList = requestService.getStatisticsByContours(dep1, dateToStr(requestStart), dateToStr(requestEnd));

        assertEquals(3, statList.size());

        ///////////////
        // Contour 1 //
        ///////////////

        final ContourRequestsStatDto contour1Stat = statList.get(0);
        assertEquals(Long.valueOf(contour1), contour1Stat.getContourId());
        assertEquals("contour1", contour1Stat.getContourName());

        // Total
        assertEquals(Long.valueOf(2), contour1Stat.getRequestsStat().getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2),
                contour1Stat.getRequestsStat().getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(2), contour1Stat.getRequestsStat().getAcceptedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2),
                contour1Stat.getRequestsStat().getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(0), contour1Stat.getRequestsStat().getPendingCnt());
        assertEquals(
                0d,
                contour1Stat.getRequestsStat().getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(0), contour1Stat.getRequestsStat().getRejectedCnt());
        assertEquals(
                0d,
                contour1Stat.getRequestsStat().getRejectedFte(), 1E-6);

        ///////////////
        // Contour 2 //
        ///////////////

        final ContourRequestsStatDto contour2Stat = statList.get(1);
        assertEquals(Long.valueOf(contour2), contour2Stat.getContourId());
        assertEquals("contour2", contour2Stat.getContourName());

        // Total
        assertEquals(Long.valueOf(5), contour2Stat.getRequestsStat().getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22, plan4, plan5),
                contour2Stat.getRequestsStat().getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(3), contour2Stat.getRequestsStat().getAcceptedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                contour2Stat.getRequestsStat().getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(1), contour2Stat.getRequestsStat().getPendingCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan5),
                contour2Stat.getRequestsStat().getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(1), contour2Stat.getRequestsStat().getRejectedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4),
                contour2Stat.getRequestsStat().getRejectedFte(), 1E-6);

        ///////////////
        // Contour 3 //
        ///////////////

        final ContourRequestsStatDto contour3Stat = statList.get(2);
        assertEquals(Long.valueOf(contour3), contour3Stat.getContourId());
        assertEquals("contour3", contour3Stat.getContourName());

        // Total
        assertEquals(Long.valueOf(2), contour3Stat.getRequestsStat().getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4, plan5),
                contour3Stat.getRequestsStat().getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(0), contour3Stat.getRequestsStat().getAcceptedCnt());
        assertEquals(
                0d,
                contour3Stat.getRequestsStat().getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(1), contour3Stat.getRequestsStat().getPendingCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan5),
                contour3Stat.getRequestsStat().getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(1), contour3Stat.getRequestsStat().getRejectedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4),
                contour3Stat.getRequestsStat().getRejectedFte(), 1E-6);
    }

    @Test
    public void testStatisticsByDeps() {

        Long project1 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 2).atStartOfDay().toLocalDate();
        LocalDate requestStart = today.minusDays(9);
        LocalDate requestEnd = today.plusDays(1);

        // proto dep with no requests
        String dep0Name = "dep0";
        final long dep0 = data.createDepartment(data.departmentId, dep0Name, 10L);

        // target dep
        String dep1Name = "dep1";
        final long dep1 = data.createDepartment(1L, dep1Name, data.departmentId);

        // sub dep of target dep
        String dep1SubName = "dep1sub";
        final long dep1sub = data.createDepartment(3L, dep1SubName, dep1);

        double load1 = 0.6;
        double load2 = 0.35;
        double load22 = 0.7;
        double load3 = 0.5;

        double load4 = 1;
        double load5 = 0.7;
        double load6 = 0.8;

        ///////////////////
        // DEP1 requests //
        ///////////////////

        // 1 day intersection, accepted
        Request r1 = data.createPlanTemplate(load1,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart.minusDays(5)),
                java.sql.Date.valueOf(requestStart),
                0L,
                project1);
        final Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // full intersection, accepted
        Request r2 = data.createPlanTemplate(load2,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // intersection, accepted
        Request r22 = data.createPlanTemplate(load22,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd.plusDays(3)),
                0L,
                project1);
        final Request plan22 = requestService.createNewRequest(r22);
        requestService.acceptRequest(plan22.getRequest_id(), plan22);

        // out of period, pending, shouldn't be in result
        Request r3 = data.createPlanTemplate(load3,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                0L,
                project1);
        final Request plan3 = requestService.createNewRequest(r3);

        //////////////////////
        // DEP1SUB requests //
        //////////////////////

        // 1 day intersection at the end, rejected
        Request r4 = data.createPlanTemplate(load4,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd),
                java.sql.Date.valueOf(requestEnd.plusDays(2)),
                0L,
                project1);
        final Request plan4 = requestService.createNewRequest(r4);
        requestService.rejectRequest(plan4.getRequest_id(), plan4);

        // full intersection, pending
        Request r5 = data.createPlanTemplate(load5,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan5 = requestService.createNewRequest(r5);

        // intersection, deleted, shouldn't be in result
        Request r6 = data.createPlanTemplate(load6,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.minusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                0L,
                project1);
        final Request plan6 = requestService.createNewRequest(r6);
        requestService.markDeleteRequest(plan6.getRequest_id());

        ////////////////
        // Assertions //
        ////////////////

        final List<DepartmentRequestsStatDto> statList = requestService.getStatisticsByDepartments(data.departmentId, dateToStr(requestStart), dateToStr(requestEnd));
        assertEquals(3, statList.size());

        statList.forEach(depStat -> {
            if (depStat.getDepId() == data.departmentId) {
                ///////////
                // Dep 0 //
                ///////////

                // Total
                assertEquals(Long.valueOf(5), depStat.getRequestsStat().getTotalCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan1, plan2, plan22, plan4, plan5),
                        depStat.getRequestsStat().getTotalFte(), 1E-6);

                // Accepted
                assertEquals(Long.valueOf(3), depStat.getRequestsStat().getAcceptedCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                        depStat.getRequestsStat().getAcceptedFte(), 1E-6);

                // Pending
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getPendingCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan5),
                        depStat.getRequestsStat().getPendingFte(), 1E-6);

                // Rejected
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getRejectedCnt());
                assertEquals(
                        0d,
                        depStat.getRequestsStat().getRejectedFte(), 1E-6);

            } else if (depStat.getDepId() == dep1) {
                ///////////
                // Dep 1 //
                ///////////

                // Total
                assertEquals(Long.valueOf(5), depStat.getRequestsStat().getTotalCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan1, plan2, plan22, plan4, plan5),
                        depStat.getRequestsStat().getTotalFte(), 1E-6);

                // Accepted
                assertEquals(Long.valueOf(3), depStat.getRequestsStat().getAcceptedCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                        depStat.getRequestsStat().getAcceptedFte(), 1E-6);

                // Pending
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getPendingCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan5),
                        depStat.getRequestsStat().getPendingFte(), 1E-6);

                // Rejected
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getRejectedCnt());
                assertEquals(
                        0d,
                        depStat.getRequestsStat().getRejectedFte(), 1E-6);

            } else if (depStat.getDepId() == dep1sub) {
                ///////////////
                // Dep 1 sub //
                ///////////////

                // Total
                assertEquals(Long.valueOf(2), depStat.getRequestsStat().getTotalCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan4, plan5),
                        depStat.getRequestsStat().getTotalFte(), 1E-6);

                // Accepted
                assertEquals(Long.valueOf(0), depStat.getRequestsStat().getAcceptedCnt());
                assertEquals(
                        0d,
                        depStat.getRequestsStat().getAcceptedFte(), 1E-6);

                // Pending
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getPendingCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan5),
                        depStat.getRequestsStat().getPendingFte(), 1E-6);

                // Rejected
                assertEquals(Long.valueOf(1), depStat.getRequestsStat().getRejectedCnt());
                assertEquals(
                        calcFte(requestStart, requestEnd, plan4),
                        depStat.getRequestsStat().getRejectedFte(), 1E-6);
            }
        });
    }

    @Test
    public void testSummaryStatistics() {

        Long project1 = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.of(2021, 4, 2).atStartOfDay().toLocalDate();
        LocalDate requestStart = today.minusDays(9);
        LocalDate requestEnd = today.plusDays(1);

        // target dep
        String dep1Name = "dep1";
        final long dep1 = data.createDepartment(1L, dep1Name, data.departmentId);

        // sub dep of target dep
        String dep1SubName = "dep1sub";
        final long dep1sub = data.createDepartment(3L, dep1SubName, dep1);

        // out of request
        String dep2Name = "dep2";
        final long dep2 = data.createDepartment(2L, dep2Name, data.departmentId);

        double load1 = 0.6;
        double load2 = 0.35;
        double load22 = 0.7;
        double load3 = 0.5;

        double load4 = 1;
        double load5 = 0.7;
        double load6 = 0.8;

        ///////////////////
        // DEP1 requests //
        ///////////////////

        // 1 day intersection, accepted
        Request r1 = data.createPlanTemplate(load1,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart.minusDays(5)),
                java.sql.Date.valueOf(requestStart),
                0L,
                project1);
        final Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // full intersection, accepted
        Request r2 = data.createPlanTemplate(load2,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // intersection, accepted
        Request r22 = data.createPlanTemplate(load22,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd.plusDays(3)),
                0L,
                project1);
        final Request plan22 = requestService.createNewRequest(r22);
        requestService.acceptRequest(plan22.getRequest_id(), plan22);

        // out of period, pending, shouldn't be in result
        Request r3 = data.createPlanTemplate(load3,
                dep1,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                0L,
                project1);
        final Request plan3 = requestService.createNewRequest(r3);

        //////////////////////
        // DEP1SUB requests //
        //////////////////////

        // 1 day intersection at the end, rejected
        Request r4 = data.createPlanTemplate(load4,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd),
                java.sql.Date.valueOf(requestEnd.plusDays(2)),
                0L,
                project1);
        final Request plan4 = requestService.createNewRequest(r4);
        requestService.rejectRequest(plan4.getRequest_id(), plan4);

        // full intersection, pending
        Request r5 = data.createPlanTemplate(load5,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan5 = requestService.createNewRequest(r5);

        // intersection, deleted, shouldn't be in result
        Request r6 = data.createPlanTemplate(load6,
                dep1sub,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.minusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                0L,
                project1);
        final Request plan6 = requestService.createNewRequest(r6);
        requestService.markDeleteRequest(plan6.getRequest_id());

        ///////////////////
        // DEP2 requests //
        ///////////////////
        // Shouldn't be in result

        // full intersection
        Request r7 = data.createPlanTemplate(load2,
                dep2,
                "",
                "",
                java.sql.Date.valueOf(requestStart),
                java.sql.Date.valueOf(requestEnd),
                0L,
                project1);
        final Request plan7 = requestService.createNewRequest(r7);

        // out of period
        Request r8 = data.createPlanTemplate(load3,
                dep2,
                "",
                "",
                java.sql.Date.valueOf(requestEnd.plusDays(4)),
                java.sql.Date.valueOf(requestEnd.plusDays(6)),
                0L,
                project1);
        final Request plan8 = requestService.createNewRequest(r8);

        ////////////////
        // Assertions //
        ////////////////

        final RequestsStatDto summaryStat = requestService.getSummaryStatistics(dep1, dateToStr(requestStart), dateToStr(requestEnd));

        // Total
        assertEquals(Long.valueOf(5), summaryStat.getTotalCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22, plan4, plan5),
                summaryStat.getTotalFte(), 1E-6);

        // Accepted
        assertEquals(Long.valueOf(3), summaryStat.getAcceptedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan1, plan2, plan22),
                summaryStat.getAcceptedFte(), 1E-6);

        // Pending
        assertEquals(Long.valueOf(1), summaryStat.getPendingCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan5),
                summaryStat.getPendingFte(), 1E-6);

        // Rejected
        assertEquals(Long.valueOf(1), summaryStat.getRejectedCnt());
        assertEquals(
                calcFte(requestStart, requestEnd, plan4),
                summaryStat.getRejectedFte(), 1E-6);
    }

    private static double calcFte(LocalDate periodFrom, LocalDate periodTo, Request... requests) {
        double loadSum = 0;
        for (Request request : requests) {
            loadSum += request.getJob_load().doubleValue() *
                    Duration.between(
                            localDateToInstant(maxOfDates(request.getDate_start().toLocalDate(), periodFrom)),
                            localDateToInstant(minOfDates(request.getDate_end().toLocalDate(), periodTo)))
                            .toDays();
        }
        return loadSum / daysBetween(periodFrom, periodTo);
    }

    private static long daysBetween(LocalDate from, LocalDate to) {
        return Duration.between(
                localDateToInstant(from),
                localDateToInstant(to)
        ).toDays();
    }

    private static LocalDate maxOfDates(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.isAfter(secondDate) ? firstDate : secondDate;
    }

    private static LocalDate minOfDates(LocalDate firstDate, LocalDate secondDate) {
        return firstDate.isBefore(secondDate) ? firstDate : secondDate;
    }

    private static Instant localDateToInstant(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Test
    public void testJobLoadCheck_Simple() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        Request r2 = data.createPlanTemplate(0.3, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // Here will be 100% job load - should be OK
        Request r3 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan3 = requestService.createNewRequest(r3);
        requestService.acceptRequest(plan3.getRequest_id(), plan3);

        // Here job load should be exceed, but this request is not accepted, so it doesn't affect on result
        Request r4 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan4 = requestService.createNewRequest(r4);

        // Here we expect, that job load will exceed on 10%, so exception should be thrown
        exceptionRule.expect(OverloadPlanException.class);

        Request r5 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan5 = requestService.createNewRequest(r5);
        requestService.acceptRequest(plan5.getRequest_id(), plan5);
    }

    @Test
    public void testJobLoadCheck_DifferentIntervals() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        // First interval
        LocalDate start1 = today.minusDays(5);
        LocalDate end1 = today.minusDays(3);

        Request r1 = data.createPlanTemplate(1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start1), java.sql.Date.valueOf(end1), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // Second interval, starts as soon as finished first, so should not be exceed
        LocalDate start2 = today.minusDays(2);
        LocalDate end2 = today.minusDays(1);

        Request r2 = data.createPlanTemplate(0.3, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start2), java.sql.Date.valueOf(end2), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // Third interval, intersects with second interval, but not exceed
        LocalDate start3 = today.minusDays(1);
        LocalDate end3 = today.plusDays(1);

        Request r3 = data.createPlanTemplate(0.7, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start3), java.sql.Date.valueOf(end3), null, projectId);
        Request plan3 = requestService.createNewRequest(r3);
        requestService.acceptRequest(plan3.getRequest_id(), plan3);

        // Fourth interval, intersects with third interval and exceeds job load on this intersection
        LocalDate start4 = today;
        LocalDate end4 = today.plusDays(3);

        // Here we expect, that job load will exceed on 10%, so exception should be thrown
        exceptionRule.expect(OverloadPlanException.class);

        Request r4 = data.createPlanTemplate(0.4, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start4), java.sql.Date.valueOf(end4), null, projectId);
        Request plan4 = requestService.createNewRequest(r4);
        requestService.acceptRequest(plan4.getRequest_id(), plan4);
    }

    @Test
    public void testJobLoadCheck_SingleDayRequest() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        // First interval
        LocalDate start1 = today.minusDays(5);
        LocalDate end1 = today.minusDays(5);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start1), java.sql.Date.valueOf(end1), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        // Second interval, starts as soon as finished first, so should not be exceed
        LocalDate start2 = today.minusDays(5);
        LocalDate end2 = today.minusDays(5);

        Request r2 = data.createPlanTemplate(0.3, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start2), java.sql.Date.valueOf(end2), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // Second interval, starts as soon as finished first, so should not be exceed
        LocalDate start3 = today.minusDays(5);
        LocalDate end3 = today.minusDays(5);

        exceptionRule.expect(OverloadPlanException.class);

        Request r3 = data.createPlanTemplate(0.2, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(start3), java.sql.Date.valueOf(end3), null, projectId);
        Request plan3 = requestService.createNewRequest(r3);
        requestService.acceptRequest(plan3.getRequest_id(), plan3);
    }

    @Test
    public void testJobLoadCheck_DifferentReqType() {
        Long projectId = data.createProject(ImmutableMap.<String, BigDecimal>builder()
                        .put(String.valueOf(data.contourId), BigDecimal.ONE)
                        .build(),
                null);

        LocalDate today = LocalDate.now().atStartOfDay().toLocalDate();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate todayPlusOneDay = today.plusDays(1);

        Request r1 = data.createPlanTemplate(0.6, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan1 = requestService.createNewRequest(r1);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        Request r2 = data.createPlanTemplate(0.3, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan2 = requestService.createNewRequest(r2);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        // Here will be 100% job load - should be OK
        Request r3 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan3 = requestService.createNewRequest(r3);
        requestService.acceptRequest(plan3.getRequest_id(), plan3);

        // Here job load should be exceed, but this request is not accepted, so it doesn't affect on result
        Request r4 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        r4.setType("employee_relocation");
        Request plan4 = requestService.createNewRequest(r4);
        requestService.acceptRequest(plan4.getRequest_id(), plan4);

        // Here we expect, that job load will exceed on 10%, so exception should be thrown
        exceptionRule.expect(OverloadPlanException.class);

        Request r5 = data.createPlanTemplate(0.1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(twoDaysAgo), java.sql.Date.valueOf(todayPlusOneDay), null, projectId);
        Request plan5 = requestService.createNewRequest(r5);
        requestService.acceptRequest(plan5.getRequest_id(), plan5);
    }
}
