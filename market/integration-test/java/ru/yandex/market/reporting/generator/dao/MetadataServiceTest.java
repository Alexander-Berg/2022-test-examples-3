package ru.yandex.market.reporting.generator.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.config.IntegrationTestConfig;
import ru.yandex.market.reporting.generator.domain.Job;
import ru.yandex.market.reporting.generator.domain.JobParameters;
import ru.yandex.market.reporting.generator.domain.JobStatusEnum;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.Profile;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class MetadataServiceTest {

    @Autowired
    private NamedParameterJdbcTemplate metadataJdbcTemplate;

    @Autowired
    private MetadataService metadataService;

    @Before
    public void init() {
        sql("truncate jobs");
        sql("select pg_advisory_unlock_all()");
    }

    @Test
    public void mustOverrideProfile() {
        MarketReportParameters reportParameters = new MarketReportParameters();
        reportParameters.setShop("shop1");
        metadataService.saveProfile(new Profile("profile1", reportParameters));
        Profile before = metadataService.getProfile("profile1");
        assertThat(before.getParameters().getShop(), is("shop1"));
        reportParameters.setShop("shop2");
        metadataService.saveProfile(new Profile("profile1", reportParameters));
        Profile after = metadataService.getProfile("profile1");
        assertThat(after.getParameters().getShop(), is("shop2"));
    }

    @Test
    public void timeoutedMustFail() {
        String timeoutedJobId = newJob();
        assertThat(metadataService.getJobStatus(timeoutedJobId).getStatus(), is(JobStatusEnum.NEW));
        try {
            sql("alter table jobs disable trigger user");
            sql("update jobs set updated_at = '1998-07-06 00:00:00'");

        } finally {
            sql("alter table jobs enable trigger user");
        }
        metadataService.failTimeoutedJobs();
        assertThat(metadataService.getJobStatus(timeoutedJobId).getStatus(), is(JobStatusEnum.FAILED));
    }

    @Test
    public void mustBeOutdated() {
        String outdatedJobId = newJob();
        sql("update jobs set submitted_at = '1998-07-06 00:00:00'");
        assertTrue(metadataService.isJobOutdated(outdatedJobId));
    }

    @Test
    public void jobWithoutLockMustBeNotScheduled() {
        String notScheduledJobId = newJob();
        List<Job> nonScheduledJobs = metadataService.getNonScheduledJobs(2);
        assertThat(nonScheduledJobs.size(), is(1));
        assertThat(nonScheduledJobs.get(0).getJobId(), is(notScheduledJobId));
    }

    @Test
    public void jobWithLockMustNotBeNotScheduled() {
        String scheduledJobId = newJob();
        metadataService.getLock(scheduledJobId);
        List<Job> nonScheduledJobs = metadataService.getNonScheduledJobs(2);
        assertThat(nonScheduledJobs.size(), is(0));
    }

    @Test
    public void jobWithoutLockMustBeDangling() {
        String danglingJobId = newJob();
        metadataService.startJob(danglingJobId);
        List<Job> danglingJobs = metadataService.getDanglingJobs();
        assertThat(danglingJobs.size(), is(1));
        assertThat(danglingJobs.get(0).getJobId(), is(danglingJobId));
    }

    @Test
    public void jobWithLockMustNotBeDangling() {
        String notDanglingJobId = newJob();
        metadataService.startJob(notDanglingJobId);
        metadataService.getLock(notDanglingJobId);
        List<Job> danglingJobs = metadataService.getDanglingJobs();
        assertThat(danglingJobs.size(), is(0));
    }

    private String newJob() {
        return metadataService.addNewJob("user1", "profile1", new JobParameters<>());
    }

    private void sql(String query) {
        metadataJdbcTemplate.getJdbcOperations().execute(query);
    }
}
