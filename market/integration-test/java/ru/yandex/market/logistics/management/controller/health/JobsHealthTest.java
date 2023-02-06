package ru.yandex.market.logistics.management.controller.health;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Тесты для {@link ru.yandex.market.logistics.management.controller.HealthController}
 */

@CleanDatabase
class JobsHealthTest extends AbstractContextualTest {

    private static final String FAILED_JOBS_URL = "/health/failedJobs";
    private static final String HANGING_JOBS_URL = "/health/hangingJobs";

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2018-07-03T12:29:00Z"), ZoneId.systemDefault());
    }

    @Test
    void failedJobsConfigNotFound() throws Exception {
        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .isEqualTo("2;hourlyReportStockStateExecutor has no monitoring config");
    }

    @Test
    @Sql({"/data/controller/health/job_monitoring_config.sql", "/data/controller/health/failed_jobs.sql"})
    void failedJobsFailed() throws Exception {
        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .contains("2;hourlyReportStockStateExecutor failed 1 times, last failed at 2018-07-03 15:10:00.056");
    }

    @Test
    @Sql({"/data/controller/health/job_monitoring_config.sql", "/data/controller/health/failed_jobs_ok.sql"})
    void failedJobsOk() throws Exception {
        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .contains("0;OK");
    }

    @Test
    @Sql({"/data/controller/health/job_monitoring_config.sql", "/data/controller/health/failed_jobs_ok.sql"})
    void failedJobsDelayExceeded() throws Exception {
        clock.setFixed(Instant.parse("2018-07-03T15:31:00Z"), ZoneId.systemDefault());

        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .contains("2;hourlyReportStockStateExecutor last finished delay exceeded");
    }

    @Test
    @Sql({
        "/data/controller/health/job_monitoring_config_multiple_fails.sql",
        "/data/controller/health/failed_jobs_multiple_ok.sql"
    })
    void failedJobsMultipleOk() throws Exception {
        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .contains("0;OK");
    }

    @Test
    @Sql({
        "/data/controller/health/job_monitoring_config_multiple_fails.sql",
        "/data/controller/health/failed_jobs_multiple_failed.sql"
    })
    void failedJobsMultipleFailed() throws Exception {
        String contentAsString = mockMvc.perform(get(FAILED_JOBS_URL + "/hourlyReportStockStateExecutor"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .contains("2;hourlyReportStockStateExecutor failed 4 times, last failed at 2018-07-03 15:09:00.056");
    }

    @Test
    @Sql({"/data/controller/health/job_monitoring_config.sql",
        "/data/controller/health/hanging_jobs.sql",
        "/data/controller/health/triggers.sql"})
    void hangingJobsHealed() throws Exception {
        mockMvc.perform(get(HANGING_JOBS_URL));

        List<String> states =
            jdbcTemplate.queryForList("SELECT DISTINCT trigger_state FROM qrtz.triggers", String.class);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM qrtz.fired_triggers", Long.class);
        softly
            .assertThat(count)
            .as("Fired triggers should become empty").isEqualTo(0);

        softly.assertThat(states)
            .as("Triggers should only ne in state waiting").containsExactly("WAITING");
    }

    @Test
    void noHangedJobsFound() throws Exception {
        String contentAsString = mockMvc.perform(get(HANGING_JOBS_URL))
            .andReturn()
            .getResponse()
            .getContentAsString();
        softly
            .assertThat(contentAsString)
            .isEqualTo("0;OK");
    }
}
