package ru.yandex.market.health.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.health.jobs.rest.TmsHealthRestController;
import ru.yandex.market.tms.quartz2.service.JobService;

@TestPropertySource("classpath:application-integration-test.properties")
@ActiveProfiles("FailedJobs")
public class FailedJobsHandleTest extends AbstractTest {

    @Autowired
    protected TmsHealthRestController restController;

    @Autowired
    protected JobService jobService;

    @Test
    @DatabaseSetup("classpath:db/6.xml")
    public void failedOk() {
        String res = fireFailedJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/6.xml")
    public void failedOKSpecificJob() {
        String res = fireFailedJob("testJob6");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;");
            assertions.assertThat(res).contains("OK");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/4.xml")
    public void failedWarn() {
        String res = fireFailedJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob4 failed at");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/4.xml")
    public void failedWarnSpecificJob() {
        String res = fireFailedJob("testJob4");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("1;");
            assertions.assertThat(res).contains("Job testJob4 failed at");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/5.xml")
    public void failedCrit() {
        String res = fireFailedJobs();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob5 failed at");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/5.xml")
    public void failedCritSpecificJob() {
        String res = fireFailedJob("testJob5");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("2;");
            assertions.assertThat(res).contains("Job testJob5 failed at");
            assertions.assertThat(res).contains("FAILED due to some exception");
        });
    }

    @Test
    @DatabaseSetup("classpath:db/11.xml")
    public void failedWithNullStatusIsIgnored() {
        String res = fireFailedJob("testJob11");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(res).startsWith("0;OK");
        });
    }
}
