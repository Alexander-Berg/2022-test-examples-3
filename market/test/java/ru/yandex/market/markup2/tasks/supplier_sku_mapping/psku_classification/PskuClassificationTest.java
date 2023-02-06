package ru.yandex.market.markup2.tasks.supplier_sku_mapping.psku_classification;

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
import ru.yandex.market.markup2.entries.group.Psku;
import ru.yandex.market.markup2.entries.group.PskusList;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.BaseSupplierMappingTypeContainerFactory;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.ClassificationStatisticsSaver;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.Comment;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataIdentity;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataItemPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.classification.SupplierOfferClassificationResponse;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.utils.reports_log.ReportLogWorker;
import ru.yandex.market.markup2.utils.tovarTree.CategoryBriefInfo;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.model.Pool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author shadoff
 */
public class PskuClassificationTest extends MockMarkupTestBase {
    private static final int BASE_POOL = 1;
    private static final String WORKER_ID = "worker_id";

    private static final long UID = 1L;
    private static final String PSKU_ID = "123";
    private static final int FIXED_CATEGORY_ID = 10;
    private static final String TEST_COMMENT_TYPE = "WAIT CONTENT";
    private static final String TEST_COMMENT_ITEM1 = "comment item 1";
    private static final String TEST_COMMENT_ITEM2 = "item 2";

    private TovarTreeProvider tovarTreeProvider;
    private PskuClassificationResultMaker resultMaker;
    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private TasksCache tasksCache;
    private TolokaApiStub tolokaApi;
    private PskuClassificationDataItemsProcessor processor;
    private UsersService usersService;
    private YangLogStorageService yangLogStorageService;
    private MarkupServiceImpl markupService;

    @Before
    public void init() throws Exception {
        yangLogStorageService = Mockito.mock(YangLogStorageService.class);
        when(yangLogStorageService.yangLogStore(any())).thenReturn(YangLogStorage.YangLogStoreResponse.newBuilder()
                .setSuccess(true)
                .build());

        usersService = Mocks.mockUsersService(Collections.singletonMap(WORKER_ID, UID));
        tovarTreeProvider = mock(TovarTreeProvider.class);
        when(tovarTreeProvider.getCategoryName(anyInt())).thenAnswer(invocation -> {
            int categoryId = invocation.getArgument(0);
            return "cat" + categoryId;
        });
        Collection<CategoryBriefInfo> allCategories = Arrays.asList(
            new CategoryBriefInfo.Builder()
                .setHid(1)
                .setName("root")
                .setPublished(true)
                .setUniqName("root 1")
                .build(),
            new CategoryBriefInfo.Builder()
                .setHid(2)
                .setName("leaf left")
                .setPublished(true)
                .setUniqName("leaf left 2")
                .setCategoryIn("in")
                .setParentHid(1)
                .build(),
            new CategoryBriefInfo.Builder()
                .setHid(3)
                .setName("leaf right")
                .setPublished(false)
                .setCategoryOut("out")
                .setUniqName("leaf right 3")
                .setParentHid(1)
                .build()
        );
        when(tovarTreeProvider.getAllCategories()).thenReturn(allCategories);
        AllBeans allBeans = createNew();
        markupManager = allBeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tasksCache = allBeans.get(TasksCache.class);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(BASE_POOL));

        UsersService usersService = Mockito.mock(UsersService.class);

        TaskDataItemsGetter taskDataItemsGetter = new TaskDataItemsGetter();
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

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        TaskTypeContainer container = BaseSupplierMappingTypeContainerFactory
            .createPskuClassificationTypeContainer(createTaskContainerParams(), tasksCache);

        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        Map<Integer, TaskTypeContainer> containersMap = new HashMap<>();
        containersMap.put(Markup.TaskType.PSKU_CLASSIFICATION_VALUE, container);
        taskTypesContainers.setTaskTypeContainers(containersMap);
        return taskTypesContainers;
    }

    private TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierOfferClassificationResponse> createTaskContainerParams() {

        PskuClassificationRequestGenerator requestGenerator =
            new PskuClassificationRequestGenerator();
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);

        resultMaker = new PskuClassificationResultMaker();
        resultMaker.setStatisticsSaver(new ClassificationStatisticsSaver(yangLogStorageService, usersService));

        processor = new PskuClassificationDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultMaker);
        processor.setTovarTreeProvider(tovarTreeProvider);

        TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierOfferClassificationResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName("psku_classification");
        params.setDataItemsProcessor(processor);
        params.setYangBasePoolId(BASE_POOL);

        params.setMaxBatchCount(100);
        params.setMinBatchCount(1);

        return params;
    }

    @Test
    public void pskuClassificationTaskTest() throws Exception {
        List<Psku> pskus = new ArrayList<>();
        Psku psku = new Psku(PSKU_ID, 17, "Title", "http://url.com", "https://ya.ru/img.png", "suuplier", "vendor");
        pskus.add(psku);
        PskusList pskusList = new PskusList(pskus);
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
                .setCount(10000)
                .setTypeId(Markup.TaskType.PSKU_CLASSIFICATION_VALUE)
                .setCategoryId(0)
                .setAutoActivate(true)
                .addParameter(ParameterType.PSKUS_LIST, pskusList)
                .build();
        markupManager.createConfig(createConfigAction);
        taskProcessManager.processNextTask();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.PSKU_CLASSIFICATION_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo classifierConfigInfo = configInfoSet.iterator().next();
        TaskInfo taskConfigInfo = classifierConfigInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskConfigInfo.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(taskConfigInfo.getTaskStatus().getSentCount()).isEqualTo(1);

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskConfigInfo.getId(), false);
        assertThat(yangPools).hasSize(1);
        YangPoolInfo poolInfo = yangPools.get(0);
        assertThat(poolInfo.getTaskId()).isEqualTo(taskConfigInfo.getId());

        Pool pool = tolokaApi.getPoolInfo(poolInfo.getPoolId());
        assertThat(pool.getPublicDescription()).isNotEmpty();
        assertThat(pool.getPrivateName()).isNotEmpty();
        assertThat(pool.getPrivateComment()).isNotEmpty();

        TaskDataItem item = taskConfigInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        SupplierOfferClassificationResponse resp = new SupplierOfferClassificationResponse(
            item.getId(),
            PSKU_ID,
            WORKER_ID,
            FIXED_CATEGORY_ID,
            Collections.singletonList(new Comment(TEST_COMMENT_TYPE, Arrays.asList(TEST_COMMENT_ITEM1, TEST_COMMENT_ITEM2)))
        );
        tolokaApi.addSingleTaskResults(pool.getId(), Collections.singletonList(resp), WORKER_ID, "output");

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(taskConfigInfo.getTaskStatus().getReceivedCount()).isEqualTo(1);
        assertThat(taskConfigInfo.getTaskStatus().getProcessedCount()).isEqualTo(1);

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

        Markup.PskuClassificationTaskResponse pskuClassificationTaskResponse =
                markupService.getPskuClassificationTaskResult(
                        Markup.GetByConfigRequest.newBuilder().setTaskConfigId(taskConfigInfo.getId()).build());
        Assert.assertEquals(Markup.ResponseStatus.Status.OK, pskuClassificationTaskResponse.getStatus().getStatus());
        Assert.assertEquals(taskConfigInfo.getId(), pskuClassificationTaskResponse.getTaskConfigId());
        Assert.assertEquals(1, pskuClassificationTaskResponse.getTaskResultCount());

        pskuClassificationTaskResponse.getTaskResultList().forEach(handlerTaskResult -> {
            Assert.assertEquals(FIXED_CATEGORY_ID, handlerTaskResult.getFixedCategoryId());
            Assert.assertEquals(PSKU_ID, handlerTaskResult.getPskuId());
            Assert.assertEquals(1, handlerTaskResult.getCommentsCount());
            Assert.assertEquals(TEST_COMMENT_TYPE, handlerTaskResult.getComments(0).getType());
            Assert.assertTrue(handlerTaskResult.getCommentsList().get(0).getItemsList().containsAll(
                    Arrays.asList(TEST_COMMENT_ITEM2, TEST_COMMENT_ITEM1)));
        });

        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
                ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService).yangLogStore(requestCaptor.capture());
        YangLogStorage.YangLogStoreRequest req = requestCaptor.getValue();
        assertThat(req.getClassificationStatisticList().size()).isEqualTo(1);
        Assertions.assertThat(req.getClassificationStatisticList())
                .extracting(
                    YangLogStorage.ClassificationStatistic::getOfferId,
                    YangLogStorage.ClassificationStatistic::getUid,
                    YangLogStorage.ClassificationStatistic::getFixedCategoryId,
                    YangLogStorage.ClassificationStatistic::getOldCategoryId)
                .containsExactlyInAnyOrder(Tuple.tuple(Long.parseLong(PSKU_ID), UID, (long) FIXED_CATEGORY_ID,
                        (long) psku.getCategoryId()));
        Assertions.assertThat(req.getClassificationStatisticList().get(0).getCommentsList().size()).isEqualTo(1);
        YangLogStorage.Comment comment = req.getClassificationStatisticList().get(0).getCommentsList().get(0);
        Assertions.assertThat(comment.getType()).isEqualTo(TEST_COMMENT_TYPE);
        Assertions.assertThat(comment.getItemsList().size()).isEqualTo(2);
        Assertions.assertThat(comment.getItemsList()).containsExactly(TEST_COMMENT_ITEM1, TEST_COMMENT_ITEM2);
    }

}
