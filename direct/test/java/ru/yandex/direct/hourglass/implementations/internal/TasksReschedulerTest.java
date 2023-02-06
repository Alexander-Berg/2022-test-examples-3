package ru.yandex.direct.hourglass.implementations.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.TaskHooks;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.hourglass.storage.Job;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.implementations.TaskIdImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.IntegerPrimaryId;
import ru.yandex.direct.hourglass.storage.implementations.memory.MemStorageImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.MutableJob;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TasksReschedulerTest {
    private MemStorageImpl storage;
    private Function<Job, TaskHooks> taskAdaptor;
    private TaskHooks taskHooks;
    private MonitoringWriter vitalSignsListener;

    private static final Instant MAX = Instant.ofEpochMilli(Long.MAX_VALUE);
    private static final Instant MIN = Instant.ofEpochMilli(Long.MIN_VALUE);

    @BeforeEach
    void before() {
        taskAdaptor = mock(Function.class);

        taskHooks = mock(TaskHooks.class);

        when(taskAdaptor.apply(any(Job.class))).thenReturn(taskHooks);

        storage = new MemStorageImpl();

        InstanceId instanceId = mock(InstanceId.class);

        when(instanceId.toString()).thenReturn("cafebabe");

        vitalSignsListener = mock(MonitoringWriter.class);

        storage.clean();
    }

    private MutableJob genJob(JobStatus status, int id, boolean needReschedule) {

        return new MutableJob()
                .setTaskId(new TaskIdImpl("test_task_" + id, "{}"))
                .setJobStatus(status)
                .setPrimaryId(new IntegerPrimaryId(id))
                .setNeedReschedule(needReschedule)
                .setTaskProcessingResult(TaskProcessingResultImpl.builder().build())
                .setNextRun(MIN);
    }

    @Test
    void processRescheduledTasks() {
        storage.addJob(genJob(JobStatus.READY, 1, false));
        storage.addJob(genJob(JobStatus.LOCKED, 2, false));
        storage.addJob(genJob(JobStatus.STOPPED, 3, false));

        storage.addJob(genJob(JobStatus.READY, 4, true));
        storage.addJob(genJob(JobStatus.LOCKED, 5, true));
        storage.addJob(genJob(JobStatus.STOPPED, 6, true));

        when(taskHooks.calculateNextRun(any())).thenReturn(MAX);

        TasksRescheduler tasksRescheduler = new TasksRescheduler(storage, new InstanceIdImpl(), taskAdaptor,
                vitalSignsListener);
        tasksRescheduler.run();

        Collection<Job> jobCollection = storage.find().whereJobStatus(JobStatus.READY).findJobs();

        assertThat(jobCollection).hasSize(2);

        Long[] idsExpectedToReady = new Long[]{1L, 4L};
        assertThat(jobCollection.stream().map(el -> ((IntegerPrimaryId) el.primaryId()).getId()).collect(toList())).containsExactlyInAnyOrder(idsExpectedToReady);

        assertThat(jobCollection.stream().filter(el -> ((IntegerPrimaryId) el.primaryId()).getId() == 4)
                .allMatch(el -> el.nextRun().equals(MAX))).isTrue();

        assertThat(jobCollection.stream().filter(el -> ((IntegerPrimaryId) el.primaryId()).getId() == 3)
                .allMatch(el -> el.nextRun() == MIN)).isTrue();
    }

}

