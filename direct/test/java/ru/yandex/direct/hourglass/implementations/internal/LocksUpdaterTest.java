package ru.yandex.direct.hourglass.implementations.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.TaskHooks;
import ru.yandex.direct.hourglass.TaskProcessingResult;
import ru.yandex.direct.hourglass.TaskThreadPool;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.SchedulerServiceImpl;
import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.hourglass.implementations.randomchoosers.SimpleRandomChooserImpl;
import ru.yandex.direct.hourglass.storage.Job;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.PrimaryId;
import ru.yandex.direct.hourglass.storage.TaskId;
import ru.yandex.direct.hourglass.storage.implementations.TaskIdImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.IntegerPrimaryId;
import ru.yandex.direct.hourglass.storage.implementations.memory.MemStorageImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.MutableJob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class LocksUpdaterTest {
    private TaskThreadPool threadPool;
    private MemStorageImpl memStorage;
    private Function<Job, TaskHooks> taskAdaptor;
    private TaskHooks taskHooks;
    private int id;
    private MonitoringWriter vitalSignsListener;

    private static final Instant MIN = Instant.ofEpochMilli(Long.MIN_VALUE);

    private void makeScheduleFromStatuses(List<JobStatus> statuses, boolean needReschedule) {

        for (JobStatus jobStatus : statuses) {
            Instant nextRun = MIN;
            TaskId taskId = new TaskIdImpl("Task: " + id, "param");
            TaskProcessingResult taskProcessingResult = TaskProcessingResultImpl.builder().build();
            PrimaryId primaryId = new IntegerPrimaryId(id);

            memStorage.addJob(new MutableJob()
                    .setJobStatus(jobStatus)
                    .setNeedReschedule(needReschedule)
                    .setNextRun(nextRun)
                    .setPrimaryId(primaryId)
                    .setTaskId(taskId)
                    .setTaskProcessingResult(taskProcessingResult));
            id++;
        }

    }

    @BeforeEach
    void before() {
        threadPool = mock(TaskThreadPool.class);

        when(threadPool.availableThreadCount()).thenReturn(10);

        taskAdaptor = mock(Function.class);

        taskHooks = mock(TaskHooks.class);

        when(taskAdaptor.apply(any(Job.class))).thenReturn(taskHooks);

        memStorage = new MemStorageImpl();

        InstanceId instanceId = mock(InstanceId.class);

        when(instanceId.toString()).thenReturn("cafebabe");

        vitalSignsListener = mock(MonitoringWriter.class);

        memStorage.clean();
        id = 1;
    }

    @Test
    void updateLocks() {
        makeScheduleFromStatuses(List.of(JobStatus.READY,
                JobStatus.READY, JobStatus.READY,
                JobStatus.READY, JobStatus.READY,
                JobStatus.LOCKED), false);

        Future firstFuture = mock(Future.class);
        Future[] futures = new Future[4];
        Deque<Future> answers = new ArrayDeque<>();
        answers.push(firstFuture);

        for (int i = 0; i < 4; i++) {
            futures[i] = mock(Future.class);
            answers.push(futures[i]);
        }

        doAnswer(invocation -> {
            ((Consumer<Future>) invocation.getArgument(0)).accept(answers.pop());
            return null;
        }).when(threadPool).execute(any(Consumer.class), any(Callable.class));

        when(taskHooks.interrupt()).thenReturn(false);

        Map<TaskId, SchedulerServiceImpl.RunningTask> running = new HashMap<>();
        InstanceId instanceId = new InstanceIdImpl();

        NewTasksRunner newTasksRunner = new NewTasksRunner(memStorage, threadPool, taskAdaptor, running,
                new SimpleRandomChooserImpl<>(), instanceId, vitalSignsListener, Duration.ofDays(1));

        newTasksRunner.run();

        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(2L))).setJobStatus(JobStatus.READY)
                .execute();
        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(3L)))
                .setJobStatus(JobStatus.EXPIRED).execute();
        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(4L)))
                .setJobStatus(JobStatus.STOPPED).execute();

        LocksUpdater locksUpdater = new LocksUpdater(running, memStorage, instanceId, vitalSignsListener);
        locksUpdater.run();

        verify(firstFuture, times(0)).cancel(true);
        verify(futures[0], times(1)).cancel(true);
        verify(futures[1], times(1)).cancel(true);
        verify(futures[2], times(1)).cancel(true);
        verify(futures[3], times(0)).cancel(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void updateLocksThanCanBeInterrupted() {
        makeScheduleFromStatuses(List.of(JobStatus.READY,
                JobStatus.READY, JobStatus.READY,
                JobStatus.READY, JobStatus.READY,
                JobStatus.LOCKED), false);

        Future firstFuture = mock(Future.class);
        Future[] futures = new Future[4];
        Deque<Future> answers = new ArrayDeque<>();
        answers.push(firstFuture);

        for (int i = 0; i < 4; i++) {
            futures[i] = mock(Future.class);
            answers.push(futures[i]);
        }

        doAnswer(invocation -> {
            ((Consumer<Future>) invocation.getArgument(0)).accept(answers.pop());
            return null;
        }).when(threadPool).execute(any(Consumer.class), any(Callable.class));

        when(taskHooks.interrupt()).thenReturn(true);

        Map<TaskId, SchedulerServiceImpl.RunningTask> running = new HashMap<>();
        InstanceId instanceId = new InstanceIdImpl();

        NewTasksRunner newTasksRunner = new NewTasksRunner(memStorage, threadPool, taskAdaptor, running,
                new SimpleRandomChooserImpl<>(), instanceId, vitalSignsListener, Duration.ofDays(1));

        newTasksRunner.run();

        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(2L))).setJobStatus(JobStatus.READY)
                .execute();
        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(3L)))
                .setJobStatus(JobStatus.EXPIRED).execute();
        memStorage.update().wherePrimaryIdIn(Collections.singleton(new IntegerPrimaryId(4L)))
                .setJobStatus(JobStatus.STOPPED).execute();

        LocksUpdater locksUpdater = new LocksUpdater(running, memStorage, instanceId, vitalSignsListener);
        locksUpdater.run();

        verify(taskHooks, times(3)).interrupt();

        verify(firstFuture, times(0)).cancel(true);
        verify(futures[0], times(0)).cancel(true);
        verify(futures[1], times(0)).cancel(true);
        verify(futures[2], times(0)).cancel(true);
        verify(futures[3], times(0)).cancel(true);
    }

    @Test
    void stopTask__finishingTask() {
        LocksUpdater locksUpdater = new LocksUpdater(new HashMap<>(), memStorage, new InstanceIdImpl(),
                vitalSignsListener);
        SchedulerServiceImpl.RunningTask runningTask = mock(SchedulerServiceImpl.RunningTask.class);
        Future firstFuture = mock(Future.class);


        when(runningTask.getTaskHooks()).thenReturn(taskHooks);
        when(runningTask.getFuture()).thenReturn(firstFuture);
        when(runningTask.isFinishing()).thenReturn(true);

        locksUpdater.stopTask(runningTask);

        verify(firstFuture, times(0)).cancel(true);
    }

    @Test
    void stopTask__runningTask() {
        LocksUpdater locksUpdater = new LocksUpdater(new HashMap<>(), memStorage, new InstanceIdImpl(),
                vitalSignsListener);

        SchedulerServiceImpl.RunningTask runningTask = mock(SchedulerServiceImpl.RunningTask.class);
        Future firstFuture = mock(Future.class);

        when(runningTask.getTaskHooks()).thenReturn(taskHooks);
        when(runningTask.getFuture()).thenReturn(firstFuture);
        when(runningTask.isFinishing()).thenReturn(false);

        locksUpdater.stopTask(runningTask);

        verify(firstFuture, times(1)).cancel(true);
    }

    @Test
    void stopTask__withInterruptSupport() {
        LocksUpdater locksUpdater = new LocksUpdater(new HashMap<>(), memStorage, new InstanceIdImpl(),
                vitalSignsListener);
        SchedulerServiceImpl.RunningTask runningTask = mock(SchedulerServiceImpl.RunningTask.class);
        Future firstFuture = mock(Future.class);

        when(runningTask.getTaskHooks()).thenReturn(taskHooks);
        when(runningTask.getFuture()).thenReturn(firstFuture);
        when(runningTask.isFinishing()).thenReturn(false);
        when(taskHooks.interrupt()).thenReturn(true);

        locksUpdater.stopTask(runningTask);

        verify(firstFuture, times(0)).cancel(true);
    }
}
