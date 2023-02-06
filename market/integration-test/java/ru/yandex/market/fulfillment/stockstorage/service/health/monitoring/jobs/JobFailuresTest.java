package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.JobName;

public class JobFailuresTest extends AbstractContextualTest {

    @Autowired
    JobInfoDao jobInfoDao;

    @Test
    public void getJobName() {
        List<JobMonitoringConfigRow> configs = jobInfoDao.getJobMonitoringConfig();

        configs.forEach(config ->
                softly
                        .assertThat(JobFailures.getJobName(config.getJobName()))
                        .doesNotContain(JobName.UNKNOWN_JOB.name()));
    }

}
