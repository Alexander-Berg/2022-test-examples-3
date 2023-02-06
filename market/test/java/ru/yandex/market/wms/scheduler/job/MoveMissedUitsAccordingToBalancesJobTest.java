package ru.yandex.market.wms.scheduler.job;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.wms.scheduler.dao.BalancesSerialInventoryDao.BATCH_SIZE_CONFIG_NAME;

@ExtendWith(MockitoExtension.class)
public class MoveMissedUitsAccordingToBalancesJobTest extends SchedulerIntegrationTest {

    @Mock
    private JobExecutionContext context;
    @SpyBean
    @Autowired
    private DbConfigService dbConfigService;
    @Autowired
    private MoveMissedUitsAccordingToBalancesJob moveMissedUitsAccordingToBalancesJob;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(dbConfigService.getConfigAsInteger(BATCH_SIZE_CONFIG_NAME, 0)).thenReturn(2);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/balances/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/balances/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void snIdUpdates_WhenMovingJobExecuted() {
        moveMissedUitsAccordingToBalancesJob.execute(context);
    }
}
