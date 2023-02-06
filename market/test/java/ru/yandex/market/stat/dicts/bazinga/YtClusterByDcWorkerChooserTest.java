package ru.yandex.market.stat.dicts.bazinga;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.bazinga.impl.CronTaskState;
import ru.yandex.commune.bazinga.impl.TaskId;
import ru.yandex.commune.bazinga.impl.WorkerState;
import ru.yandex.commune.bazinga.scheduler.CronTaskInfo;
import ru.yandex.commune.bazinga.scheduler.TaskQueueName;
import ru.yandex.commune.bazinga.scheduler.WorkerMeta;
import ru.yandex.commune.bazinga.scheduler.schedule.LastJobInfo;

import static org.mockito.Mockito.when;

@Slf4j
public class YtClusterByDcWorkerChooserTest {
    YtClusterByDcWorkerChooser chooser = new YtClusterByDcWorkerChooser();

    @Mock
    CronTaskState taskCronCompleted;

    @Mock
    CronTaskState taskRegularCompleted;

    @Mock
    CronTaskState taskCronRunning;

    @Mock
    CronTaskState taskRegularRunning;

    @Mock
    WorkerMeta worker1;

    @Mock
    WorkerState worker1State;

    @Mock
    WorkerMeta worker2;

    @Mock
    WorkerState worker2State;

    @Mock
    WorkerMeta worker3;

    @Mock
    WorkerState worker3State;

    @Mock
    CronTaskInfo newTaskCron;

    @Mock
    CronTaskInfo newTaskRegular;

    @Mock
    CronTaskInfo newTaskCpuIntensive;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        TaskId cronTaskId = new TaskId("taskCron");
        TaskId regularTaskId = new TaskId("taskRegular");

        chooser.addTaskQueue(cronTaskId, TaskQueueName.CRON);
        chooser.addTaskQueue(regularTaskId, TaskQueueName.REGULAR);

        when(taskCronCompleted.getTaskId()).thenReturn(cronTaskId);
        when(taskCronCompleted.getLastJobInfo()).thenReturn(new LastJobInfo(
                Option.of(Instant.now()),
                Option.of(Instant.now()),
                true,
                Option.of(Instant.now()),
                Option.of(Instant.now()),
                0
        ));

        when(taskRegularCompleted.getTaskId()).thenReturn(regularTaskId);
        when(taskRegularCompleted.getLastJobInfo()).thenReturn(new LastJobInfo(
                Option.of(Instant.now()),
                Option.of(Instant.now()),
                true,
                Option.of(Instant.now()),
                Option.of(Instant.now()),
                0
        ));

        when(taskCronRunning.getTaskId()).thenReturn(cronTaskId);
        when(taskCronRunning.getLastJobInfo()).thenReturn(new LastJobInfo(
                Option.of(Instant.now()),
                Option.empty(),
                true,
                Option.of(Instant.now()),
                Option.empty(),
                0
        ));

        when(taskRegularRunning.getTaskId()).thenReturn(regularTaskId);
        when(taskRegularRunning.getLastJobInfo()).thenReturn(new LastJobInfo(
                Option.of(Instant.now()),
                Option.empty(),
                true,
                Option.of(Instant.now()),
                Option.empty(),
                0
        ));

        when(worker1State.getTasks()).thenReturn(
                Cf.list(
                        taskCronCompleted,
                        taskRegularRunning
                )
        );
        when(worker1.getWorkerState()).thenReturn(worker1State);

        when(worker2State.getTasks()).thenReturn(
                Cf.list(
                        taskCronRunning,
                        taskRegularCompleted
                )
        );
        when(worker2.getWorkerState()).thenReturn(worker2State);

        when(worker3State.getTasks()).thenReturn(
                Cf.list(
                        taskCronRunning,
                        taskRegularRunning
                )
        );
        when(worker3.getWorkerState()).thenReturn(worker3State);

        when(newTaskCron.getQueueName()).thenReturn(TaskQueueName.CRON);
        when(newTaskRegular.getQueueName()).thenReturn(TaskQueueName.REGULAR);
        when(newTaskCpuIntensive.getQueueName()).thenReturn(TaskQueueName.CPU_INTENSIVE);
    }

    @Test
    public void testChoiceAlgorithm() {
        ListF<WorkerMeta> workers = Cf.list(worker1, worker2, worker3);

        ListF<WorkerMeta> workersForCronQueue = chooser.choose(newTaskCron, workers);
        Assert.assertTrue(workersForCronQueue.containsTs(worker1));
        Assert.assertFalse(workersForCronQueue.containsTs(worker2));
        Assert.assertFalse(workersForCronQueue.containsTs(worker3));

        ListF<WorkerMeta> workersForRegularQueue = chooser.choose(newTaskRegular, workers);
        Assert.assertFalse(workersForRegularQueue.containsTs(worker1));
        Assert.assertTrue(workersForRegularQueue.containsTs(worker2));
        Assert.assertFalse(workersForRegularQueue.containsTs(worker3));

        ListF<WorkerMeta> workersForCpuIntensiveQueue = chooser.choose(newTaskCpuIntensive, workers);
        Assert.assertTrue(workersForCpuIntensiveQueue.containsTs(worker1));
        Assert.assertTrue(workersForCpuIntensiveQueue.containsTs(worker2));
        Assert.assertFalse(workersForCpuIntensiveQueue.containsTs(worker3));
    }
}
