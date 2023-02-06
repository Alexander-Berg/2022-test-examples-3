package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.core.stubs.persisters.KvStoragePersisterStub;
import ru.yandex.market.markup2.dao.YangActivePoolPersister;
import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.dao.YangPoolInfoPersister;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangActivePool;
import ru.yandex.market.markup2.entries.yang.YangActivePoolKey;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.classification.SupplierOfferClassificationDataItemsProcessor;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.classification.SupplierOfferClassificationRequestGenerator;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.classification.SupplierOfferClassificationResponse;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.classification.SupplierOfferClassificationResultMaker;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.priority.YangPrioritiesProcessor;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.utils.mboc.CachingOffersService;
import ru.yandex.market.markup2.utils.mboc.CachingOffersServiceMock;
import ru.yandex.market.markup2.utils.mboc.CachingSupplierShortInfoService;
import ru.yandex.market.markup2.utils.mboc.MboCategoryServiceMock;
import ru.yandex.market.markup2.utils.mboc.OfferPriorityInfo;
import ru.yandex.market.markup2.utils.mboc.TicketPriorityInfo;
import ru.yandex.market.markup2.utils.offer.OfferFreezingService;
import ru.yandex.market.markup2.utils.tovarTree.CategoryBriefInfo;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.traits.TraitsAndSkillsService;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.ReadthroughResultsDownloader;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.YangResultsDownloader;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;
import ru.yandex.market.toloka.model.ResultItemStatus;
import ru.yandex.market.toloka.model.TaskSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 18.11.2019
 */
@Ignore("m3")
public class SupplierOfferClassificationTest extends MockMarkupTestBase {
    private static final int BASE_POOL = 1;
    private static final int MAX_TICKETS = 2;
    private static final String WORKER_ID = "worker_id";
    private static final int SUPPLIER_1 = 1;
    private static final int SUPPLIER_2 = 2;
    private static final long UID = 1L;
    private static Long idSeq = 1L;
    private AliasMakerServiceStub aliasMakerService;
    private TovarTreeProvider tovarTreeProvider;
    private SSMGenerationRequestGenerator ssmGenerationRequestGenerator;
    private SupplierOfferClassificationResultMaker resultMaker;
    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private MboCategoryServiceMock mboCategoryServiceMock;
    private TasksCache tasksCache;
    private CachingOffersService cachingOffersService;
    private TolokaApiStub tolokaApi;
    private SupplierOfferClassificationDataItemsProcessor processor;
    private YangTaskToDataItemsPersister yangTaskToDataItemsPersister;
    private YangActivePoolPersister yangActivePoolPersister;
    private YangPrioritiesProcessor yangPrioritiesProcessor;
    private UsersService usersService;
    private YangLogStorageService yangLogStorageService;
    private OfferFreezingService offerFreezingService;
    protected KvStoragePersisterStub kvStoragePersisterStub;
    protected YangResultsDownloader yangResultsDownloader;

    @Before
    public void init() throws Exception {
        yangLogStorageService = Mockito.mock(YangLogStorageService.class);
        when(yangLogStorageService.yangLogStore(any())).thenReturn(YangLogStorage.YangLogStoreResponse.newBuilder()
                .setSuccess(true)
                .build());
        usersService = Mocks.mockUsersService(Collections.singletonMap(WORKER_ID, UID));
        mboCategoryServiceMock = Mockito.spy(new MboCategoryServiceMock());
        offerFreezingService = Mockito.mock(OfferFreezingService.class);
        aliasMakerService = spy(new AliasMakerServiceStub() {
            @Override
            public List<AliasMaker.CategorySuppliers> getSupplierOffersInClassificationCategories() {
                Map<Long, Map<Long, Integer>> suppliersByCategory = new HashMap<>();
                mboCategoryServiceMock.getOfferPriorities().forEach(op -> {
                    Map<Long, Integer> suppliersMap = suppliersByCategory.computeIfAbsent(op.getCategoryId(),
                        (x) -> new HashMap<>());
                    suppliersMap.merge(op.getSupplierId(), 1, Integer::sum);
                });
                List<AliasMaker.CategorySuppliers> result = new ArrayList<>();
                suppliersByCategory.forEach((cat, map) -> {
                    AliasMaker.CategorySuppliers.Builder catBuilder = AliasMaker.CategorySuppliers.newBuilder()
                        .setCategoryId(Long.valueOf(cat).intValue());
                    map.forEach((supId, cnt) -> {
                        catBuilder.addSupplierOffersCount(AliasMaker.SupplierOffersCount.newBuilder()
                            .setOffersCount(cnt)
                            .setSupplierId(supId)
                            .setSupplierName(String.valueOf(supId))
                            .setSupplierType("3P")
                        );
                    });
                    result.add(catBuilder.build());
                });
                return result;
            }
        });
        tovarTreeProvider = mock(TovarTreeProvider.class);
        cachingOffersService = new CachingOffersServiceMock(mboCategoryServiceMock);
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
        kvStoragePersisterStub = new KvStoragePersisterStub();
        AllBeans allBeans = createNew();
        resultMaker.setTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        YangAssignmentPersister yangAssignmentPersister = allBeans.get(YangAssignmentPersister.class);
        resultMaker.setYangAssignmentPersister(yangAssignmentPersister);
        resultMaker.setStatisticsSaver(new ClassificationStatisticsSaver(yangLogStorageService, usersService));
        resultMaker.setYangPoolInfoPersister(allBeans.get(YangPoolInfoPersister.class));
        markupManager = allBeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tasksCache = allBeans.get(TasksCache.class);
        allBeans.get(AppContext.class).setUsersService(usersService);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(BASE_POOL));
        yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        yangActivePoolPersister = allBeans.get(YangActivePoolPersister.class);
        processor.setActivePoolPersister(yangActivePoolPersister);
        yangResultsDownloader = allBeans.get(YangResultsDownloader.class);
        yangPrioritiesProcessor = new YangPrioritiesProcessor(
            tolokaApi,
            allBeans.get(TasksCache.class),
            markupManager,
            allBeans.get(TaskDataUniqueCache.class),
            yangTaskToDataItemsPersister,
            yangResultsDownloader,
            new TraitsAndSkillsService(),
            Mockito.mock(OfferFreezingService.class),
            new KvStoragePersisterStub(),
            taskProcessManager,
            ImmutableMap.of(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, -1,
                    Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, -1,
                    Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, MAX_TICKETS - 1) //For test purposes
        );
        ssmGenerationRequestGenerator.setYangPrioritiesProcessor(yangPrioritiesProcessor);
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        TaskTypeContainer container = BaseSupplierMappingTypeContainerFactory
            .createSupplierOfferClassificationTypeContainer(createTaskContainerParams(), tasksCache);

        ssmGenerationRequestGenerator = new SSMGenerationRequestGenerator();
        ssmGenerationRequestGenerator.setMboCategoryService(mboCategoryServiceMock);
        ssmGenerationRequestGenerator.setMaxClassificationTickets(MAX_TICKETS);
        ssmGenerationRequestGenerator.setOfferFreezingService(offerFreezingService);
        ssmGenerationRequestGenerator.setKvStoragePersister(kvStoragePersisterStub);
        ssmGenerationRequestGenerator.setCachingSupplierShortInfoService(new CachingSupplierShortInfoService(mboCategoryServiceMock));

        TaskTypeContainer generationContainer = SSMGenerationTypeContainerFactory
            .createTypeContainer("sku tasks generation", ssmGenerationRequestGenerator);

        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        Map<Integer, TaskTypeContainer> containersMap = new HashMap<>();
        containersMap.put(Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, container);
        containersMap.put(Markup.TaskType.SUPPLIER_SKU_MAPPING_GENERATION_VALUE, generationContainer);
        taskTypesContainers.setTaskTypeContainers(containersMap);
        return taskTypesContainers;
    }

    private TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierOfferClassificationResponse> createTaskContainerParams() {

        SupplierOfferClassificationRequestGenerator requestGenerator =
            new SupplierOfferClassificationRequestGenerator();
        requestGenerator.setCachingOffersService(cachingOffersService);
        requestGenerator.setAliasMakerService(aliasMakerService);
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        requestGenerator.setOfferFreezingService(offerFreezingService);

        resultMaker = new SupplierOfferClassificationResultMaker();
        resultMaker.setMboCategoryService(mboCategoryServiceMock);

        processor = new SupplierOfferClassificationDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultMaker);
        processor.setTovarTreeProvider(tovarTreeProvider);

        TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierOfferClassificationResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName("classification");
        params.setDataItemsProcessor(processor);
        params.setYangBasePoolId(BASE_POOL);

        return params;
    }

    @Test
    public void successfullTask() throws Exception {
        OfferPriorityInfo offerPriorityInfo = of("MCP-1", 1, SUPPLIER_1);
        mboCategoryServiceMock.putOfferPriorities(Collections.singletonList(
            offerPriorityInfo
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo classifierConfigInfo = configInfoSet.iterator().next();
        TaskInfo classifierTaskInfo = classifierConfigInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(classifierTaskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(classifierTaskInfo.getTaskStatus().getSentCount()).isEqualTo(1);

        List<YangTaskToDataItems> list = yangTaskToDataItemsPersister.getByMarkupTaskId(classifierTaskInfo.getId());
        assertThat(list).hasSize(1);

        TaskDataItem item = classifierTaskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        SupplierOfferClassificationResponse resp = new SupplierOfferClassificationResponse(
            item.getId(),
            "" + offerPriorityInfo.getOfferId(),
            WORKER_ID,
            10,
            Collections.singletonList(new Comment("Type", Arrays.asList("Item1", "Item2")))
        );
        tolokaApi.addYangTaskResults(list.get(0).getTaskId(), Collections.singletonList(resp),
            WORKER_ID, "output");
        yangResultsDownloader.stopSkippingTask(classifierTaskInfo.getId());

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(classifierTaskInfo.getTaskStatus().getReceivedCount()).isEqualTo(1);
        assertThat(classifierTaskInfo.getTaskStatus().getProcessedCount()).isEqualTo(1);

        SupplierOffer.ClassificationTaskResult.Builder resultOfferBuilder =
            SupplierOffer.ClassificationTaskResult.newBuilder()
            .setFixedCategoryId(10)
            .setOfferId(resp.getOfferId())
            .setStaffLogin("staff_" + UID);
        resp.getComments().stream().map(Comment::toContentComment).forEach(resultOfferBuilder::addContentComment);
        MboCategory.UpdateSupplierOfferCategoryRequest result = MboCategory.UpdateSupplierOfferCategoryRequest
            .newBuilder()
            .addResult(resultOfferBuilder)
            .build();

        verify(mboCategoryServiceMock).updateSupplierOfferCategory(eq(result));

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
                .containsExactlyInAnyOrder(Tuple.tuple(Long.parseLong(resp.getOfferId()), UID, 10L, 1L));
        Assertions.assertThat(req.getClassificationStatisticList().get(0).getCommentsList().size())
                .isEqualTo(resp.getComments().size());
        Iterator<YangLogStorage.Comment> it = req.getClassificationStatisticList().get(0).getCommentsList().iterator();
        for (Comment comment : resp.getComments()) {
            Assertions.assertThat(it.hasNext()).isEqualTo(true);
            YangLogStorage.Comment actualComment = it.next();
            Assertions.assertThat(actualComment.getType()).isEqualTo(comment.getType());
            Assertions.assertThat(actualComment.getItemsList().size()).isEqualTo(comment.getItems().size());
            Assertions.assertThat(actualComment.getItemsList()).containsExactly(comment.getItems()
                    .toArray(new String[0]));
        }
    }

    @Test
    public void testMaxTicketsGeneration() throws Exception {
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-1", 2, SUPPLIER_1),
            of("MCP-1", 3, SUPPLIER_1),
            of("MCP-2", 1, SUPPLIER_2),
            of("MCP-3", 1, SUPPLIER_2),
            of("MCP-4", 2, SUPPLIER_2)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        mboCategoryServiceMock.addSupplier(SUPPLIER_2, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);
        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);

        List<String> tickets = configInfoSet.stream()
            .map(c -> c.getGroupInfo().getParameterValue(ParameterType.TRACKER_TICKET))
            .collect(Collectors.toList());

        assertThat(tickets).containsExactlyInAnyOrder("MCP-1","MCP-1","MCP-1","MCP-2");
    }

    @Test
    public void testCancelingTask() throws Exception {
        OfferPriorityInfo offerPriorityInfo = of("MCP-1", 1, SUPPLIER_1);
        mboCategoryServiceMock.putOfferPriorities(Collections.singletonList(
                offerPriorityInfo
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
                Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo classifierConfigInfo = configInfoSet.iterator().next();
        TaskInfo classifierTaskInfo = classifierConfigInfo.getSingleCurrentTask();

        lockStep(classifierTaskInfo.getId(), RequestsGenerator.class);
        taskProcessManager.processAll();

        assertThat(classifierTaskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(0);

        mboCategoryServiceMock.removeOffers("MCP-1");
        List<TicketPriorityInfo> priorityInfos = mboCategoryServiceMock.getTicketPriorities();
        yangPrioritiesProcessor.updatePrioritiesInClassification(priorityInfos);
        Assert.assertEquals(TaskState.CANCELED, classifierTaskInfo.getState());
        Assert.assertEquals(TaskConfigState.FORCE_FINISHED, classifierConfigInfo.getState());

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(classifierTaskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(0);
    }

    @Test
    public void testPerCategoryGeneration() throws Exception {
        List<OfferPriorityInfo> offers = Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-1", 2, SUPPLIER_2),
            of("MCP-1", 3, SUPPLIER_1),
            of("MCP-1", 1, SUPPLIER_2),
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-3", 1, SUPPLIER_2)
        );
        mboCategoryServiceMock.putOfferPriorities(offers);
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        mboCategoryServiceMock.addSupplier(SUPPLIER_2, "Duper", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(2);
        taskProcessManager.processAll();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);

        Map<String, List<TaskInfo>> byTickets = configInfoSet.stream()
            .flatMap(c -> c.getCurrentTasks().stream())
            .collect(Collectors.groupingBy(t -> t.getConfig().getGroupInfo()
                .getParameterValue(ParameterType.TRACKER_TICKET)));

        assertThat(byTickets.keySet()).containsExactlyInAnyOrder("MCP-1","MCP-3");
        List<TaskInfo> ticket1 = byTickets.get("MCP-1");

        assertThat(ticket1).hasSize(4);
        Multimap<Integer, Integer> countsByCategory = HashMultimap.create();
        ticket1.forEach(t -> countsByCategory.put(t.getCategoryId(),
            t.getProgress().getTaskStatus().getGeneratedCount()));
        assertThat(countsByCategory.get(1)).containsExactlyInAnyOrder(2, 1);
        assertThat(countsByCategory.get(2)).containsExactlyInAnyOrder(1);
        assertThat(countsByCategory.get(3)).containsExactlyInAnyOrder(1);
        Multimap<Integer, String> offerIdsByCategory = HashMultimap.create();
        ticket1.forEach(t -> getOffers(t).forEach(o -> offerIdsByCategory.put(t.getCategoryId(), o.getOfferId())));
        assertThat(offerIdsByCategory.get(1)).containsExactlyInAnyOrder(
            String.valueOf(offers.get(0).getOfferId()),
            String.valueOf(offers.get(3).getOfferId()),
            String.valueOf(offers.get(4).getOfferId())
        );
        assertThat(offerIdsByCategory.get(2)).containsExactlyInAnyOrder(
            String.valueOf(offers.get(1).getOfferId())
        );
        assertThat(offerIdsByCategory.get(3)).containsExactlyInAnyOrder(
            String.valueOf(offers.get(2).getOfferId())
        );
    }

    @Test
    public void testSamePoolGeneration() throws Exception {
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-1", 1, SUPPLIER_2),
            of("MCP-1", 1, SUPPLIER_2),
            of("MCP-1", 2, SUPPLIER_2)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        mboCategoryServiceMock.addSupplier(SUPPLIER_2, "Duper", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(2);

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        assertThat(configInfoSet).hasSize(2);
        Map<Integer, TaskConfigInfo> configInfoMap = configInfoSet.stream()
            .collect(Collectors.toMap(c -> c.getGroupInfo().getCategoryId(), c -> c));

        TaskConfigInfo category1Config = configInfoMap.get(1);
        assertThat(category1Config).isNotNull();
        assertThat(category1Config.getSimultaneousTasksCount()).isEqualTo(2);
        assertThat(category1Config.getCount()).isEqualTo(2);

        TaskConfigInfo category2Config = configInfoMap.get(2);
        assertThat(category2Config).isNotNull();
        assertThat(category2Config.getSimultaneousTasksCount()).isEqualTo(1);
        assertThat(category2Config.getCount()).isEqualTo(2);

        processAllTasksWithUnlock(taskProcessManager);
        List<TaskInfo> tasks = category1Config.getCurrentTasks();
        assertThat(tasks).hasSize(2);
        TaskInfo task1 = tasks.get(0);
        TaskInfo task2 = tasks.get(1);
        assertThat(task1.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task1.getTaskStatus().getGeneratedCount()).isGreaterThan(0);
        assertThat(task2.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task2.getTaskStatus().getGeneratedCount()).isGreaterThan(0);
        int pool1Id = getPool(task1);
        int pool2Id = getPool(task2);
        assertThat(pool1Id).isNotEqualTo(BASE_POOL);
        assertThat(pool1Id).isEqualTo(pool2Id);
        int pool3Id = getPool(category2Config.getSingleCurrentTask());
        assertThat(pool1Id).isEqualTo(pool3Id);

        YangActivePool activePool = yangActivePoolPersister
            .getByKey(new YangActivePoolKey(Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, "MCP-1"));
        assertThat(activePool).isNotNull();
        assertThat(activePool.getPoolId()).isEqualTo(pool1Id);
    }

    @Test
    public void testExistentPoolAdding() throws Exception {
        int newPoolId = tolokaApi.clonePool(BASE_POOL);
        assertThat(newPoolId).isGreaterThan(BASE_POOL);
        tolokaApi.clonePool(BASE_POOL);
        yangActivePoolPersister.save(new YangActivePool(
            new YangActivePoolKey(Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, "MCP-1"), newPoolId));
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-1", 1, SUPPLIER_1)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(2);
        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo taskConfigInfo = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true).iterator().next();
        int poolId = getPool(taskConfigInfo.getSingleCurrentTask());
        assertThat(poolId).isEqualTo(newPoolId);
    }

    @Test
    public void testNoMultipleGeneration() throws Exception {
        OfferPriorityInfo offerPriorityInfo = of("MCP-1", 1, SUPPLIER_1);
        mboCategoryServiceMock.putOfferPriorities(Collections.singletonList(
                offerPriorityInfo
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);
        processAllTasksWithUnlock(taskProcessManager);
        startGeneration(1);
        processAllTasksWithUnlock(taskProcessManager);

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
                Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());
    }

    @Test
    public void testExistentClosedPools() throws Exception {
        testExistentClosedPool(PoolCloseReason.MANUAL, true);
    }

    @Test
    public void testExistentClosedArchivedPool() throws Exception {
        testExistentClosedPool(PoolCloseReason.COMPLETED, PoolStatus.ARCHIVED,true);
    }

    @Test
    public void testExistentCompletedPool() throws Exception {
        testExistentClosedPool(PoolCloseReason.COMPLETED,false);
    }

    private void testExistentClosedPool(PoolCloseReason closeReason, boolean expectedNew) throws Exception {
        testExistentClosedPool(closeReason, PoolStatus.CLOSED, expectedNew);
    }

    private void testExistentClosedPool(PoolCloseReason closeReason, PoolStatus status,
                                        boolean expectedNew) throws Exception {
        int newPoolId = tolokaApi.clonePool(BASE_POOL);
        assertThat(newPoolId).isGreaterThan(BASE_POOL);
        yangActivePoolPersister.save(new YangActivePool(
            new YangActivePoolKey(Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, "MCP-1"), newPoolId));
        Pool pool = tolokaApi.getPoolInfo(newPoolId);
        pool.setLastCloseReason(closeReason);
        pool.setStatus(status);
        tolokaApi.updatePool(pool);
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-1", 1, SUPPLIER_2)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        mboCategoryServiceMock.addSupplier(SUPPLIER_2, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(2);
        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo taskConfigInfo = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true).iterator().next();
        int poolId = getPool(taskConfigInfo.getSingleCurrentTask());
        assertThat(poolId == newPoolId).isEqualTo(!expectedNew);
    }

    @Test
    public void testReceivingCancelledPool() throws Exception {
        mboCategoryServiceMock.putOfferPriorities(Collections.singletonList(
            of("MCP-1", 1, SUPPLIER_1)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);
        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        TaskConfigInfo classifierConfigInfo = configInfoSet.iterator().next();
        TaskInfo classifierTaskInfo = classifierConfigInfo.getSingleCurrentTask();
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(classifierTaskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(classifierTaskInfo.getTaskStatus().getSentCount()).isEqualTo(1);

        String yangTaskSuite = getYangTaskSuite(classifierTaskInfo);
        tolokaApi.cancelTaskSuite(yangTaskSuite);

        yangTaskToDataItemsPersister.getByMarkupTaskId(classifierTaskInfo.getId())
            .forEach(item -> {
                item.updateCancelledNow();
                yangResultsDownloader.stopSkippingTask(item.getMarkupTaskId());
                yangTaskToDataItemsPersister.updateCancelled(item);
            });
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(classifierTaskInfo.getTaskStatus().getLostCount()).isEqualTo(0);
        ((ReadthroughResultsDownloader) yangResultsDownloader).hackAllPoolsDownloaded();
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(classifierTaskInfo.getTaskStatus().getLostCount()).isEqualTo(1);
        assertThat(classifierTaskInfo.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    public void testCancellingExcessiveTickets() throws Exception {
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-2", 2, SUPPLIER_1)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);
        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo second = configInfoSet.stream()
            .filter(tc -> tc.getGroupInfo().getParameterValue(ParameterType.TRACKER_TICKET).equals("MCP-2"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Config not found"));
        String taskSuiteId = getYangTaskSuite(second.getSingleCurrentTask());
        TaskSuite taskSuite = tolokaApi.getTaskSuite(taskSuiteId);
        assertThat(taskSuite.getOverlap()).isNotEqualTo(0);
        yangPrioritiesProcessor.updatePrioritiesInClassification(mboCategoryServiceMock.getTicketPriorities());
        taskSuite = tolokaApi.getTaskSuite(taskSuiteId);
        assertThat(taskSuite.getOverlap()).isEqualTo(0);
    }

    @Test
    public void testNotCancellingTasksInWork() throws Exception {
        mboCategoryServiceMock.putOfferPriorities(Arrays.asList(
            of("MCP-1", 1, SUPPLIER_1),
            of("MCP-2", 2, SUPPLIER_1),
            of("MCP-2", 3, SUPPLIER_1)
        ));
        mboCategoryServiceMock.addSupplier(SUPPLIER_1, "Super", SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER);
        startGeneration(1);
        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, (x) -> true);
        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo second = configInfoSet.stream()
            .filter(tc -> tc.getGroupInfo().getParameterValue(ParameterType.TRACKER_TICKET).equals("MCP-2"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Config not found"));
        String task = getYangTask(second.getSingleCurrentTask());
        tolokaApi.addAssignment(task, ResultItemStatus.ACTIVE);
        yangPrioritiesProcessor.updatePrioritiesInClassification(mboCategoryServiceMock.getTicketPriorities());
        String taskSuiteId = getYangTaskSuite(second.getSingleCurrentTask());
        TaskSuite taskSuite = tolokaApi.getTaskSuite(taskSuiteId);
        assertThat(taskSuite.getOverlap()).isGreaterThan(0);
    }

    private List<AliasMaker.Offer> getOffers(TaskInfo taskInfo) {
        return taskInfo.getProgress().<SupplierOfferDataIdentity,
            SupplierOfferDataItemPayload, SupplierOfferClassificationResponse>getTaskDataItemsByStates(TaskDataItemState.values())
            .stream()
            .map(tdi -> tdi.getInputData().getAttributes().getOffer())
            .collect(Collectors.toList());
    }

    private void startGeneration(int offersInTask) throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(10000)
            .setTypeId(Markup.TaskType.SUPPLIER_SKU_MAPPING_GENERATION_VALUE)
            .setCategoryId(0)
            .setAutoActivate(true)
            .addParameter(ParameterType.FOR_CLASSIFICATION, true)
            .addConfigParameter(ConfigParameterType.OFFERS_IN_TASK, offersInTask)
            .build();
        markupManager.createConfig(createConfigAction);
        taskProcessManager.processNextTask();
    }

    private String getYangTask(TaskInfo taskInfo) {
        return yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0).getTaskId();
    }

    private String getYangTaskSuite(TaskInfo taskInfo) {
        return yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0).getTaskSuiteId();
    }

    private int getPool(TaskInfo taskInfo) {
        return yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0).getPoolId();
    }

    private OfferPriorityInfo of(String ticket, int categoryId, long supplierId) {
        OfferPriorityInfo result = new OfferPriorityInfo();
        result.setOfferId(idSeq++);
        result.setTicketDeadline(1);
        result.setProcessingTicketId(ticket.hashCode());
        result.setTrackerTicket(ticket);
        result.setCategoryId(categoryId);
        result.setSupplierId(supplierId);
        return result;
    }
}
