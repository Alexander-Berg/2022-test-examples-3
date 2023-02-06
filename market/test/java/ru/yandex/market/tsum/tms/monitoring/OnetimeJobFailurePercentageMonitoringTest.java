package ru.yandex.market.tsum.tms.monitoring;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.bazinga.BazingaControllerApp;
import ru.yandex.commune.bazinga.impl.JobInfoValue;
import ru.yandex.commune.bazinga.impl.JobStatus;
import ru.yandex.commune.bazinga.impl.OnetimeJob;
import ru.yandex.commune.bazinga.impl.OnetimeTaskState;
import ru.yandex.commune.bazinga.impl.TaskId;
import ru.yandex.commune.bazinga.impl.controller.BazingaController;
import ru.yandex.commune.bazinga.impl.storage.BazingaStorage;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 20.03.2018
 */
public class OnetimeJobFailurePercentageMonitoringTest {
    private ComplicatedMonitoring complicatedMonitoring;
    private BazingaStorage bazingaStorage;
    private OnetimeJobFailurePercentageMonitoring sut;
    private BazingaController bazingaController;

    private static final TaskId taskId1 = new TaskId("taskId1");
    private static final TaskId taskId2 = new TaskId("taskId2");

    @Before
    public void setUp() {
        complicatedMonitoring = new ComplicatedMonitoring();
        bazingaStorage = mock(BazingaStorage.class);
        BazingaControllerApp bazingaControllerApp = mock(BazingaControllerApp.class);
        sut = new OnetimeJobFailurePercentageMonitoring(complicatedMonitoring, bazingaControllerApp, bazingaStorage);
        sut.setFailurePercentageToCrit(33);
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
        run(true, Cf.list(job(JobStatus.READY)), Cf.list(job(JobStatus.STARTING)));
        sut.run();
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void tooManyFailed() {
        run(
            true,
            Cf.list(job(JobStatus.RUNNING)),
            Cf.list(job(JobStatus.FAILED), job(JobStatus.COMPLETED))
        );
        sut.run();
        assertMonitoringStatusIs(MonitoringStatus.CRITICAL);
    }

    @Test
    public void tooManyExpired() {
        run(
            true,
            Cf.list(job(JobStatus.RUNNING)),
            Cf.list(job(JobStatus.EXPIRED), job(JobStatus.COMPLETED))
        );
        sut.run();
        assertMonitoringStatusIs(MonitoringStatus.CRITICAL);
    }

    @Test
    public void notTooManyFailed() {
        run(
            true,
            Cf.list(job(JobStatus.READY), job(JobStatus.RUNNING)),
            Cf.list(job(JobStatus.FAILED), job(JobStatus.COMPLETED))
        );
        sut.run();
        assertMonitoringStatusIs(MonitoringStatus.OK);
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

    private static OnetimeJob job(JobStatus status) {
        return new OnetimeJob(
            null,
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
            null,
            null,
            null,
            0,
            null
        );
    }

    private void assertMonitoringStatusIs(MonitoringStatus status) {
        assertEquals(
            status,
            complicatedMonitoring.getResult(OnetimeJobFailurePercentageMonitoring.MONITORING_NAME)
                .getStatus()
        );
    }
}
