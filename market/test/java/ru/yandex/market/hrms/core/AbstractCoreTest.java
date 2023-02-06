package ru.yandex.market.hrms.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.hrms.core.config.HrmsCoreTestConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

@ActiveProfiles(TplProfiles.TESTS)
@TestPropertySource("classpath:test-application.properties")
@SpringBootTest(
        classes = HrmsCoreTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
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
        "public.violations"
})
public abstract class AbstractCoreTest {
    @Autowired
    private List<WireMockServer> mocks;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void setUpBase() {
        mocks.forEach(WireMockServer::resetToDefaultMappings);
    }

    @AfterEach
    public void tearDownBase() {
        clock.clearFixed();
    }

    protected String loadFromFile(String filename) {
        return StringTestUtil.getString(getClass(), filename);
    }

    protected void mockClock(Instant instant) {
        clock.setFixed(instant, ZoneId.systemDefault());
    }

    protected void mockClock(LocalDate date) {
        mockClock(date.atStartOfDay());
    }

    protected void mockClock(LocalDateTime dateTime) {
        clock.setFixed(DateTimeUtil.atDefaultZone(dateTime), DateTimeUtil.DEFAULT_ZONE_ID);
    }

}
