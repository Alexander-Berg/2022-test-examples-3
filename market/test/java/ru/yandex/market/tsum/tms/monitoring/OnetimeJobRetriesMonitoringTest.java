package ru.yandex.market.tsum.tms.monitoring;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.bazinga.BazingaControllerApp;
import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.commune.bazinga.impl.JobId;
import ru.yandex.commune.bazinga.impl.JobInfoValue;
import ru.yandex.commune.bazinga.impl.JobStatus;
import ru.yandex.commune.bazinga.impl.OnetimeJob;
import ru.yandex.commune.bazinga.impl.OnetimeTaskState;
import ru.yandex.commune.bazinga.impl.TaskId;
import ru.yandex.commune.bazinga.impl.controller.BazingaController;
import ru.yandex.commune.bazinga.impl.storage.BazingaStorage;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.03.2018
 */
public class OnetimeJobRetriesMonitoringTest {
    private ComplicatedMonitoring complicatedMonitoring;
    private BazingaStorage bazingaStorage;
    private OnetimeJobRetriesMonitoring sut;
    private BazingaController bazingaController;

    private static final TaskId taskId1 = new TaskId("taskId1");
    private static final TaskId taskId2 = new TaskId("taskId2");

    @Before
    public void setUp() {
        complicatedMonitoring = new ComplicatedMonitoring();
        bazingaStorage = mock(BazingaStorage.class);
        BazingaControllerApp bazingaControllerApp = mock(BazingaControllerApp.class);
        sut = new OnetimeJobRetriesMonitoring(complicatedMonitoring, bazingaControllerApp, bazingaStorage);
        sut.setRetryCountToWarn(5);
        bazingaController = mock(BazingaController.class);
        when(bazingaControllerApp.getBazingaController()).thenReturn(bazingaController);
    }

    @Test
    public void noJobs() {
        run(true, Cf.list(), Cf.list());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void notMaster() {
        run(false, Cf.list(), Cf.list());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void noFailingJobs() {
        run(true, Cf.list(job(4, JobStatus.READY)), Cf.list(job(5, JobStatus.STARTING)));
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void oneFailingJobInTask1() {
        run(true, Cf.list(job(6, JobStatus.RUNNING)), Cf.list(job(5, JobStatus.FAILED)));
        assertMonitoringStatusIs(MonitoringStatus.WARNING);
    }

    @Test
    public void oneFailingJobInTask2() {
        run(true, Cf.list(job(5, JobStatus.EXPIRED)), Cf.list(job(6, JobStatus.EXPIRED)));
        assertMonitoringStatusIs(MonitoringStatus.WARNING);
    }

    @Test
    public void oneCompletedJobWithTooManyRetries() {
        run(true, Cf.list(job(6, JobStatus.COMPLETED)), Cf.list());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void longRunningJob() {
        sut.setAllowedRetryCountPerDay(5);
        Instant creationDate = Instant.now().minus(Duration.standardDays(5));

        run(true, Cf.list(job(26, JobStatus.RUNNING, creationDate)), Cf.list());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }


    @Test
    public void longRunningJobWarning() {
        sut.setAllowedRetryCountPerDay(5);
        Instant creationDate = Instant.now().minus(Duration.standardDays(5));

        run(true, Cf.list(job(31, JobStatus.RUNNING, creationDate)), Cf.list());
        assertMonitoringStatusIs(MonitoringStatus.WARNING);
    }

    private void run(boolean isMaster, ListF<OnetimeJob> taskId1Jobs, ListF<OnetimeJob> taskId2Jobs) {
        when(bazingaController.isMaster()).thenReturn(isMaster);
        when(bazingaStorage.findOnetimeTaskStates())
            .thenReturn(Cf.list(taskId1, taskId2).map(OnetimeTaskState::initial));
        when(bazingaStorage.findLatestOnetimeJobs(eq(taskId1), any(), any(), any(), any(), any()))
            .thenReturn(taskId1Jobs);
        when(bazingaStorage.findLatestOnetimeJobs(eq(taskId2), any(), any(), any(), any(), any()))
            .thenReturn(taskId2Jobs);
        sut.run();
    }

    private static OnetimeJob job(int attempts, JobStatus status) {
        return job(attempts, status, Instant.now());
    }

    private static OnetimeJob job(int attempts, JobStatus status, Instant creationDate) {
        return new OnetimeJob(
            new FullJobId(null, new JobId(UUID.randomUUID())),
            Option.of(creationDate),
            null,
            null,
            null,
            new JobInfoValue(
                status,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            Option.of(attempts),
            null,
            null,
            0,
            null
        );
    }

    private void assertMonitoringStatusIs(MonitoringStatus status) {
        assertEquals(
            status,
            complicatedMonitoring.getResult(OnetimeJobRetriesMonitoring.MONITORING_NAME)
                .getStatus()
        );
    }
}
