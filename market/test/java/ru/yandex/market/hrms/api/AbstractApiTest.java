package ru.yandex.market.hrms.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.hrms.api.config.TestHrmsApiConfig;
import ru.yandex.market.hrms.api.util.FileTestUtil;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.startrek.client.Session;

@ActiveProfiles(TplProfiles.TESTS)
@TestPropertySource({
        "classpath:test-application.properties",
        "classpath:test-api.properties"
})
@SpringBootTest(
        classes = TestHrmsApiConfig.class
)
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class
)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
@DbUnitDataSet(nonTruncatedTables = {
        "public.databasechangelog",
        "public.databasechangeloglock",
        "public.hr_operation_type",
        "public.keyboard",
        "public.position_rank",
        "public.gap_workflow"
})
public abstract class AbstractApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TestableClock clock;
    @Autowired
    private Session trackerSession;

    protected MockMvcWithDomainId mockMvc = new MockMvcWithDomainId();

    @BeforeEach
    public void setUp() {
        mockMvc.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .alwaysDo(log())
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(trackerSession);
    }

    protected String loadFromFile(String filename) {
        return StringTestUtil.getString(getClass(), filename);
    }

    protected byte[] loadFileAsBytes(String filename) {
        return FileTestUtil.loadFileAsBytes(getClass(), filename);
    }

    protected void mockClock(LocalDate date) {
        mockClock(date.atStartOfDay());
    }

    protected void mockClock(LocalDateTime dateTime) {
        clock.setFixed(DateTimeUtil.atDefaultZone(dateTime), DateTimeUtil.DEFAULT_ZONE_ID);
    }

    protected void mockClock(Instant instant) {
        clock.setFixed(instant, ZoneId.systemDefault());
    }

    private MvcTestLogWriter log() {
        return new MvcTestLogWriter();
    }
}
