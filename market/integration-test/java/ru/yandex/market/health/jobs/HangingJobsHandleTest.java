package ru.yandex.market.health.jobs;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.health.jobs.rest.TmsHealthRestController;

@TestPropertySource("classpath:application-integration-test.properties")
@ActiveProfiles("HangingJobs")
public class HangingJobsHandleTest extends AbstractTest {

    @Autowired
    protected TmsHealthRestController restController;

    @Test
    public void longExecutionJobOK() {
        prepareDataSetAndWaitCacheEviction("classpath:db/8.xml", 0);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    public void longExecutionSpecificJobOK() {
        prepareDataSetAndWaitCacheEviction("classpath:db/8.xml", 0);
        String res = fireHangingJob("testJob8");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    public void longExecutionJobWarn() {
        prepareDataSetAndWaitCacheEviction("classpath:db/2.xml", -2);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob2 hangs since");
        });
    }

    @Test
    public void longExecutionSpecificJobWarn() {
        prepareDataSetAndWaitCacheEviction("classpath:db/2.xml", -2);
        String res = fireHangingJob("testJob2");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob2 hangs since");
        });
    }

    @Test
    public void longExecutionJobCrit() {
        prepareDataSetAndWaitCacheEviction("classpath:db/7.xml", -2);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob7 hangs since");
        });
    }

    @Test
    public void longExecutionSpecificJobCrit() {
        prepareDataSetAndWaitCacheEviction("classpath:db/7.xml", -2);
        String res = fireHangingJob("testJob7");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob7 hangs since");
        });
    }

    @Test
    public void longDelayJobOk() {
        prepareDataSetAndWaitCacheEviction("classpath:db/10.xml", 0);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    public void longDelaySpecificJobOk() {
        prepareDataSetAndWaitCacheEviction("classpath:db/10.xml", 0);
        String res = fireHangingJob("testJob10");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    public void longDelayJobWarn() {
        prepareDataSetAndWaitCacheEviction("classpath:db/3.xml", -1);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob3 has not started");
        });
    }

    @Test
    public void
    longDelaySpecificJobWarn() {
        prepareDataSetAndWaitCacheEviction("classpath:db/3.xml", -1);
        String res = fireHangingJob("testJob3");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob3 has not started");
        });
    }

    @Test
    public void longDelayJobCrit() {
        prepareDataSetAndWaitCacheEviction("classpath:db/9.xml", -3);
        String res = fireHangingJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob9 has not started");
        });
    }

    @Test
    public void longDelaySpecificJobCrit() {
        prepareDataSetAndWaitCacheEviction("classpath:db/9.xml", -3);
        String res = fireHangingJob("testJob9");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob9 has not started");
        });
    }

    private void prepareDataSetAndWaitCacheEviction(String configLocation, int hoursToAdd) {
        prepareDataSet(configLocation, hoursToAdd);
        waitCacheEviction();
    }
}
