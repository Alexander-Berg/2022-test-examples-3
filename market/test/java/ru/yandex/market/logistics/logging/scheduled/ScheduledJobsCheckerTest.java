package ru.yandex.market.logistics.logging.scheduled;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.logistics.logging.scheduled.dao.ScheduledTaskDao;
import ru.yandex.market.logistics.logging.scheduled.health.ScheduledJobsChecker;

/**
 * Test for {@link ScheduledJobsChecker}.
 */
@RunWith(Parameterized.class)
public class ScheduledJobsCheckerTest {
    @Mock
    private ScheduledTaskDao scheduledTaskDao;

    private ScheduledJobsChecker scheduledJobsChecker;

    @Parameterized.Parameter
    public List<ScheduledTaskEntry> scheduledTaskEntries;
    @Parameterized.Parameter(1)
    public CheckResult.Level level;
    @Parameterized.Parameter(2)
    public String resultMessage;

    private static final String JOB_NAME = "test job";

    private static final List<ScheduledTaskEntry> ONE_OK = Collections.singletonList(
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(12L)
            .setFinishTime(13L)
            .setStatus(JobExecutionStatus.OK)
    );
    private static final List<ScheduledTaskEntry> ONE_FAIL = Collections.singletonList(
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(13L)
            .setFinishTime(14L)
            .setStatus(JobExecutionStatus.ERROR)
    );
    private static final List<ScheduledTaskEntry> TWO_FAIL = Arrays.asList(
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(14L)
            .setFinishTime(15L)
            .setStatus(JobExecutionStatus.ERROR),
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(13L)
            .setFinishTime(14L)
            .setStatus(JobExecutionStatus.ERROR)
    );
    private static final List<ScheduledTaskEntry> FAIL_THEN_OK = Arrays.asList(
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(14L)
            .setFinishTime(15L)
            .setStatus(JobExecutionStatus.OK),
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(13L)
            .setFinishTime(14L)
            .setStatus(JobExecutionStatus.ERROR)
    );
    private static final List<ScheduledTaskEntry> OK_THEN_FAIL = Arrays.asList(
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(14L)
            .setFinishTime(15L)
            .setStatus(JobExecutionStatus.ERROR),
        new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost("any")
            .setStartTime(13L)
            .setFinishTime(14L)
            .setStatus(JobExecutionStatus.OK)
    );

    @Parameterized.Parameters()
    public static Iterable<Object[]> dataForTest() {
        return Arrays.asList(new Object[][] {
            {new ArrayList<>(), CheckResult.Level.OK, ""},
            {ONE_OK, CheckResult.Level.OK, ""},
            {ONE_FAIL, CheckResult.Level.WARNING, "Warn in " + JOB_NAME + ";"},
            {TWO_FAIL, CheckResult.Level.CRITICAL, "Crit in " + JOB_NAME + ";"},
            {FAIL_THEN_OK, CheckResult.Level.OK, ""},
            {OK_THEN_FAIL, CheckResult.Level.WARNING, "Warn in " + JOB_NAME + ";"},
        });
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.scheduledJobsChecker = new ScheduledJobsChecker(scheduledTaskDao);
    }

    @Test
    public void returnsCorrectStatus() {
        Mockito.doReturn(scheduledTaskEntries).when(scheduledTaskDao).findLastNByName(JOB_NAME, 2);
        scheduledJobsChecker.addLoggedJob(getLoggedJobInstance(JOB_NAME, 1, 2));

        CheckResult checkResult = scheduledJobsChecker.doCheck();

        Assert.assertEquals(level, checkResult.getLevel());
        Assert.assertEquals(resultMessage, checkResult.getMessage());
    }


    private LoggedJob getLoggedJobInstance(String name, int failsToWarnAfter, int failsToCritAfter) {
        return new LoggedJob() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return LoggedJob.class;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public int failsToWarnAfter() {
                return failsToWarnAfter;
            }

            @Override
            public int failsToCritAfter() {
                return failsToCritAfter;
            }
        };
    }
}
