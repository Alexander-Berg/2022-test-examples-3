package ru.yandex.market.tsum.tms.monitoring;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.bazinga.BazingaControllerApp;
import ru.yandex.commune.bazinga.impl.CronJob;
import ru.yandex.commune.bazinga.impl.CronTaskState;
import ru.yandex.commune.bazinga.impl.JobId;
import ru.yandex.commune.bazinga.impl.JobInfoValue;
import ru.yandex.commune.bazinga.impl.JobStatus;
import ru.yandex.commune.bazinga.impl.TaskId;
import ru.yandex.commune.bazinga.impl.controller.BazingaController;
import ru.yandex.commune.bazinga.impl.controller.ControllerCronTask;
import ru.yandex.commune.bazinga.impl.controller.ControllerTaskRegistry;
import ru.yandex.commune.bazinga.impl.storage.BazingaStorage;
import ru.yandex.commune.bazinga.impl.worker.BazingaHostPort;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.tsum.core.dao.ParamsDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 10.04.17
 */
public class TaskMonitoringTest {
    private ComplicatedMonitoring complicatedMonitoring;
    private BazingaStorage bazingaStorage;
    private TaskMonitoring sut;
    private BazingaController bazingaController;
    private ParamsDao paramsDao;
    private ControllerTaskRegistry controllerTaskRegistry;

    private static final TaskId taskId1 = new TaskId("taskId1");
    private static final TaskId taskId2 = new TaskId("taskId2");
    private static final TaskId taskId3 = new TaskId("taskId3");

    private static final int timeToCritHours = 72;

    @Before
    public void setUp() throws Exception {
        complicatedMonitoring = new ComplicatedMonitoring();
        bazingaStorage = mock(BazingaStorage.class);
        paramsDao = mock(ParamsDao.class);
        controllerTaskRegistry = mock(ControllerTaskRegistry.class);
        BazingaControllerApp bazingaControllerApp = mock(BazingaControllerApp.class);
        when(bazingaControllerApp.getTaskRegistry()).thenReturn(controllerTaskRegistry);
        sut = new TaskMonitoring(complicatedMonitoring, bazingaControllerApp, bazingaStorage, paramsDao);
        sut.setCronTaskFailureCountToWarn(2);
        sut.setFailingCronTasksPercentToCrit(50);
        sut.setTimeToCritHours(timeToCritHours);
        bazingaController = mock(BazingaController.class);
        when(bazingaControllerApp.getBazingaController()).thenReturn(bazingaController);
    }

    @Test
    public void noCronJobs() throws Exception {
        run(true, true, true, Cf.list(), Cf.list(), Cf.list(), System.currentTimeMillis());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void notMaster() throws Exception {
        run(false, true, true, Cf.list(), Cf.list(), Cf.list(), System.currentTimeMillis());
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void noFailingCronJobs() throws Exception {
        run(
            true,
            true,
            true,
            Cf.list(job(JobStatus.COMPLETED, null)),
            Cf.list(job(JobStatus.RUNNING, null)),
            // Игнорируем InterruptedException потому что оно возникает только при выкладках tsum tms,
            // См. st/MARKETINFRA-2734
            Cf.list(job(JobStatus.FAILED, "bla-bla InterruptedException bla-bla")),
            System.currentTimeMillis()
        );
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void tooFewFailingCronJobs() throws Exception {
        run(
            true,
            true,
            true,
            Cf.list(job(JobStatus.FAILED, null)),
            Cf.list(),
            Cf.list(),
            System.currentTimeMillis()
        );
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void oneFailingCronTaskWarn() throws Exception {
        run(
            true,
            true,
            true,
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(),
            Cf.list(),
            System.currentTimeMillis()
        );
        assertMonitoringStatusIs(MonitoringStatus.WARNING);
    }

    @Test
    public void twoFailingCronTaskCrit() throws Exception {
        run(
            true,
            true,
            true,
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(),
            System.currentTimeMillis()
        );
        assertMonitoringStatusIs(MonitoringStatus.CRITICAL);
    }

    @Test
    public void oneFailingCronTaskTooLongCrit() throws Exception {
        run(
            true,
            true,
            true,
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(),
            Cf.list(),
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(timeToCritHours + 1)
        );
        assertMonitoringStatusIs(MonitoringStatus.CRITICAL);
    }

    @Test
    public void oneFailingCronTaskTooLongOkWhenTaskMissing() throws Exception {
        run(
            true,
            false,
            true,
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(),
            Cf.list(),
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(timeToCritHours + 1)
        );
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    @Test
    public void oneFailingCronTaskTooLongOkWhenTaskDisabled() throws Exception {
        run(
            true,
            true,
            false,
            Cf.list(job(JobStatus.FAILED, null), job(JobStatus.FAILED, null)),
            Cf.list(),
            Cf.list(),
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(timeToCritHours + 1)
        );
        assertMonitoringStatusIs(MonitoringStatus.OK);
    }

    private void run(boolean isMaster, boolean taskExists, boolean taskEnabled,
                     ListF<CronJob> taskId1Jobs, ListF<CronJob> taskId2Jobs,
                     ListF<CronJob> taskId3Jobs, long lastOkTimestamp) {
        when(
            paramsDao.getLong(
                eq(TaskMonitoring.CRON_TASKS_MONITORING_NAME),
                eq(TaskMonitoring.LAST_OK_PARAM_NAME),
                anyLong()
            )
        ).thenReturn(lastOkTimestamp);
        when(bazingaController.isMaster()).thenReturn(isMaster);

        if (taskExists) {
            ControllerCronTask cronTask = mock(ControllerCronTask.class);
            when(cronTask.isEnabled()).thenReturn(taskEnabled);
            when(controllerTaskRegistry.getCronTaskO(any())).thenReturn(Option.of(cronTask));
        } else {
            when(controllerTaskRegistry.getCronTaskO(any())).thenReturn(Option.empty());
        }

        when(bazingaStorage.findCronTaskState(any())).thenReturn(Option.of(createCronTaskState(taskId1)));
        when(bazingaStorage.findCronTaskStates())
            .thenReturn(Cf.list(taskId1, taskId2, taskId3).map(TaskMonitoringTest::createCronTaskState));
        when(bazingaStorage.findLatestCronJobs(eq(taskId1), any())).thenReturn(taskId1Jobs);
        when(bazingaStorage.findLatestCronJobs(eq(taskId2), any())).thenReturn(taskId2Jobs);
        when(bazingaStorage.findLatestCronJobs(eq(taskId3), any())).thenReturn(taskId3Jobs);
        sut.run();
    }

    private static CronTaskState createCronTaskState(TaskId taskId) {
        return new CronTaskState(
            new JobId(UUID.randomUUID()),
            Option.empty(),
            taskId,
            JobStatus.COMPLETED,
            new BazingaHostPort("localhost", 8080),
            Instant.now(),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty(),
            Option.empty()
        );
    }

    private static CronJob job(JobStatus status, String exceptionMessage) {
        return new CronJob(
            null,
            new JobInfoValue(
                status,
                null,
                null,
                null,
                Option.ofNullable(exceptionMessage),
                null,
                null,
                null
            )
        );
    }

    private void assertMonitoringStatusIs(MonitoringStatus status) {
        assertEquals(
            status,
            complicatedMonitoring.getResult(TaskMonitoring.CRON_TASKS_MONITORING_NAME).getStatus()
        );
    }
}