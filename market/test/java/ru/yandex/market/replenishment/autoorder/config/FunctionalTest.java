package ru.yandex.market.replenishment.autoorder.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitRefreshMatViews;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;
import ru.yandex.market.replenishment.autoorder.utils.OsChecker;
import ru.yandex.market.replenishment.autoorder.utils.TableNamesTestQueryService;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;
import ru.yandex.market.yql_test.YqlTestConfiguration;
import ru.yandex.market.yql_test.test_listener.YqlPrefilledDataTestListener;
import ru.yandex.market.yql_test.test_listener.YqlTestListener;

import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@TestExecutionListeners(value = {
    DependencyInjectionTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    YqlTestListener.class,
    YqlPrefilledDataTestListener.class,
    MockitoTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class
})
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        ApplicationConfig.class,
        MockConfig.class,
        DbConfiguration.class,
        TableNamesTestQueryService.class,
        YqlTestConfiguration.class,
        AuditTestingHelper.class,
        SolomonTestJvmConfig.class,
        DeepmindHttpClientTestConfig.class
    },
    properties = "spring.liquibase.enabled=false" // liquibase конфигурируем вручную
)
@TestPropertySource(locations = "classpath:functional-test.properties")
@ActiveProfiles("unittest")
@DbUnitDataSet(
    before = "/Data.csv",
    nonTruncatedTables = {
        "autoorder.databasechangelog",
        "autoorder.databasechangeloglock",
    })
@DbUnitRefreshMatViews
public abstract class FunctionalTest {

    static {
        // Руками прописываем путь до нужного файла с конфигами
        // Файл должен обязательно называться log4j2-test.xml иначе log4j2 будет брать файл log4j2-test.xml,
        // который предназначен для локального запуска.
        // То же самое будет если вообще не указывать пропертю
        System.setProperty("logging.config", "classpath:log4j2-test.xml");
    }

    @LocalServerPort
    protected int randomServerPort;

    @Autowired
    protected TimeService timeService;

    @Autowired
    protected WorkbookConfiguration workbookConfiguration;

    @Autowired
    protected JavaMailSender javaMailSender;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.info("Starting test: " + description.getClassName() + " " + description.getMethodName());
        }
    };

    @Before
    public void refreshMocks() {
        setTestTime(LocalDateTime.now());
        when(workbookConfiguration.getEmptyWorkbook()).thenAnswer(invocation -> (OsChecker.getOsType().equals("linux"))
            ? new XSSFWorkbook()
            : new SXSSFWorkbook()
        );
        Mockito.doAnswer(invocation -> TestUtils.mockWorkbook(invocation.getArgument(0)))
            .when(workbookConfiguration).getWorkbook(Mockito.any());
        Mockito.doAnswer(invocation -> TestUtils.mockWorkbook(invocation.getArgument(0)))
            .when(workbookConfiguration).getWorkbook(Mockito.any(), Mockito.anyInt());
        when(javaMailSender.createMimeMessage()).thenCallRealMethod();
        Mockito.doNothing().when(javaMailSender).send(Mockito.any(SimpleMailMessage.class));
    }

    protected void setTestTime(LocalDateTime time) {
        TestUtils.mockTimeService(timeService, time);
    }

    protected void setTestTime(LocalDate date) {
        setTestTime(LocalDateTime.of(date, LocalTime.of(0, 0, 0)));
    }

}
