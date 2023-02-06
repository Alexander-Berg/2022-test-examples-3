package ru.yandex.market.markup2.workflow.taskType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.core.stubs.persisters.YangAssignmentPersisterStub;
import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.exception.CommonException;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.processor.SimpleDataItemsProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.YangTaskTypeProcessor;
import ru.yandex.market.markup2.workflow.taskType.properties.YangTaskProperties;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author galaev
 * @since 2019-06-10
 */
public class YangTaskTypeProcessorTest extends MockMarkupTestBase {
    private static final Logger log = LoggerFactory.getLogger(YangTaskTypeProcessorTest.class);

    private static final int TYPE_ID = 1;
    private static final int CATEGORY_ID = 1;
    private static final int YANG_PROJECT_ID = 1;
    private static final int YANG_BASE_POOL_ID = 1;
    private static final int BATCH_SIZE = 10;

    private int seq;
    private AllBeans allbeans;
    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TolokaApiStub tolokaApi;
    private YangAssignmentPersisterStub yangAssignmentPersister;

    @Before
    public void setUp() throws Exception {
        allbeans = createNew();
        markupManager = allbeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allbeans.get(ITaskProcessManager.class);
        tolokaApi = (TolokaApiStub) allbeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(YANG_BASE_POOL_ID));
        yangAssignmentPersister = (YangAssignmentPersisterStub) allbeans.get(YangAssignmentPersister.class);
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        Map<Integer, TaskTypeContainer> mp = new HashMap<>();
        TaskTypeContainer<I, P, R> typeContainer = new TaskTypeContainer<>();

        typeContainer.setYangProjectId(YANG_PROJECT_ID);
        typeContainer.setYangBasePoolId(YANG_BASE_POOL_ID);

        typeContainer.setMinBatchCount(BATCH_SIZE);
        typeContainer.setMaxBatchCount(BATCH_SIZE);
        typeContainer.setName("test-type");
        typeContainer.setDataUniqueStrategy(TaskDataUniqueStrategy.TYPE_CATEGORY);
        typeContainer.setPipe(Pipes.SEQUENTIALLY);

        typeContainer.setDataItemPayloadClass(P.class);
        typeContainer.setDataItemResponseClass(R.class);
        typeContainer.setDataItemIdentifierClass(I.class);

        SimpleDataItemsProcessor<I, P, R> dataItemsProcessor = new SimpleDataItemsProcessor<I, P, R>() {
            @Override
            public JsonSerializer<? super TaskDataItem<P, R>> getRequestSerializer() {
                return new JsonUtils.DefaultJsonSerializer<>();
            }
            @Override
            public Class<R> getResponseClass() {
                return R.class;
            }

            @Override
            public YangTaskProperties getYangPoolProperties(TaskInfo task,
                                                            List<TaskDataItem<P, R>> poolItems,
                                                            Pool basePool,
                                                            boolean useTraits) {
                return new YangTaskProperties()
                    .setPrivateName("pool name")
                    .setPublicDescription("description")
                    .setWillExpire("date")
                    .setPriority(100);
            }

            @Override
            public JsonDeserializer<? extends R> getResponseDeserializer() {
                return new JsonUtils.DefaultJsonDeserializer<>(R.class);
            }
        };
        dataItemsProcessor.setRequestGenerator(context -> {
            log.debug("start generation for {}", context.getTask().getId());
            while (context.getLeftToGenerate() > 0) {
                Integer item = ++seq;
                P payload = new P(item, "payload" + item);
                context.createTaskDataItem(payload);
            }
        });
        dataItemsProcessor.setResultMaker(context -> {
            log.debug("result making for {}", context.getTask().getId());
        });

        YangTaskTypeProcessor<I, P, R> yangTaskTypeProcessor = new YangTaskTypeProcessor<>(dataItemsProcessor);
        typeContainer.setProcessor(yangTaskTypeProcessor);

        mp.put(TYPE_ID, typeContainer);
        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        taskTypesContainers.setTaskTypeContainers(mp);
        return taskTypesContainers;
    }

    @Test
    public void testSendingRequests() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        assertThat(task.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getSentCount()).isEqualTo(BATCH_SIZE);

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        assertThat(yangPools).hasSize(1);
        assertThat(yangPools.get(0).getTaskId()).isEqualTo(taskId);
    }

    @Test
    public void testReceivingPoolCompleted() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        // update pool status and add results
        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        YangPoolInfo poolInfo = yangPools.get(0);
        int poolId = poolInfo.getPoolId();
        tolokaApi.addResults(poolId, generateResults(task), "test-worker");

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getReceivedCount()).isEqualTo(BATCH_SIZE);
        assertThat(poolInfo.getAssignments()).isNotEmpty();
        YangAssignmentInfo assignmentInfo = poolInfo.getAssignments().get(0);
        assertThat(assignmentInfo.getPoolId()).isEqualTo(poolId);
        assertThat(assignmentInfo.getWorkerId()).isEqualTo("test-worker");
        List<YangAssignmentInfo> persistedAssignmentInfo = yangAssignmentPersister.getByPoolId(poolId);
        assertThat(persistedAssignmentInfo).containsExactlyInAnyOrderElementsOf(poolInfo.getAssignments());
    }

    @Test
    public void testReceivingBrokenData() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        // update pool status and add results
        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        YangPoolInfo poolInfo = yangPools.get(0);
        int poolId = poolInfo.getPoolId();
        Pool pool = tolokaApi.getPoolInfo(poolId);
        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.COMPLETED);
        tolokaApi.updatePool(pool);

        tolokaApi.addResults(poolId, generateResults(task), "test-worker", new JsonUtils.DefaultJsonSerializer<R>() {
            private int i;
            @Override
            public void serialize(R value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                if (i++ == BATCH_SIZE / 2) {
                    //writing broken object
                    gen.writeStartObject();
                    gen.writeStringField("uauaua", "badaboo");
                    gen.writeEndObject();
                } else {
                    super.serialize(value, gen, serializers);
                }
            }
        });

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        assertThat(task.getTaskStatus().getReceivedFailedCount()).isEqualTo(BATCH_SIZE);
        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.PROCESSED);
    }

    @Test
    public void testReceivingPoolExpired() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        // update pool status to expired
        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        int poolId = yangPools.get(0).getPoolId();
        Pool pool = tolokaApi.getPoolInfo(poolId);
        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.EXPIRED);
        tolokaApi.updatePool(pool);

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getReceivedCount()).isZero();
        assertThat(task.getTaskStatus().getLostCount()).isEqualTo(BATCH_SIZE);
    }

    @Test
    public void testReceivingPoolClosedManually() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        // update pool status to closed manually
        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        int poolId = yangPools.get(0).getPoolId();
        Pool pool = tolokaApi.getPoolInfo(poolId);
        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.MANUAL);
        tolokaApi.updatePool(pool);

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        // all items marked failed for another retry
        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.PROCESSED);
        assertThat(task.getTaskStatus().getReceivedCount()).isZero();
        assertThat(task.getTaskStatus().getReceivedFailedCount()).isEqualTo(BATCH_SIZE);

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        // all items lost
        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getReceivedCount()).isZero();
        assertThat(task.getTaskStatus().getLostCount()).isEqualTo(BATCH_SIZE);
    }

    @Test
    public void testReceivingPoolClosedManuallyWithResults() throws CommonException {
        TaskInfo task = runTask();
        int taskId = task.getId();

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, false);
        YangPoolInfo poolInfo = yangPools.get(0);
        int poolId = yangPools.get(0).getPoolId();
        Pool pool = tolokaApi.getPoolInfo(poolId);
        pool.setStatus(PoolStatus.CLOSED);

        tolokaApi.addResults(poolId, generateResults(task), "test-worker");

        pool.setLastCloseReason(PoolCloseReason.MANUAL);
        tolokaApi.updatePool(pool);

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getReceivedCount()).isEqualTo(BATCH_SIZE);
        assertThat(poolInfo.getAssignments()).isNotEmpty();
        YangAssignmentInfo assignmentInfo = poolInfo.getAssignments().get(0);
        assertThat(assignmentInfo.getPoolId()).isEqualTo(poolId);
        assertThat(assignmentInfo.getWorkerId()).isEqualTo("test-worker");
        List<YangAssignmentInfo> persistedAssignmentInfo = yangAssignmentPersister.getByPoolId(poolId);
        assertThat(persistedAssignmentInfo).containsExactlyInAnyOrderElementsOf(poolInfo.getAssignments());
    }

    private TaskInfo runTask() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(BATCH_SIZE)
            .setTypeId(TYPE_ID)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .build();
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getCurrentTasks().get(0);
        taskProcessManager.processTask(taskInfo.getId());
        return taskInfo;
    }

    private List<R> generateResults(TaskInfo taskInfo) {
        List<R> results = new ArrayList<>();
        taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT).getItems().forEach(item -> {
            boolean approved = item.getId() % 2 == 0;
            results.add(new R(item.getId(), approved));
        });
        return results;
    }
}
