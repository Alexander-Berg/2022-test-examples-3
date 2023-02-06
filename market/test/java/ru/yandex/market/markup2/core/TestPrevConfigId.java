package ru.yandex.market.markup2.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.exception.EnrichedCommonException;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;

/**
 * @author york
 * @since 16.04.2019
 */
public class TestPrevConfigId extends TestTasksStatusesBase {
    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;

    @Before
    public void setUp() throws Exception {
        init();
        markupManager = allbeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        clearStepLocks();
    }

    @Test
    public void testNoPrevIdInRequest() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        pool.add(1);
        taskProcessManager.processAll();

        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getGeneratedRequestsCount());
        Assert.assertEquals(1, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getCurrentTasks().get(0).getState());

        pool.add(2);
        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        Assert.assertEquals(taskConfigInfo2.getGroupInfo(), taskConfigInfo.getGroupInfo());
        Assert.assertNotSame(taskConfigInfo2.getId(), taskConfigInfo.getId());
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo2.getCurrentTasks().get(0).getState());
    }

    @Test (expected = EnrichedCommonException.class)
    public void testPrevIdWithDiffParams() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        pool.add(1);
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getCurrentTasks().get(0).getState());

        createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setPrevConfigId(taskConfigInfo.getId())
            .addParameter(ParameterType.FOR_CLASSIFICATION, false)
            .build();
        markupManager.createConfig(createConfigAction);
    }

    @Test (expected = EnrichedCommonException.class)
    public void testDiffPrevIdInRequest() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        pool.add(1);
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getCurrentTasks().get(0).getState());

        createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .addParameter(ParameterType.FOR_CLASSIFICATION, false)
            .build();

        pool.add(2);
        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        Assert.assertNotSame(taskConfigInfo2.getGroupInfo(), taskConfigInfo.getGroupInfo());
        Assert.assertNotSame(taskConfigInfo2.getId(), taskConfigInfo.getId());
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo2.getCurrentTasks().get(0).getState());

        createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setPrevConfigId(taskConfigInfo2.getId())
            .setAutoActivate(true)
            .build();

        markupManager.createConfig(createConfigAction);
    }

    @Test
    public void testAfterRestart() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        pool.add(1);
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getCurrentTasks().get(0).getState());

        //restart
        allbeans = doRestart(allbeans);
        TasksCache tasksCache = allbeans.get(TasksCache.class);
        markupManager = allbeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        Assert.assertNull(tasksCache.getTaskConfig(taskConfigInfo.getId()));

        createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setPrevConfigId(createConfigAction.getPrevConfigId())
            .build();
        pool.add(2);
        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        Assert.assertEquals(taskConfigInfo.getGroupInfo().getId(), taskConfigInfo2.getGroupInfo().getId());
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo2.getCurrentTasks().get(0).getState());
    }
}
