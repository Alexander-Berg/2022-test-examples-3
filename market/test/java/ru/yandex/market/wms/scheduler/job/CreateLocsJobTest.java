package ru.yandex.market.wms.scheduler.job;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateLocsJobTest extends SchedulerIntegrationTest {
    @Autowired
    private CreateLocsJob createLocsJob;

    @Mock
    private JobExecutionContext context;

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/admin-locs/before-create-locs.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/admin-locs/after-create-locs.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void executeJobTest() {
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        createLocsJob.execute(context);
    }
}
