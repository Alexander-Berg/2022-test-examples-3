package ru.yandex.market.mstat.planner.config;

import java.util.Objects;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mstat.planner.client.HeadcountClient;
import ru.yandex.market.mstat.planner.controller.rest.IdmController;
import ru.yandex.market.mstat.planner.controller.rest.SpecializationController;
import ru.yandex.market.mstat.planner.dao.BusinessUnitDao;
import ru.yandex.market.mstat.planner.dao.CommentDao;
import ru.yandex.market.mstat.planner.dao.ContourChatDao;
import ru.yandex.market.mstat.planner.dao.ContourDao;
import ru.yandex.market.mstat.planner.dao.ContourTagDao;
import ru.yandex.market.mstat.planner.dao.EmployeeAndDepartmentDao;
import ru.yandex.market.mstat.planner.dao.HeadcountDao;
import ru.yandex.market.mstat.planner.dao.JobPeriodsDao;
import ru.yandex.market.mstat.planner.dao.ProjectDao;
import ru.yandex.market.mstat.planner.dao.ProjectWithRequestDao;
import ru.yandex.market.mstat.planner.dao.ReportDao;
import ru.yandex.market.mstat.planner.dao.RequestDao;
import ru.yandex.market.mstat.planner.dao.UserDao;
import ru.yandex.market.mstat.planner.security.tvm.TvmTicketProvider;
import ru.yandex.market.mstat.planner.service.AuthInfoService;
import ru.yandex.market.mstat.planner.service.CommentService;
import ru.yandex.market.mstat.planner.service.ContourChatsService;
import ru.yandex.market.mstat.planner.service.ContourService;
import ru.yandex.market.mstat.planner.service.DepartmentService;
import ru.yandex.market.mstat.planner.service.EmployeeService;
import ru.yandex.market.mstat.planner.service.HeadcountService;
import ru.yandex.market.mstat.planner.service.NotificationService;
import ru.yandex.market.mstat.planner.service.ProjectService;
import ru.yandex.market.mstat.planner.service.ProjectWithRequestService;
import ru.yandex.market.mstat.planner.service.RequestService;
import ru.yandex.market.mstat.planner.service.SupervisorService;
import ru.yandex.market.mstat.planner.service.UserService;
import ru.yandex.market.mstat.planner.service.VacancyService;
import ru.yandex.market.mstat.planner.task.cron.HeadcountsLoader;
import ru.yandex.market.mstat.planner.task.cron.JobPeriodsCalculator;
import ru.yandex.market.mstat.planner.task.cron.RequestExpirator;
import ru.yandex.market.mstat.planner.util.LoggingJdbcTemplate;
import ru.yandex.market.mstat.planner.util.report.forgottenemployees.ForgottenEmployeesReportGenerator;
import ru.yandex.market.mstat.planner.utils.IntegrationTestData;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.roles.Roles;

import static org.mockito.Mockito.mock;

@Configuration
@Import({ReportGeneratorsConfiguration.class, DbConfig.class})
public class ServicesAndDaoConfig {

    @Autowired
    public LoggingJdbcTemplate loggingJdbcTemplate;

    @Bean
    public IntegrationTestData testData() {
        return new IntegrationTestData();
    }

    @Bean
    public EmployeeAndDepartmentDao employeeAndDepartmentDao() {
        return new EmployeeAndDepartmentDao(loggingJdbcTemplate);
    }

    @Bean
    public ContourDao contourDao(AuthInfoService authInfoService, DepartmentService departmentService) {
        return new ContourDao(loggingJdbcTemplate, authInfoService, departmentService);
    }

    @Bean
    public ContourTagDao contourTagDao() {
        return new ContourTagDao(loggingJdbcTemplate);
    }

    @Bean ContourChatDao contourChatDao() {
        return new ContourChatDao(loggingJdbcTemplate);
    }

    @Bean
    public ContourService contourService(ProjectService projectService,
                                         DepartmentService departmentService,
                                         AuthInfoService authInfoService,
                                         ContourChatsService contourChatsService,
                                         ContourDao contourDao,
                                         ContourTagDao contourTagDao,
                                         RequestService requestService
                                         ) {
        return new ContourService(projectService,
                departmentService,
                authInfoService,
                contourChatsService,
                contourDao,
                contourTagDao,
                requestService,
                mock(SupervisorService.class));
    }

    @Bean
    ContourChatsService contourChatsService(ContourChatDao contourChatDao) {
        return new ContourChatsService(contourChatDao);
    }

    @Bean
    public ProjectDao projectDao() {
        return new ProjectDao(loggingJdbcTemplate);
    }

    @Bean
    public ReportDao reportDao(DepartmentService departmentService) {
        return new ReportDao(loggingJdbcTemplate, departmentService);
    }

    @Bean
    public SpecializationController specializationController(DepartmentService departmentService) {
        return new SpecializationController(loggingJdbcTemplate, departmentService);
    }

    @Bean
    public ProjectService projectService(
            DepartmentService departmentService,
            EmployeeService employeeService,
            ProjectDao projectDao) {
        return new ProjectService(departmentService,
                employeeService,
                projectDao
        );
    }

    @Bean
    public EmployeeService employeeService() {
        return new EmployeeService();
    }

    @Bean
    public UserService userService(UserDao userDao) {
        return new UserService(userDao);
    }

    @Bean
    public UserDao userDao() {
        return new UserDao(loggingJdbcTemplate);
    }

    @Bean
    public DepartmentService departmentService() {
        return new DepartmentService(loggingJdbcTemplate, null);
    }

    @Bean
    public ProjectWithRequestDao projectWithRequestDao(DataSource ds) {
        return new ProjectWithRequestDao(ds, loggingJdbcTemplate);
    }

    @Bean
    public RequestDao requestDao() {
        return new RequestDao(loggingJdbcTemplate);
    }

    @Bean
    public CommentDao commentDao() {
        return new CommentDao(loggingJdbcTemplate);
    }

    @Bean
    public CommentService commentService(CommentDao commentDao) {
        return new CommentService(commentDao);
    }

    @Bean
    public RequestService requestService(
            RequestDao requestDao,
            NotificationService notificationService,
            CommentService commentService,
            DepartmentService departmentService,
            EmployeeService employeeService) {
        return new RequestService(requestDao, notificationService, commentService, departmentService,
                employeeService, mock(BazingaTaskManager.class));
    }

    @Bean
    public NotificationService notificationService() {
        return Mockito.mock(NotificationService.class);
    }

    @Bean
    public ProjectWithRequestService projectWithRequestService(
            ContourService contourService,
            ProjectService projectService,
            RequestService requestService,
            ProjectWithRequestDao projectWithRequestDao,
            EmployeeService employeeService,
            AuthInfoService authInfoService,
            DepartmentService departmentService) {
        return new ProjectWithRequestService(contourService, projectService, requestService,
                projectWithRequestDao, employeeService, authInfoService, departmentService);
    }

    @Bean
    public VacancyService vacancyService(
            LoggingJdbcTemplate jdbcTemplate,
            ProjectWithRequestService projectWithRequestService) {
        return new VacancyService(jdbcTemplate, projectWithRequestService);
    }

    @Bean
    public BusinessUnitDao businessUnitDao(
            LoggingJdbcTemplate jdbcTemplate) {
        return new BusinessUnitDao(jdbcTemplate);
    }

    @Bean
    public HeadcountClient headcountClient() {
        return mock(HeadcountClient.class);
    }

    @Bean
    public HeadcountDao headcountDao(LoggingJdbcTemplate jdbcTemplate) {
        return new HeadcountDao(jdbcTemplate);
    }

    @Bean
    public HeadcountService headcountService(HeadcountDao headcountDao,
                                             DepartmentService departmentService,
                                             ProjectWithRequestService projectWithRequestService) {
        return new HeadcountService(headcountDao, departmentService, projectWithRequestService);
    }

    @Bean
    public HeadcountsLoader headcountsLoader(HeadcountService headcountService, HeadcountClient client,
                                             EmployeeService employeeService, RequestService requestService,
                                             VacancyService vacancyService, LoggingJdbcTemplate jdbcTemplate,
                                             ProjectWithRequestService projectWithRequestService) {
        return new HeadcountsLoader(headcountService, employeeService, requestService, vacancyService,
                client, jdbcTemplate, projectWithRequestService);
    }

    @Bean
    public RequestExpirator requestExpirator(RequestService requestService) {
        return new RequestExpirator(requestService);
    }

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return mock(TvmTicketProvider.class);
    }

    @Bean
    public ComplexMonitoring complexMonitoring() {
        return mock(ComplexMonitoring.class);
    }

    @Bean
    public ForgottenEmployeesReportGenerator forgottenEmployeesReportGenerator() {
        return mock(ForgottenEmployeesReportGenerator.class);
    }

    @Bean
    public JobPeriodsDao jobPeriodsDao(LoggingJdbcTemplate jdbcTemplate) {
        return new JobPeriodsDao(jdbcTemplate);
    }

    @Bean
    public JobPeriodsCalculator jobPeriodsService(JobPeriodsDao jobPeriodsDao) {
        return new JobPeriodsCalculator();
    }

    @Bean
    public IdmController idmController(ObjectProvider<TvmClient> tvmClientProvider) {
        Objects.requireNonNull(tvmClientProvider.getObject());
        return new IdmController(tvmClientProvider, false);
    }

    @Bean
    public TvmClient fakeTvmClient() {
        return new FakeTvmClient();
    }

    public static class FakeTvmClient implements TvmClient {

        public static final String VALID_TOKEN = "abc";
        public static final String OTHER_TOKEN = "xxx";

        @Override
        public ClientStatus getStatus() {
            return null;
        }

        @Override
        public String getServiceTicketFor(String alias) {
            return null;
        }

        @Override
        public String getServiceTicketFor(int clientId) {
            return null;
        }

        @Override
        public CheckedServiceTicket checkServiceTicket(String ticketBody) {
            CheckedServiceTicket serviceTicket = Mockito.mock(CheckedServiceTicket.class);
            if (ticketBody.equals(VALID_TOKEN)) {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.OK);
                Mockito.when(serviceTicket.getSrc()).thenReturn(IdmController.PRODUCTION_IDM_TVM_ID);
            } else if (ticketBody.equals(OTHER_TOKEN)) {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.OK);
                Mockito.when(serviceTicket.getSrc()).thenReturn(424242);

            } else {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.MALFORMED);
                Mockito.when(serviceTicket.getSrc()).thenReturn(-1);
            }
            return serviceTicket;
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody) {
            return null;
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv overridedBbEnv) {
            return null;
        }

        @Override
        public Roles getRoles() {
            return null;
        }

        @Override
        public void close() {

        }
    }

}
