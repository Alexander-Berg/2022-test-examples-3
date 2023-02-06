package ru.yandex.market.mstat.planner.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.dao.BusinessUnitDao;
import ru.yandex.market.mstat.planner.dao.ContourDao;
import ru.yandex.market.mstat.planner.dao.ContourTagDao;
import ru.yandex.market.mstat.planner.dao.ReportDao;
import ru.yandex.market.mstat.planner.dto.ContourDto;
import ru.yandex.market.mstat.planner.dto.ContourGroupDto;
import ru.yandex.market.mstat.planner.dto.ContourGroupLiteDto;
import ru.yandex.market.mstat.planner.dto.ContourGroupReportDto;
import ru.yandex.market.mstat.planner.dto.ContourRequest;
import ru.yandex.market.mstat.planner.dto.RequestDto;
import ru.yandex.market.mstat.planner.model.BusinessUnit;
import ru.yandex.market.mstat.planner.model.Contour;
import ru.yandex.market.mstat.planner.model.ContourChat;
import ru.yandex.market.mstat.planner.model.ContourGroup;
import ru.yandex.market.mstat.planner.model.Department;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestType;
import ru.yandex.market.mstat.planner.task.cron.JobPeriodsCalculator;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.dateToStr;
import static ru.yandex.market.mstat.planner.util.RestUtil.today;
import static ru.yandex.market.mstat.planner.util.RestUtil.todayLocalDate;

public class ContourServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private ContourService contourService;

    @Autowired
    private ContourTagDao contourTagDao;

    @Autowired
    private ContourDao contourDao;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ContourChatsService contourChatsService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private BusinessUnitDao businessUnitDao;

    @Autowired
    private JobPeriodsCalculator jobPeriodsCalculator;

    @Test
    public void testGetContoursWithTags() {
        Map<Long, Contour> contourMap = contourService.getContours();

        assertTrue(contourMap.containsKey(data.contourId));
        assertTrue(contourMap.get(data.contourId).getTags().containsKey(data.tgId));
        assertEquals("Тестовый тег", contourMap.get(data.contourId).getTags().get(data.tgId));
    }

    @Test
    public void testGetContourWithTags() {
        long tgIdWrong = contourTagDao.addTag("Неправильный тег");

        Contour getContour = contourService.getContour(data.contourId);

        assertTrue(getContour.getTags().containsKey(data.tgId));
        assertFalse(getContour.getTags().containsKey(tgIdWrong));
        assertEquals("Тестовый тег", getContour.getTags().get(data.tgId));
    }

    @Test
    public void testDeleteContourWithTag() {
        contourService.deleteContour(data.contourId);

        assertTrue(contourTagDao.getTagsByContour(data.contourId).isEmpty());
    }

    @Test
    public void testGetContourEmployees() {

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.ONE);

        Map<String, BigDecimal> contours = new HashMap<>();
        contours.put(String.valueOf(data.contourId), BigDecimal.ONE);

        Date nextMonth = java.sql.Date.valueOf(today().toLocalDate().plusMonths(1));
        Date monthAgo = java.sql.Date.valueOf(today().toLocalDate().minusMonths(1));

        // create this month fact
        ProjectWithRequest fact = projectWithRequestService.createFact(contours, colours, data.login);
        Request factRequest = requestService.getRequestById(fact.getRequest().getRequest_id());

        // create relocation
        ProjectWithRequest relocation = projectWithRequestService.createRelocation(contours, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        Request relocationRequest = requestService.getRequestById(relocation.getRequest().getRequest_id());

        jobPeriodsCalculator.execute();

        List<RequestDto> result = contourService.getContourStructure(data.contourId, dateToStr(monthAgo), dateToStr(nextMonth));

        assertTrue(result.size() == 2);
        assertEquals(result.get(0).getRequest().getRequest_id(), factRequest.getRequest_id());
        assertEquals(result.get(0).getPeriodStart(), factRequest.getDate_start());
        assertEquals(result.get(0).getPeriodEnd(), factRequest.getDate_end());
        assertEquals(result.get(1).getRequest().getRequest_id(), relocationRequest.getRequest_id());
        assertEquals(result.get(1).getPeriodStart(), java.sql.Date.valueOf(factRequest.getDate_end().toLocalDate().plusDays(1)));
        assertEquals(result.get(1).getPeriodEnd(), java.sql.Date.valueOf(nextMonth.toLocalDate()));
    }

    @Test
    public void testCalculatePlanAndDefaultJobLoad() {

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.ONE);

        Map<String, BigDecimal> contours = new HashMap<>();
        contours.put(String.valueOf(data.contourId), BigDecimal.ONE);

        // create relocation
        ProjectWithRequest relocation = projectWithRequestService.createRelocation(contours, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        Request relocationRequest = requestService.getRequestById(relocation.getRequest().getRequest_id());

        //create two plan requests
        double load1 = 0.2;
        double load2 = 0.4;
        long dep = data.departmentId;
        String login = data.login;
        String sizeRequest = "1m";
        java.sql.Date startDate = java.sql.Date.valueOf(today().toLocalDate().minusDays(2));
        java.sql.Date endDate = java.sql.Date.valueOf(today().toLocalDate().plusDays(4));

        Long projectId = data.createProject(contours, null);

        Request r1 = data.createPlanTemplate(load1, dep, login, sizeRequest, startDate, endDate, null, projectId);
        Request r2 = data.createPlanTemplate(load2, dep, login, sizeRequest, startDate, endDate, null, projectId);

        Request plan1 = requestService.createNewRequest(r1);
        Request plan2 = requestService.createNewRequest(r2);

        requestService.acceptRequest(plan1.getRequest_id(), plan1);
        requestService.acceptRequest(plan2.getRequest_id(), plan2);

        jobPeriodsCalculator.execute();

        // test getContourEmployees
        List<RequestDto> result1 = contourService.getContourStructure(data.contourId, dateToStr(startDate), dateToStr(today().toLocalDate().minusDays(1)));
        List<RequestDto> result2 = contourService.getContourStructure(data.contourId, dateToStr(today()), dateToStr(endDate));
        List<RequestDto> fullResult = contourService.getContourStructure(data.contourId, dateToStr(startDate), dateToStr(endDate));

        assertEquals(2, result1.size());
        assertEquals(3, result2.size());
        assertEquals(3, fullResult.size());

        for (RequestDto r : fullResult) {
            if (r.getRequest().getType().equals(RequestType.employee_relocation.name())) {
                assertEquals(0, r.getJobLoad().compareTo(BigDecimal.valueOf(1 - load1 - load2)));
            }
        }
    }

    @Test
    public void testCalculateResultJobLoadAndFte() {

        String login2 = "TestEmp";
        data.createEmployee(data.departmentId, login2);

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.ONE);

        Map<String, BigDecimal> oneContour =
                ImmutableMap.<String, BigDecimal>builder().put(String.valueOf(data.contourId), BigDecimal.valueOf(1)).build();

        long contourId2 = data.createTestContour(data.groupId, "newContour");

        double half = 0.5;
        Map<String, BigDecimal> contours = new HashMap<>();
        contours.put(String.valueOf(data.contourId), BigDecimal.valueOf(half));
        contours.put(String.valueOf(contourId2), BigDecimal.valueOf(half));

        int dayFromPlan = 2;
        int dayToEndPlan = 4;
        int dayRequestTo = 5;
        java.sql.Date startDate = java.sql.Date.valueOf(today().toLocalDate().minusDays(dayFromPlan));
        java.sql.Date endDate = java.sql.Date.valueOf(today().toLocalDate().plusDays(dayToEndPlan));
        java.sql.Date finalDate = java.sql.Date.valueOf(today().toLocalDate().plusDays(dayRequestTo));

        ProjectWithRequest relocation1 = projectWithRequestService.createRelocation(contours, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        ProjectWithRequest relocation2 = projectWithRequestService.createRelocation(oneContour, colours, login2, data.departmentId, AuthInfoService.PLANNER, null);

        //create plan request
        double load = 0.2;
        Long projectId = data.createProject(oneContour, null);
        Request r = data.createPlanTemplate(load, data.departmentId, data.login, "6d", startDate, endDate, null, projectId);
        Request plan = requestService.createNewRequest(r);
        requestService.acceptRequest(plan.getRequest_id(), plan);

        /*
        Табличка для лучшего понимания как происходят расчеты.
        Обе временных границы запроса inclusive. Поэтому везде +1.
        Запрос за 8 дней. Должны получать вот такие RequestDto:
        || emp | type | job_load | period ||
        || data.login | plan | 0.2 | start-end 7d ||
        || data.login | default | (1-0.2) * 0.5 = 0.4 | today-end 5d ||
        || data.login | default | 0.5 | end-final 1d ||
        (0.2*2+0.4*5+0.2*5+0.5*1)/8 = 0.4875 - результирующая загрузка data.login за 8 дней
        || login2 | default |1 | today-final 6d ||
        (1*6)/8=0.75 - результирующая загрузка login2 за 8 дней
         */
        double defaultLoad = 1;
        double resultJobLoad1 = (load * (dayFromPlan + dayToEndPlan + 1)
                + (defaultLoad - load) * half * (dayToEndPlan + 1)
                + half * (dayRequestTo - dayToEndPlan))
                / (dayRequestTo + dayFromPlan + 1);
        double resultJobLoad2 = (defaultLoad * (dayRequestTo + 1)) / (dayRequestTo + dayFromPlan + 1);
        double sumJobLoad = resultJobLoad1 + resultJobLoad2;
        double resultContour2 = ((defaultLoad - load) * half * (dayToEndPlan + 1)
                + half * (dayRequestTo - dayToEndPlan)) / (dayRequestTo + dayFromPlan + 1);

        jobPeriodsCalculator.execute();

        List<RequestDto> result = contourService.getContourStructure(data.contourId, dateToStr(startDate), dateToStr(finalDate));

        assertEquals(4, result.size());
        for (RequestDto req : result) {
            if (req.getEmployee().getLogin().equals(data.login)) {
                assertEquals(resultJobLoad1, req.getResultEmployeeLoad().doubleValue(), 1E-6);
            }
            if (req.getEmployee().getLogin().equals(login2)) {
                assertEquals(resultJobLoad2, req.getResultEmployeeLoad().doubleValue(), 1E-6);
            }
        }

        List<ContourDto> groupResult = contourService.getContourGroupStructure(data.groupId, dateToStr(startDate), dateToStr(finalDate), null);
        assertEquals(2, groupResult.size());
        for (ContourDto contour : groupResult) {
            if (contour.getId() == data.contourId) {
                assertEquals(sumJobLoad, contour.getSize().doubleValue(), 1E-6);
            }
            if (contour.getId() == contourId2) {
                assertEquals(resultContour2, contour.getSize().doubleValue(), 1E-6);
            }
        }
    }

    @Test
    public void testGetContourEmployeesWithBusyDays() {

        Map<String, BigDecimal> contours = new HashMap<>();
        contours.put(String.valueOf(data.contourId), BigDecimal.ONE);

        ProjectWithRequest reloc = projectWithRequestService.createRelocation(contours, null, data.login,
                null, AuthInfoService.PLANNER, null);
        Request relocRequest = requestService.getRequestById(reloc.getRequest().getRequest_id());

        long startDate = 2;
        long endDate = 4;
        data.createTestDuty(1, 1, data.login, todayLocalDate().plusDays(startDate), todayLocalDate().plusDays(endDate));

        jobPeriodsCalculator.execute();

        long period = 10;
        List<RequestDto> result = contourService.getContourStructure(data.contourId, dateToStr(todayLocalDate()), dateToStr(todayLocalDate().plusDays(period)));

        assertEquals(1, result.size());
        RequestDto requestDto = result.get(0);
        assertEquals(Long.valueOf(endDate-startDate), requestDto.getBusyDays());
        assertEquals((double) (period + 1 - (endDate - startDate)) / (double) (period + 1),
                requestDto.getCleanLoad().doubleValue(), 1E-6);

    }

    @Test
    public void getGroupDto() {

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours = new HashMap<>();
        contours.put(String.valueOf(data.contourId), BigDecimal.ONE);

        Date nextMonth = java.sql.Date.valueOf(today().toLocalDate().plusMonths(1));
        Date monthAgo = java.sql.Date.valueOf(today().toLocalDate().minusMonths(1));

        ProjectWithRequest relocation = projectWithRequestService.createRelocation(contours, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);

        jobPeriodsCalculator.execute();

        ContourGroupDto groupDto = contourService.getGroupDto(data.groupId, dateToStr(today()), dateToStr(nextMonth), null);

        assertEquals(1, groupDto.getContours().size());
        assertEquals(data.contourId, groupDto.getContours().get(0).getId());
        assertEquals(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP), groupDto.getContours().get(0).getSize());
        assertEquals(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP), groupDto.getSize());
    }

    @Test
    public void getContourGroupsDto() {

        LocalDate nextMonth = today().toLocalDate().plusMonths(1);
        LocalDate monthAgo = today().toLocalDate().minusMonths(1);

        long newDep = data.createDepartment(118L, "Важнейший департамент",1694L);
        String login = "newEmployee";
        data.createEmployee(newDep, login);

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours1 = new HashMap<>();
        contours1.put(String.valueOf(data.contourId), BigDecimal.ONE);

        long contour2Id = data.createTestContour(data.groupId, "newTestContour");
        Map<String, BigDecimal> contours2 = new HashMap<>();
        contours2.put(String.valueOf(contour2Id), BigDecimal.ONE);

        ProjectWithRequest relocation1 = projectWithRequestService.createRelocation(contours1, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        ProjectWithRequest relocation2 = projectWithRequestService.createRelocation(contours2, colours, login, data.departmentId, AuthInfoService.PLANNER, null);

        jobPeriodsCalculator.execute();

        List<ContourGroupDto> listGroupDtoWithFilter = contourService.getContourGroupsDto(today().toLocalDate(), nextMonth, null, data.departmentId, null);
        listGroupDtoWithFilter = contourService.filterEmptyContours(listGroupDtoWithFilter);
        List<ContourGroupDto> listGroupDto = contourService.getContourGroupsDto(today().toLocalDate(), nextMonth) ;

        assertEquals(1, listGroupDto.size());
        assertEquals(2, listGroupDto.get(0).getContours().size());
        assertEquals(1, listGroupDtoWithFilter.get(0).getContours().size());
        assertEquals(data.contourId, listGroupDtoWithFilter.get(0).getContours().get(0).getId());

        List<Long> listIds = listGroupDto.get(0).getContours().stream().map(ContourDto::getId).collect(Collectors.toList());
        assertTrue(listIds.contains(data.contourId));
        assertTrue(listIds.contains(contour2Id));

//        testing filtering groups by BU
        Long group1Id = data.createTestGroup("Group 1");
        Long group2Id = data.createTestGroup("Group 2");
        data.setBusinessUnitGroup(group1Id, Set.of(1L));
        data.setBusinessUnitGroup(group2Id, Set.of(34L));
        data.setBusinessUnitGroup(data.groupId, Set.of(34L));
        List<ContourGroupDto> cgList = contourService.getContourGroupsDto(today().toLocalDate(), nextMonth, 1L, null, null);
        assertEquals(1, cgList.size());
        assertEquals("Group 1", cgList.get(0).getName());


    }

    @Test
    public void contourBusinessUnit() {
        BusinessUnit bu = businessUnitDao.getAllBusinessUnits().get(0);
        Contour contour = contourService.getContour(data.contourId);
        contour.setBusinessUnitIds(null);

        contour.setTarget("Target");
        contour.setDescription("Description");
        contour.setBusinessUnitIds(singleton(bu.getId()));
        contour.setColors(ImmutableMap.<Long, Double>builder().put(data.colorId, (double) 1).build());
        ContourRequest cr = new ContourRequest();
        cr.setContour(contour);
        contourService.updateContour(contour.getId(), cr);

        contour = contourService.getContour(data.contourId);

        assertTrue(contour.getBusinessUnitIds().contains(bu.getId()));

        List<Contour> contours = contourService.getContoursByBusinessUnit(bu.getId());
        assertEquals(contours.get(0).getId(), contour.getId());
    }

    @Test
    public void testGetContourGroupReport() {

        LocalDate nextMonth = today().toLocalDate().plusMonths(1);
        LocalDate monthAgo = today().toLocalDate().minusMonths(1);

        Map<String, BigDecimal> colours = new HashMap<>();
        colours.put("w", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours1 = new HashMap<>();
        contours1.put(String.valueOf(data.contourId), BigDecimal.ONE);

        long contour2Id = data.createTestContour(data.groupId, "newTestContour");
        Map<String, BigDecimal> contours2 = new HashMap<>();
        contours2.put(String.valueOf(contour2Id), BigDecimal.ONE);

        ProjectWithRequest relocation1 = projectWithRequestService.createRelocation(contours1, colours, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        ProjectWithRequest pwd = projectWithRequestService.createFact(contours2, colours, data.login);

        Long projectId1 = data.createProject(contours1, null);
        Request rPlan = data.createPlanTemplate(1, data.departmentId, data.login, "3d",
                java.sql.Date.valueOf(monthAgo), java.sql.Date.valueOf(nextMonth), null, projectId1);
        Request plan1 = requestService.createNewRequest(rPlan);
        requestService.acceptRequest(plan1.getRequest_id(), plan1);

        jobPeriodsCalculator.execute();

        List<ContourGroupReportDto> contourGroupReportDtoList = reportDao.getContourGroupReport(monthAgo, nextMonth,
                singletonList(data.departmentId), null, singletonList(data.groupId));

        assertEquals(contourGroupReportDtoList.size(), 2);

        List<String> names = contourGroupReportDtoList.stream().map(ContourGroupReportDto::getContourName).collect(Collectors.toList());
        assertTrue(names.contains(contourService.getContour(contour2Id).getName()));
        assertTrue(names.contains(contourService.getContour(data.contourId).getName()));

    }

    @Test
    public void testGetContourGroupsWithContoursGroupedByGroupId() {
        final long group1Id = data.createTestGroup("group1");
        final long group2Id = data.createTestGroup("group2");
        final long group3Id = data.createTestGroup("group3");

        final long g1c1 = data.createTestContour(group1Id, "g1c1");
        final long g1c2 = data.createTestContour(group1Id, "g1c2");

        final long g2c1 = data.createTestContour(group2Id, "g2c1");
        final long g2c2 = data.createTestContour(group2Id, "g2c2");

        final long g3c1 = data.createTestContour(group3Id, "g3c1");
        final long g3c2 = data.createTestContour(group3Id, "g3c2");

        contourService.deleteContour(g3c1);
        contourService.deleteContour(g3c2);
        contourService.deleteContourGroup(group3Id);

        final Map<Long, ContourGroupLiteDto> contoursByGroupId = contourService.getContourGroupsWithContoursGroupedByGroupId();

        final ContourGroupLiteDto contourGroupLiteDto1 = contoursByGroupId.get(group1Id);
        assertEquals(2, contourGroupLiteDto1.getContours().size());
        final List<Long> contoursG1 = contourGroupLiteDto1.getContours().stream().map(Contour::getId).collect(Collectors.toList());
        assertTrue(contoursG1.contains(g1c1));
        assertTrue(contoursG1.contains(g1c2));

        final ContourGroupLiteDto contourGroupLiteDto2 = contoursByGroupId.get(group2Id);
        assertEquals(2, contourGroupLiteDto2.getContours().size());
        final List<Long> contoursG2 = contourGroupLiteDto2.getContours().stream().map(Contour::getId).collect(Collectors.toList());
        assertTrue(contoursG2.contains(g2c1));
        assertTrue(contoursG2.contains(g2c2));

        // third group was deleted, but it should be present in result
        final ContourGroupLiteDto contourGroupLiteDto3 = contoursByGroupId.get(group3Id);
        assertEquals(2, contourGroupLiteDto3.getContours().size());
        final List<Long> contoursG3 = contourGroupLiteDto3.getContours().stream().map(Contour::getId).collect(Collectors.toList());
        assertTrue(contoursG3.contains(g3c1));
        assertTrue(contoursG3.contains(g3c2));
    }

    @Test
    public void testSetNewBusinessUnitContourGroup() {
        ContourGroup contourGroup = new ContourGroup();
        contourGroup.setName("Testing business unit");
        contourGroup.setBusinessUnitIds(Set.of(1L));
        long contourGroupId = contourDao.createNewContourGroup(contourGroup);
        contourService.setNewBusinessUnitContourGroup(contourGroupId, contourGroup.getBusinessUnitIds());
    }

    @Test
    public void testDeletedContourWithRequests(){

//        test with actual request
        long contourId_1 = data.createTestContour(111L, "Contour1");
        Map<String, BigDecimal> contours_1 = new HashMap<>();
        contours_1.put(String.valueOf(contourId_1), BigDecimal.ONE);
        long projectId_1 =  data.createProject(contours_1, null);
        Request requestTemplate_1 = data.createTemplateDefaultRequest("login_1", projectId_1, 123L);
        Request request_1 = requestService.createNewRequest(requestTemplate_1);
        requestService.acceptRequest(request_1.getRequest_id(), request_1);
        jobPeriodsCalculator.execute();
        contourService.deleteContour(contourId_1);
        Contour contour_1 = contourService.getContour(contourId_1);
        assertEquals(false, contour_1.getDeleted());

//        test with future request without actual request
        Map<String, BigDecimal> contours_2 = new HashMap<>();
        contours_2.put(String.valueOf(data.contourId), BigDecimal.ONE);
        long projectId =  data.createProject(contours_2, null);

        Request requestTemplate_2 = data.createTemplateDefaultRequest("login1", projectId, 123L);
        requestTemplate_2.setDate_start(Date.valueOf(LocalDate.now().plusDays(1)));
        Request request_2 = requestService.createNewRequest(requestTemplate_2);
        request_2.setResolution("First part of comment.");
        requestService.acceptRequest(request_2.getRequest_id(), request_2);
        jobPeriodsCalculator.execute();

        contourService.deleteContour(data.contourId);
        Contour contour_2 = contourService.getContour(data.contourId);
        assertEquals(true, contour_2.getDeleted());

        String expectedComment = String.format("First part of comment.\n" +
                " Заявка отклонена по причине удаления контура %s. Если это актуальная заявка, " +
                "оформите, пожалуйста, на актуальный контур.", contour_2.getName());
        String comment = commentService.getComment(request_2.getRequest_id());
        assertEquals(expectedComment, comment);
    }

    @Test
    public void testAddAndDeleteContourChats(){
        List<ContourChat> newContourChats = new ArrayList<>();
        ContourChat contourChatPrimary = new ContourChat();
        contourChatPrimary.setContourId(data.contourId);
        contourChatPrimary.setLink("http://bla.bla");
        contourChatPrimary.setDescription("Основной чат контура");
        contourChatPrimary.setPrimary(true);
        newContourChats.add(contourChatPrimary);

        ContourChat contourChatSecondary = new ContourChat();
        contourChatSecondary.setContourId(data.contourId);
        contourChatSecondary.setLink("http://bla2.bla");
        contourChatSecondary.setDescription("Desc2");
        contourChatSecondary.setPrimary(false);
        newContourChats.add(contourChatSecondary);

        contourChatsService.updateChats(data.contourId, newContourChats);

        List<ContourChat> contourChats = contourChatsService.getChats(data.contourId);
        assertEquals(2, contourChats.size());
    }

    @Test
    public void testDepartmentContour(){

        String depName102 = "102. Contour1";
        String depName203 = "203. Contour2";
//        root deps. No contour
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (211913, 962, 'daniilsh', 'Бизнес-группа Eкома и Райдтеха', 0, false)");
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (62131, 204703, 'styskin', 'Поисковый портал' , 0 , false)");
//        deep level -1 deps.
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (101, 211913, 'daniilsh', '101. No contour', 0, false)");
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (102, 211913, 'daniilsh', '{dep102}', 0, false)".replace("{dep102}", depName102));
//        depp level -2 deps
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (201, 101, 'daniilsh', '201. No contour', 0, false)");
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (202, 102, 'daniilsh', '202. Contour1 inherited from 102', 0, false)");
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (203, 102, 'daniilsh', '{dep203}', 0, false)".replace("{dep203}", depName203));
//        deep level -3 deps
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (302, 202, 'daniilsh', '302. Contour1 inherited from 102', 0, false)");
        data.jdbcTemplate.exec("INSERT INTO public.departments (department_id, department_parent_id, department_head, department_name, cell_limit, deleted) " +
                "                                           VALUES (303, 203, 'daniilsh', '303. Contour2 inherited from 203', 0, false)");

        String contour0Name = "Contour0";
        String contour1Name = "Contour1";
        String contour2Name = "Contour2";
        Long contour0Id = data.createTestContour(123L, contour0Name);
        Long contour1Id = data.createTestContour(123L, contour1Name);
        Long contour2Id = data.createTestContour(123L, contour2Name);
        Map<String, BigDecimal> contour0Fill = new HashMap<>();
        contour0Fill.put(contour0Id.toString(), BigDecimal.valueOf(1));
        Map<String, BigDecimal> contour1Fill = new HashMap<>();
        contour1Fill.put(contour1Id.toString(), BigDecimal.valueOf(1));
        Map<String, BigDecimal> contour2Fill = new HashMap<>();
        contour2Fill.put(contour2Id.toString(), BigDecimal.valueOf(1));

        contourService.setDepartmentContour(211913L, contour0Fill);
        contourService.setDepartmentContour(102L, contour1Fill);
        contourService.setDepartmentContour(203L, contour2Fill);

        List<Department> dep_tree = departmentService.departmentsTreeWithContour(null);

        Department dep211913 = dep_tree.get(0);
        assertEquals(contour0Fill, dep211913.getContours());
        assertEquals(false, dep211913.getContourInherited());

        Department dep101 = new Department();
        Department dep102 = new Department();
        for(Department d : dep211913.getChildren()) {
            if (d.getId() == 102L) {dep102 = d;}
            else {dep101 = d;}
        }
        assertEquals(contour0Fill, dep101.getContours());
        assertEquals(true, dep101.getContourInherited());
        assertEquals(contour1Fill, dep102.getContours());
        assertEquals(false, dep102.getContourInherited());

        Department dep202 = new Department();
        Department dep203 = new Department();
        for(Department d : dep102.getChildren()) {
            if (d.getId() == 203L) {dep203 = d;}
            else {dep202 = d;}
        }
        assertEquals(contour1Fill, dep202.getContours());
        assertEquals(true, dep202.getContourInherited());
        assertEquals(contour2Fill, dep203.getContours());
        assertEquals(false, dep203.getContourInherited());

        Department dep302 = dep202.getChildren().get(0);
        Department dep303 = dep203.getChildren().get(0);
        assertEquals(contour1Fill, dep302.getContours());
        assertEquals(true, dep302.getContourInherited());
        assertEquals(contour2Fill, dep303.getContours());
        assertEquals(true, dep303.getContourInherited());
    }
}
