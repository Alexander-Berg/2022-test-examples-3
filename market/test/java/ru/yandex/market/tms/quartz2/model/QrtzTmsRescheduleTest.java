package ru.yandex.market.tms.quartz2.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

@ContextConfiguration
@ActiveProfiles("development")
class QrtzTmsRescheduleTest extends FunctionalTest {

    private static final String EXPECTED_CRON_EXPRESSION = "0 0 1 1 * ? 2036";
    @Autowired
    private JobService jobService;

    @Test
    @DbUnitDataSet
    void testTmsReschedule() throws SchedulerException {
        Collection<JobInfo> allJobs = jobService.getAllJobs();
        List<JobInfo> jobsInfo = new ArrayList<>(allJobs);
        JobInfo jobInfoTemplate = jobsInfo.get(0);
        JobInfo expected = new JobInfo(
                jobInfoTemplate.getJobKey(),
                jobInfoTemplate.getDescription(),
                jobInfoTemplate.getJobDataMap(),
                Collections.singletonMap(TriggerKey.triggerKey(jobInfoTemplate.getJobName()), EXPECTED_CRON_EXPRESSION),
                jobInfoTemplate.getPreviousJobFire()
        );
        jobService.rescheduleJob("testExecutor", EXPECTED_CRON_EXPRESSION);
        allJobs = jobService.getAllJobs();
        jobsInfo = new ArrayList<>(allJobs);
        Assertions.assertEquals(expected, jobsInfo.get(0));
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class
    })
    public static class Config {

        @Bean
        @CronTrigger(
                cronExpression = "0 0 12 1 * ? 2042",
                description = "Test executor"
        )
        public Executor testExecutor() {
            return context -> {
            };
        }

    }

}
