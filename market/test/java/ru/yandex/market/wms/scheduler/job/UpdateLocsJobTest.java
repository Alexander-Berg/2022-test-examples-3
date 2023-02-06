package ru.yandex.market.wms.scheduler.job;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.CreateLocDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateLocsJobTest extends SchedulerIntegrationTest {

    @Autowired
    private UpdateLocsJob updateLocsJob;

    @Mock
    private JobExecutionContext context;

    @Autowired
    @SpyBean
    private CreateLocDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/admin-locs/before-update-locs.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/admin-locs/after-update-locs.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void executeJobTest() throws JobExecutionException {
        when(dao.checkColumnExists(anyString())).thenReturn(true);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        updateLocsJob.execute(context);
    }
}
