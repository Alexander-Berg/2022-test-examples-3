package ru.yandex.market.pricelabs.tms.services.database;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.timing.TimingContext;
import ru.yandex.market.common.util.timing.TimingItem;
import ru.yandex.market.pricelabs.database.Database;
import ru.yandex.market.pricelabs.misc.EmptyArg;
import ru.yandex.market.pricelabs.misc.PricelabsRuntimeException;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.ToJsonString;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.services.database.model.Job;
import ru.yandex.market.pricelabs.services.database.model.JobStatus;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.ProcessingRouter;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.models.ModelsArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersResult;
import ru.yandex.market.pricelabs.tms.services.database.TasksService.UniqueType;
import ru.yandex.misc.net.HostnameUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.misc.random.Random2.R;

@Slf4j
public class TasksServiceImplTest extends AbstractTmsSpringConfiguration {

    static final OffersArg OFFERS_ARG1 = TmsTestUtils.defaultOffersArg(1);
    static final OffersArg OFFERS_ARG2 = TmsTestUtils.defaultOffersArg(2);
    static final OffersArg OFFERS_ARG3 = TmsTestUtils.defaultOffersArg(3);
    static final OffersArg OFFERS_ARG4 = TmsTestUtils.defaultOffersArg(4);
    static final OffersArg OFFERS_ARG5 = TmsTestUtils.defaultOffersArg(5);
    static final OffersArg OFFERS_ARG6 = TmsTestUtils.defaultOffersArg(6);
    static final ModelsArg MODELS_ARG1 = new ModelsArg(1, 1, 1);
    static final ModelsArg MODELS_ARG2 = new ModelsArg(2, 2, 2);
    static final ModelsArg MODELS_ARG3 = new ModelsArg(3, 3, 3);

    static final long DELAY = TimeUnit.SECONDS.toMillis(10);

    @Autowired
    private TasksService tasksService;

    @Autowired
    private Database database;

    @Autowired
    private ProcessingRouter router;

    @BeforeEach
    void init() {
        testControls.cleanupTasksService();
    }

    @Test
    void testCheckActiveJobs() {
        assertTrue(tasksService.getActiveJobs().isEmpty());

        Job job1 = job(0, OFFERS_ARG1);
        checkActiveJobs(job1);
    }

    @Test
    void testCheckActiveJobLastType() {
        Job job1 = job(0, OFFERS_ARG1);
        checkActiveJobs(job1);

        set(job1, 0, 0, 0, 0); // Сбросим вычисляемые атрибуты
        assertEquals(job1, tasksService.getLastJob(JobType.SHOP_LOOP_FULL).orElseThrow());
        assertTrue(tasksService.getLastJob(JobType.SHOP_LOOP_FULL_PRIORITY).isEmpty());
    }

    @Test
    void testCreateJobEvenWithoutItems() {
        assertThrows(IllegalArgumentException.class, () -> job(0));
    }

    @Test
    void testActiveJobsUnique() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 0, OFFERS_ARG1);
        checkActiveJobs(job1);
    }

    @Test
    void testActiveJobsNonUnique() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);
        Job job11 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);
        Job job12 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);

        checkActiveJobs(job1, job11, job12);
    }

    @Test
    void testActiveJobsNonUniqueLastType() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);
        Job job11 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);
        Job job12 = job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1);

        checkActiveJobs(job1, job11, job12);

        // Сбросим вычисляемые атрибуты
        set(job12, 0, 0, 0, 0);
        assertEquals(job12, tasksService.getLastJob(JobType.SHOP_LOOP_FULL).orElseThrow());
    }

    @Test
    void testRegisterJobUnique() {
        Job job1 = job(0, OFFERS_ARG1);

        assertTrue(tasksService.registerJob(UniqueType.ByType, JobType.SHOP_LOOP_FULL, ToJsonString.wrap(""),
                List.of(OFFERS_ARG1)).isEmpty());

        checkActiveJobs(job1);
    }

    @Test
    void testCompleteJob() {
        Job job1 = job(0, OFFERS_ARG1);
        checkActiveJobs(job1);

        completeJob(job1, null);
        checkActiveJobs();
        checkCompleteTasks(job1);
    }

    @Test
    void testAbortJob() {
        Job job1 = job(0, OFFERS_ARG1);
        checkActiveJobs(job1);

        abortJob(job1);
        checkActiveJobs();
        checkCompleteTasks(job1);
    }

    @Test
    void testUpdateUnknownTask() {
        var t = new Task();
        t.setTask_id(1);
        assertThrows(PricelabsRuntimeException.class, () ->
                tasksService.updateTask(t));
    }

    @Test
    void testRegisterTask() {
        Job job1 = job(0, OFFERS_ARG1, OFFERS_ARG2);
        checkRestartableTasks(job1);
        checkActiveJobs(job1);
        checkCompleteTasks(job1);
    }

    @Test
    void testRegisterTaskDuplicate() {
        var job = job(0, OFFERS_ARG1, OFFERS_ARG1);
        checkActiveJobs(job);
    }

    @Test
    void testUpdateTask() {
        Instant created = getInstant();
        Job job1 = job(1, OFFERS_ARG1);
        set(job1, 0, 0, 0, 1);
        checkActiveJobs(job1);

        var scheduled = tasksService.getScheduledTasks();
        assertEquals(1, scheduled.size());
        var scheduledTask = scheduled.get(0);
        assertEquals(1, scheduledTask.getShop_id());
        assertNull(scheduledTask.getUid()); // uid не заполняется, т.к. вызов идет не через MockMvc

        Task expect = new Task();
        expect.setJob_id(job1.getJob_id());
        expect.setTask_id(scheduledTask.getTask_id());
        expect.setCreated(created);
        expect.setStarted(null);
        expect.setUpdated(created);
        expect.setType(JobType.SHOP_LOOP_FULL);
        expect.setStatus(TaskStatus.SCHEDULED);
        expect.setArgs(OFFERS_ARG1.toJsonString());
        expect.setMax_restart_count(1);
        expect.setRestart_count(0);
        expect.setTotal_rows(0);
        expect.setTotal_time_millis(0);
        expect.setInfo(null);
        expect.setRefresh_sec(1);
        expect.setHostname(null);
        expect.setStats(null);
        expect.setShop_id(scheduledTask.getShop_id());
        expect.setUid(scheduledTask.getUid());
        assertEquals(expect, scheduledTask);

        TimingUtils.addTime(10);
        Task task1 = startScheduledTask();

        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);

        Instant started = getInstant();
        // Обновили состояние файла
        expect.setStarted(started);
        expect.setStarted_first(started);
        expect.setUpdated(started);
        expect.setStatus(TaskStatus.RUNNING);
        expect.setHostname(HostnameUtils.localHostname());

        assertEquals(expect, task1);

        TimingUtils.addTime(150);

        var ctx = TimingContext.newRoot().getContext();

        TimingItem total = ctx.getTotal();
        total.addRowCount(100);
        total.addActiveTime(99);

        updateTask(task1);
        task1.setUpdated(getInstant());
        task1.setTotal_rows(100);
        task1.setTotal_time_millis(99);
        assertEquals(List.of(task1), tasksService.getAllTasks(job1.getJob_id()));
        assertEquals(List.of(task1), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.RUNNING));
        assertEquals(List.of(), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.SUCCESS));
        assertEquals(List.of(), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.FAILURE));
        assertEquals(List.of(), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.SCHEDULED));

        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);

        total.addRowCount(1);
        TimingUtils.addTime(1);

        var result = new OffersResult();
        result.addHides_delete(2);
        result.addOffers_new(1);
        result.addOffers_update(1);
        result.setShop_balance(4.44);
        tasksService.completeTask(task1, TaskStatus.FAILURE, "state", result);

        task1.setUpdated(getInstant());
        task1.setTotal_rows(101);
        task1.setTotal_time_millis(99);
        task1.setInfo("state");
        task1.setStatus(TaskStatus.FAILURE);
        task1.setStats("{}"); // ctx.toJsonString()
        task1.setResult(result.toJsonString());

        compareTasks(List.of(), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.RUNNING));
        compareTasks(List.of(task1), tasksService.getAllTasks(job1.getJob_id(), TaskStatus.FAILURE));

        checkRestartableTasks(job1, task1);
        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);

        // Обновили состояние
        expect.setUpdated(getInstant());
        expect.setStatus(TaskStatus.FAILURE);
        expect.setTotal_rows(101);
        expect.setTotal_time_millis(task1.getTotal_time_millis());
        expect.setInfo("state");
        expect.setStats("{}"); // "{\"total\":{\"rowCount\":101,\"activeCycles\":1,\"activeTimeMillis\":99}}"
        expect.setResult("{\"offers_new\":1,\"offers_update\":1,\"hides_delete\":2,\"shop_balance\":4.44}");

        assertTrue(task1.getTotal_time_millis() >= 99);
        assertEquals(expect, task1);
        checkActiveJobs(job1);
        checkCompleteTasks(job1);
    }

    @Test
    void testUpdateTaskToInvalidState_SUCCESS() {
        Job job1 = job(1, OFFERS_ARG1);
        Task task1 = startScheduledTask();
        successTask(task1);
        checkNoScheduledTasks();

        assertThrows(PricelabsRuntimeException.class, () -> successTask(task1));
        assertThrows(PricelabsRuntimeException.class, () -> completeTaskFailure(task1));
        assertThrows(PricelabsRuntimeException.class, () -> updateTask(task1));

        set(job1, 0, 1, 0, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testUpdateTaskToInvalidState_FAILURE() {
        Job job1 = job(1, OFFERS_ARG1);
        Task task1 = startScheduledTask();
        completeTaskFailure(task1);
        checkNoScheduledTasks();

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);

        assertThrows(PricelabsRuntimeException.class, () -> successTask(task1));
        assertThrows(PricelabsRuntimeException.class, () -> completeTaskFailure(task1));
        assertThrows(PricelabsRuntimeException.class, () -> updateTask(task1));

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testUpdateTaskToInvalidState_UNRECOVERABLE() {
        Job job1 = job(1, OFFERS_ARG1);
        Task task1 = startScheduledTask();
        tasksService.completeTask(task1, TaskStatus.UNRECOVERABLE, null, null);
        checkNoScheduledTasks();

        set(job1, 0, 0, 0, 0);
        job1.setUnrecoverable_items(1);
        checkActiveJobs(job1);

        assertThrows(PricelabsRuntimeException.class, () -> successTask(task1));
        assertThrows(PricelabsRuntimeException.class, () -> completeTaskFailure(task1));
        assertThrows(PricelabsRuntimeException.class, () -> updateTask(task1));

        checkActiveJobs(job1);
    }


    @Test
    void testUpdateTaskToInvalidState_SCHEDULED() {
        Job job1 = job(1, OFFERS_ARG1);
        var tasks = tasksService.getScheduledTasks();
        assertEquals(1, tasks.size());

        Task task1 = tasks.get(0);

        set(job1, 0, 0, 0, 1);
        checkActiveJobs(job1);

        assertThrows(PricelabsRuntimeException.class, () -> successTask(task1));
        assertThrows(PricelabsRuntimeException.class, () -> updateTask(task1));

        set(job1, 0, 0, 0, 1);
        checkActiveJobs(job1);
    }

    @Test
    void testUpdateTaskToValidState_SCHEDULED() {
        Job job1 = job(1, OFFERS_ARG1);
        var tasks = tasksService.getScheduledTasks();
        assertEquals(1, tasks.size());

        Task task1 = tasks.get(0);

        set(job1, 0, 0, 0, 1);
        checkActiveJobs(job1);

        completeTaskFailure(task1);
        checkNoScheduledTasks();

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testRestartableTasksMultiple() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 1, OFFERS_ARG1);
        set(job1, 0, 0, 0, 1);
        checkActiveJobs(job1);

        var task1 = startScheduledTask(JobType.SHOP_LOOP_FULL);
        checkNoScheduledTasks();

        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);

        var ctx = TimingContext.newRoot().getContext();
        checkRestartableTasks(job1);

        updateTask(task1);
        task1.setStatus(TaskStatus.RUNNING);
        checkRestartableTasks(job1);
        checkActiveJobs(job1);

        tasksService.completeTask(task1, TaskStatus.FAILURE, "complete", null);
        task1.setStatus(TaskStatus.FAILURE);
        task1.setInfo("complete");
        task1.setStats("{}");
        checkRestartableTasks(job1, task1);
        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);

        checkCompleteTasks(job1);
    }

    @Test
    void testRestartableTasksWithDelay() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 1, OFFERS_ARG1, OFFERS_ARG2);
        checkActiveJobs(job1);

        Task task1 = startScheduledTask();
        Task task2 = startScheduledTask();
        checkNoScheduledTasks();

        var ctx = TimingContext.newRoot().getContext();
        checkRestartableTasks(job1);

        set(job1, 2, 0, 0, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        completeTaskFailure(task1);
        task1.setStatus(TaskStatus.FAILURE);
        task1.setUpdated(task1.getUpdated().plusMillis(100));
        task1.setStats("{}");
        checkRestartableTasks(job1, task1);
        set(job1, 1, 0, 1, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));
        var task1Restarted = startScheduledTask();
        assertRestarted(task1, task1Restarted);
        checkNoScheduledTasks();

        checkRestartableTasks(job1);
        set(job1, 2, 0, 0, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        completeTaskFailure(task2);
        task2.setStatus(TaskStatus.FAILURE);
        task2.setUpdated(task2.getUpdated().plusMillis(300));
        task2.setStats("{}");
        checkRestartableTasks(job1, task2);
        set(job1, 1, 0, 1, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));
        var task2Restarted = startScheduledTask();
        assertRestarted(task2, task2Restarted);

        checkRestartableTasks(job1);
        set(job1, 2, 0, 0, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        updateTask(task1Restarted);
        checkRestartableTasks(job1);
        set(job1, 2, 0, 0, 0);
        checkActiveJobs(job1);
        TimingUtils.addTime(100);

        successTask(task1Restarted);
        task1Restarted.setStatus(TaskStatus.SUCCESS);
        task1Restarted.setUpdated(task1Restarted.getUpdated().plusMillis(300).plusMillis(100));
        task1Restarted.setStats("{}");
        checkRestartableTasks(job1);
        set(job1, 1, 1, 0, 0);
        checkActiveJobs(job1);
        checkCompleteTasks(job1, task1Restarted);

        TimingUtils.addTime(1000); // Нужно ждать 3x времени, чтобы появился таска для перезапуска
        checkRestartableTasks(job1);
        TimingUtils.addTime(1000);
        checkRestartableTasks(job1);

        TimingUtils.addTime(8000);
        log.info("Tasks: {}", tasksService.getAllTasks(job1.getJob_id()));
        checkRestartableTasks(job1, task2Restarted);
        set(job1, 1, 1, 0, 0);
        checkActiveJobs(job1);

        updateTask(task2Restarted);
        checkRestartableTasks(job1);
        set(job1, 1, 1, 0, 0);
        checkActiveJobs(job1);
        checkCompleteTasks(job1, task1Restarted);
        checkRestartableTasks(job1);
    }

    @Test
    void testRestartableTasksWithDelayFailure() {
        Job job1 = job(2, OFFERS_ARG1);
        checkActiveJobs(job1);

        Task task1 = startScheduledTask();
        checkNoScheduledTasks();
        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);

        TimingUtils.addTime(DELAY + 1);

        checkRestartableTasks(job1, task1);
        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));

        Task task1Restarted1 = startScheduledTask();
        assertRestarted(task1, task1Restarted1);


        TimingUtils.addTime(DELAY + 1);
        checkRestartableTasks(job1, task1Restarted1);
        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));

        Task task1Restarted2 = startScheduledTask();
        assertRestarted(task1Restarted1, task1Restarted2);

        TimingUtils.addTime(DELAY + 1);
        checkRestartableTasks(job1, task1Restarted2);

        assertEquals(1, tasksService.restartTasks(job1.getJob_id())); // на самом деле, задача была завершена
        checkNoScheduledTasks();

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testRestartableTasksFailure() {
        Job job1 = job(2, OFFERS_ARG1);
        checkActiveJobs(job1);

        Task task1 = startScheduledTask();
        checkNoScheduledTasks();
        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);
        assertEquals(task1.getStarted_first(), task1.getStarted());

        completeTaskFailure(task1);
        task1.setStatus(TaskStatus.FAILURE);
        task1.setStats("{}");
        checkRestartableTasks(job1, task1);
        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));

        Task task1Restarted1 = startScheduledTask();
        assertRestarted(task1, task1Restarted1);

        completeTaskFailure(task1Restarted1);
        task1Restarted1.setStatus(TaskStatus.FAILURE);
        task1Restarted1.setStats("{}");
        checkRestartableTasks(job1, task1Restarted1);
        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));

        Task task1Restarted2 = startScheduledTask();
        assertRestarted(task1Restarted1, task1Restarted2);

        completeTaskFailure(task1Restarted2);

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);

        checkRestartableTasks(job1);

        assertEquals(0, tasksService.restartTasks(job1.getJob_id())); // на самом деле, задача была завершена
        checkNoScheduledTasks();

        set(job1, 0, 0, 1, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testRestartableTasksUnrecoverable() {
        Job job1 = job(2, OFFERS_ARG1);
        checkActiveJobs(job1);

        Task task1 = startScheduledTask();
        checkNoScheduledTasks();
        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1);

        tasksService.completeTask(task1, TaskStatus.UNRECOVERABLE, null, null);
        task1.setStatus(TaskStatus.UNRECOVERABLE);
        task1.setStats("{}");
        checkRestartableTasks(job1);
        assertEquals(0, tasksService.restartTasks(job1.getJob_id()));

        set(job1, 0, 0, 0, 0);
        job1.setUnrecoverable_items(1);
        checkActiveJobs(job1);

        checkRestartableTasks(job1);

        assertEquals(0, tasksService.restartTasks(job1.getJob_id())); // на самом деле, задача была завершена
        checkNoScheduledTasks();

        checkActiveJobs(job1);
    }

    @Test
    void testRestartableTasksMultipleFails() {
        Job job1 = job(1, OFFERS_ARG1, OFFERS_ARG2);
        checkActiveJobs(job1);

        Task task1 = startScheduledTask();
        Task task2 = startScheduledTask();
        checkNoScheduledTasks();
        set(job1, 2, 0, 0, 0);
        checkActiveJobs(job1);

        completeTaskFailure(task1);
        completeTaskFailure(task2);
        task1.setStatus(TaskStatus.FAILURE);
        task1.setStats("{}");
        task2.setStatus(TaskStatus.FAILURE);
        task2.setStats("{}");
        checkRestartableTasks(job1, task1, task2);
        assertEquals(2, tasksService.restartTasks(job1.getJob_id()));

        Task task1Restarted1 = startScheduledTask();
        assertRestarted(task1, task1Restarted1);

        Task task2Restarted1 = startScheduledTask();
        assertRestarted(task2, task2Restarted1);

        completeTaskFailure(task1Restarted1);
        TimingUtils.addTime(DELAY + 1);

        set(job1, 1, 0, 1, 0);
        checkActiveJobs(job1);

        checkRestartableTasks(job1, task2Restarted1);

        assertEquals(1, tasksService.restartTasks(job1.getJob_id())); // Зависшая задача будет завершена
        checkNoScheduledTasks();

        set(job1, 0, 0, 2, 0);
        checkActiveJobs(job1);
    }

    @Test
    void testRestart() {
        Job job = job(2, OFFERS_ARG1);
        Task task = startScheduledTask();
        checkNoScheduledTasks();
        assertEquals(task.getStarted_first(), task.getStarted());

        completeTaskFailure(task);
        assertEquals(task.getStarted_first(), task.getStarted());
        task.setStatus(TaskStatus.FAILURE);
        task.setStats("{}");

        checkRestartableTasks(job, task);

        TimingUtils.addTime(100);
        assertEquals(1, tasksService.restartTasks(job.getJob_id()));

        TimingUtils.addTime(100);
        var taskRestarted = startScheduledTask();
        assertRestarted(task, taskRestarted);
        checkRestartableTasks(job);
        checkNoScheduledTasks();

        set(job, 1, 0, 0, 0);
        checkActiveJobs(job);
        checkCompleteTasks(job);


        completeTaskFailure(taskRestarted);
        taskRestarted.setStatus(TaskStatus.FAILURE);
        taskRestarted.setStats("{}");

        checkRestartableTasks(job, taskRestarted);

        assertEquals(1, tasksService.restartTasks(job.getJob_id()));
        var taskRestarted2 = startScheduledTask();
        assertRestarted(taskRestarted, taskRestarted2);
        checkRestartableTasks(job);

        completeTaskFailure(taskRestarted2);
        checkRestartableTasks(job);

        set(job, 0, 0, 1, 0);
        checkActiveJobs(job);
        checkCompleteTasks(job);
    }

    @Test
    void testAbortThenNoRestart() {
        Job job = job(2, OFFERS_ARG1);
        startScheduledTask();
        checkNoScheduledTasks();

        abortJob(job);
        checkRestartableTasks(job);
        checkActiveJobs();
    }

    @Test
    void testNormalCycle() {
        for (int i = 0; i < 5; i++) {
            TimingUtils.addTime(100);

            Job job = job(1, OFFERS_ARG1);
            set(job, 0, 0, 0, 1);
            checkActiveJobs(job);

            Task task = startScheduledTask();
            set(job, 1, 0, 0, 0);
            checkActiveJobs(job);
            checkNoScheduledTasks();


            successTask(task);
            set(job, 0, 1, 0, 0);
            checkActiveJobs(job);

            completeJob(job, "Looks OK");
            checkActiveJobs();

            task.setStatus(TaskStatus.SUCCESS);
            task.setStats("{}");
            checkCompleteTasks(job, task);
        }
    }

    @Test
    void testDifferentPriorities() {
        Job job1 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.None, OFFERS_ARG1);
        Job job2 = job(JobType.SHOP_LOOP_FULL_PRIORITY, 1, UniqueType.None, OFFERS_ARG3, OFFERS_ARG4);
        Job job3 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.None, OFFERS_ARG2);

        assertEquals(OFFERS_ARG3.toJsonString(), startScheduledTask(JobType.SHOP_LOOP_FULL_PRIORITY).getArgs());
        set(job2, 1, 0, 0, 1);
        checkActiveJobs(job1, job2, job3);

        assertEquals(OFFERS_ARG4.toJsonString(), startScheduledTask(JobType.SHOP_LOOP_FULL_PRIORITY).getArgs());
        set(job2, 2, 0, 0, 0);
        checkActiveJobs(job1, job2, job3);

        assertEquals(OFFERS_ARG1.toJsonString(), startScheduledTask(JobType.SHOP_LOOP_FULL).getArgs());
        set(job1, 1, 0, 0, 0);
        checkActiveJobs(job1, job2, job3);

        assertEquals(OFFERS_ARG2.toJsonString(), startScheduledTask(JobType.SHOP_LOOP_FULL).getArgs());
        set(job3, 1, 0, 0, 0);
        checkActiveJobs(job1, job2, job3);

        checkNoScheduledTasks();
    }

    @Test
    void testFailureNoType() {

        assertThrows(NullPointerException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(null)
                        .args(ToJsonString.wrap(""))
                        .tasks(List.of(OFFERS_ARG1))
                        .build()));
    }

    @Test
    void testFailureInvalidArg() {
        assertThrows(IllegalArgumentException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(JobType.SHOP_LOOP_FULL)
                        .args(ToJsonString.wrap(""))
                        .tasks(List.of(MODELS_ARG1))
                        .build()));
    }

    @Test
    void testFailureNoArg() {
        assertThrows(NullPointerException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(JobType.SHOP_LOOP_FULL)
                        .args(null)
                        .tasks(List.of(OFFERS_ARG1))
                        .build()));
    }

    @Test
    void testFailureEmptyTasks() {
        assertThrows(IllegalArgumentException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(JobType.SHOP_LOOP_FULL)
                        .args(ToJsonString.wrap(""))
                        .tasks(List.of())
                        .build()));
    }

    @Test
    void testFailureNullTasks() {
        assertThrows(NullPointerException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(JobType.SHOP_LOOP_FULL)
                        .args(ToJsonString.wrap(""))
                        .tasks(null)
                        .build()));
    }

    @Test
    void testFailureEmptyRefreshValue() {
        assertThrows(IllegalArgumentException.class,
                () -> tasksService.registerJob(JobParams.builder()
                        .unique(UniqueType.ByType)
                        .type(JobType.SHOP_LOOP_FULL)
                        .args(ToJsonString.wrap(""))
                        .tasks(List.of(OFFERS_ARG1))
                        .refreshSec(0)
                        .build()));
    }

    @Test
    void testFailureInvalidTasksForUniqueType() {
        assertThrows(IllegalArgumentException.class,
                () -> tasksService.registerJob(null, UniqueType.ByShop, JobType.SHOP_LOOP_FULL,
                        ToJsonString.wrap(""), List.of(new EmptyArg())));
    }

    @Test
    void testFailureUpdateUnknownTask() {
        job(0, OFFERS_ARG1);
        startScheduledTask();

        var t = new Task();
        t.setTask_id(0);
        assertThrows(PricelabsRuntimeException.class,
                () -> tasksService.updateTask(t));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testFailureUpdateTaskWithNullCtx() {
        job(0, OFFERS_ARG1);
        var task1 = startScheduledTask();

        assertThrows(NullPointerException.class,
                () -> tasksService.updateTask(task1, null));
    }

    @Test
    void testFailureCompleteUnknownTask() {
        var t = new Task();
        t.setTask_id(0);
        assertThrows(PricelabsRuntimeException.class,
                () -> tasksService.completeTask(t, TaskStatus.SUCCESS, null, null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testFailureCompleteTaskWithNullType() {
        job(0, OFFERS_ARG1);
        var task1 = startScheduledTask();

        assertThrows(PricelabsRuntimeException.class,
                () -> tasksService.completeTask(task1, null, null, null));
    }

    @Test
    void testFailureCompleteTaskWithInvalidTypeRUNNING() {
        job(0, OFFERS_ARG1);
        var task1 = startScheduledTask();

        assertThrows(PricelabsRuntimeException.class,
                () -> tasksService.completeTask(task1, TaskStatus.RUNNING,
                        null, null));
    }

    @Test
    void testFailureCompleteTaskWithInvalidTypeSCHEDULED() {
        job(0, OFFERS_ARG1);
        var task1 = startScheduledTask();

        assertThrows(PricelabsRuntimeException.class,
                () -> tasksService.completeTask(task1, TaskStatus.SCHEDULED,
                        null, null));
    }

    @Test
    void testLargeFields() {
        final int maxLen = 1000;
        var arg = TmsTestUtils.defaultOffersArg();
        Job job1 = tasksService.registerJob(JobParams.builder()
                .unique(UniqueType.None)
                .type(JobType.SHOP_LOOP_FULL)
                .args(ToJsonString.wrap(R.nextString(maxLen)))
                .tasks(List.of(arg))
                .maxRestartCount(1)
                .refreshSec(1)
                .build()).orElseThrow();
        var task1 = startScheduledTask();
        tasksService.completeTask(task1, TaskStatus.SUCCESS,
                R.nextString(maxLen), ToJsonString.wrap(R.nextString(maxLen)));
        completeJob(job1, R.nextString(maxLen));
    }

    @Test
    void testWithStats() {
        Job job1 = job(1, OFFERS_ARG1);

        Task task1 = startScheduledTask();
        try (var ctx = TimingContext.newRoot()) {
            ctx.getContext().getGroup("job1").getTiming("t2").addActiveTime(0);

            tasksService.completeTask(task1, TaskStatus.FAILURE, "result", null);
        }

        task1.setStatus(TaskStatus.FAILURE);
//        task1.setStats("{\"children\":{\"job1\":{\"items\":{\"t2\":{\"activeCycles\":1}}}}}");
        task1.setStats("{}");
        task1.setInfo("result");

        log.info("Tasks: {}", tasksService.getAllTasks(job1.getJob_id()));
        checkRestartableTasks(job1, task1);
    }

    @Test
    void testWithParent() {
        Job job1 = job(1, OFFERS_ARG1);
        Job job2 = tasksService.registerJob(JobParams.builder()
                .parentJob(job1)
                .unique(UniqueType.None)
                .type(JobType.SHOP_LOOP_FULL)
                .args(ToJsonString.wrap("arg"))
                .tasks(List.of(OFFERS_ARG2))
                .refreshSec(1)
                .build()).orElseThrow();
        job2.setScheduled_items(1);
        assertEquals(job1.getJob_id(), job2.getParent_job_id());
        assertFalse(job2.isUser_job());
        checkActiveJobs(job1, job2);
    }

    @Test
    void testWithUserJob() {
        Job job1 = job(1, OFFERS_ARG1);
        Job job2 = tasksService.registerJob(JobParams.builder()
                .parentJob(job1)
                .unique(UniqueType.None)
                .type(JobType.SHOP_LOOP_FULL)
                .args(ToJsonString.wrap("arg"))
                .tasks(List.of(OFFERS_ARG2))
                .maxRestartCount(1)
                .refreshSec(1)
                .userJob(true)
                .build()).orElseThrow();
        job2.setScheduled_items(1);
        assertEquals(job1.getJob_id(), job2.getParent_job_id());
        assertTrue(job2.isUser_job());
        checkActiveJobs(job1, job2);
    }

    @Test
    void testStartTasksDifferentPriorities0() {
        var limits1 = TasksServiceImplUnitTest.asObjectMap(
                Map.of(JobType.SHOP_LOOP_FULL, 0));

        Supplier<List<Task>> starter1 = () -> tasksService.startTasks(4, limits1);

        job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1, OFFERS_ARG2, OFFERS_ARG3);

        checkTasks(starter1.get());

        checkTasks(tasksService.startTasks(4), JobType.SHOP_LOOP_FULL, JobType.SHOP_LOOP_FULL, JobType.SHOP_LOOP_FULL);
        checkTasks(tasksService.startTasks(4));
    }

    @Test
    void testStartTasksDifferentPriorities3() {
        var limits1 = TasksServiceImplUnitTest.asObjectMap(
                Map.of(JobType.SHOP_LOOP_FULL, 2));

        Supplier<List<Task>> starter1 = () -> tasksService.startTasks(1, limits1);

        job(JobType.SHOP_LOOP_FULL, 0, UniqueType.None, OFFERS_ARG1, OFFERS_ARG2, OFFERS_ARG3);

        checkTasks(starter1.get(), JobType.SHOP_LOOP_FULL);
        checkTasks(starter1.get(), JobType.SHOP_LOOP_FULL);
        checkTasks(starter1.get(), JobType.SHOP_LOOP_FULL);
        assertEquals(List.of(), starter1.get());
    }

    @Test
    void testHandleInvalidJobs() {
        Job job1 = job(0, OFFERS_ARG1);

        database.execRW(jdbc ->
                jdbc.update("update tms_table_jobs set type = 555444 where job_id = ?", job1.getJob_id()));

        checkActiveJobs();

        checkActiveJobs();

        assertEquals(JobStatus.ABORTED.value(), (int) database.callRO(jdbc ->
                jdbc.queryForObject("select status from tms_table_jobs where job_id = ?",
                        Integer.class, job1.getJob_id())));
    }

    @Test
    void testHandleInvalidTasks() {
        Job job1 = job(1, OFFERS_ARG1);

        database.execRW(jdbc ->
                jdbc.update("update tms_table_tasks set type = 555444 where job_id = ?", job1.getJob_id()));

        checkActiveJobs(job1);

        assertEquals(List.of(), tasksService.startTasks(1));

        checkRestartableTasks(job1);

        TimingUtils.addTime(DELAY + 1);

        checkRestartableTasks(job1); // type в задаче нельзя распарсить - считаем, что задачу нельзя рестартовать

        assertEquals(1, tasksService.restartTasks(job1.getJob_id()));

        TimingUtils.addTime(DELAY + 1);
        assertEquals(0, tasksService.restartTasks(job1.getJob_id()));

    }

    @Test
    void testHandleInvalidTasksMulti() {
        Job job1 = job(1, OFFERS_ARG1, OFFERS_ARG2, OFFERS_ARG3, OFFERS_ARG4);

        database.execRW(jdbc ->
                jdbc.update("update tms_table_tasks set type = 555444 where job_id = ? and shop_id in (1, 2)",
                        job1.getJob_id()));

        checkActiveJobs(job1);

        checkTasks(tasksService.startTasks(4), JobType.SHOP_LOOP_FULL, JobType.SHOP_LOOP_FULL);
    }

    @Test
    void testUniqueByShop() {
        var job1 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.ByShop, OFFERS_ARG1);
        var job2 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.ByShop, OFFERS_ARG2);

        var empty = tasksService.registerJob(null, UniqueType.ByShop, JobType.SHOP_LOOP_FULL,
                ToJsonString.wrap(""), List.of(OFFERS_ARG1));
        assertTrue(empty.isEmpty());

        assertEquals(OFFERS_ARG1.getShopId(), job1.getType_index());
        assertEquals(OFFERS_ARG2.getShopId(), job2.getType_index());
    }

    @Test
    void preventFromRunningParallelShopLoopsTimeout() {
        var job1 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.None, OFFERS_ARG1);
        var job2 = job(JobType.SHOP_LOOP_FULL, 1, UniqueType.None, OFFERS_ARG1, OFFERS_ARG2);

        Task task1 = startScheduledTask();
        assertEquals(job1.getJob_id(), task1.getJob_id());

        Task task3 = startScheduledTask();
        assertEquals(job2.getJob_id(), task3.getJob_id());
        checkNoScheduledTasks();

        TimingUtils.addTime(4000);
        checkNoScheduledTasks();

        TimingUtils.addTime(1000);
        checkNoScheduledTasks();

        TimingUtils.addTime(1);
        Task task2 = startScheduledTask();
        assertEquals(job2.getJob_id(), task2.getJob_id());

        checkNoScheduledTasks();
    }

    private void checkTasks(List<Task> tasks, JobType... types) {
        assertEquals(types.length, tasks.size());
        assertEquals(tasks.stream().map(Task::getType).collect(Collectors.toList()), List.of(types));

    }

    private Job job(int restarts, OffersArg... tasks) {
        return job(JobType.SHOP_LOOP_FULL, restarts, tasks);
    }

    @SafeVarargs
    private <T extends ToJsonString> Job job(JobType jobType, int restarts, T... tasks) {
        return job(jobType, restarts, UniqueType.ByType, tasks);
    }

    @SafeVarargs
    private <T extends ToJsonString> Job job(JobType jobType, int restarts, UniqueType unique, T... tasks) {
        var job = tasksService.registerJob(JobParams.builder()
                .unique(unique)
                .type(jobType)
                .args(ToJsonString.wrap(""))
                .tasks(List.of(tasks))
                .maxRestartCount(restarts)
                .refreshSec(1)
                .build()).orElseThrow();
        job.setScheduled_items(tasks.length);
        return job;
    }

    private void completeJob(Job job, @Nullable String text) {
        tasksService.completeJob(job, text);
        job.setStatus(JobStatus.COMPLETE);
    }

    private void abortJob(Job job) {
        tasksService.abortJob(job.getJob_id());
        job.setStatus(JobStatus.ABORTED);
    }

    private void checkActiveJobs(Job... jobs) {
        testControls.checkActiveJobs(jobs);
    }

    private void checkRestartableTasks(Job job, Task... tasks) {
        compareTasks(List.of(tasks), tasksService.getRestartableTasks(job.getJob_id()));
    }

    private void checkCompleteTasks(Job job, Task... tasks) {
        compareTasks(List.of(tasks), tasksService.getCompleteTasks(job.getJob_id()));
    }

    private void compareTasks(List<Task> expect, List<Task> actual) {
        var expectSet = Utils.toMapLong(expect, Task::getTask_id);
        for (var actualTask : actual) {
            var expectTask = expectSet.get(actualTask.getTask_id());
            if (expectTask != null) {
                expectTask.setTotal_time_millis(actualTask.getTotal_time_millis());
            }
        }
        assertEquals(expect, actual);
    }

    private Task startScheduledTask() {
        return testControls.startScheduledTask(JobType.SHOP_LOOP_FULL);
    }

    private Task startScheduledTask(JobType jobType) {
        return testControls.startScheduledTask(jobType);
    }

    private void checkNoScheduledTasks() {
        testControls.checkNoScheduledTasks();
    }


    private void successTask(Task task) {
        tasksService.completeTask(task, TaskStatus.SUCCESS, null, null);
    }

    private void completeTaskFailure(Task task) {
        tasksService.completeTask(task, TaskStatus.FAILURE, null, null);
    }

    private void updateTask(Task task) {
        tasksService.updateTask(task);
    }

    private static void set(Job job, int running, int success, int failure, int scheduled) {
        job.setRunning_items(running);
        job.setSuccess_items(success);
        job.setFailure_items(failure);
        job.setScheduled_items(scheduled);
    }

    public static void assertRestarted(Task task, Task taskRestarted) {
        assertEquals(task.getJob_id(), taskRestarted.getJob_id());
        assertTrue(task.getTask_id() < taskRestarted.getTask_id(),
                () -> task.getTask_id() + " < " + taskRestarted.getTask_id());
        assertEquals(task.getHostname(), taskRestarted.getHostname());
        assertFalse(task.getCreated().isAfter(taskRestarted.getCreated()),
                () -> task.getCreated() + " <= " + taskRestarted.getCreated());
        assertEquals(task.getStarted_first(), taskRestarted.getStarted_first());
        assertNotNull(task.getStarted());
        assertNotNull(taskRestarted.getStarted());
        assertFalse(task.getStarted().isAfter(taskRestarted.getStarted()),
                () -> task.getStarted() + " <= " + taskRestarted.getStarted());
        assertFalse(task.getUpdated().isAfter(taskRestarted.getUpdated()),
                () -> task.getUpdated() + " <= " + taskRestarted.getUpdated());

        assertFalse(taskRestarted.getCreated().isAfter(taskRestarted.getStarted()),
                () -> taskRestarted.getCreated() + " <= " + taskRestarted.getStarted());
        assertFalse(taskRestarted.getStarted().isAfter(taskRestarted.getUpdated()),
                () -> taskRestarted.getStarted() + " <= " + taskRestarted.getUpdated());
        assertEquals(task.getType(), taskRestarted.getType());
        assertEquals(TaskStatus.RUNNING, taskRestarted.getStatus());
        assertEquals(task.getArgs(), taskRestarted.getArgs());
        assertNull(taskRestarted.getStats());
        assertEquals(task.getMax_restart_count(), taskRestarted.getMax_restart_count());
        assertEquals(task.getRestart_count() + 1, taskRestarted.getRestart_count());
        assertTrue(taskRestarted.getRestart_count() <= taskRestarted.getMax_restart_count(),
                taskRestarted.getRestart_count() + " <= " + taskRestarted.getMax_restart_count());
        assertNull(taskRestarted.getInfo());
    }
}
