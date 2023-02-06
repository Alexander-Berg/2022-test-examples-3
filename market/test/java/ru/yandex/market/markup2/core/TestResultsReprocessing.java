package ru.yandex.market.markup2.core;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
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
import ru.yandex.market.markup2.processors.task.DataItems;
import ru.yandex.market.markup2.processors.task.TaskProgress;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponseReceiverContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.markup2.workflow.taskType.processor.SimpleDataItemsProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.SkippingTaskTypeProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author york
 * @since 26.07.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestResultsReprocessing extends MockMarkupTestBase {
    private static final Logger log = LogManager.getLogger();
    private static final int TYPE_ID = 1;
    private static final int CATEGORY_ID = 1;

    private int seq;
    private boolean doFail;

    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TasksCache tasksCache;
    private AllBeans allbeans;
    private Set<I> processed;

    @Before
    public void setUp() throws Exception {
        allbeans = createNew();
        markupManager = allbeans.get(MarkupManager.class);
        tasksCache = allbeans.get(TasksCache.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        processed = new HashSet<>();
    }


    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        Map<Integer, TaskTypeContainer> mp = new HashMap<>();
        TaskTypeContainer<I, P, R> typeContainer = new TaskTypeContainer<>();

        typeContainer.setHitmanCode("yaya");
        typeContainer.setMinBatchCount(1);
        typeContainer.setMaxBatchCount(2);
        typeContainer.setName("qqq");
        typeContainer.setDataUniqueStrategy(TaskDataUniqueStrategy.TYPE_CATEGORY);
        typeContainer.setPipe(Pipes.SEQUENTIALLY);

        typeContainer.setDataItemPayloadClass(P.class);
        typeContainer.setDataItemResponseClass(R.class);
        typeContainer.setDataItemIdentifierClass(I.class);

        SimpleDataItemsProcessor<I, P, R> simpleHitmanDataItemsProcessor = new SimpleDataItemsProcessor
            <I, P, R>() {
            @Override
            public JsonSerializer<? super TaskDataItem<P, R>> getRequestSerializer() {
                return new JsonUtils.DefaultJsonSerializer<>();
            }
            @Override
            public Class<R> getResponseClass() {
                return R.class;
            }
            @Override
            public JsonDeserializer<? extends R> getResponseDeserializer() {
                return new JsonUtils.DefaultJsonDeserializer<>(R.class);
            }
        };
        simpleHitmanDataItemsProcessor.setRequestGenerator(context -> {
            log.debug("start generation for {}", context.getTask().getId());
            while (context.getLeftToGenerate() > 0) {
                Integer item = ++seq;
                P payload = new P(item, "payload" + item);
                context.createTaskDataItem(payload);
            }
        });
        simpleHitmanDataItemsProcessor.setResultMaker(resultsContext -> {
            log.debug("start processing for {} do fail {}", resultsContext.getTask().getId(), doFail);

            resultsContext.getTaskDataItems().forEach(i -> {
                if (doFail) {
                    resultsContext.failResult(i, false, "fail", "fail");
                } else {
                    processed.add(i.getInputData().getDataIdentifier());
                }
            });
        });
        SkippingTaskTypeProcessor<I, P, R> skippingTaskTypeProcessor =
            new SkippingTaskTypeProcessor<I, P, R>(simpleHitmanDataItemsProcessor) {
                @Override
                public void receiveResponses(ResponseReceiverContext context) {
                    log.debug("start receiving for {}", context.getTask().getId());
                    TaskProgress progress = context.getTask().getProgress();
                    DataItems<I, P, R> dataItems = progress.getDataItemsByState(TaskDataItemState.SENT);
                    Collection<TaskDataItem<P, R>> items = new ArrayList<>(dataItems.getItems());
                    progress.changeDataItemsState(items, TaskDataItemState.SENT,
                        TaskDataItemState.SUCCESSFUL_RESPONSE);
                    context.addDataItems(items);
                }
            };

        typeContainer.setProcessor(skippingTaskTypeProcessor);

        mp.put(TYPE_ID, typeContainer);
        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        taskTypesContainers.setTaskTypeContainers(mp);
        return taskTypesContainers;
    }

    @Test
    public void testReprocess() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .build();
        doFail = true;
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getCurrentTasks().get(0);
        int firstTaskId = taskInfo.getId();
        taskProcessManager.processTask(firstTaskId);
        Assert.assertTrue(taskInfo.getState().isFinished());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getFailedProcessingRequestsCount());
        taskInfo = actualize(taskInfo);
        Assert.assertNotSame(taskInfo.getId(), firstTaskId); //new task created
        int secondTaskId = taskInfo.getId();
        clearStepLocks();
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertTrue(taskInfo.getState().isFinished());
        Assert.assertEquals(4, taskConfigInfo.getGroupInfo().getStatistic().getFailedProcessingRequestsCount());
        taskInfo = actualize(taskInfo);
        int thirdTaskId = taskInfo.getId();
        clearStepLocks();
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertTrue(taskInfo.getState().isFinished());
        Assert.assertEquals(6, taskConfigInfo.getGroupInfo().getStatistic().getFailedProcessingRequestsCount());

        markupManager.forceFinishTaskConfig(taskConfigInfo.getId());
        processAllTasksWithUnlock(taskProcessManager);
        Assert.assertEquals(TaskConfigState.FORCE_FINISHED, taskConfigInfo.getState());

        doFail = false;
        allbeans = doRestart(allbeans);
        taskInfo = markupManager.restartResultProcessing(firstTaskId, true);
        Assert.assertEquals(firstTaskId, taskInfo.getId());
        Assert.assertEquals(TaskState.FORCE_FINISHING, taskInfo.getState());
        processAllTasksWithUnlock(taskProcessManager);
        Assert.assertTrue(taskInfo.getState().isFinished());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());
        Assert.assertEquals(4, taskConfigInfo.getGroupInfo().getStatistic().getFailedProcessingRequestsCount());


        taskInfo = markupManager.restartResultProcessing(secondTaskId, true);
        Assert.assertEquals(secondTaskId, taskInfo.getId());
        Assert.assertEquals(TaskState.FORCE_FINISHING, taskInfo.getState());
        processAllTasksWithUnlock(taskProcessManager);
        Assert.assertTrue(taskInfo.getState().isFinished());
        Assert.assertEquals(4, taskConfigInfo.getGroupInfo().getStatistic().getProcessedRequestsCount());
        Assert.assertEquals(2, taskConfigInfo.getGroupInfo().getStatistic().getFailedProcessingRequestsCount());

        Assert.assertEquals(4, processed.size());
    }


    private TaskInfo actualize(TaskInfo info) {
        return tasksCache.getTaskInfosByConfig(info.getConfig().getId(), (t) -> !t.getState().isFinished())
            .iterator().next();
    }
}
