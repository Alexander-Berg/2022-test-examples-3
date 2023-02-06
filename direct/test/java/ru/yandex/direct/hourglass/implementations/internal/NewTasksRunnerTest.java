package ru.yandex.direct.hourglass.implementations.internal;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.RandomChooser;
import ru.yandex.direct.hourglass.TaskHooks;
import ru.yandex.direct.hourglass.TaskThreadPool;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.SchedulerServiceImpl;
import ru.yandex.direct.hourglass.storage.Job;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.PrimaryId;
import ru.yandex.direct.hourglass.storage.TaskId;
import ru.yandex.direct.hourglass.storage.implementations.TaskIdImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.IntegerPrimaryId;
import ru.yandex.direct.hourglass.storage.implementations.memory.MemStorageImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.MutableJob;

import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewTasksRunnerTest {
    private NewTasksRunner newTasksRunner;
    private MemStorageImpl storage = new MemStorageImpl();
    private TaskThreadPool threadPool;
    private Function<Job, TaskHooks> taskAdaptor;
    private RandomChooser<PrimaryId> randomChooser;
    private TaskHooks taskHooks;
    private Map<TaskId, SchedulerServiceImpl.RunningTask> runningTaskMap;

    @BeforeEach
    void init() {
        storage.clean();
        threadPool = mock(TaskThreadPool.class);
        randomChooser = mock(RandomChooser.class);
        taskAdaptor = mock(Function.class);
        taskHooks = mock(TaskHooks.class);
        runningTaskMap = new HashMap<>();

        when(randomChooser.choose(anyCollection(), anyInt()))
                .then(argv -> {
                    Collection<PrimaryId> primaryIds = argv.getArgument(0);
                    int n = argv.getArgument(1);
                    return new ArrayList<>(primaryIds).subList(0, Math.min(primaryIds.size(), n));
                });

        newTasksRunner = new NewTasksRunner(storage, threadPool,
                taskAdaptor, runningTaskMap,
                randomChooser,
                new InstanceIdImpl(),
                mock(MonitoringWriter.class), Duration.ofMinutes(2));
    }

    private MutableJob genJob(JobStatus status, int id, boolean needReschedule, Instant localDateTime) {
        MutableJob job = genJob(status, id, needReschedule);
        return job.setNextRun(localDateTime);
    }

    private MutableJob genJob(JobStatus status, int id, boolean needReschedule) {

        return new MutableJob()
                .setTaskId(new TaskIdImpl("test_task_" + id, "{}"))
                .setJobStatus(status)
                .setPrimaryId(new IntegerPrimaryId(id))
                .setNeedReschedule(needReschedule)
                .setNextRun(Instant.now());
    }

    @Test
    void getNewTasks() {
        storage.addJob(genJob(JobStatus.EXPIRED, 1, false));
        storage.addJob(genJob(JobStatus.EXPIRED, 2, false));
        storage.addJob(genJob(JobStatus.LOCKED, 3, false));
        storage.addJob(genJob(JobStatus.READY, 4, false));
        storage.addJob(genJob(JobStatus.STOPPED, 5, false));
        storage.addJob(genJob(JobStatus.ARCHIVED, 6, false));
        storage.addJob(genJob(JobStatus.READY, 7, false));
        storage.addJob(genJob(JobStatus.READY, 8, false));
        storage.addJob(genJob(JobStatus.READY, 9, false));
        storage.addJob(genJob(JobStatus.READY, 10, true));
        storage.addJob(genJob(JobStatus.LOCKED, 11, true));

        Future firstFuture = mock(Future.class);
        Future[] futures = new Future[3];

        for (int i = 0; i < 3; i++) {
            futures[i] = mock(Future.class);
        }

        when(threadPool.availableThreadCount()).thenReturn(100);
        when(taskAdaptor.apply(any(Job.class))).thenReturn(taskHooks);
        newTasksRunner.run();

        verify(taskAdaptor, times(4)).apply(any(Job.class));

        Collection<Job> jobs = storage.find().whereJobStatus(JobStatus.LOCKED).findJobs();

        Map<Long, Job> longJobMap =
                jobs.stream().collect(Collectors.toMap(el -> ((IntegerPrimaryId) el.primaryId()).getId(), el -> el));

        assertThat(longJobMap).hasSize(6);
        assertThat(longJobMap.keySet()).containsExactlyInAnyOrder(3L, 4L, 7L, 8L, 9L, 11L);
    }

    @Test
    public void isJobMissed_OldJob() {
        Job job = mock(Job.class);

        Instant now = Instant.now();
        when(job.nextRun()).thenReturn(now.minus(1, HOURS));

        assertThat(newTasksRunner.isJobMissed(job, now)).isTrue();
    }

    @Test
    public void isJobMissed_FreshJob() {
        Job job = mock(Job.class);

        Instant now = Instant.now();
        when(job.nextRun()).thenReturn(now.minusSeconds(20));

        assertThat(newTasksRunner.isJobMissed(job, now)).isFalse();
    }

    @Test
    public void isJobMissed_futureRun() {
        Job job = mock(Job.class);

        Instant now = Instant.now();
        when(job.nextRun()).thenReturn(now.plusSeconds(20));

        assertThat(newTasksRunner.isJobMissed(job, now)).isFalse();
    }

    @Test
    public void getMissedTasks_checkLimit() {
        Job job = mock(Job.class);

        Instant now = Instant.now();
        when(job.nextRun()).thenReturn(now.minus(1, HOURS));
        when(job.primaryId()).thenReturn(new IntegerPrimaryId(79));

        Collection<PrimaryId> primaryIds = newTasksRunner.getMissedTasks(List.of(job), 1);

        assertThat(primaryIds.size()).isEqualTo(1);
        assertThat(primaryIds).containsExactlyInAnyOrder(new IntegerPrimaryId(79));
    }

    @Test
    public void getMissedTasks_overLimit() {
        Job job1 = mock(Job.class);

        Instant now = Instant.now();
        when(job1.nextRun()).thenReturn(now.minus(1, HOURS));
        when(job1.primaryId()).thenReturn(new IntegerPrimaryId(79));

        Job job2 = mock(Job.class);

        when(job2.nextRun()).thenReturn(now.minus(1, HOURS));
        when(job2.primaryId()).thenReturn(new IntegerPrimaryId(83));

        Collection<PrimaryId> primaryIds = newTasksRunner.getMissedTasks(List.of(job1, job2), 1);

        assertThat(primaryIds.size()).isEqualTo(1);
        assertThat(primaryIds).containsExactlyInAnyOrder(new IntegerPrimaryId(79));
    }

    @Test
    public void getMissedTasks_noLimit() {
        Job job1 = mock(Job.class);

        Instant now = Instant.now();
        when(job1.nextRun()).thenReturn(now.minus(1, HOURS));
        when(job1.primaryId()).thenReturn(new IntegerPrimaryId(79));

        Job job2 = mock(Job.class);

        when(job2.nextRun()).thenReturn(now.minus(1, HOURS));
        when(job2.primaryId()).thenReturn(new IntegerPrimaryId(83));

        Collection<PrimaryId> primaryIds = newTasksRunner.getMissedTasks(List.of(job1, job2), 0);

        assertThat(primaryIds).isEmpty();
    }

    @Test
    public void getReadyJobs__selectAllReadyJobs() {
        storage.addJob(genJob(JobStatus.EXPIRED, 1, false));
        storage.addJob(genJob(JobStatus.EXPIRED, 2, false));
        storage.addJob(genJob(JobStatus.LOCKED, 3, false));
        storage.addJob(genJob(JobStatus.READY, 4, false));
        storage.addJob(genJob(JobStatus.STOPPED, 5, false));
        storage.addJob(genJob(JobStatus.ARCHIVED, 6, false));
        storage.addJob(genJob(JobStatus.READY, 7, false));
        storage.addJob(genJob(JobStatus.READY, 8, false));
        storage.addJob(genJob(JobStatus.READY, 9, false));
        storage.addJob(genJob(JobStatus.READY, 10, true));
        storage.addJob(genJob(JobStatus.LOCKED, 11, true));

        Collection<PrimaryId> primaryIds = newTasksRunner.getReadyJobs(100);

        assertThat(primaryIds)
                .containsExactlyInAnyOrder(new IntegerPrimaryId(4L), new IntegerPrimaryId(7L), new IntegerPrimaryId(8L),
                        new IntegerPrimaryId(9L));
    }


    @Test
    public void getReadyJobs__selectAllReadyJobsButNotRunningNow() {
        storage.addJob(genJob(JobStatus.EXPIRED, 1, false));
        storage.addJob(genJob(JobStatus.EXPIRED, 2, false));
        storage.addJob(genJob(JobStatus.LOCKED, 3, false));
        storage.addJob(genJob(JobStatus.READY, 4, false));
        storage.addJob(genJob(JobStatus.STOPPED, 5, false));
        storage.addJob(genJob(JobStatus.ARCHIVED, 6, false));
        storage.addJob(genJob(JobStatus.READY, 7, false));
        storage.addJob(genJob(JobStatus.READY, 8, false));
        storage.addJob(genJob(JobStatus.READY, 9, false));
        storage.addJob(genJob(JobStatus.READY, 10, true));
        storage.addJob(genJob(JobStatus.LOCKED, 11, true));

        SchedulerServiceImpl.RunningTask firstRunningTask = mock(SchedulerServiceImpl.RunningTask.class);
        SchedulerServiceImpl.RunningTask secondRunningTask = mock(SchedulerServiceImpl.RunningTask.class);

        Job firstJob = mock(Job.class);
        Job secondJob = mock(Job.class);

        when(firstRunningTask.getJob()).thenReturn(firstJob);
        when(secondRunningTask.getJob()).thenReturn(secondJob);

        when(firstJob.primaryId()).thenReturn(new IntegerPrimaryId(4L));
        when(secondJob.primaryId()).thenReturn(new IntegerPrimaryId(7L));

        runningTaskMap.put(new TaskIdImpl("TestJob1", ""), firstRunningTask);
        runningTaskMap.put(new TaskIdImpl("TestJob2", ""), secondRunningTask);

        Collection<PrimaryId> primaryIds = newTasksRunner.getReadyJobs(100);

        assertThat(primaryIds)
                .containsExactlyInAnyOrder(new IntegerPrimaryId(8L), new IntegerPrimaryId(9L));
    }


    @Test
    public void getReadyJobs__selectAllChosenAndAllMissed() {
        Instant now = Instant.now();
        storage.addJob(genJob(JobStatus.EXPIRED, 1, false));
        storage.addJob(genJob(JobStatus.EXPIRED, 2, false));
        storage.addJob(genJob(JobStatus.LOCKED, 3, false));
        storage.addJob(genJob(JobStatus.READY, 4, false));
        storage.addJob(genJob(JobStatus.STOPPED, 5, false));
        storage.addJob(genJob(JobStatus.ARCHIVED, 6, false));
        storage.addJob(genJob(JobStatus.READY, 7, false));
        storage.addJob(genJob(JobStatus.READY, 8, false));
        storage.addJob(genJob(JobStatus.READY, 9, false));
        storage.addJob(genJob(JobStatus.READY, 10, true));
        storage.addJob(genJob(JobStatus.LOCKED, 11, true));

        storage.addJob(genJob(JobStatus.READY, 12, false, now.minus(5, MINUTES)));
        storage.addJob(genJob(JobStatus.READY, 13, false, now.minus(3, MINUTES)));
        storage.addJob(genJob(JobStatus.READY, 14, false, now.minus(1, MINUTES)));


        when(randomChooser.choose(anyCollection(), anyInt()))
                .then(argv -> {
                    Collection<PrimaryId> primaryIds = argv.getArgument(0);

                    primaryIds = primaryIds.stream()
                            .filter(el -> ((IntegerPrimaryId) el).getId() != 12 && ((IntegerPrimaryId) el).getId() != 13
                                    && ((IntegerPrimaryId) el).getId() != 14)
                            .collect(Collectors.toList());

                    int n = argv.getArgument(1);
                    return new ArrayList<>(primaryIds).subList(0, Math.min(primaryIds.size(), n));
                });


        Collection<PrimaryId> primaryIds = newTasksRunner.getReadyJobs(100);

        assertThat(primaryIds)
                .containsExactlyInAnyOrder(new IntegerPrimaryId(4L), new IntegerPrimaryId(7L), new IntegerPrimaryId(8L),
                        new IntegerPrimaryId(9L), new IntegerPrimaryId(12L), new IntegerPrimaryId(13L));
    }

    @Test
    void test() throws IOException {
        Map<String, String> map = Map.of("zis_daemon", "true", "param", "{shard:1,cluster:\"HAHN\"}");
        var mapper = new ObjectMapper().configure(ORDER_MAP_ENTRIES_BY_KEYS, true);
        var srt = mapper.writeValueAsString(map);
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
        Map<String, String> resultMap = mapper.readValue(srt, mapType);
        System.out.println(srt);
    }

    @JsonPropertyOrder(alphabetic = true)
    private static class MapTest {
        private Map<String, String> map;

    }
}
