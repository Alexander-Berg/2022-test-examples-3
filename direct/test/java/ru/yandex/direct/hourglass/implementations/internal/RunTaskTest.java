package ru.yandex.direct.hourglass.implementations.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.TaskHooks;
import ru.yandex.direct.hourglass.TaskProcessingResult;
import ru.yandex.direct.hourglass.TaskThreadPool;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.SchedulerServiceImpl;
import ru.yandex.direct.hourglass.implementations.randomchoosers.SimpleRandomChooserImpl;
import ru.yandex.direct.hourglass.storage.Job;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.TaskId;
import ru.yandex.direct.hourglass.storage.implementations.TaskIdImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.IntegerPrimaryId;
import ru.yandex.direct.hourglass.storage.implementations.memory.MemStorageImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.MutableJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunTaskTest {
    private static final Instant MAX = Instant.ofEpochMilli(Long.MAX_VALUE);
    private static MemStorageImpl memStorage = new MemStorageImpl();
    private TaskThreadPool threadPool = mock(TaskThreadPool.class);
    private MonitoringWriter monitoringWriter = mock(MonitoringWriter.class);
    private TaskHooks taskHooks = null;
    private Function<Job, TaskHooks> taskAdaptor = (t) -> taskHooks;
    private Map<TaskId, SchedulerServiceImpl.RunningTask> runningMap = new HashMap<>();
    private SchedulerServiceImpl.RunningTask runningTask;
    private NewTasksRunner newTasksRunner = new NewTasksRunner(memStorage, threadPool, taskAdaptor, runningMap,
            new SimpleRandomChooserImpl<>(), new InstanceIdImpl(), monitoringWriter, Duration.ofDays(1));

    @BeforeEach
    void init() {
        memStorage.clean();
        runningMap.clear();

        runningTask = mock(SchedulerServiceImpl.RunningTask.class);

        TaskId taskId = new TaskIdImpl("TestJobName", "TestJobParams");

        MutableJob mutableJob = new MutableJob()
                .setJobStatus(JobStatus.LOCKED)
                .setNeedReschedule(false)
                .setNextRun(Instant.MIN)
                .setTaskId(taskId)
                .setTaskProcessingResult(null)
                .setPrimaryId(new IntegerPrimaryId(87L));

        memStorage.addJob(mutableJob);

        runningTask = new SchedulerServiceImpl.RunningTask(taskHooks, mutableJob, mock(Future.class));

        runningMap.put(taskId, runningTask);
    }

    @Test
    void runTaskTest() {
        Collection<Job> jobs = memStorage.find().wherePrimaryIdIn(List.of(new IntegerPrimaryId(87L))).findJobs();
        Job myJob = jobs.iterator().next();

        assertThat(jobs).hasSize(1);

        taskHooks = mock(TaskHooks.class);
        when(taskHooks.calculateNextRun(any(TaskProcessingResult.class))).thenReturn(MAX);


        newTasksRunner.runTask(myJob, taskHooks);

        verify(taskHooks, times(1)).calculateNextRun(any(TaskProcessingResult.class));
        assertThat(myJob.nextRun()).isEqualTo(MAX);
        assertThat(myJob.jobStatus()).isEqualTo(JobStatus.READY);
        assertThat(runningTask.isFinishing()).isTrue();

    }

    @Test
    void runTaskWithException() {
        Collection<Job> jobs = memStorage.find().wherePrimaryIdIn(List.of(new IntegerPrimaryId(87L))).findJobs();
        Job myJob = jobs.iterator().next();

        assertThat(jobs).hasSize(1);

        taskHooks = mock(TaskHooks.class);

        doAnswer(invocation -> {
            TaskProcessingResult taskProcessingResult = invocation.getArgument(0);

            assertThat(taskProcessingResult.lastException()).isInstanceOf((RuntimeException.class));

            return MAX;
        }).when(taskHooks).calculateNextRun(any(TaskProcessingResult.class));

        doAnswer(invocation -> {
            throw new RuntimeException();
        }).when(taskHooks).run();

        newTasksRunner.runTask(myJob, taskHooks);

        verify(taskHooks, times(1)).calculateNextRun(any(TaskProcessingResult.class));

        assertThat(myJob.nextRun()).isEqualTo(MAX);
        assertThat(JobStatus.READY).isEqualTo(myJob.jobStatus());
    }

}


