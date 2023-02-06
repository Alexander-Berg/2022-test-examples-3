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
public class CleanEmptyLocationsJobTest extends SchedulerIntegrationTest {

    @Autowired
    private CleanEmptyLocationsJob cleanEmptyLocationsJob;

    @Mock
    private JobExecutionContext context;

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/empty-loc/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/empty-loc/after-removing.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void deleteLocationFromEmptyLocTest() {
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        cleanEmptyLocationsJob.execute(context);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/empty-loc/before-with-balances.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/empty-loc/after-removing-with-balances.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void deleteLocationFromEmptyLocWhenBalanceExistsTest() {
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        cleanEmptyLocationsJob.execute(context);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup(value = "/db/dao/empty-loc/before-with-FK.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/empty-loc/after-removing-with-fk.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void deleteLocationFromEmptyLocWhenFKConstraintExceptionTest() {
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        cleanEmptyLocationsJob.execute(context);
    }
}
