package ru.yandex.market.logistics.management.service.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitConfiguration(
        databaseConnection = "dbUnitDatabaseConnectionDbQueue"
)
public class DbQueueHealthControllerTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("/data/repository/health/dbqueue/has_failed_checker_tasks.xml")
    void hasFailedJobsFailsTest() throws Exception {
        mockMvc.perform(get("/health/snapshot_diff_task/failed"))
            .andExpect(status().isOk())
            .andExpect(content().string("2;Some jobs are failed: " +
                "{name='SNAPSHOT_DIFF_TASK', retrying=1, failed=1}"));
    }

    @Test
    @DatabaseSetup("/data/repository/health/dbqueue/has_retrying_checker_tasks.xml")
    void hasRetryingJobsWarnTest() throws Exception {
        mockMvc.perform(get("/health/snapshot_diff_task/failed"))
            .andExpect(status().isOk())
            .andExpect(content().string("1;Some jobs are retrying over threshold: " +
                "{name='SNAPSHOT_DIFF_TASK', retrying=1, failed=0}"));
    }

    @Test
    @DatabaseSetup("/data/repository/health/dbqueue/has_no_failed_tasks.xml")
    void noFailedJobsSuccessfulTest() throws Exception {
        mockMvc.perform(get("/health/snapshot_diff_task/failed"))
            .andExpect(status().isOk())
            .andExpect(content().string("0;OK"));
    }

    @Test
    void badInputHandled() throws Exception {
        mockMvc.perform(get("/health/some_nonexistent_queue/failed"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                "2;Exception acquired on health checking: Trying to get dbQueue monitoring for " +
                "non-existent queue: some_nonexistent_queue"));
    }

}
