package ru.yandex.market.markup2.tasks.logs_processing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.dao.MarkupDao;
import ru.yandex.market.markup2.dao.YangActivePoolPersister;
import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.dao.YangPoolInfoPersister;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.IdsList;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.OfferFilters;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.exception.CommonException;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.processors.task.TaskStatus;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.utils.aliasmaker.AliasMakerService;
import ru.yandex.market.markup2.utils.bad_offers.BadOffersServiceClient;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.traits.TraitsAndSkillsService;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.AbstractPipeStepProcessor;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.resultMaker.ResultsMaker;
import ru.yandex.market.markup2.workflow.taskFinalizer.TaskFinalizer;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mbo.offers.BadOffersApi;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.YangResultsDownloader;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.ResultItemStatus;
import ru.yandex.market.toloka.model.TaskSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author york
 * @since 29.08.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogsProcessingTaskTest extends MockMarkupTestBase {
    protected static final Logger log = LogManager.getLogger();
    private static final int YANG_PROJECT_ID = 9999;
    private static final int YANG_BASE_POOL_ID = 10000;

    protected static final int CATEGORY_ID = 100000;

    private static final long MODEL_ID = 123213;
    private static final long SKU_ID = 123213213;
    private static final long SKU2_ID = 123213215;

    private static final String WORKER1_ID = "worker1";
    private static final String WORKER2_ID = "worker2";
    private static final String WORKER3_ID = "worker3";
    private static final long WORKER1_UID = 1001;
    private static final long WORKER2_UID = 1002;
    private static final long WORKER3_UID = 1003;

    protected static final int PRIORITY = 10;

    protected static final Map<String, Long> USERS_MAP = ImmutableMap.of(
        WORKER1_ID, WORKER1_UID,
        WORKER2_ID, WORKER2_UID,
        WORKER3_ID, WORKER3_UID
    );

    protected List<AliasMaker.Offer> offersList;
    protected List<Integer> tasksForInspection;
    protected int offerSeq;

    protected MarkupManager markupManager;
    protected MarkupDao markupDao;
    protected TaskProcessManagerStub taskProcessManager;
    protected TolokaApiStub tolokaApi;
    protected TasksCache tasksCache;

    protected AliasMakerService aliasMakerService;
    protected TovarTreeProvider tovarTreeProvider;
    protected BadOffersServiceClient badOffersServiceClient;
    protected MboMappingsService mboMappingsService;
    protected UsersService usersService;
    protected YangLogStorageService yangLogStorageService;
    protected Map<Boolean, SimpleLogsProcessingRequestGenerator> requestGeneratorMap;
    protected LogsProcessingPropertiesGetter propertiesGetter;
    protected LogsProcessingPropertiesGetter inspectionPropertiesGetter;
    protected TraitsAndSkillsService traitsAndSkillsService;
    protected YangAssignmentPersister yangAssignmentPersister;
    protected YangTaskToDataItemsPersister yangTaskToDataItemsPersister;
    protected YangPoolInfoPersister yangPoolInfoPersister;
    protected YangResultsDownloader yangResultsDownloader;
    protected boolean switched = true;

    @Before
    public void setUp() throws Exception {
        offersList = new ArrayList<>();
        tasksForInspection = new ArrayList<>();
        offerSeq = 1;
        aliasMakerService = mock(AliasMakerService.class);
        Mockito.when(aliasMakerService.getNotSkutchedOffers(anyInt(), anyInt(),
            anySet(), anySet(), anyBoolean(), any(OfferFilters.class), any())).then(invocation -> {
                Set<String> excluded = invocation.getArgument(2);
                Set<String> excludedClust = invocation.getArgument(3);
                return offersList.stream()
                    .filter(o -> !excluded.contains(o.getOfferId()))
                    .filter(o -> !excludedClust.contains(o.getClusterId()))
                    .limit(invocation.<Integer>getArgument(1))
                    .collect(Collectors.toList());
        });

        tovarTreeProvider = Mocks.mockTovarTreeProvider();
        badOffersServiceClient = mock(BadOffersServiceClient.class);
        mboMappingsService = mock(MboMappingsService.class);
        Mockito.when(mboMappingsService.addProductInfo(any())).thenReturn(
            MboMappings.ProviderProductInfoResponse.newBuilder()
                .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                .build()
        );
        usersService = Mocks.mockUsersService(USERS_MAP);
        yangLogStorageService = mock(YangLogStorageService.class);
        Mockito.when(yangLogStorageService.yangLogStore(any())).thenReturn(
            YangLogStorage.YangLogStoreResponse.newBuilder()
                .setSuccess(true)
                .build()
        );
        Mockito.when(yangLogStorageService.yangResolveNeedInspection(any())).then(invocation -> {
                YangLogStorage.YangResolveNeedInspectionRequest req = invocation.getArgument(0);
                return YangLogStorage.YangResolveNeedInspectionResponse.newBuilder()
                    .setNeedInspection(tasksForInspection.contains(Integer.parseInt(req.getTaskId())))
                    .build();
            }
        );
        requestGeneratorMap = new HashMap<>();
        propertiesGetter = new LogsProcessingPropertiesGetter();
        propertiesGetter.setConfigSwitchChecker(configInfo -> switched);
        inspectionPropertiesGetter = new LogsProcessingPropertiesGetter();
        inspectionPropertiesGetter.setInspection(true);
        inspectionPropertiesGetter.setConfigSwitchChecker(configInfo -> switched);

        createNew();
        markupManager = allBeans.get(MarkupManager.class);
        markupDao = allBeans.get(MarkupDao.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(YANG_BASE_POOL_ID));
        tasksCache = allBeans.get(TasksCache.class);
        yangAssignmentPersister = allBeans.get(YangAssignmentPersister.class);
        yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        yangPoolInfoPersister = allBeans.get(YangPoolInfoPersister.class);
        traitsAndSkillsService = new TraitsAndSkillsService();
        traitsAndSkillsService.setTolokaApi(tolokaApi);
        traitsAndSkillsService.setUsersService(usersService);
        propertiesGetter.setUsersService(usersService);
        propertiesGetter.setTolokaApi(tolokaApi);
        propertiesGetter.setTovarTreeProvider(tovarTreeProvider);
        propertiesGetter.setTraitsAndSkillsService(traitsAndSkillsService);
        propertiesGetter.setYangTaskToDataItemsPersister(yangTaskToDataItemsPersister);
        propertiesGetter.setYangAssignmentPersister(yangAssignmentPersister);

        inspectionPropertiesGetter.setUsersService(usersService);
        inspectionPropertiesGetter.setTolokaApi(tolokaApi);
        inspectionPropertiesGetter.setTovarTreeProvider(tovarTreeProvider);
        inspectionPropertiesGetter.setTraitsAndSkillsService(traitsAndSkillsService);
        inspectionPropertiesGetter.setYangTaskToDataItemsPersister(yangTaskToDataItemsPersister);
        inspectionPropertiesGetter.setYangAssignmentPersister(yangAssignmentPersister);
        inspectionPropertiesGetter.setTasksCache(tasksCache);
        yangResultsDownloader = allBeans.get(YangResultsDownloader.class);
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        TaskTypeContainer taskContainer = LogsProcessingTypeContainerFactory.createTypeContainer(
            createTaskContainerParams(false, "logs_processing",
                Markup.TaskType.LOGS_PROCESSING_INSPECTION_VALUE), tasksCache, propertiesGetter,
                inspectionPropertiesGetter);

        TaskTypeContainer inspectionTaskContainer = LogsProcessingTypeContainerFactory
            .createInspectionTypeContainer(
                createTaskContainerParams(true, "logs_processing inspection", null),
                    tasksCache, propertiesGetter,
                    inspectionPropertiesGetter);

        Map<Integer, TaskTypeContainer> mp = new HashMap<>();
        mp.put(Markup.TaskType.LOGS_PROCESSING_VALUE, taskContainer);
        mp.put(Markup.TaskType.LOGS_PROCESSING_INSPECTION_VALUE, inspectionTaskContainer);
        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        taskTypesContainers.setTaskTypeContainers(mp);

        return taskTypesContainers;
    }

    private TaskTypeContainerParams<LogsProcessingDataIdentity, LogsProcessingDataItemPayload,
        LogsProcessingResponse> createTaskContainerParams(boolean inspection, String taskName,
                                                          Integer dependTaskTypeId) {
        TaskTypeContainerParams<LogsProcessingDataIdentity, LogsProcessingDataItemPayload,
            LogsProcessingResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName(taskName);
        params.setYangBasePoolId(YANG_BASE_POOL_ID);
        params.setYangProjectId(YANG_PROJECT_ID);

        SimpleLogsProcessingRequestGenerator requestGenerator = new SimpleLogsProcessingRequestGenerator();
        requestGeneratorMap.put(inspection, requestGenerator);
        requestGenerator.setAliasMakerService(aliasMakerService);
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        requestGenerator.setInspection(inspection);

        LogsProcessingDataItemsProcessor dataItemsProcessor = new LogsProcessingDataItemsProcessor();
        dataItemsProcessor.setRequestGenerator(requestGenerator);
        dataItemsProcessor.setYangActivePoolPersister(allBeans.get(YangActivePoolPersister.class));

        LogsProcessingResultMaker resultMaker = new LogsProcessingResultMaker();
        resultMaker.setInspection(inspection);
        resultMaker.setBadOffersServiceClient(badOffersServiceClient);
        resultMaker.setMboMappingsService(mboMappingsService);
        resultMaker.setUsersService(usersService);
        resultMaker.setYangLogStorageService(yangLogStorageService);
        resultMaker.setDependentTaskTypeId(dependTaskTypeId);
        resultMaker.setTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        resultMaker.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));

        dataItemsProcessor.setResultMaker(resultMaker);

        params.setDataItemsProcessor(dataItemsProcessor);
        return params;
    }

    @Test
    public void testUsersFilter() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .addConfigParameter(ConfigParameterType.USER_IDS, new IdsList(Arrays.asList(WORKER1_UID, WORKER2_UID)))
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .setAutoActivate(true)
            .build();
        addOffers(1);
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        TaskSuite taskSuite = tolokaApi.getAllTaskSuites().iterator().next();
        assertThat(taskSuite.getReservedFor()).containsExactlyInAnyOrder(WORKER1_ID, WORKER2_ID);
    }

    @Test
    public void testUnknownUserFilter() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .addConfigParameter(ConfigParameterType.USER_IDS, new IdsList(Arrays.asList(Long.MAX_VALUE, WORKER2_UID)))
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .setAutoActivate(true)
            .build();
        addOffers(1);
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    public void testIgnoreClusters() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setSimultaneousTasksCount(2)
            .setScheduleInterval(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        addOffers(2, (o) -> o.setClusterId(10L));

        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> taskInfos = configInfo.getCurrentTasks();
        taskProcessManager.processAll();
        assertThat(taskInfos.get(0).getTaskStatus().getGeneratedCount()).isEqualTo(1);
        verify(aliasMakerService).getNotSkutchedOffers(eq(CATEGORY_ID), eq(1), eq(new HashSet<>()),
            eq(new HashSet<>()), eq(false), any(), any());
        verify(aliasMakerService).getNotSkutchedOffers(eq(CATEGORY_ID), eq(1),
            eq(Collections.singleton(offersList.get(0).getOfferId())),
            eq(Collections.singleton(10L)), eq(false), any(), any());
        assertThat(taskInfos.get(1).getTaskStatus().getGeneratedCount()).isEqualTo(0);
        addOffers(1);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(taskInfos.get(1).getTaskStatus().getGeneratedCount()).isEqualTo(1);
    }

    @Test
    public void testNoInspection() throws CommonException {
        testNoInspection(this::checkPool);
    }

    protected void testNoInspection(Consumer<Pool> poolCheck) throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(3)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        addOffers(3);
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(3);
        assertThat(taskInfo.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        YangPoolInfo yangPool = getPool(taskInfo.getId(), false);
        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> items = new ArrayList<>(
            taskInfo.getProgress()
                .<LogsProcessingDataIdentity, LogsProcessingDataItemPayload, LogsProcessingResponse>getDataItemsByState(
                    TaskDataItemState.SENT).getItems());
        Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator =
            items.iterator();
        Pool pool = tolokaApi.getPoolInfo(yangPool.getPoolId());
        poolCheck.accept(pool);

        addResultsToYang(taskInfo.getId(), yangPool.getPoolId(), Arrays.asList(
            resp(iterator, LogsProcessingResponse.FinalStatus.TRASH, null, null, null),
            resp(iterator, LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID),
            resp(iterator, LogsProcessingResponse.FinalStatus.CANNOT_BE_IMPROVED, SKU_ID, MODEL_ID, null)
        ), WORKER1_ID);

        processAllTasksWithUnlock(taskProcessManager);

        yangPool = getPool(taskInfo.getId(), true);

        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
            ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService).yangLogStore(requestCaptor.capture());

        YangLogStorage.YangLogStoreRequest req = requestCaptor.getValue();
        assertThat(req.getCategoryId()).isEqualTo((long) CATEGORY_ID);
        assertThat(req.getTaskType()).isEqualTo(YangLogStorage.YangTaskType.WHITE_LOGS);
        assertThat(req.getId()).isEqualTo(String.valueOf(taskInfo.getId()));
        assertThat(req.getContractorInfo()).isEqualTo(
            YangLogStorage.OperatorInfo.newBuilder()
                .setUid(WORKER1_UID)
                .setPoolId(String.valueOf(pool.getId()))
                .setPoolName(getExpectedPoolNameForResult(pool))
                .setTaskId(yangPool.getAssignments().get(0).getTaskId())
                .setAssignmentId(yangPool.getAssignments().get(0).getAssignmentId())
                .setTaskSuiteCreatedDate(tolokaApi.getTaskSuite(yangPool.getAssignments().get(0)
                    .getTaskSuiteId()).getCreated())
                .build()
        );
        assertThat(req.getMatchingStatisticList()).containsExactlyInAnyOrder(
            YangLogStorage.MatchingStatistic.newBuilder()
                .setUid(WORKER1_UID)
                .setOfferId(getOfferId(items.get(0)))
                .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER)
                .build(),
            YangLogStorage.MatchingStatistic.newBuilder()
                .setUid(WORKER1_UID)
                .setOfferId(getOfferId(items.get(1)))
                .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
                .setModelId(MODEL_ID)
                .build(),
            YangLogStorage.MatchingStatistic.newBuilder()
                .setUid(WORKER1_UID)
                .setOfferId(getOfferId(items.get(2)))
                .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
                .setModelId(MODEL_ID)
                .setMarketSkuId(SKU_ID)
                .build()
        );

        ArgumentCaptor<Multimap> badOffersRequestCaptor = ArgumentCaptor.forClass(Multimap.class);
        Mockito.verify(badOffersServiceClient).addBadOffers(Mockito.eq(CATEGORY_ID), Mockito.eq(WORKER1_UID),
            badOffersRequestCaptor.capture());

        Multimap<BadOffersApi.BadOfferType, AliasMaker.Offer> map = badOffersRequestCaptor.getValue();
        assertThat(map.keySet()).containsExactlyInAnyOrder(BadOffersApi.BadOfferType.TRASH,
            BadOffersApi.BadOfferType.MAPPED_TO_SKU);

        ArgumentCaptor<MboMappings.ProviderProductInfoRequest> productUpdateArgumentCaptor =
            ArgumentCaptor.forClass(MboMappings.ProviderProductInfoRequest.class);

        AliasMaker.Offer mappedOffer = items.get(2).getInputData().getOffer();
        Mockito.verify(mboMappingsService).addProductInfo(productUpdateArgumentCaptor.capture());
        assertThat(productUpdateArgumentCaptor.getValue()).isEqualTo(
            MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                    .setShopId((int) mappedOffer.getShopId())
                    .setMarketSkuId(SKU_ID)
                    .setShopSkuId(mappedOffer.getShopOfferId())
                    .setTitle(mappedOffer.getTitle())
                    .setDescription(mappedOffer.getDescription())
                    .setShopCategoryName(mappedOffer.getShopCategoryName())
                    .addBarcode(mappedOffer.getBarcode())
                    .setVendor(mappedOffer.getVendorName())
                    .setVendorCode(mappedOffer.getVendorCode())
                    .setMarketCategoryId(CATEGORY_ID)
                    .setMarketModelId(MODEL_ID)
                    .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                    .setUserLogin(usersService.getUserByWorkerId(WORKER1_ID).getStaffLogin())
                    .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.CONTENT)
                    .setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.UNKNOWN)
                )
                .build());
    }







    @Test
    public void testOldStatusIsWorking() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        List<AliasMaker.Offer> offers = addOffers(1);
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo currentTask = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        addResultsForTask(currentTask, (poolId, items) -> {
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(currentTask.getId(), poolId, Arrays.asList(
                resp(iterator, LogsProcessingResponse.FinalStatus.MATCHED, null, MODEL_ID, SKU_ID)
            ), WORKER1_ID);
        });
        processAllTasksWithUnlock(taskProcessManager);

        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
            ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService).yangLogStore(requestCaptor.capture());

        YangLogStorage.YangLogStoreRequest req = requestCaptor.getValue();
        assertThat(req.getMatchingStatisticList()).containsExactly(
            YangLogStorage.MatchingStatistic.newBuilder()
                .setUid(WORKER1_UID)
                .setOfferId(offers.get(0).getOfferId())
                .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
                .setModelId(MODEL_ID)
                .build());

        Mockito.verifyZeroInteractions(badOffersServiceClient);
    }

    @Test
    public void testAsyncGeneration() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(3)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> currentTasks = configInfo.getCurrentTasks();
        assertThat(currentTasks).hasSize(3);
        requestGeneratorMap.get(false).setAsyncGeneration(true);
        addOffers(1);
        String offerId = offersList.get(0).getOfferId();

        int allTasksReq = (int) (1.5d * 3);
        int taskId = taskProcessManager.processNextTask();
        verify(aliasMakerService).getNotSkutchedOffers(eq(CATEGORY_ID), eq(1),
            eq(new HashSet<>()), eq(new HashSet<>()), eq(false), any(), eq(Optional.of(allTasksReq)));

        TaskInfo task = currentTasks.stream().filter(t -> t.getId() == taskId).findFirst().get();
        assertThat(task.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);

        allTasksReq = (int) (1.5d * 2);
        taskProcessManager.processNextTask();
        verify(aliasMakerService).getNotSkutchedOffers(eq(CATEGORY_ID), eq(1),
            eq(Collections.singleton(offerId)), eq(new HashSet<>()), eq(false), any(),
            eq(Optional.of(allTasksReq)));
    }

    @Test
    public void testUseGoodIdsForFiltering() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(3)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> currentTasks = configInfo.getCurrentTasks();
        assertThat(currentTasks).hasSize(3);
        requestGeneratorMap.get(false).setUseGoodIdsForFiltering(true);
        addOffers(1, (o) -> o.setGoodId("good"));

        int taskId = taskProcessManager.processNextTask();
       TaskInfo task = currentTasks.stream().filter(t -> t.getId() == taskId).findFirst().get();
        assertThat(task.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);

        taskProcessManager.processNextTask();
        verify(aliasMakerService).getNotSkutchedOffers(eq(CATEGORY_ID), eq(1),
            eq(Collections.singleton("good")), eq(new HashSet<>()), eq(false), any(),
            eq(Optional.empty()));
    }

    @Test
    public void testWithInspection() throws CommonException {
        testWithInspection(this::checkPool, this::checkPoolInspection);
    }

    @SuppressWarnings("checkstyle:methodlength")
    protected void testWithInspection(Consumer<Pool> poolCheck,
                                      Consumer<Pool> inspectionPoolCheck) throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(2)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(3)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> currentTasks = configInfo.getCurrentTasks();
        assertThat(currentTasks).hasSize(3);

        addOffers(6);
        taskProcessManager.processAll();

        assertThat(getValues(currentTasks, TaskStatus::getGenerationStatus)).containsExactlyInAnyOrder(
            ProgressStatus.FINISHED, ProgressStatus.FINISHED, ProgressStatus.FINISHED
        );

        //with inspection
        addResultsForTask(currentTasks.get(0), (poolId, items) -> {
            Pool pool = tolokaApi.getPoolInfo(poolId);
            poolCheck.accept(pool);
            tasksForInspection.add(currentTasks.get(0).getId());
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(currentTasks.get(0).getId(), poolId, Arrays.asList(
                resp(iterator, LogsProcessingResponse.FinalStatus.TRASH, null, null, null),
                resp(iterator, LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID)
                ), WORKER1_ID);
        });
        //with inspection
        addResultsForTask(currentTasks.get(1), (poolId, items) -> {
            tasksForInspection.add(currentTasks.get(1).getId());
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(currentTasks.get(1).getId(), poolId, Arrays.asList(
                resp(iterator, LogsProcessingResponse.FinalStatus.CANNOT_BE_IMPROVED, null, null, null),
                resp(iterator, LogsProcessingResponse.FinalStatus.CANNOT_BE_IMPROVED, null, MODEL_ID, null)
            ), WORKER1_ID);
        });
        //without inspection
        addResultsForTask(currentTasks.get(2), (poolId, items) -> {
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(currentTasks.get(2).getId(), poolId, Arrays.asList(
                resp(iterator, LogsProcessingResponse.FinalStatus.TRASH, null, null, null),
                resp(iterator, LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID)
            ), WORKER1_ID);
        });
        processAllTasksWithUnlock(taskProcessManager);

        assertThat(getValues(currentTasks, TaskStatus::getProcessingStatus)).containsExactlyInAnyOrder(
            ProgressStatus.FINISHED, ProgressStatus.FINISHED, ProgressStatus.FINISHED
        );
        assertThat(getValues(currentTasks, TaskStatus::getFinalizationStatus)).containsExactlyInAnyOrder(
            ProgressStatus.NOT_STARTED, ProgressStatus.NOT_STARTED, ProgressStatus.FINISHED
        );
        assertThat(configInfo.getDependConfigId()).isGreaterThan(0);

        // inspection start
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(configInfo.getDependConfigId());
        List<TaskInfo> inspectionTasks = inspectionConfig.getCurrentTasks();

        assertThat(getValues(inspectionTasks, TaskStatus::getGenerationStatus)).containsExactlyInAnyOrder(
            ProgressStatus.FINISHED, ProgressStatus.FINISHED
        );
        assertThat(getValues(inspectionTasks, TaskStatus::getGeneratedCount)).containsExactlyInAnyOrder(
            2, 2
        );
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(inspectionTasks.stream().map(t -> t.getState())).containsExactlyInAnyOrder(
            TaskState.RUNNING, TaskState.RUNNING
        );
        Mockito.verify(yangLogStorageService).yangLogStore(any());
        Mockito.verify(badOffersServiceClient).addBadOffers(Mockito.eq(CATEGORY_ID), Mockito.eq(WORKER1_UID),
            any(Multimap.class));
        Mockito.verify(mboMappingsService, Mockito.never()).addProductInfo(any());

        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> firstItems = new ArrayList<>();
        TaskInfo inspectionTask = getByHead(inspectionTasks, currentTasks.get(0));
        addResultsForTask(inspectionTask, (poolId, items) -> {
            Pool pool = tolokaApi.getPoolInfo(poolId);
            inspectionPoolCheck.accept(pool);
            TaskSuite taskSuite = getTaskSuite(inspectionTask.getId(), poolId);
            assertThat(taskSuite.getUnavailableFor())
                .containsExactly(WORKER1_ID);

            firstItems.addAll(items);
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(inspectionTask.getId(), poolId, Arrays.asList(
                //TRASH->MATCHED
                resp(iterator, LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID),
                //MATCHED to SKU1 -> MATCHED to SKU2
                resp(iterator, LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU2_ID)
            ), WORKER2_ID);
        });

        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> secondItems = new ArrayList<>();
        TaskInfo inspectionTask1 = getByHead(inspectionTasks, currentTasks.get(1));
        addResultsForTask(inspectionTask1, (poolId, items) -> {
            secondItems.addAll(items);
            Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator = items.iterator();
            addResultsToYang(inspectionTask1.getId(), poolId, Arrays.asList(
                //Not mapped -> Manually mapped to SKU_ID
                resp(iterator, LogsProcessingResponse.FinalStatus.CANNOT_BE_IMPROVED, SKU_ID, MODEL_ID, null),
                //Leave as it is
                resp(iterator, LogsProcessingResponse.FinalStatus.CANNOT_BE_IMPROVED, null, MODEL_ID, null)
            ), WORKER3_ID);
        });
        processAllTasksWithUnlock(taskProcessManager);

        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
            ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService, Mockito.times(3)).yangLogStore(requestCaptor.capture());

        List<YangLogStorage.YangLogStoreRequest> logStorageResults = new ArrayList<>(requestCaptor.getAllValues());
        logStorageResults.remove(0);

        Pool pool = tolokaApi.getPoolInfo(getPool(currentTasks.get(0).getId(), true).getPoolId());
        TaskSuite taskSuite = getTaskSuite(currentTasks.get(0).getId(), pool.getId());
        YangAssignmentInfo assignmentInfo = getAssignmentInfo(pool, currentTasks.get(0).getId());
        Pool poolInspection =  tolokaApi.getPoolInfo(getPool(inspectionTask.getId(), true).getPoolId());
        TaskSuite taskSuiteInspection = getTaskSuite(inspectionTask.getId(), poolInspection.getId());
        YangAssignmentInfo assignmentInfoInspection = getAssignmentInfo(poolInspection, inspectionTask.getId());
        //===============
        Pool pool1 = tolokaApi.getPoolInfo(getPool(currentTasks.get(1).getId(), true).getPoolId());
        TaskSuite taskSuite1 = getTaskSuite(currentTasks.get(1).getId(), pool1.getId());
        YangAssignmentInfo assignmentInfo1 = getAssignmentInfo(pool1, currentTasks.get(1).getId());
        Pool poolInspection1 =  tolokaApi.getPoolInfo(getPool(inspectionTask1.getId(), true).getPoolId());
        TaskSuite taskSuiteInspection1 = getTaskSuite(inspectionTask1.getId(), poolInspection1.getId());
        YangAssignmentInfo assignmentInfoInspection1 = getAssignmentInfo(poolInspection1, inspectionTask1.getId());

        assertThat(logStorageResults).containsExactlyInAnyOrder(
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setId(String.valueOf(currentTasks.get(0).getId()))
                .setHitmanId(currentTasks.get(0).getId())
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setContractorInfo(
                    YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(WORKER1_UID)
                        .setPoolId(String.valueOf(pool.getId()))
                        .setPoolName(getExpectedPoolNameForResult(pool))
                        .setTaskId(assignmentInfo.getTaskId())
                        .setAssignmentId(assignmentInfo.getAssignmentId())
                        .setTaskSuiteCreatedDate(taskSuite.getCreated())
                        .build()
                )
                .setInspectorInfo(
                    YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(WORKER2_UID)
                        .setPoolId(String.valueOf(poolInspection.getId()))
                        .setPoolName(getExpectedPoolNameForResultInspection(poolInspection))
                        .setTaskId(assignmentInfoInspection.getTaskId())
                        .setAssignmentId(assignmentInfoInspection.getAssignmentId())
                        .setTaskSuiteCreatedDate(taskSuiteInspection.getCreated())
                        .build()
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                            .setUid(WORKER1_UID)
                            .setOfferId(getOfferId(firstItems.get(0)))
                            .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER)
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                        .setUid(WORKER2_UID)
                        .setOfferId(getOfferId(firstItems.get(0)))
                        .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
                        .setModelId(MODEL_ID)
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                            .setUid(WORKER1_UID)
                            .setOfferId(getOfferId(firstItems.get(1)))
                            .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
                            .setModelId(MODEL_ID)
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                        .setUid(WORKER2_UID)
                        .setOfferId(getOfferId(firstItems.get(1)))
                        .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED)
                        .setModelId(MODEL_ID)
                )
                .build(),
            YangLogStorage.YangLogStoreRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setId(String.valueOf(currentTasks.get(1).getId()))
                .setHitmanId(currentTasks.get(1).getId())
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setContractorInfo(
                    YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(WORKER1_UID)
                        .setPoolId(String.valueOf(pool1.getId()))
                        .setPoolName(getExpectedPoolNameForResult(pool1))
                        .setTaskId(assignmentInfo1.getTaskId())
                        .setAssignmentId(assignmentInfo1.getAssignmentId())
                        .setTaskSuiteCreatedDate(taskSuite1.getCreated())
                        .build()
                )
                .setInspectorInfo(
                    YangLogStorage.OperatorInfo.newBuilder()
                        .setUid(WORKER3_UID)
                        .setPoolId(String.valueOf(poolInspection1.getId()))
                        .setPoolName(getExpectedPoolNameForResultInspection(poolInspection1))
                        .setTaskId(assignmentInfoInspection1.getTaskId())
                        .setAssignmentId(assignmentInfoInspection1.getAssignmentId())
                        .setTaskSuiteCreatedDate(taskSuiteInspection1.getCreated())
                        .build()
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                        .setUid(WORKER1_UID)
                        .setOfferId(getOfferId(secondItems.get(0)))
                        .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
                )
                .addMatchingStatistic(
                    YangLogStorage.MatchingStatistic.newBuilder()
                        .setUid(WORKER3_UID)
                        .setOfferId(getOfferId(secondItems.get(0)))
                        .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
                        .setModelId(MODEL_ID)
                        .setMarketSkuId(SKU_ID)
                )
                .addMatchingStatistic(//no changes from inspector
                    YangLogStorage.MatchingStatistic.newBuilder()
                        .setUid(WORKER1_UID)
                        .setOfferId(getOfferId(secondItems.get(1)))
                        .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED)
                        .setModelId(MODEL_ID)
                )
                .build()
        );
        Mockito.verify(badOffersServiceClient, Mockito.times(2)).addBadOffers(Mockito.eq(CATEGORY_ID),
            Mockito.eq(WORKER1_UID),
            any(Multimap.class));
    }

    protected YangAssignmentInfo getAssignmentInfo(Pool pool, int taskId) {
        TaskSuite taskSuite = getTaskSuite(taskId, pool.getId());
        return yangAssignmentPersister.getByTaskSuiteId(taskSuite.getId()).get(0);
    }

    @Test
    public void testCorrectStatesWithInspection() throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(2)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();

        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> currentTasks = configInfo.getCurrentTasks(); //1, 2
        assertThat(currentTasks).hasSize(2);

        addOffers(2);
        taskProcessManager.processAll();

        assertThat(getValues(currentTasks, TaskStatus::getGenerationStatus)).containsExactlyInAnyOrder(
            ProgressStatus.FINISHED, ProgressStatus.FINISHED
        );
        addSimpleResults(true, currentTasks.get(0));
        addSimpleResults(false, currentTasks.get(1));
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(configInfo.getDependConfigId()).isGreaterThan(0);

        // inspection start
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(configInfo.getDependConfigId());
        List<TaskInfo> inspectionTasks = inspectionConfig.getCurrentTasks(); //3
        assertThat(inspectionTasks).hasSize(1);
        processAllTasksWithUnlock(taskProcessManager);
        checkStates(currentTasks, TaskState.COMPLETED, TaskState.RUNNING);
        assertThat(configInfo.getCurrentTasks()).hasSize(2); // new task is generated

        addSimpleInspectionResults(inspectionTasks, currentTasks.get(0));
        processAllTasksWithUnlock(taskProcessManager);
        checkStates(inspectionTasks, TaskState.COMPLETED);
        processAllTasksWithUnlock(taskProcessManager);
        checkStates(currentTasks, TaskState.COMPLETED, TaskState.COMPLETED);

        //new tasks are generated
        List<TaskInfo> nextTasks = configInfo.getCurrentTasks(); //4, 5
        assertThat(nextTasks).hasSize(2);
        assertThat(currentTasks).isNotEqualTo(
            nextTasks
        );
        //all with inspection
        addSimpleResults(true, nextTasks.toArray(new TaskInfo[0]));
        processAllTasksWithUnlock(taskProcessManager);
        List<TaskInfo> nextInspectionTasks = inspectionConfig.getCurrentTasks(); //6, 7
        assertThat(nextInspectionTasks).isNotEqualTo(
            inspectionTasks
        );
        addSimpleInspectionResults(nextInspectionTasks, nextTasks.toArray(new TaskInfo[0]));
        processAllTasksWithUnlock(taskProcessManager);
        processAllTasksWithUnlock(taskProcessManager);
        checkStates(nextInspectionTasks, TaskState.COMPLETED, TaskState.COMPLETED);
        checkStates(nextTasks, TaskState.COMPLETED, TaskState.COMPLETED);

        //check FORCE FINISHING
        nextTasks = configInfo.getCurrentTasks(); //8, 9
        assertThat(nextTasks).hasSize(2);
        processAllTasksWithUnlock(taskProcessManager);
        addSimpleResults(true, nextTasks.get(0)); //8 should have inspection
        addActiveAssignment(nextTasks.get(1)); // 9 is taken
        processAllTasksWithUnlock(taskProcessManager);
        nextInspectionTasks = inspectionConfig.getCurrentTasks(); //10
        assertThat(nextInspectionTasks).hasSize(1);

        markupManager.forceFinishTaskConfig(configInfo.getId());
        processAllTasksWithUnlock(taskProcessManager);
        addSimpleInspectionResults(nextInspectionTasks, nextTasks.get(0));
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(nextInspectionTasks.get(0).getState()).isEqualTo(TaskState.COMPLETED);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(nextTasks.get(0).getState()).isEqualTo(TaskState.FORCE_FINISHED);

        assertThat(nextTasks.get(1).getState()).isEqualTo(TaskState.FORCE_FINISHING);
        addSimpleResults(true, nextTasks.get(1));
        processAllTasksWithUnlock(taskProcessManager);
        nextInspectionTasks = inspectionConfig.getCurrentTasks();
        addSimpleInspectionResults(nextInspectionTasks, nextTasks.get(1));
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(nextInspectionTasks.get(0).getState()).isEqualTo(TaskState.COMPLETED);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(nextTasks.get(1).getState()).isEqualTo(TaskState.FORCE_FINISHED);

        assertThat(configInfo.getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
        assertThat(inspectionConfig.getState()).isEqualTo(TaskConfigState.DISABLED);
    }

    @Test
    public void testForceFinishingMainTaskNotGenerated() throws CommonException {
        testOnlyMainTask(RequestsGenerator.class, (t) -> {}, (t) -> {}, false);
    }

    @Test
    public void testForceFinishingMainTaskNotSent() throws CommonException {
        testOnlyMainTask(RequestsSender.class, (t) -> {}, (t) -> {}, false);
    }

    @Test
    public void testForceFinishingMainTaskSentNotTaken() throws CommonException {
        testOnlyMainTask(ResponsesReceiver.class, (t) -> {}, (t) -> {
            allBeans.get(YangResultsDownloader.class).stopSkippingTask(t.getId());
        }, false);
    }

    @Test
    public void testForceFinishingMainTaskSentAndTaken() throws CommonException {
        testOnlyMainTask(ResponsesReceiver.class, (t) -> {
            addActiveAssignment(t);
        },(t) -> {
            addSimpleResults(false, t);
        }, true);
    }

    @Test
    public void testForceFinishingMainReceived() throws CommonException {
        testOnlyMainTask(ResultsMaker.class, (t) -> addSimpleResults(false, t), (t) -> {}, true);
    }

    @Test
    public void testForceFinishingMainProcessed() throws CommonException {
        testOnlyMainTask(TaskFinalizer.class, (t) -> addSimpleResults(false, t), (t) -> {}, true);
    }

    @Test
    public void testForceFinishingWithInspectionMainSent() throws CommonException {
        testMainTaskWithInspection(ResponsesReceiver.class);
    }

    @Test
    public void testForceFinishingWithInspectionMainReceived() throws CommonException {
        testMainTaskWithInspection(ResultsMaker.class);
    }

    @Test
    public void testForceFinishingWithInspectionMainProcessed() throws CommonException {
        testMainTaskWithInspection(ResultsMaker.class);
    }

    @Test
    public void testForceFinishingWhenInspectionIsSentAndTaken() throws CommonException {
        testMainTaskFinishedInspectionInProgress(ResponsesReceiver.class,
            (main, inspection) -> addActiveAssignment(inspection),
            (main, inspection) -> addSimpleInspectionResults(Collections.singletonList(inspection), main));
    }

    @Test
    public void testForceFinishingWhenInspectionReceived() throws CommonException {
        testMainTaskFinishedInspectionInProgress(ResultsMaker.class,
            (main, inspection) -> addSimpleInspectionResults(Collections.singletonList(inspection), main),
            (main, inspection) -> {});
    }

    @Test
    public void testForceFinishingComplex() throws CommonException {
        //Two MainTask are over with inspection.
        //MainTask3 is over and Inspection3 is in progress
        //MainTask4 is in progress
        //Force finishing then completing Inspection3 -> MainTask4 should be completed with inspection
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(2)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();
        addOffers(4);
        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> currentTasks = configInfo.getCurrentTasks();
        taskProcessManager.processAll();
        addSimpleResults(true, currentTasks.toArray(new TaskInfo[0]));
        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(configInfo.getDependConfigId());
        List<TaskInfo> inspectionTasks = inspectionConfig.getCurrentTasks();
        assertThat(inspectionTasks).hasSize(2);
        addSimpleInspectionResults(inspectionTasks, currentTasks.toArray(new TaskInfo[0]));
        processAllTasksWithUnlock(taskProcessManager); //inspection tasks are over
        checkStates(inspectionTasks, TaskState.COMPLETED, TaskState.COMPLETED);
        processAllTasksWithUnlock(taskProcessManager); //main tasks are over
        checkStates(currentTasks, TaskState.COMPLETED, TaskState.COMPLETED);
        currentTasks = configInfo.getCurrentTasks(); //updating state
        checkStates(currentTasks, TaskState.RUNNING, TaskState.RUNNING);
        processAllTasksWithUnlock(taskProcessManager);
        TaskInfo mainTask3 = currentTasks.get(0);
        TaskInfo mainTask4 = currentTasks.get(1);
        assertThat(mainTask3.getTaskStatus().isSendingFinished()).isEqualTo(true);
        assertThat(mainTask4.getTaskStatus().isSendingFinished()).isEqualTo(true);
        addSimpleResults(true, mainTask3, mainTask4);
        removeStepLock(mainTask3.getId(), ResponsesReceiver.class, taskProcessManager);
        taskProcessManager.processAll();
        assertThat(mainTask3.getTaskStatus().isProcessingFinished()).isEqualTo(true);
        assertThat(mainTask4.getTaskStatus().isProcessingFinished()).isEqualTo(false);
        inspectionTasks = inspectionConfig.getCurrentTasks();
        assertThat(inspectionTasks).hasSize(1);
        TaskInfo inspectionTask3 = inspectionTasks.get(0);
        assertThat(inspectionTask3.getHeadTaskId()).isEqualTo(mainTask3.getId());
        assertThat(inspectionTask3.getTaskStatus().isSendingFinished()).isEqualTo(true);
        assertThat(inspectionTask3.getTaskStatus().isReceivingFinished()).isEqualTo(false);
        addSimpleInspectionResults(inspectionTasks, mainTask3);

        //So here we are, this is out target state
        markupManager.forceFinishTaskConfig(configInfo.getId());
        removeStepLock(inspectionTask3.getId(), ResponsesReceiver.class, taskProcessManager);
        taskProcessManager.processAll();
        assertThat(inspectionTask3.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(inspectionTask3.getConfig().getState()).isEqualTo(TaskConfigState.ACTIVE);
        assertThat(inspectionTask3.getTaskStatus().getProcessedCount()).isEqualTo(1);
        processAllTasksWithUnlock(taskProcessManager);
        inspectionTasks = inspectionConfig.getCurrentTasks();
        assertThat(inspectionTasks).hasSize(1);
        TaskInfo inspectionTask4 = inspectionTasks.get(0);
        assertThat(inspectionTask4.getHeadTaskId()).isEqualTo(mainTask4.getId());
        addSimpleInspectionResults(inspectionTasks, mainTask4);
        processAllTasksWithUnlock(taskProcessManager);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(inspectionTask4.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(inspectionTask4.getTaskStatus().getProcessedCount()).isEqualTo(1);
        assertThat(mainTask3.getState()).isEqualTo(TaskState.FORCE_FINISHED);
        assertThat(mainTask4.getState()).isEqualTo(TaskState.FORCE_FINISHED);
        assertThat(mainTask4.getConfig().getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
        assertThat(inspectionTask4.getConfig().getState()).isEqualTo(TaskConfigState.DISABLED);
    }



    private void testOnlyMainTask(Class<? extends AbstractPipeStepProcessor> step,
                              Consumer<TaskInfo> resultsAdding,
                              Consumer<TaskInfo> afterUnlocking,
                              boolean shouldBeFinished) throws CommonException{
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();

        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo currentTask = configInfo.getSingleCurrentTask();
        addOffers(1);
        if (!step.equals(RequestsGenerator.class) && !step.equals(RequestsSender.class)) {
            taskProcessManager.processAll(); //should be locked at resp receiving
            resultsAdding.accept(currentTask);
            removeStepLock(currentTask.getId(), ResponsesReceiver.class, taskProcessManager);
        }
        lockStep(currentTask.getId(), step);
        taskProcessManager.processAll();
        checkPreviousStepsAreOver(currentTask, step);
        markupManager.forceFinishTaskConfig(configInfo.getId());
        afterUnlocking.accept(currentTask);
        processAllTasksWithUnlock(taskProcessManager);

        assertThat(currentTask.getState()).isEqualTo(TaskState.FORCE_FINISHED);
        assertThat(currentTask.getConfig().getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
        if (!shouldBeFinished) {
            assertThat(currentTask.getTaskStatus().getProcessedCount()).isEqualTo(0);
        } else {
            assertThat(currentTask.getTaskStatus().getProcessedCount()).isEqualTo(1);
        }
    }

    private void testMainTaskWithInspection(Class<? extends AbstractPipeStepProcessor> step) throws CommonException{
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();

        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo currentTask = configInfo.getSingleCurrentTask();
        addOffers(1);
        taskProcessManager.processAll();
        addSimpleResults(true, currentTask); //step is ResponsesReceiver or ResultsMaker
        removeStepLock(currentTask.getId(), ResponsesReceiver.class, taskProcessManager);
        lockStep(currentTask.getId(), step);
        taskProcessManager.processAll();
        markupManager.forceFinishTaskConfig(configInfo.getId());
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(currentTask.getState()).isEqualTo(TaskState.FORCE_FINISHING);
        assertThat(currentTask.getConfig().getState()).isEqualTo(TaskConfigState.FORCE_FINISHING);

        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(configInfo.getDependConfigId());
        assertThat(inspectionConfig).isNotNull();
        TaskInfo inspectionTask = inspectionConfig.getSingleCurrentTask();
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(inspectionTask.getTaskStatus().isSendingFinished()).isEqualTo(true);
        addSimpleInspectionResults(Collections.singletonList(inspectionTask), currentTask);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(inspectionTask.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(inspectionTask.getTaskStatus().getProcessedCount()).isEqualTo(1);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(currentTask.getState()).isEqualTo(TaskState.FORCE_FINISHED);
        assertThat(currentTask.getConfig().getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
    }


    private void testMainTaskFinishedInspectionInProgress(Class<? extends AbstractPipeStepProcessor> step,
                                                          BiConsumer<TaskInfo, TaskInfo> resultsAdding,
                                                          BiConsumer<TaskInfo, TaskInfo> afterUnlocking)
            throws CommonException{
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(1)
            .setTypeId(Markup.TaskType.LOGS_PROCESSING_VALUE)
            .setCategoryId(CATEGORY_ID)
            .setAutoActivate(true)
            .setScheduleInterval(1)
            .setSimultaneousTasksCount(1)
            .addConfigParameter(ConfigParameterType.PRIORITY, PRIORITY)
            .build();

        TaskConfigInfo configInfo = markupManager.createConfig(createConfigAction);
        TaskInfo currentTask = configInfo.getSingleCurrentTask();
        addOffers(1);
        taskProcessManager.processAll();
        addSimpleResults(true, currentTask); //step is ResponsesReceiver or ResultsMaker
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(currentTask.getState()).isEqualTo(TaskState.RUNNING);
        assertThat(currentTask.getConfig().getState()).isEqualTo(TaskConfigState.ACTIVE);
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(configInfo.getDependConfigId());
        assertThat(inspectionConfig).isNotNull();
        TaskInfo inspectionTask = inspectionConfig.getSingleCurrentTask();

        if (!step.equals(RequestsGenerator.class) && !step.equals(RequestsSender.class)) {
            taskProcessManager.processAll(); //should be locked at resp receiving
            resultsAdding.accept(currentTask, inspectionTask);
            removeStepLock(inspectionTask.getId(), ResponsesReceiver.class, taskProcessManager);
        }
        lockStep(inspectionTask.getId(), step);
        taskProcessManager.processAll();
        checkPreviousStepsAreOver(inspectionTask, step);

        markupManager.forceFinishTaskConfig(configInfo.getId());

        afterUnlocking.accept(currentTask, inspectionTask);
        processAllTasksWithUnlock(taskProcessManager);
        processAllTasksWithUnlock(taskProcessManager);
        assertThat(inspectionTask.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(inspectionTask.getTaskStatus().getProcessedCount()).isEqualTo(1);
        assertThat(inspectionTask.getConfig().getState()).isEqualTo(TaskConfigState.DISABLED);
        assertThat(currentTask.getConfig().getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
        assertThat(currentTask.getState()).isEqualTo(TaskState.FORCE_FINISHED);
    }

    private void addActiveAssignment(TaskInfo taskInfo) {
        YangPoolInfo pool = getPool(taskInfo.getId(), false);
        TaskSuite taskSuite = getTaskSuite(taskInfo.getId(), pool.getPoolId());
        tolokaApi.addAssignment(pool.getPoolId(), taskSuite.getId(), ResultItemStatus.ACTIVE);
    }

    private void checkPreviousStepsAreOver(TaskInfo currentTask, Class<? extends AbstractPipeStepProcessor> step) {
        Map<Class<? extends AbstractPipeStepProcessor>, Boolean> stepResults = new LinkedHashMap<>();
        stepResults.put(RequestsGenerator.class, currentTask.getTaskStatus().isGenerationFinished());
        stepResults.put(RequestsSender.class, currentTask.getTaskStatus().isSendingFinished());
        stepResults.put(ResponsesReceiver.class, currentTask.getTaskStatus().isReceivingFinished());
        stepResults.put(ResultsMaker.class, currentTask.getTaskStatus().isProcessingFinished());

        boolean targetState = true;
        for (Map.Entry<Class<? extends AbstractPipeStepProcessor>, Boolean> entry : stepResults.entrySet()) {
            if (entry.getKey().equals(step)) {
                targetState = false;
            }
            assertThat(entry.getValue()).as("step for %s should have finished = %s",
                entry.getKey().getSimpleName(), targetState)
                .isEqualTo(targetState);
        }
    }

    private void checkStates(Collection<TaskInfo> taskInfos, TaskState... states) {
        assertThat(taskInfos.stream().map(t -> t.getState())).containsExactlyInAnyOrder(states);
    }

    private void addSimpleResults(boolean addToInspection, TaskInfo... taskInfos) {
        Arrays.stream(taskInfos).forEach(taskInfo -> {
            if (addToInspection) {
                tasksForInspection.add(taskInfo.getId());
            }
            addResultsForTask(taskInfo, (poolId, items) -> {
                TaskSuite taskSuite = getTaskSuite(taskInfo.getId(), poolId);
                tolokaApi.removeAssignments(poolId, taskSuite.getId());
                addResultsToYang(taskInfo.getId(), poolId, Arrays.asList(
                    resp(items.iterator(), LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID)
                ), WORKER1_ID);
            });
        });
    }

    private void addSimpleInspectionResults(List<TaskInfo> allInspectionTasks, TaskInfo... taskInfos) {
        Arrays.stream(taskInfos).forEach(taskInfo -> {
            TaskInfo inspectionTask = getByHead(allInspectionTasks, taskInfo);
            addResultsForTask(inspectionTask, (poolId, items) -> {
                TaskSuite taskSuite = getTaskSuite(inspectionTask.getId(), poolId);
                tolokaApi.removeAssignments(poolId, taskSuite.getId());
                addResultsToYang(inspectionTask.getId(), poolId, Arrays.asList(
                    resp(items.iterator(), LogsProcessingResponse.FinalStatus.SKUTCHED, null, MODEL_ID, SKU_ID)
                ), WORKER3_ID);
            });
        });
    }

    private TaskInfo getByHead(List<TaskInfo> inspectionTasks, TaskInfo taskInfo) {
        return inspectionTasks.stream().filter(t -> t.getHeadTaskId() == taskInfo.getId())
            .findFirst().get();
    }

    private void addResultsForTask(TaskInfo taskInfo, BiConsumer<Integer,
            List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>>> consumer) {
        List<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> items = new ArrayList<>(
            taskInfo.getProgress()
                .<LogsProcessingDataIdentity, LogsProcessingDataItemPayload, LogsProcessingResponse>getDataItemsByState(
                    TaskDataItemState.SENT).getItems());
        YangPoolInfo yangPool = getPool(taskInfo.getId(), false);
        consumer.accept(yangPool.getPoolId(), items);
    }



    private <T> List<T> getValues(List<TaskInfo> tasks, Function<TaskStatus, T> f) {
        return tasks.stream().map(t -> f.apply(t.getTaskStatus()))
            .collect(Collectors.toList());
    }

    private LogsProcessingResponse resp(
        Iterator<TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse>> iterator,
        LogsProcessingResponse.FinalStatus status,
        Long mapSkuId,
        Long modelId,
        Long marketSkuId) {
        TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse> item = iterator.next();
        return new LogsProcessingResponse(
            item.getId(),
            getOfferId(item),
            "",
            null,
            mapSkuId,
            modelId,
            marketSkuId,
            status,
            false,
            null,
            null
        );
    }

    private String getOfferId(TaskDataItem<LogsProcessingDataItemPayload, LogsProcessingResponse> item) {
        return item.getInputData().getOffer().getOfferId();
    }

    private List<AliasMaker.Offer> addOffers(int count) {
        return addOffers(count, (x) -> {});
    }

    private List<AliasMaker.Offer> addOffers(int count, Consumer<AliasMaker.Offer.Builder> enrich) {
        List<AliasMaker.Offer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AliasMaker.Offer.Builder builder = AliasMaker.Offer.newBuilder()
                .setOfferId(Integer.toHexString(offerSeq++));
            enrich.accept(builder);
            offers.add(builder.build());
        }
        offersList.addAll(offers);
        return offers;
    }

    private String categorySkillFilter(String prefix) {
        return prefix + "_" + CATEGORY_ID + TolokaApiStub.SKILL_EQ;
    }

    protected void checkPool(Pool pool) {
        String poolName = getExpectedPoolName(pool);
        Assert.assertEquals(poolName, pool.getPrivateName());

        assertThat(tolokaApi.filtersToString(pool.getFilter()))
                .isEqualToIgnoringCase(operatorSkill() + " || " + superoperatorSkill());
    }

    protected void checkPoolInspection(Pool pool) {
        checkPool(pool);
    }

    protected String getExpectedPoolName(Pool pool) {
        if (YangPoolsCache.getInstance().containsPool(pool.getId())) {
            return getExpectedPoolName(pool);
        }
        return "!DEV! Единый пул WHITE_LOGS";
    }

    protected String getExpectedPoolNameForResult(Pool pool) {
        if (YangPoolsCache.getInstance().containsPool(pool.getId())) {
            return getExpectedPoolNameForResult(pool);
        }
        return String.format("Обработка логов: Unique cat%d [%d]", CATEGORY_ID, CATEGORY_ID);
    }

    protected String getExpectedPoolNameForResultInspection(Pool pool) {
        if (YangPoolsCache.getInstance().containsPool(pool.getId())) {
            return getExpectedPoolNameForResult(pool);
        }
        return String.format("Постприемка Обработка логов: Unique cat%d [%d]", CATEGORY_ID, CATEGORY_ID);
    }

    protected void addResultsToYang(int taskId,
                                    int poolId,
                                    List<LogsProcessingResponse> responses,
                                    String workerId) {
        yangResultsDownloader.stopSkippingTask(taskId);
        List<YangTaskToDataItems> items = yangTaskToDataItemsPersister.getByMarkupTaskId(taskId);
        if (items.size() == 0) {
            addResultsToYang(taskId, poolId, responses, workerId);
            return;
        }
        String yangTaskId = items.get(0).getTaskId();
        log.debug("adding results for task {}", yangTaskId);
        tolokaApi.addYangTaskResults(yangTaskId,
                workerId, tolokaApi.convertBlueLogs(responses));
    }

    protected YangPoolInfo getPool(int taskId, boolean received) {
        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(taskId, received);
        if (yangPools.size() > 0) {
            return getPool(taskId, received);
        }
        YangTaskToDataItems t = yangTaskToDataItemsPersister.getByMarkupTaskId(taskId).get(0);
        YangPoolInfo poolInfo = yangPoolInfoPersister.getValue(t.getPoolId());
        poolInfo.addAssignments(yangAssignmentPersister.getByTaskId(t.getTaskId()));
        return poolInfo;
    }

    protected TaskSuite getTaskSuite(int taskId, Integer poolId) {
        if (YangPoolsCache.getInstance().containsPool(poolId)) {
            return getTaskSuite(taskId, poolId);
        }
        List<YangTaskToDataItems> items = yangTaskToDataItemsPersister.getByMarkupTaskId(taskId);
        String taskSuiteId = items.get(0).getTaskSuiteId();
        boolean inspection = tasksCache.getTask(taskId).hasHeadTask();
        BigDecimal override = items.get(0).getIssuingOrderOverride();
        BigDecimal expectedOverrideBD = inspection ?
                BaseLogsProcessingPropertiesGetter.INSPECTION_ORDER_DELTA.add(new BigDecimal(PRIORITY)) :
                new BigDecimal(PRIORITY);
        assertThat(override).isEqualTo(expectedOverrideBD);
        TaskSuite taskSuite = tolokaApi.getTaskSuite(taskSuiteId);
        assertThat(taskSuite.getIssuingOrderOverride()).isEqualTo(expectedOverrideBD.doubleValue());
        return taskSuite;
    }

    private String operatorSkill() {
        return TraitsAndSkillsService.getOperatorSkillName(YangLogStorage.YangTaskType.WHITE_LOGS)
                + TolokaApiStub.SKILL_EQ;
    }
    private String superoperatorSkill() {
        return TraitsAndSkillsService.getSuperOperatorSkillName(YangLogStorage.YangTaskType.WHITE_LOGS)
                + TolokaApiStub.SKILL_EQ;
    }
}
