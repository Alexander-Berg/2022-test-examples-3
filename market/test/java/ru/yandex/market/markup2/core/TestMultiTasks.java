package ru.yandex.market.markup2.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.entries.IStatisticInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.resultMaker.ResultsMaker;
import ru.yandex.market.markup2.workflow.taskFinalizer.TaskFinalizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author york
 * @since 22.10.2018
 */
public class TestMultiTasks extends TestTasksStatusesBase {
    private static final Logger log = LogManager.getLogger();

    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TasksCache tasksCache;

    @Before
    public void setUp() throws Exception {
        init();
        markupManager = allbeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        tasksCache = allbeans.get(TasksCache.class);
        clearStepLocks();
    }

    @Test
    public void testMutitaskCreation() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setSimultaneousTasksCount(2)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        Collection<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        Assert.assertEquals(2, taskInfos.size());

        pool.add(1);
        taskProcessManager.processAll();

        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());
        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());

        List<TaskState> states = getStates(taskInfos);
        assertThat(states).containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.COMPLETED);

        log.debug("--------- add to pool ---------");
        pool.add(1);
        processAllTasksWithUnlock(taskProcessManager);

        states = getStates(taskInfos);
        assertThat(states).containsExactlyInAnyOrder(TaskState.COMPLETED, TaskState.COMPLETED);

        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());
    }

    @Test
    public void testMutitaskScheduled() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setSimultaneousTasksCount(2)
            .setScheduleInterval(1)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        Collection<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        Assert.assertEquals(2, taskInfos.size());

        pool.add(1);
        taskProcessManager.processAll();

        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());
        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());

        assertThat(getStates(taskInfos))
            .containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.COMPLETED);
        //one task is finished but other is created - all current are running
        assertThat(getStates(taskConfigInfo.getCurrentTasks()))
            .containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.RUNNING);

        //lock new task so that old task can be finished
        for (TaskInfo task : taskConfigInfo.getCurrentTasks()) {
            if (taskInfos.contains(task)) {
                removeStepLock(task.getId(), RequestsGenerator.class, taskProcessManager);
            } else {
                lockStep(task.getId(), RequestsGenerator.class);
            }
        }

        pool.add(2);
        taskProcessManager.processAll();

        assertThat(getStates(taskInfos))
            .containsExactlyInAnyOrder(TaskState.COMPLETED, TaskState.COMPLETED);

        Assert.assertEquals(2, taskConfigInfo.getCurrentTasks().size());
        assertThat(getStates(taskConfigInfo.getCurrentTasks()))
            .containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.RUNNING);
    }

    @Test
    public void testRestarting() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setSimultaneousTasksCount(2)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        Collection<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        pool.add(1);
        taskProcessManager.processAll();
        assertThat(getStates(taskInfos)).containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.COMPLETED);

        //one of tasks should be completed
        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskInfos = taskConfigInfo.getCurrentTasks();
        assertThat(getStates(taskInfos)).containsExactlyInAnyOrder(TaskState.RUNNING, TaskState.COMPLETED);
        pool.add(1);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(getStates(taskInfos)).containsExactlyInAnyOrder(TaskState.COMPLETED, TaskState.COMPLETED);
    }

    @Test
    public void testRestartingDiffStages() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setSimultaneousTasksCount(2)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        pool.add(1);
        pool.add(2);
        lockStep(taskInfos.get(0).getId(), RequestsGenerator.class);
        lockStep(taskInfos.get(1).getId(), RequestsGenerator.class);
        taskProcessManager.processAll();

        Assert.assertEquals(0, getStatsCnt(taskInfos, IStatisticInfo::getGeneratedRequestsCount));

        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskInfos = taskConfigInfo.getCurrentTasks();

        lockStep(taskInfos.get(0).getId(), RequestsSender.class);
        lockStep(taskInfos.get(1).getId(), RequestsSender.class);
        taskProcessManager.processAll();

        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getGeneratedRequestsCount));
        Assert.assertEquals(0, getStatsCnt(taskInfos, IStatisticInfo::getSentRequestsCount));

        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskInfos = taskConfigInfo.getCurrentTasks();

        lockStep(taskInfos.get(0).getId(), ResponsesReceiver.class);
        lockStep(taskInfos.get(1).getId(), ResponsesReceiver.class);
        taskProcessManager.processAll();

        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getGeneratedRequestsCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getSentRequestsCount));
        Assert.assertEquals(0, getStatsCnt(taskInfos, IStatisticInfo::getReceivedResponsesCount));

        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskInfos = taskConfigInfo.getCurrentTasks();

        lockStep(taskInfos.get(0).getId(), ResultsMaker.class);
        lockStep(taskInfos.get(1).getId(), ResultsMaker.class);
        taskProcessManager.processAll();

        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getGeneratedRequestsCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getSentRequestsCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getReceivedResponsesCount));
        Assert.assertEquals(0, getStatsCnt(taskInfos, IStatisticInfo::getProcessedRequestsCount));

        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskInfos = taskConfigInfo.getCurrentTasks();

        lockStep(taskInfos.get(0).getId(), TaskFinalizer.class);
        lockStep(taskInfos.get(1).getId(), TaskFinalizer.class);
        taskProcessManager.processAll();

        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getGeneratedRequestsCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getSentRequestsCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getReceivedResponsesCount));
        Assert.assertEquals(2, getStatsCnt(taskInfos, IStatisticInfo::getProcessedRequestsCount));

        //Statistic not updated now
        Assert.assertEquals(0, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());

        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());

        taskProcessManager.processAll();
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getSentRequestsCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getReceivedResponsesCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getReceivedResponsesCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());
    }

    @Test
    public void testNotAutostart() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(false)
            .setSimultaneousTasksCount(2)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        Assert.assertEquals(0, taskInfos.size());

        taskInfos = markupManager.createTasks(taskConfigInfo.getId(), Arrays.asList(1, 2));
        Assert.assertEquals(2, taskInfos.size());
        Assert.assertTrue(taskInfos.stream().map(ti -> ti.getHeadTaskId()).collect(Collectors.toList())
            .containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testCreateSingle() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(false)
            .setSimultaneousTasksCount(2)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);

        TaskInfo info = markupManager.createSingleTask(taskConfigInfo.getId(), 100);

        Assert.assertEquals(100, (int) info.getHeadTaskId());

        Assert.assertEquals(1, taskConfigInfo.getCurrentTasks().size());
        Assert.assertEquals(info, taskConfigInfo.getCurrentTasks().get(0));
    }

    private int getStatsCnt(List<TaskInfo> taskInfos, Function<IStatisticInfo, Integer> getter) {
        return taskInfos.stream().mapToInt(t -> getter.apply(t.getTaskStatus())).sum();
    }

    private List<TaskState> getStates(Collection<TaskInfo> taskInfos) {
        return taskInfos.stream().map(t -> t.getState()).collect(Collectors.toList());
    }

    private void restart() throws Exception {
        log.debug("----------- doing restart -----------");
        clearStepLocks();
        allbeans = doRestart(allbeans);
        markupManager = allbeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        tasksCache = allbeans.get(TasksCache.class);
    }
}
