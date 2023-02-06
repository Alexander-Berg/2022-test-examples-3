package ru.yandex.market.markup2.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.AbstractTaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.generation.IRequestGenerator;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.resultMaker.IResultMaker;
import ru.yandex.market.markup2.workflow.resultMaker.ResultsMaker;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.markup2.workflow.taskType.processor.SkippingTaskTypeProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author york
 * @since 25.05.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestUniqueContext extends MockMarkupTestBase {
    private static final Logger log = LogManager.getLogger();
    private static final int CATEGORY_UNIQUE_TYPE_ID = 1;
    private static final int TASK_UNIQUE_TYPE_ID = 2;
    private static final int CATEGORY_ONLINE_TYPE_ID = 3;
    private static final int CATEGORY_ONLINE_MULTI_TYPE_ID = 4;
    private static final int CATEGORY_ONLINE_MULTI_TYPE_ID_2 = 5;
    private static final int CATEGORY_ONLINE_MULTI_TYPE_ID_3 = 6;
    private static final int CATEGORY_ID = 10;
    private static final int CATEGORY_ID2 = 11;

    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TasksCache tasksCache;
    private AllBeans allbeans;

    private Multimap<Integer, Integer> pools;
    private Multimap<Integer, Integer> resultsByCat;
    private Multimap<Integer, Integer> resultsByTask;

    private Map<Integer, Map<Integer, TaskDataItemState>> toFailByTask;

    @Before
    public void setUp() throws Exception {
        allbeans = createNew();
        markupManager = allbeans.get(MarkupManager.class);
        tasksCache = allbeans.get(TasksCache.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        pools = ArrayListMultimap.create();
        resultsByCat = ArrayListMultimap.create();
        resultsByTask = ArrayListMultimap.create();
        toFailByTask = new HashMap<>();
        clearStepLocks();
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        Map<Integer, TaskTypeContainer> mp = new HashMap<>();
        mp.put(CATEGORY_UNIQUE_TYPE_ID, createIContainer(TaskDataUniqueStrategy.TYPE_CATEGORY));
        mp.put(TASK_UNIQUE_TYPE_ID, createIContainer(TaskDataUniqueStrategy.TASK));
        mp.put(CATEGORY_ONLINE_TYPE_ID, createIContainer(TaskDataUniqueStrategy.ONLINE_CATEGORY));
        mp.put(CATEGORY_ONLINE_MULTI_TYPE_ID, createI2Container(TaskDataUniqueStrategy.ONLINE_CATEGORY_MULTITASK));
        mp.put(CATEGORY_ONLINE_MULTI_TYPE_ID_2, createI2Container(TaskDataUniqueStrategy.ONLINE_CATEGORY_MULTITASK));
        mp.put(CATEGORY_ONLINE_MULTI_TYPE_ID_3, createIContainer(TaskDataUniqueStrategy.ONLINE_CATEGORY_MULTITASK));
        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        taskTypesContainers.setTaskTypeContainers(mp);
        return taskTypesContainers;
    }

    private TaskTypeContainer createI2Container(TaskDataUniqueStrategy typeCategory) {
        return createContainer(typeCategory, I2.class, P2.class, P2::new);
    }

    private TaskTypeContainer createIContainer(TaskDataUniqueStrategy typeCategory) {
        return createContainer(typeCategory, I.class, P.class, P::new);
    }

    private <Ident extends HasId, Payl extends AbstractTaskDataItemPayload<Ident>> TaskTypeContainer<Ident, Payl, R>
        createContainer(
            TaskDataUniqueStrategy uniqueStrategy,
            Class<Ident> identClass,
            Class<Payl> paylClass,
            BiFunction<Integer, String, Payl> creator) {
        TaskTypeContainer<Ident, Payl, R> typeContainer = new TaskTypeContainer<>();

        typeContainer.setHitmanCode("yaya");
        typeContainer.setMinBatchCount(1);
        typeContainer.setMaxBatchCount(2);
        typeContainer.setName("qqq");
        typeContainer.setDataUniqueStrategy(uniqueStrategy);
        typeContainer.setPipe(Pipes.FULLY_PARALLEL);

        typeContainer.setDataItemPayloadClass(paylClass);
        typeContainer.setDataItemResponseClass(R.class);
        typeContainer.setDataItemIdentifierClass(identClass);

        BiFunction<TaskInfo, TaskDataItem<Payl, R>, TaskDataItemState> stateFunction = (task, item) -> {
            Map<Integer, TaskDataItemState> forTask =
                toFailByTask.getOrDefault(task.getId(), new HashMap<>());

            TaskDataItemState nextState = forTask.get(item.getInputData().getDataIdentifier().getId());
            return nextState == null || nextState == TaskDataItemState.FAILED_PROCESSING ?
                TaskDataItemState.SUCCESSFUL_RESPONSE : nextState;
        };

        IRequestGenerator<Ident, Payl, R> requestGenerator = context -> {
            log.debug("start generation for {}, pools {}", context.getTask().getId(), pools);
            Collection<Integer> pool = pools.get(context.getCategoryId());
            Iterator<Integer> iter = pool.iterator();
            while (context.getLeftToGenerate() > 0 && iter.hasNext()) {
                Integer item = iter.next();
                Payl payload = creator.apply(item, "payload" + item);
                if (context.createTaskDataItem(payload)) {
                    log.debug("Taking {} for ({}) ", item, context.getTask().getId());
                    iter.remove();
                } else {
                    log.debug("Not created {} for ({}) ", item, context.getTask().getId());
                }
            }
        };

        IResultMaker<Ident, Payl, R> resultCollector = resultsContext -> {
            log.debug("start processing for {}", resultsContext.getTask().getId());

            resultsContext.getTaskDataItems().forEach(i -> {
                int id = i.getInputData().getDataIdentifier().getId();
                Map<Integer, TaskDataItemState> forTask =
                    toFailByTask.getOrDefault(resultsContext.getTask().getId(), new HashMap<>());

                if (forTask.get(id) == TaskDataItemState.FAILED_PROCESSING) {
                    log.debug("Failing result {} for task {}", id, resultsContext.getTask().getId());
                    resultsContext.failResult(i, false, "q", "qq");
                } else {
                    resultsByCat.put(resultsContext.getCategoryId(), id);
                    resultsByTask.put(resultsContext.getTask().getId(), id);
                }
            });
        };
        SkippingTaskTypeProcessor<Ident, Payl, R> skippingTaskTypeProcessor = createProcessor(
            requestGenerator,
            resultCollector,
            stateFunction,
            R.class
        );

        typeContainer.setProcessor(skippingTaskTypeProcessor);
        return typeContainer;
    }

    @Test
    public void testTaskUnique() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(TASK_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getSingleCurrentTask();
        addToPool(CATEGORY_ID, 1, 1, 1, 2, 2);
        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());

        TaskInfo nextTask = markupManager.createSingleTask(taskConfigInfo.getId(), null);
        processAllTasksWithUnlock();

        Assert.assertEquals(TaskState.COMPLETED, nextTask.getState());
        Assert.assertEquals(new HashSet<>(getResultForTask(taskInfo.getId())),
            new HashSet<>(getResultForTask(nextTask.getId())));
    }

    @Test
    public void testCategoryUniqueSimple() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        addToPool(CATEGORY_ID, 1, 1, 1);
        taskProcessManager.processAll();
        Assert.assertEquals(1, resultsByCat.get(CATEGORY_ID).size());

        CreateConfigAction createConfigAction2 = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction2);
        clearStepLocks();
        taskProcessManager.processAll();
        Assert.assertEquals(1, taskConfigInfo2.getSingleCurrentTask().getTaskStatus().getInaneGenerateCount());

        CreateConfigAction createConfigActionCat2 = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID2)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfoCat2 = markupManager.createConfig(createConfigActionCat2);
        addToPool(CATEGORY_ID2, 1, 1);
        clearStepLocks();
        taskProcessManager.processAll();
        Assert.assertEquals(1, getResultForCat(CATEGORY_ID2).size());

        //checking after restart
        restart();
        addToPool(CATEGORY_ID, 2, 2, 3, 3, 4, 4);
        addToPool(CATEGORY_ID2, 2, 2, 3, 3, 4, 4);

        processAllTasksWithUnlock();
        TaskInfo taskInfo = getTaskInfo(taskConfigInfo);
        TaskInfo taskInfo2 = getTaskInfo(taskConfigInfo2);
        TaskInfo taskInfoCat2 = getTaskInfo(taskConfigInfoCat2);

        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());
        Assert.assertEquals(TaskState.COMPLETED, taskInfo2.getState());
        Assert.assertEquals(TaskState.COMPLETED, taskInfoCat2.getState());

        Set<Integer> st = new HashSet<>(getResultForCat(CATEGORY_ID));

        List<Integer> ints = getResultForCat(CATEGORY_ID);
        ints.retainAll(getResultForCat(CATEGORY_ID2));

        //check no dublicates inside category
        Assert.assertEquals(st.size(), getResultForCat(CATEGORY_ID).size());
        Assert.assertEquals(4, getResultForCat(CATEGORY_ID).size());

        //check dublicates between categories
        Assert.assertTrue(ints.size() > 0);
    }

    @Test
    public void testCategoryUniqueMultistatuses() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(4)
            .setTypeId(CATEGORY_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getSingleCurrentTask();
        addToPool(CATEGORY_ID, 1, 2, 3, 4);

        Map<Integer, TaskDataItemState> mp = new HashMap<>();
        mp.put(1, TaskDataItemState.LOST_RESPONSE);
        mp.put(2, TaskDataItemState.CANNOT_RESPONSE);
        toFailByTask.put(taskConfigInfo.getSingleCurrentTask().getId(), mp);
        lockStep(taskConfigInfo.getSingleCurrentTask().getId(), ResultsMaker.class);
        taskProcessManager.processAll();
        mp.put(3, TaskDataItemState.FAILED_PROCESSING);
        processAllTasksWithUnlock();
        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());
        Assert.assertEquals(1, getResultForCat(CATEGORY_ID).size());
        Assert.assertEquals(0, pools.get(CATEGORY_ID).size());

        //now check that only lost can be taken again
        TaskInfo nextTask = markupManager.createSingleTask(taskConfigInfo.getId(), null);
        addToPool(CATEGORY_ID, 1, 2, 3, 4);
        processAllTasksWithUnlock();
        Assert.assertEquals(TaskState.RUNNING, nextTask.getState());
        Assert.assertEquals(3, pools.get(CATEGORY_ID).size());
        Assert.assertFalse(pools.get(CATEGORY_ID).contains(1));

        //making restart
        restart();
        nextTask = getTaskInfo(taskConfigInfo); // update reference
        processAllTasksWithUnlock();

        //check situation wasn't changed
        Assert.assertEquals(TaskState.RUNNING, nextTask.getState());
        Assert.assertEquals(3, pools.get(CATEGORY_ID).size());

        addToPool(CATEGORY_ID, 5, 6, 7);
        processAllTasksWithUnlock();
        Assert.assertEquals(TaskState.COMPLETED, nextTask.getState());
    }

    @Test
    public void testInactiveConfigsLoadedForOfflineContext() throws Exception {
        addToPool(CATEGORY_ID, 1, 2, 3);

        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_UNIQUE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();

        assertThat(taskConfigInfo.getSingleCurrentTask().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(taskConfigInfo.getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);

        // all configs are loaded after restart
        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskConfigInfo2 = tasksCache.getTaskConfig(taskConfigInfo2.getId());
        assertThat(taskConfigInfo.getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);
    }

    @Test
    public void testCompletedConfigsAreNotLoadedForOnlineContext() throws Exception {
        addToPool(CATEGORY_ID, 1, 2, 3);

        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_ONLINE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();

        assertThat(taskConfigInfo.getSingleCurrentTask().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(taskConfigInfo.getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);

        // only active configs are loaded after restart
        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskConfigInfo2 = tasksCache.getTaskConfig(taskConfigInfo2.getId());
        assertThat(taskConfigInfo).isNull();
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);
    }

    @Test
    public void testFailedProcessingConfigsAreLoadedForOnlineContext() throws Exception {
        addToPool(CATEGORY_ID, 1, 2, 3);

        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_ONLINE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        toFailByTask.put(taskConfigInfo.getSingleCurrentTask().getId(),
            Collections.singletonMap(1, TaskDataItemState.FAILED_PROCESSING));
        taskProcessManager.processAll();

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();

        assertThat(taskConfigInfo.getSingleCurrentTask().getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(taskConfigInfo.getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(taskConfigInfo.isFailedProcessing()).isEqualTo(true);
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);

        // both configs are loaded after restart
        restart();
        taskConfigInfo = tasksCache.getTaskConfig(taskConfigInfo.getId());
        taskConfigInfo2 = tasksCache.getTaskConfig(taskConfigInfo2.getId());
        assertThat(taskConfigInfo).isNotNull();
        assertThat(taskConfigInfo.getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(taskConfigInfo.isFailedProcessing()).isEqualTo(true);
        assertThat(taskConfigInfo2.getState()).isEqualTo(TaskConfigState.ACTIVE);

        // check that failed item cannot be taken again
        addToPool(CATEGORY_ID, 1, 2, 3);
        taskProcessManager.processAll();
        assertThat(taskConfigInfo2.getSingleCurrentTask().getState()).isEqualTo(TaskState.RUNNING);
        Assert.assertEquals(3, pools.get(CATEGORY_ID).size()); // none taken
    }

    @Test
    public void testCategoryOnline() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_ONLINE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        addToPool(CATEGORY_ID, 1, 1);

        taskProcessManager.processAll();
        Assert.assertEquals(1, resultsByCat.get(CATEGORY_ID).size());

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction);
        taskProcessManager.processAll();
        Assert.assertEquals(0, taskConfigInfo2.getSingleCurrentTask().getTaskStatus().getGeneratedCount());

        addToPool(CATEGORY_ID, 2);
        clearStepLocksWithScheduling(taskProcessManager);
        taskProcessManager.processTask(taskConfigInfo.getSingleCurrentTask().getId());

        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getSingleCurrentTask().getState());
        processAllTasksWithUnlock();
        //first config is finished => value '1' can be taken by second
        Assert.assertEquals(1, taskConfigInfo2.getSingleCurrentTask().getTaskStatus().getGeneratedCount());
    }

    @Test
    public void testCategoryOnlineMultitask() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_ONLINE_MULTI_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        addToPool(CATEGORY_ID, 1, 1, 1, 1);

        taskProcessManager.processAll();
        //one value '1' is taken
        Assert.assertEquals(3, pools.get(CATEGORY_ID).size());
        Assert.assertEquals(1, resultsByCat.get(CATEGORY_ID).size());

        CreateConfigAction createConfigAction2 = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(CATEGORY_ONLINE_MULTI_TYPE_ID_2)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();

        TaskConfigInfo taskConfigInfo2 = markupManager.createConfig(createConfigAction2);
        taskProcessManager.processAll();
        Assert.assertEquals(0, taskConfigInfo2.getSingleCurrentTask().getTaskStatus().getGeneratedCount());

        CreateConfigAction createConfigActionDiffType = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(CATEGORY_ONLINE_TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();
        //task with diff identity type can easily take '1'
        TaskConfigInfo taskConfigInfoDiffType = markupManager.createConfig(createConfigActionDiffType);

        CreateConfigAction createConfigAction3 = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(CATEGORY_ONLINE_MULTI_TYPE_ID_3)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setDeactivatePrevious(false)
            .build();
        //this task has different identity so it can take '1'
        TaskConfigInfo taskConfigInfo3 = markupManager.createConfig(createConfigAction3);

        taskProcessManager.processAll();
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo3.getSingleCurrentTask().getState());
        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfoDiffType.getSingleCurrentTask().getState());

        addToPool(CATEGORY_ID, 2);
        clearStepLocksWithScheduling(taskProcessManager);
        taskProcessManager.processTask(taskConfigInfo.getSingleCurrentTask().getId());

        Assert.assertEquals(TaskState.COMPLETED, taskConfigInfo.getSingleCurrentTask().getState());
        processAllTasksWithUnlock();
        //first config is finished => value '1' can be taken by second
        Assert.assertEquals(1, taskConfigInfo2.getSingleCurrentTask().getTaskStatus().getGeneratedCount());
    }

    private void processAllTasksWithUnlock() {
        processAllTasksWithUnlock(taskProcessManager);
    }

    private List<Integer> getResultForCat(int categoryId) {
        return new ArrayList<>(resultsByCat.get(categoryId));
    }

    private List<Integer> getResultForTask(int taskId) {
        return new ArrayList<>(resultsByTask.get(taskId));
    }

    private TaskInfo getTaskInfo(TaskConfigInfo taskConfigInfo) {
        return getTasksCache().getTask(taskConfigInfo.getSingleCurrentTask().getId());
    }

    private TasksCache getTasksCache() {
        return allbeans.get(TasksCache.class);
    }

    private void restart() throws Exception {
        log.debug("Doing restart");
        allbeans = doRestart(allbeans);
        markupManager = allbeans.get(MarkupManager.class);
        tasksCache = allbeans.get(TasksCache.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
    }

    private void addToPool(int categoryId, Integer... values) {
        pools.putAll(categoryId, Arrays.asList(values));
    }

}
