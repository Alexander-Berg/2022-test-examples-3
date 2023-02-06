package ru.yandex.market.tsum.pipelines.idx.jobs;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.utils.ZoneUtils;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WaitDeployWindowJobTest {
    @Autowired
    private JobTester jobTester;

    private WaitDeployWindowJob getJob(WaitDeployWindowJob.WaitDeployWindowConfig config) {
        return jobTester
            .jobInstanceBuilder(WaitDeployWindowJob.class)
            .withResource(config)
            .create();
    }

    @Test
    public void deployWindowWorks() {
        WaitDeployWindowJob job = getJob(
            new WaitDeployWindowJob.WaitDeployWindowConfig(
                8,
                18,
                Sets.newHashSet(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
            )
        );

        Assert.assertTrue(job.checkDateInDeployWindow(ZonedDateTime.of(
            2018, 8, 28, 8, 0, 0, 0, ZoneId.of(ZoneUtils.MOSCOW_ZONE)
        )));

        Assert.assertFalse(job.checkDateInDeployWindow(ZonedDateTime.of(
            2018, 8, 28, 18, 0, 0, 0, ZoneId.of(ZoneUtils.MOSCOW_ZONE)
        )));

        Assert.assertFalse(job.checkDateInDeployWindow(ZonedDateTime.of(
            2018, 8, 28, 8, 0, 0, 0, ZoneId.of(ZoneUtils.YEKATERINBURG_ZONE)
        )));

        Assert.assertTrue(job.checkDateInDeployWindow(ZonedDateTime.of(
            2018, 8, 28, 18, 0, 0, 0, ZoneId.of(ZoneUtils.YEKATERINBURG_ZONE)
        )));
    }

    @Test
    public void deployWindowStringWorks() {
        String actualString = getJob(
            new WaitDeployWindowJob.WaitDeployWindowConfig(
                0,
                24,
                Sets.newHashSet(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
            )
        ).getDeployWindowString();

        Assert.assertEquals("0:00 to 24:00, Tue,Thu,Fri", actualString);
    }
}
