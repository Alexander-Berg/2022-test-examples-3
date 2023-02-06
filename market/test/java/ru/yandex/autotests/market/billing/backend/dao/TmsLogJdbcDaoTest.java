package ru.yandex.autotests.market.billing.backend.dao;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.market.billing.backend.core.console.ConsoleConfig;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.jobstate.JobState;
import ru.yandex.autotests.market.billing.backend.core.dao.tmslog.TmsLogDao;
import ru.yandex.autotests.market.billing.backend.core.dao.tmslog.TmsLogDaoFactory;
import ru.yandex.autotests.market.common.tmsconsole.TmsConsole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: jkt
 * Date: 12.10.12
 * Time: 14:47
 */
public class TmsLogJdbcDaoTest {

    private static final String JOB_FOR_TESTING = "getClicksExecutor";

    private static final Logger LOG = Logger.getLogger(TmsLogJdbcDaoTest.class);

    private TmsLogDao dao;

    @Before
    public void initDao() {
        dao = TmsLogDaoFactory.getBillingInstance();
    }

    @Ignore
    @Test
    public void testGetJobLastFireState() {
        JobState lastFireState = dao.getJobLastFireState(JOB_FOR_TESTING);
        assertThat(lastFireState).as("Last fire state can not be null").isNotNull();
        assertThat(lastFireState.getJobName()).as("Job name is null.").isNotNull();
        LOG.debug(lastFireState);
    }

    @Ignore
    @Test
    public void testWaitForJobToFinish() {
        ConsoleConfig config = new ConsoleConfig();
        TmsConsole tmsConsole = TmsConsole.startSession(
                config.getBillingConsoleHost(),
                config.getBillingConsolePort(),
                config.getJobTriggersFile());
        tmsConsole.runJob(JOB_FOR_TESTING);
        JobState jobStateAfterWait = dao.waitForJobToFinish(JOB_FOR_TESTING);
        assertThat(jobStateAfterWait)
                .as("Job should run on tms console command request, but it did not")
                .isNotNull();
        assertThat(jobStateAfterWait.getStatus())
                .as("Job is still working after wait. " + jobStateAfterWait)
                .isNotNull();
    }

    @Ignore
    @Test
    public void testWaitForJobToSucceed() throws Exception {
        JobState jobState = dao.waitForJobToSucceed(JOB_FOR_TESTING);
        assertThat(jobState.isSuccessful()).isTrue();
    }


}
