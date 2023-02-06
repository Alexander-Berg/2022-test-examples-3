package ru.yandex.market.mstat.planner.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.controller.rest.exceptions.NotFoundException;
import ru.yandex.market.mstat.planner.model.Project;
import ru.yandex.market.mstat.planner.model.ProjectWithRequest;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestType;
import ru.yandex.market.mstat.planner.util.HtmlUtil;
import ru.yandex.market.mstat.planner.util.parser.employeescontours.ContourAllocationOverloadErrorData;
import ru.yandex.market.mstat.planner.util.parser.employeescontours.ParsingError;
import ru.yandex.market.mstat.planner.util.parser.employeescontours.ParsingErrorType;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mstat.planner.util.RestUtil.parseDate;
import static ru.yandex.market.mstat.planner.util.RestUtil.toSqlDate;
import static ru.yandex.market.mstat.planner.util.RestUtil.today;
import static ru.yandex.market.mstat.planner.util.RestUtil.todayLocalDate;

public class ProjectWithRequestServiceTest extends AbstractDbIntegrationTest {

    @Autowired
    private ProjectWithRequestService projectWithRequestService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProjectService projectService;

    @Test
    public void testCreateRelocation() {

        Map<String, BigDecimal> colours1 = new HashMap<>();
        colours1.put("w", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours1 = new HashMap<>();
        contours1.put("11", BigDecimal.valueOf(1));

        ProjectWithRequest pwd = projectWithRequestService.createRelocation(contours1, colours1, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        Request requestFromDB = requestService.getRequestById(pwd.getRequest().getRequest_id());
        Project projectFromDB = projectService.get(pwd.getProject().getProject_id());

        assertEquals(requestFromDB.getType(), RequestType.employee_relocation.name());
        assertEquals(requestFromDB.getEmployee(), data.login);
        assertEquals("1000y", requestFromDB.getJob_size());
        assertEquals("relocation-rq-" + data.login, requestFromDB.getDescription());
        assertEquals(requestFromDB.getDepartment_id_from().longValue(), data.departmentId);
        assertEquals(requestFromDB.getDate_start(), today());
        assertEquals(requestFromDB.getDate_end(), Date.valueOf(
                LocalDate.now()
                        .plusYears(1000)
                        .minusDays(1)
                        .atStartOfDay()
                        .toLocalDate()
        ));

        assertTrue(projectFromDB.getContours().containsKey("11"));
        assertTrue(projectFromDB.getColors().containsKey("w"));
        assertEquals(projectFromDB.getColors().get("w"), BigDecimal.valueOf(1));
        assertEquals(projectFromDB.getProject_desc(), "relocation-" + data.login + "-" + HtmlUtil.currentMonthStart());

        Map<String, BigDecimal> colours2 = new HashMap<>();
        colours2.put("b", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours2 = new HashMap<>();
        contours2.put("11", BigDecimal.valueOf(1));

        ProjectWithRequest newPwd = projectWithRequestService.createRelocation(contours2, colours2, data.login, data.departmentId, AuthInfoService.PLANNER, null);
        Request newRequestFromDB = requestService.getRequestById(newPwd.getRequest().getRequest_id());
        Project newProjectFromDB = projectService.get(newPwd.getProject().getProject_id());

        assertEquals(newRequestFromDB.getType(), RequestType.employee_relocation.name());
        assertEquals(newRequestFromDB.getEmployee(), data.login);
        assertEquals("1000y", newRequestFromDB.getJob_size());
        assertEquals("relocation-rq-" + data.login, newRequestFromDB.getDescription());
        assertEquals(newRequestFromDB.getDepartment_id_from().longValue(), data.departmentId);
        assertEquals(newRequestFromDB.getDate_start(), today());
        assertEquals(newRequestFromDB.getDate_end(), Date.valueOf(
                LocalDate.now()
                        .plusYears(1000)
                        .minusDays(1)
                        .atStartOfDay()
                        .toLocalDate()
        ));

        assertTrue(newProjectFromDB.getContours().containsKey("11"));
        assertTrue(newProjectFromDB.getColors().containsKey("b"));
        assertEquals(newProjectFromDB.getColors().get("b"), BigDecimal.valueOf(1));
        assertFalse(newProjectFromDB.getColors().containsKey("w"));
        assertEquals(newProjectFromDB.getProject_desc(), "relocation-" + data.login + "-" + HtmlUtil.currentMonthStart());

        assertThrows(NotFoundException.class, () -> {
            requestService.getRequestById(pwd.getRequest().getRequest_id());
        });
//       TODO MARKETQPLANNER-1655
//        assertThrows(NotFoundException.class, () -> {
//            projectService.get(pwd.getProject().getProject_id());
//        });
    }


    @Test
    public void testCreateFact() {

        Map<String, BigDecimal> colours1 = new HashMap<>();
        colours1.put("w", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours1 = new HashMap<>();
        contours1.put("11", BigDecimal.valueOf(1));

        ProjectWithRequest pwd = projectWithRequestService.createFact(contours1, colours1, data.login);
        Request requestFromDB = requestService.getRequestById(pwd.getRequest().getRequest_id());
        Project projectFromDB = projectService.get(pwd.getProject().getProject_id());

        assertEquals(requestFromDB.getType(), RequestType.fact.name());
        assertEquals(requestFromDB.getEmployee(), data.login);
        assertEquals("1m", requestFromDB.getJob_size());
        assertEquals("fact-rq-" + data.login, requestFromDB.getDescription());
        assertEquals(requestFromDB.getDepartment_id_from().longValue(), data.departmentId);
        assertEquals(requestFromDB.getDate_start(), toSqlDate(parseDate(HtmlUtil.currentMonthStart())));
        assertEquals(requestFromDB.getDate_end(), toSqlDate(parseDate(HtmlUtil.currentMonthEnd())));

        assertTrue(projectFromDB.getContours().containsKey("11"));
        assertTrue(projectFromDB.getColors().containsKey("w"));
        assertEquals(projectFromDB.getColors().get("w"), BigDecimal.valueOf(1));
        assertEquals(projectFromDB.getProject_desc(), "fact-" + data.login + "-" + HtmlUtil.currentMonthStart());

        Map<String, BigDecimal> colours2 = new HashMap<>();
        colours2.put("b", BigDecimal.valueOf(1));

        Map<String, BigDecimal> contours2 = new HashMap<>();
        contours2.put("11", BigDecimal.valueOf(1));

        ProjectWithRequest newPwd = projectWithRequestService.createFact(contours2, colours2, data.login);
        Request newRequestFromDB = requestService.getRequestById(newPwd.getRequest().getRequest_id());
        Project newProjectFromDB = projectService.get(newPwd.getProject().getProject_id());

        assertEquals(newRequestFromDB.getType(), RequestType.fact.name());
        assertEquals(newRequestFromDB.getEmployee(), data.login);
        assertEquals("1m", newRequestFromDB.getJob_size());
        assertEquals("fact-rq-" + data.login, newRequestFromDB.getDescription());
        assertEquals(newRequestFromDB.getDepartment_id_from().longValue(), data.departmentId);
        assertEquals(newRequestFromDB.getDate_start(), toSqlDate(parseDate(HtmlUtil.currentMonthStart())));
        assertEquals(newRequestFromDB.getDate_end(), toSqlDate(parseDate(HtmlUtil.currentMonthEnd())));

        assertTrue(newProjectFromDB.getContours().containsKey("11"));
        assertTrue(newProjectFromDB.getColors().containsKey("b"));
        assertEquals(newProjectFromDB.getColors().get("b"), BigDecimal.valueOf(1));
        assertFalse(newProjectFromDB.getColors().containsKey("w"));
        assertEquals(newProjectFromDB.getProject_desc(), "fact-" + data.login + "-" + HtmlUtil.currentMonthStart());

        assertEquals(newRequestFromDB.getRequest_id(), requestFromDB.getRequest_id());
        assertEquals(newProjectFromDB.getProject_id(), projectFromDB.getProject_id());

        ProjectWithRequest pwd3 = projectWithRequestService.createRelocation(null, null, data.login, data.departmentId, AuthInfoService.PLANNER, "autovacancy");
        Project p = projectService.get(pwd3.getProject().getProject_id());
        assertNotNull(p.getContours());

    }

    @Test
    public void testExcelFileLoad() throws IOException {
        try (InputStream is = RequestServiceTest.class.getResourceAsStream("contours_relocation.xlsx")) {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);
            assertEquals(2, parsingErrors.size());
        }
    }

    @FunctionalInterface
    interface InputStreamConsumer {
        void process(InputStream is) throws IOException;
    }

    private void createExcelWorkbook(Object[][] data, InputStreamConsumer isConsumer) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet1");

        int rowNum = 0;

        for (Object[] dataRow : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Object field : dataRow) {
                Cell cell = row.createCell(colNum++);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                } else if (field instanceof Double) {
                    cell.setCellValue((Double) field);
                } else if (field instanceof Long) {
                    cell.setCellValue((Long) field);
                }
            }
        }

        PipedInputStream in = new PipedInputStream();

        new Thread(() -> {
            try (final PipedOutputStream out = new PipedOutputStream(in)) {
                workbook.write(out);
                isConsumer.process(in);
                in.close();
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }).start();

    }

    @Test
    public void testRelocationFromExcel_SuccessfulLoad() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);
        final String user2 = "user2";
        data.createEmployee(data.departmentId, user2);

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");
        final long ctr2 = data.createTestContour(data.groupId, "Contour2");
        final long ctr3 = data.createTestContour(data.groupId, "Contour3");
        final long ctr4 = data.createTestContour(data.groupId, "Contour4");

        Object[][] data = {
                {user1, ctr1, "Some desc", 30},
                {user1, ctr2, "Some desc", 30},
                {user1, ctr3, "Some desc", 40},

                {user2, ctr1, "Some desc", 30},
                {user2, ctr2, "Some desc", 15},
                {user2, ctr3, "Some desc", 45},
                {user2, ctr4, "Some desc", 10}
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors;

            parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertTrue(parsingErrors.isEmpty());

            final Map<String, ProjectWithRequest> pwrs = projectService
                    .getProjectsWithRequestsByEmployeesIds(Arrays.asList(user1, user2));

            assertEquals(2, pwrs.size());

            // Check first employee
            final ProjectWithRequest pwr1 = pwrs.get(user1);

            final Project p1 = pwr1.getProject();
            assertEquals(3, p1.getContours().size());
            assertEquals(BigDecimal.valueOf(0.3), p1.getContours().get(String.valueOf(ctr1)));
            assertEquals(BigDecimal.valueOf(0.3), p1.getContours().get(String.valueOf(ctr2)));
            assertEquals(BigDecimal.valueOf(0.4), p1.getContours().get(String.valueOf(ctr3)));

            final Request r1 = pwr1.getRequest();
            assertEquals(user1, r1.getEmployee());

            // Check first employee
            final ProjectWithRequest pwr2 = pwrs.get(user2);

            final Project p2 = pwr2.getProject();
            assertEquals(4, p2.getContours().size());
            assertEquals(BigDecimal.valueOf(0.3), p2.getContours().get(String.valueOf(ctr1)));
            assertEquals(BigDecimal.valueOf(0.15), p2.getContours().get(String.valueOf(ctr2)));
            assertEquals(BigDecimal.valueOf(0.45), p2.getContours().get(String.valueOf(ctr3)));
            assertEquals(BigDecimal.valueOf(0.1), p2.getContours().get(String.valueOf(ctr4)));
        });
    }

    @Test
    public void testRelocationFromExcel_ContourNotExists() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");

        final long notExistedContour = 999999;

        Object[][] data = {
                {user1, ctr1, "Some desc", 30},
                {user1, notExistedContour, "Some desc", 30}
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertNull(parsingError.getCellNum());
            assertNull(parsingError.getRowNum());
            assertEquals(ParsingErrorType.CONTOUR_NOT_EXISTS, parsingError.getErrorType());
            assertEquals(Collections.singleton(notExistedContour), parsingError.getErrorData());

            // Check that nothing insert when we have errors
            final Map<String, ProjectWithRequest> pwrs = projectService
                    .getProjectsWithRequestsByEmployeesIds(Collections.singletonList(user1));

            assertTrue(pwrs.isEmpty());
        });
    }

    @Test
    public void testRelocationFromExcel_EmployeeNotExists() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);
        final String notExistedUser = "user2";

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");

        Object[][] data = {
                {user1, ctr1, "Some desc", 30},

                {notExistedUser, ctr1, "Some desc", 15}
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertNull(parsingError.getCellNum());
            assertNull(parsingError.getRowNum());
            assertEquals(ParsingErrorType.EMPLOYEE_NOT_EXISTS, parsingError.getErrorType());
            assertEquals(Collections.singleton(notExistedUser), parsingError.getErrorData());
        });
    }

    @Test
    public void testRelocationFromExcel_ContourAllocationOverload() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");
        final long ctr2 = data.createTestContour(data.groupId, "Contour2");
        final long ctr3 = data.createTestContour(data.groupId, "Contour3");

        Object[][] data = {
                {user1, ctr1, "Some desc", 30},
                {user1, ctr2, "Some desc", 40},
                {user1, ctr3, "Some desc", 50}
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertNull(parsingError.getCellNum());
            assertNull(parsingError.getRowNum());
            assertEquals(ParsingErrorType.CONTOURS_ALLOCATION_OVERLOAD, parsingError.getErrorType());

            final ContourAllocationOverloadErrorData errorData = (ContourAllocationOverloadErrorData) parsingError.getErrorData();

            assertEquals(Double.valueOf(20), errorData.getOverload());
            assertEquals(user1, errorData.getEmployeeLogin());
            assertEquals(BigDecimal.valueOf(0.3), errorData.getContoursAllocation().get(String.valueOf(ctr1)));
            assertEquals(BigDecimal.valueOf(0.4), errorData.getContoursAllocation().get(String.valueOf(ctr2)));
            assertEquals(BigDecimal.valueOf(0.5), errorData.getContoursAllocation().get(String.valueOf(ctr3)));
        });
    }

    @Test
    public void testRelocationFromExcel_EmptyCell() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");

        Object[][] data = {
                {user1, ctr1, "Some desc"}
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertEquals(Integer.valueOf(3), parsingError.getCellNum());
            assertEquals(Integer.valueOf(0), parsingError.getRowNum());
            assertEquals(ParsingErrorType.EMPTY_CELL, parsingError.getErrorType());
            assertNull(parsingError.getErrorData());
        });
    }

    @Test
    public void testRelocationFromExcel_WrongContourIdData() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);

        final String wrongContourIdData = "efdsfsdf";

        Object[][] data = {
                {user1, wrongContourIdData, "Some desc", 30},
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertEquals(Integer.valueOf(1), parsingError.getCellNum());
            assertEquals(Integer.valueOf(0), parsingError.getRowNum());
            assertEquals(ParsingErrorType.WRONG_CELL_CONTENT, parsingError.getErrorType());
            assertEquals(wrongContourIdData, parsingError.getErrorData());
        });
    }

    @Test
    public void testRelocationFromExcel_WrongContourLoadData() {
        final String user1 = "user1";
        data.createEmployee(data.departmentId, user1);

        final long ctr1 = data.createTestContour(data.groupId, "Contour1");

        final double wrongContourLoadData = -345;

        Object[][] data = {
                {user1, ctr1, "Some desc", wrongContourLoadData},
        };

        createExcelWorkbook(data, is -> {
            final List<ParsingError> parsingErrors = projectWithRequestService.createRelocationFromExcel(is);

            assertEquals(1, parsingErrors.size());

            final ParsingError parsingError = parsingErrors.get(0);
            assertEquals(Integer.valueOf(3), parsingError.getCellNum());
            assertEquals(Integer.valueOf(0), parsingError.getRowNum());
            assertEquals(ParsingErrorType.WRONG_CELL_CONTENT, parsingError.getErrorType());
            assertEquals(wrongContourLoadData, parsingError.getErrorData());
        });
    }

    @Test
    public void testAppointEmployeesDepartmentContour() {
        data.createDepartment(217L, "child1_1generation_inherited", data.departmentId);
        data.createDepartment(218L, "child2_1generation_NOT_inherited", data.departmentId);
        data.createDepartment(317L, "child3_2generation_inherited", 217L);
        data.createDepartment(318L, "child4_2generation_NOT_inherited", 218L);

        String child1_1generation_inherited = "child1_1generation_inherited";
        data.createEmployee(217L, child1_1generation_inherited);
        String child2_1generation_NOT_inherited = "child2_1generation_NOT_inherited";
        data.createEmployee(218L, child2_1generation_NOT_inherited);
        String child3_2generation_inherited = "child3_2generation_inherited";
        data.createEmployee(317L, child3_2generation_inherited);
        String child4_2generation_NOT_inherited = "child4_2generation_NOT_inherited";
        data.createEmployee(318L, child4_2generation_NOT_inherited);

//        создадим один актуальный реквест для проверки что он переназначится
        String employeeHasOldActualRequest = "employeeHasOldActualRequest";
        data.createEmployee(data.departmentId, employeeHasOldActualRequest);
        Long contourIdOld = data.createTestContour(123L, "OldContour");
        Map<String, BigDecimal> contourFillOld = new HashMap<>();
        contourFillOld.put(contourIdOld.toString(), BigDecimal.valueOf(1));
        Long projectIdOld = data.createProject(contourFillOld, null);

        java.sql.Date dateFrom = java.sql.Date.valueOf(todayLocalDate().minusDays(3));
        java.sql.Date dateTo = java.sql.Date.valueOf(todayLocalDate().plusDays(3));
        Request oldActualRequest = data.createPlanTemplate(
                1,
                data.departmentId,
                employeeHasOldActualRequest,
                "",
                dateFrom,
                dateTo,
                0L,
                projectIdOld
        );
        oldActualRequest.setType(RequestType.employee_relocation.name());
        requestService.createNewRequest(oldActualRequest, true);

//        исходная картина. Только у employeeHasOldActualRequest есть актуальный реквест. Остальные люди во всех департмнтах безработные
        Request requestHasOldActualRequestBefore = requestService.getActualNotEmptyRelocationByEmployee(employeeHasOldActualRequest);
        Request requestNoOldActualRequestBefore = requestService.getActualNotEmptyRelocationByEmployee(data.login);
        Request child1_1generation_inheritedBefore = requestService.getActualNotEmptyRelocationByEmployee(child1_1generation_inherited);
        assertEquals(projectIdOld, requestHasOldActualRequestBefore.getProject_id());
        assertEquals(null, requestNoOldActualRequestBefore);
        assertEquals(null, child1_1generation_inheritedBefore);

//        создаём дефолтный контур департамента для коренвого data.DepartmentId
        Long contourIdNew = data.createTestContour(123L, "DepartmentDefaultContour");
        Map<String, BigDecimal> contourFillNew = new HashMap<>();
        contourFillNew.put(contourIdNew.toString(), BigDecimal.valueOf(1));
        Long projectIdNew = data.createProject(contourFillNew, null);
        data.jdbcTemplate.exec(("INSERT INTO department_project(department_id, project_id) " +
                "VALUES({d}, {p})").replace("{d}", String.valueOf(data.departmentId)).replace("{p}", String.valueOf(projectIdNew)));

//        создаём дефолтный контур департамента для ребёнка чтоб убедится что сотрудники ни этого департамента ни его дети не унаследуют контур корневого депа
        data.jdbcTemplate.exec(("INSERT INTO department_project(department_id, project_id) " +
                "VALUES({d}, {p})").replace("{d}", String.valueOf(218L)).replace("{p}", "33333"));

        projectWithRequestService.appointEmployeesDepartmentContour(data.departmentId, null);

        Request requestHasOldActualRequestAfter = requestService.getActualNotEmptyRelocationByEmployee(employeeHasOldActualRequest);
        Request requestNoOldActualRequestAfter = requestService.getActualNotEmptyRelocationByEmployee(data.login);
        Request child1_1generation_inheritedAfter = requestService.getActualNotEmptyRelocationByEmployee(child1_1generation_inherited);
        Request child2_1generation_NOT_inheritedAfter = requestService.getActualNotEmptyRelocationByEmployee(child2_1generation_NOT_inherited);
        Request child3_2generation_inheritedAfter = requestService.getActualNotEmptyRelocationByEmployee(child3_2generation_inherited);
        Request child4_2generation_NOT_inheritedAfter = requestService.getActualNotEmptyRelocationByEmployee(child4_2generation_NOT_inherited);

        assertEquals(projectIdNew, requestHasOldActualRequestAfter.getProject_id());
        assertEquals(projectIdNew, requestNoOldActualRequestAfter.getProject_id());
        assertEquals(projectIdNew, child1_1generation_inheritedAfter.getProject_id());
        assertEquals(null, child2_1generation_NOT_inheritedAfter);
        assertEquals(projectIdNew, child3_2generation_inheritedAfter.getProject_id());
        assertEquals(null, child4_2generation_NOT_inheritedAfter);
    }
}
