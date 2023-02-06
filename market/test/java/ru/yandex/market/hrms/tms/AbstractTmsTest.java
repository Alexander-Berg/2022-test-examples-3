package ru.yandex.market.hrms.tms;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.hrms.tms.config.HrmsTmsTestConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

@ActiveProfiles(TplProfiles.TESTS)
@TestPropertySource({"classpath:test-application.properties", "classpath:test-tms.properties"})
@SpringBootTest(
        classes = HrmsTmsTestConfig.class
)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class
)
@DbUnitDataSet(nonTruncatedTables = {
        "public.databasechangelog",
        "public.databasechangeloglock",
        "public.hr_operation_type",
        "public.separate_division",
        "public.position_rank",
        "public.gap_workflow",
        "public.keyboard",
        "tms.qrtz_fired_triggers",
        "tms.qrtz_paused_trigger_grps",
        "tms.qrtz_scheduler_state",
        "tms.qrtz_locks",
        "tms.qrtz_simple_triggers",
        "tms.qrtz_cron_triggers",
        "tms.qrtz_simprop_triggers",
        "tms.qrtz_blob_triggers",
        "tms.qrtz_triggers",
        "tms.qrtz_job_details",
        "tms.qrtz_calendars",
        "tms.qrtz_log"
})
@AutoConfigureMockMvc
public abstract class AbstractTmsTest {

    @Autowired
    protected MockMvc mockMvc;

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

    protected void mockClock(LocalDate date) {
        mockClock(date.atStartOfDay());
    }

    protected void mockClock(LocalDateTime dateTime) {
        clock.setFixed(DateTimeUtil.atDefaultZone(dateTime), DateTimeUtil.DEFAULT_ZONE_ID);
    }

    protected void mockClock(Instant instant) {
        mockClock(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }
}
