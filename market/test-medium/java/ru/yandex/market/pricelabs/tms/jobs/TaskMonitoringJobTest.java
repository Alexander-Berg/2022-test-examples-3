package ru.yandex.market.pricelabs.tms.jobs;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.database.Database;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.ToJsonString;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.services.database.model.Job;
import ru.yandex.market.pricelabs.services.database.model.JobStats;
import ru.yandex.market.pricelabs.services.database.model.JobStatus;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.ShopLoopJobStats;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;
import ru.yandex.market.pricelabs.tms.juggler.JugglerStateCollector;
import ru.yandex.market.pricelabs.tms.juggler.TmsJugglerApi;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersResult;
import ru.yandex.market.pricelabs.tms.services.database.JobParams;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;
import ru.yandex.market.pricelabs.tms.services.database.TasksService.UniqueType;
import ru.yandex.market.pricelabs.tms.services.database.TasksServiceImplTest;
import ru.yandex.market.pricelabs.tms.services.juggler.JugglerBatchEvents;
import ru.yandex.market.pricelabs.tms.services.juggler.JugglerEvent;
import ru.yandex.market.pricelabs.tms.services.juggler.JugglerStatus;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class TaskMonitoringJobTest extends AbstractTmsSpringConfiguration {

    private static final OffersArg OFFERS_ARG1 = TmsTestUtils.defaultOffersArg(1);
    private static final OffersArg OFFERS_ARG2 = TmsTestUtils.defaultOffersArg(2);
    private static final OffersArg OFFERS_ARG3 = TmsTestUtils.defaultOffersArg(3);
    private static final OffersArg OFFERS_ARG4 = TmsTestUtils.defaultOffersArg(4);

    static final long DELAY = TimeUnit.SECONDS.toMillis(10);

    @Autowired
    private TasksService tasksService;

    @Autowired
    private Database database;

    @Autowired
    private JugglerStateCollector jugglerStateCollector;

    @Autowired
    @Qualifier("mockWebServerJuggler")
    private MockWebServerControls mockWebServerJuggler;

    @Autowired
    private TmsJugglerApi tmsJugglerApi;

    @BeforeEach
    void init() {
        testControls.executeInParallel(
                () -> testControls.cleanupTasksService()
        );
    }

    @Test
    void testMonitoringEmpty() {
        testControls.taskMonitoringJob();
        checkNoScheduledTasks();
        checkActiveJobs();
    }

    @Test
    void testMonitoringWithoutRestartableItems() {
        Job job = job(0, OFFERS_ARG1);

        testControls.taskMonitoringJob();
        checkActiveJobs(job);

        assertEquals(OFFERS_ARG1.toJsonString(), startScheduledTask().getArgs());
        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        set(job, 1, 0, 0, 0);
        checkActiveJobs(job);

        assertEquals(2, listJobStats(job.getJob_id()).size());
        assertEquals(List.of(), listShopLoopStats(job.getJob_id()));
    }

    @Test
    void testMonitoringSuccess() {
        Job job = job(0, OFFERS_ARG1);
        Task task = startScheduledTask();

        var jobList = List.of(job.getJob_id());
        assertEquals(List.of(), tasksService.getCompleteJobStats(jobList));
        assertEquals(List.of(), tasksService.getCompleteShopLoopStats(jobList));

        successTask(task);
        checkRestartableItems(job);

        testControls.taskMonitoringJob();

        checkNoScheduledTasks();

        job.setStatus(JobStatus.COMPLETE);
        set(job, 0, 1, 0, 0);

        var expectJobStats = List.of(addExtendedStatFields(transformJob(job)));
        var expectShopLoopJobStats = List.of(shopLoopJobStats(job, new OffersResult()));
        assertEquals(expectJobStats, listJobStats(job.getJob_id()));
        assertEquals(expectShopLoopJobStats, listShopLoopStats(job.getJob_id()));

        assertEquals(expectJobStats, tasksService.getCompleteJobStats(jobList));
        assertEquals(expectShopLoopJobStats, tasksService.getCompleteShopLoopStats(jobList));
    }

    @Test
    void testMonitoringWithRestartableItemsButNoRestarts() {
        Job job = job(0, OFFERS_ARG1);
        Task task = startScheduledTask();

        failureTask(task);

        checkRestartableItems(job);

        testControls.taskMonitoringJob();

        checkNoScheduledTasks();
    }


    @Test
    void testMonitoringWithRestartableItems() {
        Job job = job(1, OFFERS_ARG1);
        Task task = startScheduledTask();

        failureTask(task);

        task.setStatus(TaskStatus.FAILURE);
        task.setStats("{}");
        checkRestartableItems(job, task);

        testControls.taskMonitoringJob();

        var taskRestarted = startScheduledTask();
        assertRestarted(task, taskRestarted);
        checkNoScheduledTasks();
        set(job, 1, 0, 0, 0);
        checkActiveJobs(job);

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();
        checkActiveJobs(job);

        checkRestartableItems(job); // no restartable

    }

    @Test
    void testMonitoringWithRestartableItemsOutdated() {
        Job job = job(1, OFFERS_ARG1);
        Task task = startScheduledTask();
        TimingUtils.addTime(DELAY);
        checkRestartableItems(job);

        TimingUtils.addTime(1);
        checkRestartableItems(job, task);

        testControls.taskMonitoringJob();

        var taskRestarted = startScheduledTask();
        assertRestarted(task, taskRestarted);
        checkNoScheduledTasks();

        checkRestartableItems(job); // no restartable

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();
    }

    @Test
    void testMonitoringWithMultipleRestarts() {
        Job job = job(2, OFFERS_ARG1);
        Task task = startScheduledTask();
        failureTask(task);

        // Первый fail -> рестарт

        task.setStatus(TaskStatus.FAILURE);
        task.setStats("{}");
        checkRestartableItems(job, task);

        testControls.taskMonitoringJob();

        var taskRestarted = startScheduledTask();
        assertRestarted(task, taskRestarted);
        set(job, 1, 0, 0, 0);
        checkActiveJobs(job);


        // Второй рестарт

        failureTask(taskRestarted);
        taskRestarted.setStatus(TaskStatus.FAILURE);
        taskRestarted.setStats("{}");
        checkRestartableItems(job, taskRestarted);

        testControls.taskMonitoringJob();

        var taskRestarted2 = startScheduledTask();
        assertRestarted(taskRestarted, taskRestarted2);
        checkNoScheduledTasks();
        checkActiveJobs(job);
        task.setRestart_count(2);


        // Больше рестартов не будет
        failureTask(taskRestarted2);
        checkRestartableItems(job);


        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        assertEquals(3, listJobStats(job.getJob_id()).size());
        assertEquals(List.of(), listShopLoopStats(job.getJob_id()));

        var jobList = List.of(job.getJob_id());
        set(job, 0, 0, 1, 0);
        job.setStatus(JobStatus.COMPLETE);
        assertEquals(List.of(addExtendedStatFields(transformJob(job))), tasksService.getCompleteJobStats(jobList));
        assertEquals(List.of(), tasksService.getCompleteShopLoopStats(jobList));
    }

    @Test
    void testMonitoringWithMultipleStates() {
        var now = getInstant();
        Job job = job(1, OFFERS_ARG1, OFFERS_ARG2, OFFERS_ARG3, OFFERS_ARG4);
        Task task1 = startScheduledTask();
        Task task2 = startScheduledTask();
        Task task3 = startScheduledTask();
        Task task4 = startScheduledTask();

        checkRestartableItems(job);

        failureTask(task1);
        failureTask(task3);

        task1.setStatus(TaskStatus.FAILURE);
        task1.setStats("{}");
        task3.setStatus(TaskStatus.FAILURE);
        task3.setStats("{}");
        checkRestartableItems(job, task1, task3);

        set(job, 2, 0, 2, 0);
        checkActiveJobs(job);

        testControls.taskMonitoringJob();
        var task1Restarted = startScheduledTask();
        var task3Restarted = startScheduledTask();
        assertRestarted(task1, task1Restarted);
        assertRestarted(task3, task3Restarted);
        checkNoScheduledTasks();

        set(job, 4, 0, 0, 0);
        checkActiveJobs(job);
        checkRestartableItems(job);

        // Успешно заканчиваем обработку
        successTask(task1Restarted);

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        set(job, 3, 1, 0, 0);
        checkActiveJobs(job);
        checkRestartableItems(job);

        // Падаем еще раз
        failureTask(task3Restarted);

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        set(job, 2, 1, 1, 0);
        checkActiveJobs(job);
        checkRestartableItems(job);

        //
        successTask(task2);

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        set(job, 1, 2, 1, 0);
        checkActiveJobs(job);
        checkRestartableItems(job);


        //
        successTask(task4);

        set(job, 0, 3, 1, 0);
        checkActiveJobs(job);

        testControls.taskMonitoringJob();
        checkNoScheduledTasks();

        checkRestartableItems(job);

        assertEquals(5, listJobStats(job.getJob_id()).size());
        assertEquals(4, listShopLoopStats(job.getJob_id()).size());

        var jobList = List.of(job.getJob_id());
        set(job, 0, 3, 1, 0);
        job.setStatus(JobStatus.COMPLETE);
        assertEquals(List.of(addExtendedStatFields(transformJob(job))), tasksService.getCompleteJobStats(jobList));
        assertEquals(List.of(shopLoopJobStats(job, new OffersResult())),
                tasksService.getCompleteShopLoopStats(jobList));

        set(job, 0, 0, 0, 0);
        job.setInfo("Has failures in tasks");
        assertEquals(List.of(job), tasksService.getAllJobs(JobType.getLowPriorityJobs(), now));
        assertEquals(List.of(), tasksService.getAllJobs(JobType.getLowPriorityJobs(), now.plusMillis(1)));
    }

    @Test
    @Timeout(35)
    void testMonitoringWithMultipleStatesWithJugglerCheck() throws InterruptedException {
        this.testMonitoringWithMultipleStates();
        Job job = tasksService.getLastJob(JobType.SHOP_LOOP_FULL).orElseThrow();

        mockWebServerJuggler.cleanup();
        jugglerStateCollector.reportEvents();

        RecordedRequest plJobs = mockWebServerJuggler.waitMessage();
        RecordedRequest quartzJobs = mockWebServerJuggler.waitMessage();
        mockWebServerJuggler.checkNoMessages();

        assertEquals("POST /events HTTP/1.1", plJobs.toString());

        JugglerBatchEvents expectPl = Objects.requireNonNull(Utils.fromJsonResource("tms/jobs/juggler_pl_sample.json",
                JugglerBatchEvents.class));
        expectPl.getEvents().forEach(event -> {
            event.setUnixTimestamp(getInstant().getEpochSecond());
            event.setDescription(event.getDescription().replace("${jobId}", String.valueOf(job.getJob_id())));
        });
        JugglerBatchEvents expectQuartz = getExpectedQuartzJobs();

        assertEquals(expectPl, parseActualJobs(plJobs));
        assertEquals(expectQuartz, parseActualJobs(quartzJobs));
    }

    @Test
    void taskMonitoringJobWithInvalid() {
        Job job = job(1, OFFERS_ARG1);

        database.execRW(jdbc ->
                jdbc.update("update tms_table_tasks set type = 555444 where job_id = ?", job.getJob_id()));

        assertEquals(List.of(), tasksService.startTasks(1));

        testControls.taskMonitoringJob();

        TimingUtils.addTime(DELAY + 1);

        assertEquals(1, tasksService.restartTasks(job.getJob_id()));

        testControls.taskMonitoringJob();
    }

    @Test
    void taskMonitoringJobWithUnrecoverable() {
        job(1, OFFERS_ARG1);
        Task task1 = startScheduledTask();
        tasksService.completeTask(task1, TaskStatus.UNRECOVERABLE, null, null);

        testControls.taskMonitoringJob();
    }

    @Test
    void testMonitoringForUserJob() {
        var params = JobParams.builder()
                .unique(UniqueType.ByType)
                .type(JobType.SHOP_LOOP_FULL)
                .args(ToJsonString.wrap(""))
                .tasks(List.of(OFFERS_ARG1))
                .maxRestartCount(0)
                .refreshSec(1)
                .userJob(true)
                .build();
        Job job = tasksService.registerJob(params).orElseThrow();
        job.setScheduled_items(1);
        checkActiveJobs(job);

        Task task = startScheduledTask();

        var jobList = List.of(job.getJob_id());
        assertEquals(List.of(), tasksService.getCompleteJobStats(jobList));
        assertEquals(List.of(), tasksService.getCompleteShopLoopStats(jobList));

        successTask(task);
        checkRestartableItems(job);

        testControls.taskMonitoringJob();

        // Проигнорировано (из-за user_job)
        testControls.checkNoScheduledTasks();

        job.setStatus(JobStatus.COMPLETE);
        set(job, 0, 1, 0, 0);

    }

    //

    private List<JobStats> listJobStats(long jobId) {
        return tasksService.getJobStats(jobId);
    }

    private List<ShopLoopJobStats> listShopLoopStats(long jobId) {
        return tasksService.getShopLoopStats(jobId);
    }

    private Job job(int restarts, OffersArg... args) {
        var params = JobParams.builder()
                .unique(UniqueType.ByType)
                .type(JobType.SHOP_LOOP_FULL)
                .args(ToJsonString.wrap(""))
                .tasks(List.of(args))
                .maxRestartCount(restarts)
                .refreshSec(1)
                .build();
        Job job = tasksService.registerJob(params).orElseThrow();
        job.setScheduled_items(args.length);
        checkActiveJobs(job);
        return job;
    }

    private Task startScheduledTask() {
        return startScheduledTask(JobType.SHOP_LOOP_FULL);
    }

    private Task startScheduledTask(JobType type) {
        return testControls.startScheduledTask(type);
    }

    private void successTask(Task task) {
        tasksService.completeTask(task, TaskStatus.SUCCESS, null, null);
    }

    private void failureTask(Task task) {
        tasksService.completeTask(task, TaskStatus.FAILURE, null, null);
    }

    private void checkNoScheduledTasks() {
        testControls.checkNoScheduledTasks();
    }

    private void checkActiveJobs(Job... jobs) {
        testControls.checkActiveJobs(jobs);
    }

    private void checkActiveJobTypes(JobType... types) {
        testControls.checkActiveJobTypes(types);
    }

    private void checkRestartableItems(Job job, Task... items) {
        var actual = tasksService.getRestartableTasks(job.getJob_id());
        actual.forEach(t -> t.setTotal_time_millis(0));
        assertEquals(List.of(items), actual);
    }

    private void assertRestarted(Task task, Task taskRestarted) {
        TasksServiceImplTest.assertRestarted(task, taskRestarted);
    }

    public JobStats addExtendedStatFields(JobStats jobStats) {
        jobStats.setExecution_time(tasksService.allTaskExecutionTIme(jobStats.getJob_id()));
        jobStats.setFirst_task_time(tasksService.getFirstStartedTaskTime(jobStats.getJob_id()));
        return jobStats;
    }

    private static void set(Job job, int running, int success, int failure, int scheduled) {
        job.setRunning_items(running);
        job.setSuccess_items(success);
        job.setFailure_items(failure);
        job.setScheduled_items(scheduled);
    }

    public static JobStats transformJob(Job job) {
        return JobStats.fromJob(job);
    }

    public static ShopLoopJobStats shopLoopJobStats(Job job, OffersResult result) {
        ShopLoopJobStats stats = new ShopLoopJobStats();
        stats.setJob_id(job.getJob_id());
        stats.setUpdated(job.getUpdated());
        stats.setStatus(job.getStatus());
        stats.setStats(result.toJsonString());
        return stats;
    }

    /**
     * получение списка джоб в виде строки для теста testMonitoringWithMultipleStatesWithJugglerCheck
     *
     * @return строка со всеми кварцовыми джобами
     */
    private String getExpectQuartzDescription() {
        StringBuilder description = new StringBuilder("Monitoring results.\n");
        Arrays.stream(QuartzJobs.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(CronTrigger.class)).sorted(new Comparator<Method>() {
                    @Override
                    public int compare(Method o1, Method o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                }).forEach(method -> {
                    description.append(method.getName())
                            .append(": CRIT, [Job ")
                            .append(method.getName())
                            .append(" has never been started yet.]\n");
                });
        // удаляем лишний \n
        description.deleteCharAt(description.length() - 1);
        return description.toString();
    }

    /**
     * заполнение объекта JugglerBatchEvents
     *
     * @return JugglerBatchEvents
     */
    private JugglerBatchEvents getExpectedQuartzJobs() {
        JugglerEvent event = new JugglerEvent();
        event.setHost("pricelabs_tms2-junit");
        event.setService("quartz_job_status");
        event.setInstance("");
        event.setStatus(JugglerStatus.CRIT);
        event.setTags(new HashSet<>());
        event.setDescription(getExpectQuartzDescription());
        event.setUnixTimestamp(getInstant().getEpochSecond());
        return new JugglerBatchEvents("pricelabs", List.of(event));
    }

    private JugglerBatchEvents parseActualJobs(RecordedRequest recordedRequest) {
        String json = recordedRequest.getBody().readString(StandardCharsets.UTF_8);
        log.info("Actual: {}", json);
        return Objects.requireNonNull(Utils.fromJsonString(json, JugglerBatchEvents.class));
    }
}
