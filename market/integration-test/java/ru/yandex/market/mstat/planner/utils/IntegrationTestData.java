package ru.yandex.market.mstat.planner.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import ru.yandex.market.mstat.planner.controller.rest.SpecializationController;
import ru.yandex.market.mstat.planner.dao.ContourDao;
import ru.yandex.market.mstat.planner.dao.ContourTagDao;
import ru.yandex.market.mstat.planner.dao.EmployeeAndDepartmentDao;
import ru.yandex.market.mstat.planner.dao.HeadcountDao;
import ru.yandex.market.mstat.planner.dao.ProjectDao;
import ru.yandex.market.mstat.planner.model.Contour;
import ru.yandex.market.mstat.planner.model.ContourGroup;
import ru.yandex.market.mstat.planner.model.Employee;
import ru.yandex.market.mstat.planner.model.Headcount;
import ru.yandex.market.mstat.planner.model.HeadcountVacancy;
import ru.yandex.market.mstat.planner.model.Project;
import ru.yandex.market.mstat.planner.model.Request;
import ru.yandex.market.mstat.planner.model.RequestStatus;
import ru.yandex.market.mstat.planner.model.RequestType;
import ru.yandex.market.mstat.planner.model.Specialization;
import ru.yandex.market.mstat.planner.service.AuthInfoService;
import ru.yandex.market.mstat.planner.util.LoggingJdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static ru.yandex.market.mstat.planner.util.RestUtil.today;

@Profile({"integration-tests"})
public class IntegrationTestData {

    @Autowired
    private EmployeeAndDepartmentDao employeeAndDepartmentDao;

    @Autowired
    private ContourDao contourDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private ContourTagDao contourTagDao;

    @Autowired
    private HeadcountDao headcountDao;

    @Autowired
    public LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private SpecializationController specializationController;

    @Autowired
    private DataSource ds;

    public long colorId;
    public long buId;
    public long groupId;
    public long contourId;
    public long tgId;
    public long parentDepartmentId;
    public long departmentId;
    public String login;

    public void createData() {
        this.parentDepartmentId = 1694L;
        this.departmentId = createDepartment(117L, "Важнейший департамент", parentDepartmentId);
        this.login = "MyEmployee";
        createEmployee(departmentId, login);
        this.colorId = createTestColor("AD007" ,"Зеленый");
        this.buId = createBU(departmentId, "Юнит");
        this.groupId = createTestGroup("Тестовая группа");
        this.contourId = createTestContour(groupId, "Тестовый контур");
        this.tgId = createTag(contourId);
    }

    public void destroyData() {
        deleteEntity("contour_colors");
        deleteEntity("contour_groups");
        deleteEntity("contours");
        deleteEntity("tags");
        deleteEntity("departments");
        deleteEntity("employees");
        deleteEntity("projects");
        deleteEntity("requests");
        deleteEntity("business_unit_specializations");
        deleteEntity("business_units");
        deleteEntity("specialization");
        deleteEntity("headcount_allocation");
        deleteEntity("headcount_vacancy");
        deleteEntity("job_periods");
        deleteEntity("colors");
    }

    public void deleteEntity(String table) {
        jdbcTemplate.exec("delete from " + table);
    }

    public long createTestColor(String name, String code) {
        return jdbcTemplate.query("" +
                "INSERT INTO colors (code, name) VALUES (:code ,:name)\n" +
                        "returning id",
                "code", code,
                "name", name,
                Long.class);
    }

    public long createBU(long dep, String name) {
        return jdbcTemplate.query("" +
                        "insert into business_units (root_department_id, title) VALUES (:dep ,:name)\n" +
                        "returning business_unit_id",
                "dep", dep,
                "name", name,
                Long.class);
    }

    public long createTestGroup(String name) {
        ContourGroup group = new ContourGroup();
        group.setName(name);
        return contourDao.createNewContourGroup(group);
    }

    public void setBusinessUnitGroup(long contourGroupId, Set<Long> businessUnitIds) {
        contourDao.setContourGroupBusinessUnit(contourGroupId, businessUnitIds);
    }

    public long createTestContour(long groupId, String name) {
        Contour contour = new Contour();
        contour.setGroupId(groupId);
        contour.setName(name);
        contour.setTarget("target1");
        contour.setFteLimit(1L);
        contour.setDescription("desc1");
        contour.setStQueues("queue1");
        contour.setStProjectTickets("projectTicket1");
        contour.setWikiLinks("wiki1");
        contour.setBoards("board1");
        contour.setTechLead(login);
        contour.setProductOwner(login);
        contour.setEmployer(login);
        contour.setStProjectsFilterId(123);
        contour.setStTag("tag");
        contour.setStTagAutoMarkup(true);
        return contourDao.createNewContour(contour);
    }

    public long createTestDuty(long id, long typeId, String login, LocalDate from, LocalDate to) {
        return jdbcTemplate.query("" +
                        "insert into public.duty(duty_id, duty_type_id, duty_type_name, login, is_approved, start_date, end_date) " +
                        "VALUES (:id, :type_id, 'OnCall дежурство', :login, true, :from, :to)\n" +
                        "returning duty_id",
                "id", id,
                "type_id", typeId,
                "login", login,
                "from", from,
                "to", to,
                Long.class);
    }

    public long createTag(long contourId) {
        long tgId = contourTagDao.addTag("Тестовый тег");
        contourTagDao.updateContourTags(contourId, Collections.singleton(tgId));
        return tgId;
    }

    public long createDepartment(long departmentId, String depName, long parentDepartmentId) {
        employeeAndDepartmentDao.createDepartment(departmentId, parentDepartmentId, "BigBoss", depName);
        return departmentId;
    }

    public void createEmployee(long departmentId, String login) {
        Employee employee = new Employee();
        employee.setLogin(login);
        employee.setDepartment_id(departmentId);
        employee.setName(login);
        employee.setDismissed(false);
        employeeAndDepartmentDao.createEmployee(employee, "dev");
    }

    public void createEmployee(long departmentId, String login, String name) {
        Employee employee = new Employee();
        employee.setLogin(login);
        employee.setDepartment_id(departmentId);
        employee.setName(name);
        employee.setDismissed(false);
        employeeAndDepartmentDao.createEmployee(employee, "dev");
    }

    public Request createPlanTemplate(double load, long dep, String login, String sizeRequest,
                                      java.sql.Date startDate, java.sql.Date endDate, Long specialization_id, Long projectId) {
        Request request = new Request();
        request.setEmployee(login);
        request.setDepartment_id_from(dep);
        request.setJob_load_requested(BigDecimal.valueOf(load));
        request.setJob_load(BigDecimal.valueOf(load));
        request.setJob_load_perc_requested((int) (load * 100));
        request.setJob_load_perc((int) (load * 100));
        request.setStatus(RequestStatus.accepted.name());
        request.setType(RequestType.plan.name());
        request.setDescription(login);
        request.setDate_start_requested(startDate);
        request.setDate_start(startDate);
        request.setJob_size_requested(sizeRequest);
        request.setJob_size(sizeRequest);
        request.setDate_end(endDate);
        request.setRequest_author("mstattest");
        request.setStatus_changed_by("mstattest");
        if (specialization_id != null) {
            request.setSpecialization_id(specialization_id);
        }
        if (projectId != null) {
            request.setProject_id(projectId);
        }

        return request;
    }

    public Request createTemplateDefaultRequest(String login, Long projectId, Long dep) {
        Request request = new Request();
        request.setEmployee(login);
        request.setType(RequestType.employee_relocation.name());
        request.setProject_id(projectId);
        request.setJob_load_requested(BigDecimal.valueOf(1));
        request.setJob_load(BigDecimal.valueOf(1));
        request.setJob_load_perc_requested(100);
        request.setJob_load_perc(100);
        request.setDescription("relocation-rq-" + login);
        request.setDate_start_requested(today());
        request.setDate_start(today());
        request.setJob_size_requested("1000y");
        request.setJob_size("1000y");
        request.setDate_end(
                java.sql.Date.valueOf(
                        LocalDate.now()
                                .plusYears(1000)
                                .minusDays(1)
                                .atStartOfDay()
                                .toLocalDate()
                )
        );
        request.setDepartment_id_from(dep);
        request.setRequest_author(AuthInfoService.PLANNER);
        request.setStatus_changed_by(AuthInfoService.PLANNER);
        return request;
    }

    public long createProject(Map<String, BigDecimal> contours, Map<String, BigDecimal> colors) {
        Project p = new Project();
        p.setCreated_by("pavellysenko");
        p.setProject_desc("тестовый проект");
        p.setContours(contours);
        return projectDao.createProject(p);
    }

    public Specialization createSpecialization(long depId, String name, String employee) {
        Specialization specialization = new Specialization();
        specialization.setSpecialization_department_id(depId);
        specialization.setSpecialization_short(name);
        specialization.setSpecialization_desc(name);
        specialization.setEmployee(employee);
        specialization.setEmployee_name(employee);
        return specializationController.createNewSpecialization(specialization);
    }

    public Headcount createHeadcount(Integer headcount,
                                     Long headcountId,
                                     Long depId,
                                     String employeeLogin,
                                     java.sql.Date startDate,
                                     java.sql.Date endDate) {
        final Headcount headcountInstance = new Headcount();
        headcountInstance.setHeadcount(headcount);
        headcountInstance.setHeadcountId(headcountId);
        headcountInstance.setStaffDepartmentId(depId);
        headcountInstance.setCategory("Professionals");
        headcountInstance.setIsHomeworker(false);
        headcountInstance.setIsIntern(false);
        headcountInstance.setIsMaternity(false);
        headcountInstance.setDepartmentId(depId);
        headcountInstance.setEmployee(employeeLogin);
        headcountInstance.setStatus("OCCUPIED");
        headcountInstance.setStartDate(startDate);
        headcountInstance.setEndDate(endDate);
        headcountInstance.setId(headcountDao.saveHeadcount(headcountInstance));
        return headcountInstance;
    }

    public HeadcountVacancy createHeadcountVacancy(
            Long headcountId,
            String jobTicket,
            java.sql.Date startDate,
            java.sql.Date endDate,
            String status
    ) {
        final HeadcountVacancy headcountVacancy = new HeadcountVacancy();
        headcountVacancy.setHeadcountId(headcountId);
        headcountVacancy.setJobTicket(jobTicket);
        headcountVacancy.setStartDate(startDate);
        headcountVacancy.setEndDate(endDate);
        headcountVacancy.setStatus(status);
        headcountDao.saveVacancy(headcountVacancy);
        return headcountVacancy;
    }

    public DataSource getDatasource() {
        return ds;
    }

}
