package ru.yandex.market.logistics.logging.scheduled;

import java.util.List;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.logistics.logging.scheduled.dao.ScheduledTaskDao;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringTestConfiguration.class})
@RunWith(SpringRunner.class)
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = {
        DbUnitTestExecutionListener.class,
    }
)
public class ScheduledJobsLoggerTest {

    private static final String JOB_NAME = "some job name";
    private static final String HOST_NAME = "some host name";

    private static final long START_TIME = 100;
    private static final long FINISH_TIME = 200;

    private final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    private ScheduledTaskDao scheduledTaskDao;

    @Test
    @ExpectedDatabase(
        value = "/data/scheduled/after/add_log_entry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void logToDbTest() {
        ScheduledTaskEntry scheduledTaskEntry = new ScheduledTaskEntry()
            .setName(JOB_NAME)
            .setHost(HOST_NAME)
            .setStartTime(START_TIME)
            .setStatus(JobExecutionStatus.OK);

        scheduledTaskDao.add(scheduledTaskEntry);
    }

    @Test
    @DatabaseSetup(value = "/data/scheduled/before/get_log_entry.xml")
    @ExpectedDatabase(
        value = "/data/scheduled/after/get_log_entry.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void getLogFromDbTest() {
        List<ScheduledTaskEntry> result = scheduledTaskDao.findLastNByName(JOB_NAME, 1);

        softly.assertThat(result.size()).isEqualTo(1);
        softly.assertThat(result.get(0).getId()).isEqualTo(1);
        softly.assertThat(result.get(0).getFailCause()).isEqualTo(null);
        softly.assertThat(result.get(0).getName()).isEqualTo(JOB_NAME);
        softly.assertThat(result.get(0).getHost()).isEqualTo(HOST_NAME);
        softly.assertThat(result.get(0).getStartTime()).isEqualTo(START_TIME);
        softly.assertThat(result.get(0).getFinishTime()).isEqualTo(FINISH_TIME);
        softly.assertThat(result.get(0).getStatus()).isEqualTo(JobExecutionStatus.OK);
    }

    @Test
    @DatabaseSetup(value = "/data/scheduled/before/delete_log_entries.xml")
    @ExpectedDatabase(
        value = "/data/scheduled/after/delete_log_entries.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void deleteFromDbTest() {
        scheduledTaskDao.deleteAllByFinishTimeBefore(350);
    }
}
