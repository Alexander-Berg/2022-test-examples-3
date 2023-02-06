package ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_generation;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.MarkupServiceImpl;
import ru.yandex.market.markup2.MarkupApiManager;
import ru.yandex.market.markup2.TaskConfigsGetter;
import ru.yandex.market.markup2.TaskDataItemsGetter;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.dao.MarkupDao;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.group.PskuIds;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.DataItems;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.Comment;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionDataIdentity;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionDataItemsProcessor;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionDataPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionRequestGenerator;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionResponse;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionResultMaker;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionTaskPropertiesGetter;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.msku_from_psku_gen_inspection.MskuFromPskuGenInspectionTypeContainerFactory;
import ru.yandex.market.markup2.utils.reports_log.ReportLogWorker;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.Task;
import ru.yandex.market.toloka.model.TaskSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author shadoff
 * created on 2019-10-25
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MskuFromPskuGenerationTest extends MockMarkupTestBase {
    private static final Logger log = LogManager.getLogger();
    private static final int TASK_TIMEOUT_MS = 30000;

    private static final int YANG_BASE_POOL_ID = 12345;
    private static final String TEST_WORKER_ID = "asdas";
    private static final String TEST_OFFER_MAPPING_STATUS = "MAPPED";
    private static final Long TEST_MSKU = 1234L;

    private static final String TEST_WORKER_ID2 = "qwer";
    private static final String TEST_OFFER_MAPPING_STATUS2 = "TRASH";
    private static final Long TEST_MSKU2 = 1235L;

    private static final String TEST_COMMENT = "Test comment";
    private static final String TEST_COMMENT_TYPE = "FOR_REVISION";
    private static final String TEST_COMMENT_ITEM1 = "comment item 1";
    private static final String TEST_COMMENT_ITEM2 = "item 2";

    private static final long TEST_PSKU_ID_DELETED = 10L;
    private static final long TEST_PSKU_ID_DELETED_BEFORE_INSPECTION = 20L;
    private static final long TEST_PSKU_ID_1 = 30L;
    private static final long TEST_PSKU_ID_2 = 40L;

    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TasksCache tasksCache;
    private TovarTreeProvider tovarTreeProvider;
    private YangLogStorageService yangLogStorageService;
    private UsersService usersService;
    private AllBeans allBeans;
    private TolokaApiStub tolokaApi;
    private TaskDataItemsGetter taskDataItemsGetter;
    private MskuFromPskuGenerationTaskPropertiesGetter mskuFromPskuGenerationTaskPropertiesGetter;
    private MskuFromPskuGenInspectionTaskPropertiesGetter mskuFromPskuGenInspectionTaskPropertiesGetter;
    private MarkupServiceImpl markupService;

    @Before
    public void setUp() throws Exception {
        mskuFromPskuGenerationTaskPropertiesGetter
                = new MskuFromPskuGenerationTaskPropertiesGetter();
        mskuFromPskuGenInspectionTaskPropertiesGetter
                = new MskuFromPskuGenInspectionTaskPropertiesGetter();
        tovarTreeProvider = Mockito.mock(TovarTreeProvider.class);
        usersService = Mockito.mock(UsersService.class);
        when(usersService.getUidByWorkerIdOrDefault(Mockito.eq(TEST_WORKER_ID))).thenReturn(1L);
        when(usersService.getUidByWorkerIdOrDefault(Mockito.eq(TEST_WORKER_ID2))).thenReturn(2L);
        yangLogStorageService = Mockito.mock(YangLogStorageService.class);
        when(yangLogStorageService.yangLogStore(any())).thenReturn(YangLogStorage.YangLogStoreResponse.newBuilder()
                .setSuccess(true)
                .build());

        allBeans = createNew();
        markupManager = allBeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tasksCache = allBeans.get(TasksCache.class);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        mskuFromPskuGenerationTaskPropertiesGetter.setTolokaApi(allBeans.get(TolokaApi.class));
        mskuFromPskuGenInspectionTaskPropertiesGetter.setTolokaApi(allBeans.get(TolokaApi.class));
        taskDataItemsGetter = new TaskDataItemsGetter();
        taskDataItemsGetter.setMarkupDao(allBeans.get(MarkupDao.class));
        taskDataItemsGetter.setTasksCache(allBeans.get(TasksCache.class));


        MarkupApiManager markupApiManager = new MarkupApiManager();
        markupApiManager.setCache(tasksCache);
        markupApiManager.setMarkupManager(markupManager);

        TaskConfigsGetter taskConfigsGetter = new TaskConfigsGetter();
        taskConfigsGetter.setApiManager(markupApiManager);
        taskConfigsGetter.setMarkupDao(allBeans.get(MarkupDao.class));
        taskConfigsGetter.setTasksCache(tasksCache);

        ReportLogWorker reportLogWorker = Mockito.mock(ReportLogWorker.class);
        markupService = new MarkupServiceImpl();
        markupService.setApiManager(markupApiManager);
        markupService.setTaskConfigsGetter(taskConfigsGetter);
        markupService.setReportLogWorker(reportLogWorker);
        markupService.setTaskDataItemsGetter(taskDataItemsGetter);
        markupService.setUsersService(usersService);
    }

    @Test(timeout = TASK_TIMEOUT_MS)
    public void testAllProcess() throws Exception {
        clearStepLocks();
        List<Long> pskuIdsList = ImmutableList.of(TEST_PSKU_ID_1, TEST_PSKU_ID_2,
            TEST_PSKU_ID_DELETED, TEST_PSKU_ID_DELETED_BEFORE_INSPECTION);
        PskuIds pskuIds = new PskuIds(pskuIdsList);

        allBeans.get(TolokaApi.class).createPool(new Pool().setId(YANG_BASE_POOL_ID));

        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
                .setCount(4)
                .setTypeId(Markup.TaskType.MSKU_FROM_PSKU_GENERATION_VALUE)
                .setCategoryId(91491)
                .setAutoActivate(true)
                .addParameter(ParameterType.PSKU_IDS, pskuIds)
                .setSimultaneousTasksCount(1)
                .setMaxBatchSize(100)
                .setMinBatchSize(1)
                .setYangPoolId(YANG_BASE_POOL_ID)
                .build();

        mockNeedInspectionCall(true);

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getSingleCurrentTask();

        when(usersService.getUserByTaskId(taskInfo.getId()))
            .thenReturn(MboUsers.MboUser.newBuilder().setStaffLogin(TEST_WORKER_ID).build());
        log.debug("process task {}", taskInfo.getId());
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());

        processTaskInYang(taskInfo, false);

        // receiving
        clearStepLocks();
        taskProcessManager.processTask(taskInfo.getId());
        // task is running because of inspection
        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());

        // test handler returns nothing
        Markup.MskuFromPskuGenerationTaskResponse mskuFromPskuGenerationTaskResult =
                markupService.getMskuFromPskuGenerationTaskResult(
                        Markup.GetByConfigRequest.newBuilder().setTaskConfigId(taskConfigInfo.getId()).build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, mskuFromPskuGenerationTaskResult.getStatus().getStatus());
        Assert.assertEquals(0, mskuFromPskuGenerationTaskResult.getTaskResultCount());
        Assert.assertEquals("", mskuFromPskuGenerationTaskResult.getUserLogin());

        Markup.GetTaskInfoResponse lastTaskInfo = markupService.getLastTaskInfo(
                Markup.GetTaskInfoRequest.newBuilder()
                        .addItem(Markup.GetTaskInfoRequestItem.newBuilder()
                                .setConfigId(taskConfigInfo.getId())
                                .setReturnAllStates(true)
                                .setOnlyLast(true)
                                .build())
                        .build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, lastTaskInfo.getStatus().getStatus());
        Assert.assertEquals(Markup.TaskState.RUNNING_TASK, lastTaskInfo.getTaskInfo(0).getState());

        // checking inspection
        Assert.assertTrue(taskConfigInfo.hasDependConfigId());
        int dependConfigId = taskConfigInfo.getDependConfigId();
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(dependConfigId);
        TaskInfo inspectionTask = inspectionConfig.getSingleCurrentTask();
        Assert.assertEquals(TaskState.RUNNING, inspectionTask.getState());

        when(usersService.getUserByTaskId(inspectionTask.getId()))
            .thenReturn(MboUsers.MboUser.newBuilder().setStaffLogin(TEST_WORKER_ID2).build());

        log.debug("process task {}", inspectionTask.getId());
        taskProcessManager.processTask(inspectionTask.getId());
        Assert.assertEquals(TaskState.RUNNING, inspectionTask.getState());

        // process inspection
        processTaskInYang(inspectionTask, true);

        // finalize
        clearStepLocks();
        log.info("finalizing task {}", inspectionTask.getId());
        taskProcessManager.processTask(inspectionTask.getId());
        Assert.assertEquals(TaskState.COMPLETED, inspectionTask.getState());

        // finalize main task
        clearStepLocks();
        log.info("finalizing task {}", taskInfo.getId());
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());

        // check configs are disabled
        Assert.assertTrue(inspectionConfig.getState().isFinalState());
        Assert.assertTrue(taskConfigInfo.getState().isFinalState());

        // check results are written
        List<TaskDataItem<MskuFromPskuGenInspectionDataPayload, MskuFromPskuGenInspectionResponse>>
                inspectionDataItems =
                taskDataItemsGetter.get(inspectionTask.getId(), Markup.TaskType.MSKU_FROM_PSKU_GEN_INSPECTION_VALUE);

        Assert.assertEquals(1, inspectionDataItems.size());
        MskuFromPskuGenInspectionResponse inspectionResponseInfo = inspectionDataItems.get(0).getResponseInfo();

        List<MskuFromPskuGenerationResponse> taskResult = inspectionResponseInfo.getTaskResult();
        Assert.assertEquals(4, taskResult.size());
        taskResult.forEach(responseInfo -> {
                    Assert.assertEquals(TEST_COMMENT, responseInfo.getComment());
                    Assert.assertEquals(TEST_MSKU2, responseInfo.getMarketSkuId());
                    Assert.assertEquals(TEST_WORKER_ID2, responseInfo.getWorkerId());
                    Assert.assertEquals(TEST_OFFER_MAPPING_STATUS2, responseInfo.getOfferMappingStatus().name());
                    Assert.assertEquals(1, responseInfo.getComments().size());
                    Assert.assertTrue(responseInfo.getComments().get(0).getItems().containsAll(
                            Arrays.asList(TEST_COMMENT_ITEM2, TEST_COMMENT_ITEM1)));
                    Assert.assertEquals(TEST_COMMENT_TYPE, responseInfo.getComments().get(0).getType());
                    Assert.assertEquals(responseInfo.getDeleted(),
                        shouldBeDeleted(Long.parseLong(responseInfo.getOfferId()), true));
                });

        lastTaskInfo = markupService.getLastTaskInfo(
                Markup.GetTaskInfoRequest.newBuilder()
                        .addItem(Markup.GetTaskInfoRequestItem.newBuilder()
                                .setConfigId(taskConfigInfo.getId())
                                .setReturnAllStates(true)
                                .setOnlyLast(true)
                                .build())
                        .build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, lastTaskInfo.getStatus().getStatus());
        Assert.assertEquals(Markup.TaskState.COMPLETED_TASK, lastTaskInfo.getTaskInfo(0).getState());

        mskuFromPskuGenerationTaskResult =
                markupService.getMskuFromPskuGenerationTaskResult(
                        Markup.GetByConfigRequest.newBuilder().setTaskConfigId(taskConfigInfo.getId()).build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, mskuFromPskuGenerationTaskResult.getStatus().getStatus());
        Assert.assertEquals(4, mskuFromPskuGenerationTaskResult.getTaskResultCount());
        Assert.assertEquals(TEST_WORKER_ID2, mskuFromPskuGenerationTaskResult.getUserLogin());

        mskuFromPskuGenerationTaskResult.getTaskResultList().forEach(handlerTaskResult -> {
            Assert.assertEquals(TEST_COMMENT, handlerTaskResult.getComment());
            Assert.assertEquals(String.valueOf(TEST_MSKU2), handlerTaskResult.getMskuId());
            Assert.assertEquals(TEST_WORKER_ID2, handlerTaskResult.getWorkerId());
            Assert.assertEquals(TEST_OFFER_MAPPING_STATUS2, handlerTaskResult.getMappingStatus().name());
            Assert.assertEquals(1, handlerTaskResult.getCommentsCount());
            Assert.assertTrue(handlerTaskResult.getCommentsList().get(0).getItemsList().containsAll(
                    Arrays.asList(TEST_COMMENT_ITEM2, TEST_COMMENT_ITEM1)));
            Assert.assertEquals(TEST_COMMENT_TYPE, handlerTaskResult.getCommentsList().get(0).getType());
            Assert.assertEquals(handlerTaskResult.getDeleted(),
                shouldBeDeleted(Long.parseLong(handlerTaskResult.getPskuId()), true));
        });
        // Добавить проверку на то, что ручка янговской статистики дергается с правильными параметрами
        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
            ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService).yangLogStore(requestCaptor.capture());

        YangLogStorage.YangLogStoreRequest req = requestCaptor.getValue();

        // two (operator + inspector) for TEST_PSKU_ID_1
        // two (operator + inspector) for  TEST_PSKU_ID_2,
        // nothing for TEST_PSKU_ID_DELETED,
        // one (operator) for TEST_PSKU_ID_DELETED_BEFORE_INSPECTION
        assertThat(req.getMappingStatisticCount()).isEqualTo(5);

        Assertions.assertThat(req.getMappingStatisticList())
            .extracting(YangLogStorage.MappingStatistic::getOfferId, YangLogStorage.MappingStatistic::getUid)
            .containsExactlyInAnyOrder(
                Tuple.tuple(TEST_PSKU_ID_DELETED_BEFORE_INSPECTION, req.getContractorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_1, req.getContractorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_1, req.getInspectorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_2, req.getContractorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_2, req.getInspectorInfo().getUid()));
    }

    @Test(timeout = TASK_TIMEOUT_MS)
    public void testNoInspectionProcess() throws Exception {
        clearStepLocks();
        List<Long> pskuIdsList = ImmutableList.of(TEST_PSKU_ID_1, TEST_PSKU_ID_2,
            TEST_PSKU_ID_DELETED, TEST_PSKU_ID_DELETED_BEFORE_INSPECTION);
        PskuIds pskuIds = new PskuIds(pskuIdsList);

        allBeans.get(TolokaApi.class).createPool(new Pool().setId(YANG_BASE_POOL_ID));

        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
                .setCount(4)
                .setTypeId(Markup.TaskType.MSKU_FROM_PSKU_GENERATION_VALUE)
                .setCategoryId(91491)
                .setAutoActivate(true)
                .addParameter(ParameterType.PSKU_IDS, pskuIds)
                .setSimultaneousTasksCount(1)
                .setMaxBatchSize(100)
                .setMinBatchSize(1)
                .setYangPoolId(YANG_BASE_POOL_ID)
                .build();

        mockNeedInspectionCall(false);

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = taskConfigInfo.getSingleCurrentTask();

        when(usersService.getUserByTaskId(taskInfo.getId()))
            .thenReturn(MboUsers.MboUser.newBuilder().setStaffLogin(TEST_WORKER_ID).build());

        log.debug("process task {}", taskInfo.getId());
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());

        processTaskInYang(taskInfo, false);

        // finalizing
        clearStepLocks();
        log.info("finalizing task {}", taskInfo.getId());
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());

        // check config is disabled
        Assert.assertTrue(taskConfigInfo.getState().isFinalState());

        // check results are written
        List<TaskDataItem<MskuFromPskuGenerationDataPayload, MskuFromPskuGenerationResponse>> dataItems =
                taskDataItemsGetter.get(taskInfo.getId(), Markup.TaskType.MSKU_FROM_PSKU_GENERATION_VALUE);

        Assert.assertEquals(4, dataItems.size());
        dataItems.forEach(item -> {
            MskuFromPskuGenerationResponse responseInfo = item.getResponseInfo();
            Assert.assertEquals(TEST_COMMENT, responseInfo.getComment());
            Assert.assertEquals(TEST_MSKU, responseInfo.getMarketSkuId());
            Assert.assertEquals(TEST_WORKER_ID, responseInfo.getWorkerId());
            Assert.assertEquals(TEST_OFFER_MAPPING_STATUS, responseInfo.getOfferMappingStatus().name());
            Assert.assertEquals(1, responseInfo.getComments().size());
            Assert.assertTrue(responseInfo.getComments().get(0).getItems().containsAll(
                    Arrays.asList(TEST_COMMENT_ITEM2, TEST_COMMENT_ITEM1)));
            Assert.assertEquals(TEST_COMMENT_TYPE, responseInfo.getComments().get(0).getType());
            Assert.assertEquals(responseInfo.getDeleted(),
                shouldBeDeleted(Long.parseLong(responseInfo.getOfferId()), false));
        });

        Markup.GetTaskInfoResponse lastTaskInfo = markupService.getLastTaskInfo(
                Markup.GetTaskInfoRequest.newBuilder()
                        .addItem(Markup.GetTaskInfoRequestItem.newBuilder()
                                .setConfigId(taskConfigInfo.getId())
                                .setReturnAllStates(true)
                                .setOnlyLast(true)
                                .build())
                        .build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, lastTaskInfo.getStatus().getStatus());
        Assert.assertEquals(Markup.TaskState.COMPLETED_TASK, lastTaskInfo.getTaskInfo(0).getState());

        Markup.MskuFromPskuGenerationTaskResponse mskuFromPskuGenerationTaskResult =
                markupService.getMskuFromPskuGenerationTaskResult(
                        Markup.GetByConfigRequest.newBuilder().setTaskConfigId(taskConfigInfo.getId()).build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, mskuFromPskuGenerationTaskResult.getStatus().getStatus());
        Assert.assertEquals(4, mskuFromPskuGenerationTaskResult.getTaskResultCount());
        Assert.assertEquals(TEST_WORKER_ID, mskuFromPskuGenerationTaskResult.getUserLogin());

        mskuFromPskuGenerationTaskResult.getTaskResultList().forEach(handlerTaskResult -> {
            Assert.assertEquals(TEST_COMMENT, handlerTaskResult.getComment());
            Assert.assertEquals(String.valueOf(TEST_MSKU), handlerTaskResult.getMskuId());
            Assert.assertEquals(TEST_WORKER_ID, handlerTaskResult.getWorkerId());
            Assert.assertEquals(TEST_OFFER_MAPPING_STATUS, handlerTaskResult.getMappingStatus().name());
            Assert.assertEquals(1, handlerTaskResult.getCommentsCount());
            Assert.assertTrue(handlerTaskResult.getCommentsList().get(0).getItemsList().containsAll(
                    Arrays.asList(TEST_COMMENT_ITEM2, TEST_COMMENT_ITEM1)));
            Assert.assertEquals(TEST_COMMENT_TYPE, handlerTaskResult.getCommentsList().get(0).getType());
            Assert.assertEquals(handlerTaskResult.getDeleted(),
                shouldBeDeleted(Long.parseLong(handlerTaskResult.getPskuId()), false));
        });

        // check no depend config
        Assert.assertFalse(taskConfigInfo.hasDependConfigId());

        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
            ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService).yangLogStore(requestCaptor.capture());
        YangLogStorage.YangLogStoreRequest req = requestCaptor.getValue();
        assertThat(req.getMappingStatisticCount()).isEqualTo(3);
        Assertions.assertThat(req.getMappingStatisticList())
            .extracting(YangLogStorage.MappingStatistic::getOfferId, YangLogStorage.MappingStatistic::getUid)
            .containsExactlyInAnyOrder(
                Tuple.tuple(TEST_PSKU_ID_DELETED_BEFORE_INSPECTION, req.getContractorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_1, req.getContractorInfo().getUid()),
                Tuple.tuple(TEST_PSKU_ID_2, req.getContractorInfo().getUid()));
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache cache) {
        TaskTypeContainerParams<MskuFromPskuGenerationDataIdentity, MskuFromPskuGenerationDataPayload,
                MskuFromPskuGenerationResponse> params =
                createTaskContainerParams("test");
        TaskTypeContainer taskTypeContainer = MskuFromPskuGenerationTypeContainerFactory
                .createTaskTypeContainer(params, cache);

        TaskTypeContainerParams<MskuFromPskuGenInspectionDataIdentity,
                MskuFromPskuGenInspectionDataPayload, MskuFromPskuGenInspectionResponse> inspectionParams =
                createInspectionTaskContainerParams("inspection test");

        TaskTypeContainer inspectionTaskTypeContainer = MskuFromPskuGenInspectionTypeContainerFactory
                .createTypeContainer(inspectionParams, cache);

        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();

        Map<Integer, TaskTypeContainer> mp = new HashMap<>();
        mp.put(Markup.TaskType.MSKU_FROM_PSKU_GENERATION_VALUE, taskTypeContainer);
        mp.put(Markup.TaskType.MSKU_FROM_PSKU_GEN_INSPECTION_VALUE, inspectionTaskTypeContainer);
        taskTypesContainers.setTaskTypeContainers(mp);

        return taskTypesContainers;
    }

    private TaskTypeContainerParams<MskuFromPskuGenerationDataIdentity, MskuFromPskuGenerationDataPayload,
            MskuFromPskuGenerationResponse> createTaskContainerParams(
            String taskName) {

        MskuFromPskuGenerationRequestGenerator requestGenerator = new MskuFromPskuGenerationRequestGenerator();
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        MskuFromPskuGenerationStatisticsSaver statisticsSaver =
                new MskuFromPskuGenerationStatisticsSaver(yangLogStorageService, usersService);

        MskuFromPskuGenerationResultMaker resultSaver = new MskuFromPskuGenerationResultMaker(
                statisticsSaver, yangLogStorageService, usersService);

        MskuFromPskuGenerationDataItemsProcessor processor = new MskuFromPskuGenerationDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultSaver);
        processor.setPropertiesGetter(mskuFromPskuGenerationTaskPropertiesGetter);
        processor.setTovarTreeProvider(tovarTreeProvider);

        TaskTypeContainerParams<MskuFromPskuGenerationDataIdentity, MskuFromPskuGenerationDataPayload,
                MskuFromPskuGenerationResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName(taskName);
        params.setDataItemsProcessor(processor);

        return params;
    }

    private TaskTypeContainerParams<MskuFromPskuGenInspectionDataIdentity,
            MskuFromPskuGenInspectionDataPayload, MskuFromPskuGenInspectionResponse>
    createInspectionTaskContainerParams(
                    String taskName) {
        MskuFromPskuGenInspectionRequestGenerator requestGenerator = new MskuFromPskuGenInspectionRequestGenerator();
        MskuFromPskuGenerationStatisticsSaver statisticsSaver =
                new MskuFromPskuGenerationStatisticsSaver(yangLogStorageService, usersService);

        MskuFromPskuGenInspectionResultMaker resultSaver = new MskuFromPskuGenInspectionResultMaker(
                statisticsSaver);

        MskuFromPskuGenInspectionDataItemsProcessor processor = new MskuFromPskuGenInspectionDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultSaver);
        processor.setPropertiesGetter(mskuFromPskuGenInspectionTaskPropertiesGetter);
        processor.setTovarTreeProvider(tovarTreeProvider);

        TaskTypeContainerParams<MskuFromPskuGenInspectionDataIdentity, MskuFromPskuGenInspectionDataPayload,
                MskuFromPskuGenInspectionResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName(taskName);
        params.setDataItemsProcessor(processor);

        return params;
    }

    private TaskInfo actualize(TaskInfo info) {
        return tasksCache.getTaskInfosByConfig(info.getConfig().getId(), (t) -> !t.getState().isFinished())
                .iterator().next();
    }

    protected Task processTaskInYang(TaskInfo task, boolean inspection) {
        // check sending finished
        assertThat(task.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(task.getId(), false);
        assertThat(yangPools).hasSize(1);
        YangPoolInfo poolInfo = yangPools.get(0);
        assertThat(poolInfo.getTaskId()).isEqualTo(task.getId());

        Pool pool = tolokaApi.getPoolInfo(poolInfo.getPoolId());
        assertThat(pool.getPublicDescription()).isNotEmpty();
        assertThat(pool.getPrivateName()).isNotEmpty();
        assertThat(pool.getPrivateComment()).isNotEmpty();
        List<TaskSuite> suites = tolokaApi.getTaskSuites(pool.getId());
        assertThat(suites).hasSize(1);
        List<Task> tasks = suites.get(0).getTasks();
        assertThat(tasks).hasSize(1);

        // update pool status and add results
        if (inspection) {
            tolokaApi.addResultsBlueLogsScheme(pool.getId(),
                    generateInspectionResults(task),
                    true, TEST_WORKER_ID2);
        } else {
            tolokaApi.addResultsBlueLogsScheme(pool.getId(),
                    generateResults(task),
                    false, TEST_WORKER_ID);
        }
        return tasks.get(0);
    }

    protected List<MskuFromPskuGenerationResponse> generateResults(TaskInfo task) {
        List<MskuFromPskuGenerationResponse> responses = new ArrayList<>();
        DataItems<MskuFromPskuGenerationDataIdentity, MskuFromPskuGenerationDataPayload,
                MskuFromPskuGenerationResponse> items =
                task.getProgress().getDataItemsByState(TaskDataItemState.SENT);
        items.getItems().forEach(item -> {
            String pskuId = item.getInputData().getDataIdentifier().getPskuId();
            boolean deleted = Long.parseLong(pskuId) == TEST_PSKU_ID_DELETED;
            MskuFromPskuGenerationResponse resp = new MskuFromPskuGenerationResponse(item.getId(),
                    pskuId,
                    TEST_WORKER_ID,
                    TEST_OFFER_MAPPING_STATUS,
                    TEST_MSKU,
                    null,
                    TEST_COMMENT,
                    ImmutableList.of(new Comment(TEST_COMMENT_TYPE,
                            ImmutableList.of(TEST_COMMENT_ITEM1, TEST_COMMENT_ITEM2))),
                    0L,
                    deleted);
            responses.add(resp);
        });
        return responses;
    }

    protected List<MskuFromPskuGenInspectionResponse> generateInspectionResults(TaskInfo task) {
        List<MskuFromPskuGenInspectionResponse> responses = new ArrayList<>();
        DataItems<MskuFromPskuGenInspectionDataIdentity, MskuFromPskuGenInspectionDataPayload,
                MskuFromPskuGenInspectionResponse> items =
                task.getProgress().getDataItemsByState(TaskDataItemState.SENT);
        items.getItems().forEach(item -> {
            List<MskuFromPskuGenerationResponse> responses2 = new ArrayList<>();
            item.getInputData().getAttributes().getTaskResult().forEach(item2 -> {
                String pskuId = item2.getOfferId();
                long pskuIdL = Long.parseLong(pskuId);
                boolean deleted = pskuIdL == TEST_PSKU_ID_DELETED
                               || pskuIdL == TEST_PSKU_ID_DELETED_BEFORE_INSPECTION;
                    MskuFromPskuGenerationResponse taskResponse = new MskuFromPskuGenerationResponse(item2.getId(),
                        item2.getOfferId(),
                        TEST_WORKER_ID2,
                        TEST_OFFER_MAPPING_STATUS2,
                        TEST_MSKU2,
                        null,
                        TEST_COMMENT,
                        ImmutableList.of(new Comment(TEST_COMMENT_TYPE,
                            ImmutableList.of(TEST_COMMENT_ITEM1, TEST_COMMENT_ITEM2))),
                        0L,
                        deleted);
                    responses2.add(taskResponse);
                });
            MskuFromPskuGenInspectionResponse resp = new MskuFromPskuGenInspectionResponse(item.getId(),
                    ImmutableList.copyOf(responses2));
            responses.add(resp);
        });
        return responses;
    }

    protected void mockNeedInspectionCall(boolean needInspection) {
        when(yangLogStorageService.yangResolveNeedInspection(any())).thenReturn(
                YangLogStorage.YangResolveNeedInspectionResponse.newBuilder()
                        .setNeedInspection(needInspection)
                        .setDebugInformation("Debug info")
                        .build());
    }

    private boolean shouldBeDeleted(long offerId, boolean inspection) {
        if (offerId == TEST_PSKU_ID_1) {
            return false;
        } else if (offerId == TEST_PSKU_ID_2) {
            return false;
        } else if (offerId == TEST_PSKU_ID_DELETED_BEFORE_INSPECTION) {
            return inspection;
        } else if (offerId == TEST_PSKU_ID_DELETED) {
            return true;
        } else {
            throw new RuntimeException("Unable to decide whether psku should be deleted");
        }
    }
}
