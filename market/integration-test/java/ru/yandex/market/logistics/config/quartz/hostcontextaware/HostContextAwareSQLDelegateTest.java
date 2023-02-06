package ru.yandex.market.logistics.config.quartz.hostcontextaware;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.spi.OperableTrigger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@DatabaseSetup(
    value = "/data/prepare/basic_schedule.xml",
    connection = "dbUnitDatabaseConnection"
)
class HostContextAwareSQLDelegateTest extends AbstractContextualTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MasterEnvironmentPredicate masterEnvironmentPredicate;

    private HostContextAwareSQLDelegate delegate;

    @BeforeEach
    void setUp() throws NoSuchDelegateException {
        delegate = new HostContextAwareSQLDelegate();
        delegate.initialize(
            log,
            "qrtz_",
            "qrtz2-scheduler",
            null,
            null,
            false,
            null
        );
    }

    @Test
    void selectTrigger_returnsNullWhenTriggerDoesNotExist()
        throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {

        OperableTrigger trigger = delegate.selectTrigger(
            dataSource.getConnection(),
            new TriggerKey("nonExistentTrigger")
        );
        assertThat(trigger).isNull();
    }

    @Test
    public void selectTrigger_returnsTriggerWhenTriggerExists()
        throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {

        OperableTrigger trigger = delegate.selectTrigger(
            dataSource.getConnection(),
            new TriggerKey("testTrigger")
        );
        assertThat(trigger).isNotNull();
        assertThat(trigger.getJobKey()).isEqualTo(new JobKey("testJob"));
    }

    @Test
    void selectTrigger_returnsTriggerWhenTriggerShouldExecuteOnMasterAndEnvIsMaster()
        throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {
        when(masterEnvironmentPredicate.isMaster(any())).thenReturn(true);

        OperableTrigger trigger = delegate.selectTrigger(
            dataSource.getConnection(),
            new TriggerKey("testTriggerWithJobData")
        );
        assertThat(trigger).isNotNull();
        assertThat(trigger.getJobKey()).isEqualTo(new JobKey("testJobWithJobData"));
    }

    @Test
    void selectTrigger_returnsNullWhenTriggerShouldExecuteOnMasterAndEnvIsMaster()
        throws SQLException, JobPersistenceException, IOException, ClassNotFoundException {
        when(masterEnvironmentPredicate.isMaster(any())).thenReturn(false);

        OperableTrigger trigger = delegate.selectTrigger(
            dataSource.getConnection(),
            new TriggerKey("testTriggerWithJobData")
        );
        assertThat(trigger).isNull();
    }
}
