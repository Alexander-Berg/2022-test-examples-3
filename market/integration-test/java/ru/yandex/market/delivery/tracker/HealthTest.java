package ru.yandex.market.delivery.tracker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.delivery.tracker.configuration.QuartzJobsConfiguration;
import ru.yandex.market.delivery.tracker.service.health.CachedHealthServiceStatuses;
import ru.yandex.market.delivery.tracker.service.health.CachedJobStatuses;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class HealthTest extends AbstractContextualTest {

    @Autowired
    private CachedHealthServiceStatuses cachedHealthServiceStatuses;
    @Autowired
    private CachedJobStatuses cachedJobStatuses;

    @Test
    void ping() throws Exception {
        String contentAsString = mockMvc.perform(get("/ping"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertions()
            .assertThat(contentAsString)
            .isEqualTo("0;ok");
    }

    @Test
    void pingAfterActualizing() throws Exception {

        cachedHealthServiceStatuses.actualizeResult();

        String contentAsString = mockMvc.perform(get("/ping"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertions()
            .assertThat(contentAsString)
            .isEqualTo("0;ok");
    }

    @Test
    void jobStatusAfterActualizingOnEmptyDatabase() throws Exception {

        cachedJobStatuses.checkHangingJobs();

        String contentAsString = mockMvc.perform(get("/jobStatus"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertions()
            .assertThat(contentAsString)
            .contains("availabilityCheckingBatchSupplierExecutor was not triggered yet")
            .contains("pushSolomonQueueSizeExecutor was not triggered yet")
            .contains("trackStatusSynchronizerExecutor was not triggered yet")
            .contains("synchronizeSourcesExecutor was not triggered yet")
            .contains("pushSolomonUnprocessedBatchesCountExecutor was not triggered yet")
            .contains("queueOrdersOfTracksForPushingExecutor was not triggered yet");
    }


    @Test
    @DatabaseSetup(type = DatabaseOperation.REFRESH, value = "/database/states/tms_jobs_3_failed.xml")
    void jobFailureByJobName() throws Exception {
        String jobName = "synchronizeSourcesExecutor";
        cachedJobStatuses.checkFailedJobs(jobName);

        String contentAsString = mockMvc.perform(get("/failedJobs/{jobName}", jobName))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertions()
            .assertThat(contentAsString)
            .as("1 failed job fired").contains("finished with error: Job Exception:")
            .doesNotContain("queueOrdersOfTracksForPushingExecutor")
            .doesNotContain("pushSolomonUnprocessedBatchesCountExecutor")
            .doesNotContain("pushSolomonQueueSizeExecutor");
    }

    @Test
    void allQuartzJobsExistInJobMonitoringConfig() {
        List<String> configJobNames =
            jdbcTemplate.queryForList("SELECT DISTINCT job_name FROM delivery_tracker.job_monitoring_config",
                String.class);

        List<String> quartzJobs = Arrays.stream(QuartzJobsConfiguration.class.getMethods())
            .filter(method -> method.isAnnotationPresent(CronTrigger.class))
            .map(this::getBeanName).collect(Collectors.toList());

        assertions().assertThat(configJobNames)
            .as("Jobs in quartz config and job configs table should be the same")
            .hasSameElementsAs(quartzJobs);
    }

    private String getBeanName(Method method) {
        return Optional.ofNullable(method.getAnnotation(Qualifier.class))
            .map(Qualifier::value).orElse(method.getName());
    }
}
