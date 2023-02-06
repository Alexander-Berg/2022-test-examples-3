package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.core.stubs.persisters.KvStoragePersisterStub;
import ru.yandex.market.markup2.dao.FrozenOffersPersister;
import ru.yandex.market.markup2.dao.MarkupDao;
import ru.yandex.market.markup2.dao.TaskDataItemOperationStatusPersister;
import ru.yandex.market.markup2.dao.YangActivePoolPersister;
import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.dao.YangAssignmentResultsPersister;
import ru.yandex.market.markup2.dao.YangPoolInfoPersister;
import ru.yandex.market.markup2.dao.YangResultsPoolStatusPersister;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.exception.CommonException;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.DataItems;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.tasks.logs_processing.OfferDeserializer;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionDataIdentity;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionDataItemsProcessor;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionDataPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionRequestGenerator;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionResponse;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionResultMaker;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionTaskPropertiesGetter;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionTypeContainerFactory;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationDataItemsProcessor;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationRequestGenerator;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationResponse;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationResultMaker;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationStatisticSaver;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation.SupplierMappingModerationTaskPropertiesGetter;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.priority.YangPrioritiesProcessor;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.utils.mboc.CachingOffersServiceMock;
import ru.yandex.market.markup2.utils.mboc.CachingSupplierShortInfoService;
import ru.yandex.market.markup2.utils.mboc.MboCategoryServiceMock;
import ru.yandex.market.markup2.utils.mboc.OfferPriorityInfo;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.offer.OfferFreezingService;
import ru.yandex.market.markup2.utils.parameter.CategoryParametersService;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.traits.TraitsAndSkillsService;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.markup2.workflow.taskType.processor.SinglePoolSwitchChecker;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.YangResultsDownloaderStub;
import ru.yandex.market.toloka.model.Filter;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.Task;
import ru.yandex.market.toloka.model.TaskSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author galaev
 * @since 2019-06-18
 */
@SuppressWarnings("checkstyle:MagicNumber")
@Ignore("m3")
public abstract class SkuMappingTestBase extends MockMarkupTestBase {
    protected static final Logger log = LogManager.getLogger();

    protected static final int TASK_TIMEOUT_MS = 30000;
    protected static final long MODIFICATION_DATE = System.currentTimeMillis();

    protected static final int SWITCHED_CATEGORY_ID = 2;
    protected static final int SWITCHED_CATEGORY_ID_1 = 3;
    protected static final long SUPPLIER_ID = 42;
    protected static final long SUPPLIER_ID_1 = 24;
    protected static final long SUPPLIER_ID_2 = 11;

    protected static final int YANG_PROJECT_ID = 1;
    protected static final int YANG_BASE_POOL_ID = 1;
    protected static final int BATCH_SIZE = 10;
    protected static final int CATEGORY_ID = 1;
    protected static final long TEST_UID = 100;
    protected static final String TEST_WORKER_ID = "test-worker";
    protected static final long TEST_INSPECTOR_UID = 200;
    protected static final String TEST_INSPECTOR_WORKER_ID = "test-inspector";
    protected static final long TEST_INSPECTOR2_UID = 300;
    protected static final String TEST_INSPECTOR2_WORKER_ID = "test-inspector2";
    protected static final long TEST_MSKU = 1000L;
    private static int processingTicketIdSeq = 1;

    protected static final Map<String, Long> UID_MAP = ImmutableMap.of(
            TEST_WORKER_ID, TEST_UID,
            TEST_INSPECTOR_WORKER_ID, TEST_INSPECTOR_UID,
            TEST_INSPECTOR2_WORKER_ID, TEST_INSPECTOR2_UID
    );

    protected final List<String> filterSkills =
            Arrays.asList("operator_", "super_operator_", "manager_", "skill_");

    protected Map<String, Integer> processingTicketIdByTicket = new HashMap<>();

    protected AliasMakerServiceStub aliasMakerService;
    protected YangLogStorageService yangLogStorageService;
    protected UsersService usersService;
    protected TovarTreeProvider tovarTreeProvider;
    protected TolokaApiStub tolokaApi;

    protected MarkupManager markupManager;
    protected TaskProcessManagerStub taskProcessManager;
    protected SupplierSkuMappingTaskPropertiesGetter mappingTaskPropertiesGetter;
    protected MappingInspectionTaskPropertiesGetter inspectionTaskPropertiesGetter;
    protected SupplierMappingModerationTaskPropertiesGetter moderationTaskPropertiesGetter;
    protected MarkupDao markupDao;
    protected TasksCache tasksCache;
    protected TraitsAndSkillsService traitsAndSkillsService;
    protected YangTaskToDataItemsPersister yangTaskToDataItemsPersister;
    protected YangAssignmentPersister yangAssignmentPersister;
    protected YangResultsDownloaderStub yangResultsDownloader;

    protected MboCategoryServiceMock mboCategoryService;
    protected CachingOffersServiceMock cachingOffersServiceMock;
    private SupplierSkuMappingChangeListener mappingTaskChangeListener;
    protected SSMGenerationRequestGenerator ssmGenerationRequestGenerator;
    protected YangPrioritiesProcessor yangPrioritiesProcessor;
    protected OfferFreezingService offerFreezingService;

    protected TaskTypesContainers taskTypesContainers;

    protected KvStoragePersisterStub kvStoragePersisterStub;
    protected ModelStorageService modelStorageService;
    protected CategoryParametersService categoryParametersService;

    protected static long offerIdSeq;


    @Before
    public void setUp() throws Exception {
        mboCategoryService = new MboCategoryServiceMock();
        offerFreezingService = new OfferFreezingService();
        cachingOffersServiceMock = new CachingOffersServiceMock(mboCategoryService);
        kvStoragePersisterStub = new KvStoragePersisterStub();

        aliasMakerService = new AliasMakerServiceStub();
        aliasMakerService.putOffers(CATEGORY_ID, loadSampleOffers());

        yangLogStorageService = Mockito.mock(YangLogStorageService.class);
        when(yangLogStorageService.yangLogStore(any())).thenReturn(YangLogStorage.YangLogStoreResponse.newBuilder()
                .setSuccess(true)
                .build());

        usersService = Mocks.mockUsersService(UID_MAP);
        tovarTreeProvider = Mocks.mockTovarTreeProvider();
        traitsAndSkillsService = new TraitsAndSkillsService();

        mappingTaskPropertiesGetter = new SupplierSkuMappingTaskPropertiesGetter();
        mappingTaskPropertiesGetter.setConfigSwitchChecker(new SinglePoolSwitchChecker());
        inspectionTaskPropertiesGetter = new MappingInspectionTaskPropertiesGetter();
        inspectionTaskPropertiesGetter.setConfigSwitchChecker(new SinglePoolSwitchChecker());
        moderationTaskPropertiesGetter = new SupplierMappingModerationTaskPropertiesGetter();
        moderationTaskPropertiesGetter.setConfigSwitchChecker(new SinglePoolSwitchChecker());
        modelStorageService = Mockito.mock(ModelStorageService.class);
        categoryParametersService = Mockito.mock(CategoryParametersService.class);
        mappingTaskChangeListener = new SupplierSkuMappingChangeListener();
        mappingTaskChangeListener.setExecutorService(Mocks.instantExecutorService());

        initAllBeans();
        allBeans.get(AppContext.class).setUsersService(usersService);
        markupManager = allBeans.get(MarkupManager.class);
        markupDao = allBeans.get(MarkupDao.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(YANG_BASE_POOL_ID));
        tasksCache = allBeans.get(TasksCache.class);
        traitsAndSkillsService.setUsersService(usersService);
        traitsAndSkillsService.setTolokaApi(tolokaApi);
        yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        yangAssignmentPersister = allBeans.get(YangAssignmentPersister.class);

        mappingTaskPropertiesGetter.setTolokaApi(tolokaApi);
        mappingTaskPropertiesGetter.setTraitsAndSkillsService(traitsAndSkillsService);
        inspectionTaskPropertiesGetter.setTolokaApi(tolokaApi);
        inspectionTaskPropertiesGetter.setTraitsAndSkillsService(traitsAndSkillsService);
        inspectionTaskPropertiesGetter
                .setYangTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        inspectionTaskPropertiesGetter.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));
        moderationTaskPropertiesGetter.setTolokaApi(tolokaApi);
        moderationTaskPropertiesGetter.setTraitsAndSkillsService(traitsAndSkillsService);

        mappingTaskChangeListener.setMboCategoryService(mboCategoryService);
        mappingTaskChangeListener.setOperationStatusPersister(allBeans.get(TaskDataItemOperationStatusPersister.class));
        mappingTaskChangeListener.setYangTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        mappingTaskChangeListener.setYangPoolInfoPersister(
                allBeans.get(YangPoolInfoPersister.class));
        mappingTaskChangeListener.setSinglePoolSwitchChecker(new SinglePoolSwitchChecker());
        mappingTaskChangeListener.setTasksCache(allBeans.get(TasksCache.class));
        mappingTaskChangeListener.setTolokaApi(allBeans.get(TolokaApi.class));
        mappingTaskChangeListener.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));
        mappingTaskChangeListener.setYangTaskToDataItemsPersister(yangTaskToDataItemsPersister);

        FrozenOffersPersister frozenOffersPersister = allBeans.get(FrozenOffersPersister.class);
        offerFreezingService.setFrozenOffersPersister(frozenOffersPersister);
        offerFreezingService.setTasksCache(tasksCache);
        offerFreezingService.setMarkupManager(markupManager);

        yangResultsDownloader = new YangResultsDownloaderStub();
        yangResultsDownloader.setTolokaApi(tolokaApi);
        yangResultsDownloader.setYangResultsPoolStatusPersister(allBeans.get(YangResultsPoolStatusPersister.class));
        yangResultsDownloader.setYangTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        yangResultsDownloader.setYangAssignmentResultsPersister(allBeans.get(YangAssignmentResultsPersister.class));
        yangResultsDownloader.setTransactionTemplate(allBeans.get(TransactionTemplate.class));
        yangResultsDownloader.setTaskProcessManager(taskProcessManager);
        yangPrioritiesProcessor = new YangPrioritiesProcessor(
                tolokaApi,
                tasksCache,
                markupManager,
                allBeans.get(TaskDataUniqueCache.class),
                yangTaskToDataItemsPersister,
                yangResultsDownloader,
                new TraitsAndSkillsService(),
                offerFreezingService,
                kvStoragePersisterStub,
                taskProcessManager,
                ImmutableMap.of(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, -1,
                        Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, -1,
                        Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, -1)) {
            @Override
            protected ScheduledExecutorService getExecutorService() {
                return Mockito.mock(ScheduledExecutorService.class);
            }

            @Override
            protected ExecutorService getAsyncRequestsExecutorService() {
                return Mocks.instantExecutorService();
            }
        };

        ssmGenerationRequestGenerator.setYangPrioritiesProcessor(yangPrioritiesProcessor);
    }

    protected AliasMaker.Offer offer(long supplierId, String trackerTicket) {
        return AliasMaker.Offer.newBuilder()
                .setOfferId(String.valueOf(offerIdSeq++))
                .setShopId(supplierId)
                .setTrackerTicket(trackerTicket)
                .build();
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        ssmGenerationRequestGenerator = new SSMGenerationRequestGenerator();
        ssmGenerationRequestGenerator.setMboCategoryService(mboCategoryService);
        ssmGenerationRequestGenerator.setOfferFreezingService(offerFreezingService);
        ssmGenerationRequestGenerator.setKvStoragePersister(kvStoragePersisterStub);
        ssmGenerationRequestGenerator.setCachingSupplierShortInfoService(new CachingSupplierShortInfoService(mboCategoryService));

        TaskTypeContainer generationContainer = SSMGenerationTypeContainerFactory
                .createTypeContainer("sku tasks generation", ssmGenerationRequestGenerator);

        TaskTypeContainerParams mappingContainerParams = createMappingTaskContainerParams(false, "sku mapping");
        TaskTypeContainer mappingContainer = BaseSupplierMappingTypeContainerFactory
                .createSkuMappingNewPipelineTypeContainer(mappingContainerParams, tasksCache);

        TaskTypeContainerParams contentLabContainerParams = createMappingTaskContainerParams(true, "lab sku mapping");
        TaskTypeContainer contentLabContainer =
                BaseSupplierMappingTypeContainerFactory.createMappingToDummyModelTypeContainer(
                        contentLabContainerParams, tasksCache);

        TaskTypeContainerParams inspectionContainerParams = createInspectionTaskContainerParams();
        TaskTypeContainer inspectionTask = MappingInspectionTypeContainerFactory.createTypeContainer(
                inspectionContainerParams, tasksCache);

        TaskTypeContainerParams moderationContainerParams = createModerationTaskContainerParams(false);
        TaskTypeContainerParams moderationToPskuContainerParams = createModerationTaskContainerParams(true);
        TaskTypeContainer moderationContainer = BaseSupplierMappingTypeContainerFactory
                .createMappingModerationTypeContainer(moderationContainerParams, tasksCache);
        TaskTypeContainer moderationToPskuContainer = BaseSupplierMappingTypeContainerFactory
                .createMappingModerationTypeContainer(moderationToPskuContainerParams, tasksCache);

        taskTypesContainers = new TaskTypesContainers();
        Map<Integer, TaskTypeContainer> containersMap = new HashMap<>();
        containersMap.put(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, mappingContainer);
        containersMap.put(Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, moderationContainer);
        containersMap.put(Markup.TaskType.SUPPLIER_MAPPING_MODERATION_TO_PSKU_VALUE, moderationToPskuContainer);
        containersMap.put(Markup.TaskType.SUPPLIER_SKU_MAPPING_TO_DUMMY_MODEL_VALUE, contentLabContainer);
        containersMap.put(Markup.TaskType.SKU_MAPPING_INSPECTION_VALUE, inspectionTask);

        containersMap.put(Markup.TaskType.SUPPLIER_SKU_MAPPING_GENERATION_VALUE, generationContainer);

        taskTypesContainers.setTaskTypeContainers(containersMap);

        return taskTypesContainers;
    }

    private TaskTypeContainerParams<MappingInspectionDataIdentity, MappingInspectionDataPayload,
            MappingInspectionResponse> createInspectionTaskContainerParams() {
        MappingResultSaver resultSaver = new MappingResultSaver(mboCategoryService);
        MappingStatisticSaver statisticsSaver = new MappingStatisticSaver(yangLogStorageService, usersService);

        MappingInspectionResultMaker resultMaker = new MappingInspectionResultMaker(resultSaver, statisticsSaver,
        mappingTaskPropertiesGetter, inspectionTaskPropertiesGetter);
        resultMaker.setTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        resultMaker.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));

        MappingInspectionDataItemsProcessor inspectionDataItemsProcessor = new MappingInspectionDataItemsProcessor();
        inspectionDataItemsProcessor.setPropertiesGetter(inspectionTaskPropertiesGetter);

        MappingInspectionRequestGenerator inspectionRequestGenerator = new MappingInspectionRequestGenerator();
        inspectionDataItemsProcessor.setRequestGenerator(inspectionRequestGenerator);
        inspectionDataItemsProcessor.setTovarTreeProvider(tovarTreeProvider);
        inspectionDataItemsProcessor.setPropertiesGetter(inspectionTaskPropertiesGetter);
        inspectionDataItemsProcessor.setResultMaker(resultMaker);
        inspectionDataItemsProcessor.setYangActivePoolPersister(allBeans.get(YangActivePoolPersister.class));
        inspectionDataItemsProcessor.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));
        inspectionDataItemsProcessor.setYangTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));

        TaskTypeContainerParams params = new TaskTypeContainerParams();
        params.setTaskName("sku mapping inspection");
        params.setDataItemsProcessor(inspectionDataItemsProcessor);
        params.setMinBatchCount(BATCH_SIZE);
        params.setMaxBatchCount(BATCH_SIZE);
        params.setYangBasePoolId(YANG_BASE_POOL_ID);
        params.setYangProjectId(YANG_PROJECT_ID);
        params.setTaskName("inspection");
        params.setConfigSwitchChecker(new SinglePoolSwitchChecker());
        params.setSwitchedCategories(SWITCHED_CATEGORY_ID + "," + SWITCHED_CATEGORY_ID_1);

        return params;
    }

    private TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierSkuMappingResponse> createMappingTaskContainerParams(
            boolean createOnly, String taskName) {

        SupplierSkuMappingRequestGenerator requestGenerator = new SupplierSkuMappingRequestGenerator();
        requestGenerator.setAliasMakerService(aliasMakerService);
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        requestGenerator.setCachingOffersService(cachingOffersServiceMock);
        requestGenerator.setOfferFreezingService(offerFreezingService);

        MappingResultSaver resultSaver = new MappingResultSaver(mboCategoryService);
        MappingStatisticSaver statisticsSaver = new MappingStatisticSaver(yangLogStorageService, usersService);

        SupplierSkuMappingResultCollector resultCollector = new SupplierSkuMappingResultCollector(
                resultSaver, statisticsSaver, yangLogStorageService, usersService,
                mappingTaskPropertiesGetter,
                false, SupplierSkuMappingResultCollector.InspectionType.PARTIAL);
        resultCollector.setTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        resultCollector.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));

        SupplierSkuMappingDataItemsProcessor processor = new SupplierSkuMappingDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultCollector);
        processor.setPropertiesGetter(mappingTaskPropertiesGetter);
        processor.setTovarTreeProvider(tovarTreeProvider);
        processor.setYangActivePoolPersister(allBeans.get(YangActivePoolPersister.class));

        ssmGenerationRequestGenerator.setPoolsForTestSuppliers(ImmutableMap.of(10316738L, 113621));
        ssmGenerationRequestGenerator.setCurrentEnvironmentIsTesting(true);

        TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
                SupplierSkuMappingResponse> params = new TaskTypeContainerParams<>();

        params.setMinBatchCount(BATCH_SIZE);
        params.setMaxBatchCount(BATCH_SIZE);
        params.setYangBasePoolId(YANG_BASE_POOL_ID);
        params.setYangProjectId(YANG_PROJECT_ID);
        params.setTaskName(taskName);
        params.setDataItemsProcessor(processor);
        params.setConfigSwitchChecker(new SinglePoolSwitchChecker());
        params.setSwitchedCategories(SWITCHED_CATEGORY_ID + "," + SWITCHED_CATEGORY_ID_1);
        params.setStateChangeListeners(Collections.singletonList(mappingTaskChangeListener));
        return params;
    }

    protected TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
        SupplierMappingModerationResponse> createModerationTaskContainerParams(boolean toPsku) {

        SupplierMappingModerationRequestGenerator requestGenerator = new SupplierMappingModerationRequestGenerator();
        requestGenerator.setAliasMakerService(aliasMakerService);
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        requestGenerator.setCachingOffersService(cachingOffersServiceMock);
        requestGenerator.setOfferFreezingService(offerFreezingService);

        SupplierMappingModerationResultMaker resultCollector = new SupplierMappingModerationResultMaker() {
            @Override
            protected long getModificationDate() {
                return MODIFICATION_DATE;
            }
        };
        SupplierMappingModerationStatisticSaver statisticSaver =
                new SupplierMappingModerationStatisticSaver(yangLogStorageService, usersService);
        resultCollector.setMappingModerationStatisticSaver(statisticSaver);
        resultCollector.setCategoryParametersService(categoryParametersService);
        resultCollector.setModelStorageService(modelStorageService);
        resultCollector.setTaskPropertiesGetter(moderationTaskPropertiesGetter);
        resultCollector.setMboCategoryService(mboCategoryService);
        resultCollector.setTaskToDataItemsPersister(allBeans.get(YangTaskToDataItemsPersister.class));
        resultCollector.setYangAssignmentPersister(allBeans.get(YangAssignmentPersister.class));

        SupplierMappingModerationDataItemsProcessor processor = new SupplierMappingModerationDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultCollector);
        processor.setPropertiesGetter(moderationTaskPropertiesGetter);
        processor.setTovarTreeProvider(tovarTreeProvider);
        processor.setYangActivePoolPersister(allBeans.get(YangActivePoolPersister.class));

        TaskTypeContainerParams<SupplierOfferDataIdentity, SupplierOfferDataItemPayload,
            SupplierMappingModerationResponse> params = new TaskTypeContainerParams<>();
        params.setMinBatchCount(BATCH_SIZE);
        params.setMaxBatchCount(BATCH_SIZE);
        params.setYangProjectId(YANG_PROJECT_ID);
        params.setYangBasePoolId(YANG_BASE_POOL_ID);
        params.setTaskName("moderation" + (toPsku ? "ToPsku" : ""));
        params.setDataItemsProcessor(processor);
        params.setStateChangeListeners(Collections.singletonList(mappingTaskChangeListener));
        return params;
    }

    protected TaskInfo runTask(int typeId) throws CommonException {
        return runTask(typeId, SUPPLIER_ID);
    }

    protected List<TaskInfo> runTasks(int typeId, int simultaneousTasks) throws CommonException {
        return runTasks(typeId, simultaneousTasks, SUPPLIER_ID);
    }

    protected TaskInfo runTask(int typeId, long supplierId) throws CommonException {
        return runTasks(typeId, 1, supplierId).get(0);
    }

    protected List<TaskInfo> runTasks(int typeId, int simultaneousTasks, long supplierId) throws CommonException {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
                .setCount(BATCH_SIZE)
                .setTypeId(typeId)
                .setCategoryId(CATEGORY_ID)
                .setAutoActivate(true)
                .setSimultaneousTasksCount(simultaneousTasks)
                .addParameter(ParameterType.SUPPLIER_ID, supplierId)
                .addConfigParameter(ConfigParameterType.SUPPLIER_NAME, "sup!")
                .addConfigParameter(ConfigParameterType.SUPPLIER_TYPE, "11P")
                .build();
        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigAction);
        List<TaskInfo> taskInfos = taskConfigInfo.getCurrentTasks();
        taskInfos.forEach(taskInfo -> taskProcessManager.processTask(taskInfo.getId()));
        return taskInfos;
    }

    protected void mockNeedInspectionCall(boolean needInspection) {
        when(yangLogStorageService.yangResolveNeedInspection(any())).thenReturn(
                YangLogStorage.YangResolveNeedInspectionResponse.newBuilder()
                        .setNeedInspection(needInspection)
                        .setDebugInformation("Debug info")
                        .build());
    }

    protected AliasMaker.Offer offer(long supplierId) {
        return AliasMaker.Offer.newBuilder()
                .setOfferId(String.valueOf(offerIdSeq++))
                .setShopId(supplierId)
                .build();
    }

    protected void addOffers(int categoryId, AliasMaker.Offer... offers) {
        addOffers(categoryId, null, offers);
    }

    protected void addOffers(int categoryId, MboCategory.GetOffersPrioritiesRequest matchingRequest,
                             AliasMaker.Offer... offers) {
        addOffers(categoryId, this::getTicketForSupplier, matchingRequest, offers);
    }

    protected String getTicketForSupplier(Long supplierId) {
        return "MCP-" + supplierId;
    }

    protected void addOffers(int categoryId, Function<Long, String> ticketFunction,
                             MboCategory.GetOffersPrioritiesRequest matchingRequest,
                             AliasMaker.Offer... offers) {
        Map<Long, String> ticketBySupplier = new HashMap<>();

        List<OfferPriorityInfo> priorityInfos = new ArrayList<>();
        Set<Integer> supplierIds = new HashSet<>();

        Arrays.stream(offers).forEach(o -> {
            String ticket = o.hasTrackerTicket() ? o.getTrackerTicket() :
                    ticketBySupplier.computeIfAbsent(o.getShopId(), ticketFunction);
            String ticketStr = ticket;
            if (ticket.isEmpty()) {
                ticketStr = SSMGenerationUtil.generateFakeTicketName(o.getShopId());
            }
            Integer processingTicketId = getProcessingTicketId(ticketStr);
            OfferPriorityInfo priorityInfo = new OfferPriorityInfo()
                    .setOfferId(Long.parseLong(o.getOfferId()))
                    .setSupplierId( o.getShopId())
                    .setCategoryId(categoryId)
                    .setTrackerTicket(ticket)
                    .setTicketDeadline(System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1))
                    .setCritical(false)
                    .setProcessingTicketId(processingTicketId);
            priorityInfos.add(priorityInfo);
            supplierIds.add((int) o.getShopId());
        });
        aliasMakerService.putOffers(categoryId, offers);
        mboCategoryService.putOfferPriorities(priorityInfos, matchingRequest);
        supplierIds.forEach(supplierId -> mboCategoryService.addSupplier(supplierId, "Test supplier " + supplierId,
                SupplierOffer.SupplierType.TYPE_FIRST_PARTY));
    }

    protected Integer getProcessingTicketId(String ticket) {
        return processingTicketIdByTicket.computeIfAbsent(ticket,
                (x) -> processingTicketIdSeq++);
    }

    protected void runGeneration(boolean withSupplierMapping,
                                 boolean forFullMapping,
                                 boolean forContentLab,
                                 int offersInTask) throws Exception {
        runGeneration(offersInTask, builder ->
                builder.addParameter(ParameterType.WITH_SUPPLIER_MAPPING, withSupplierMapping)
                .addParameter(ParameterType.FOR_FULL_MAPPING, forFullMapping)
                .addParameter(ParameterType.FOR_CONTENT_LAB, forContentLab));
    }
    
    protected void runGeneration(int offersInTask,
                                 Consumer<CreateConfigAction.Builder> upd) throws Exception {
        CreateConfigAction.Builder createConfigActionBuilder = new CreateConfigAction.Builder()
                .setCount(10000)
                .setTypeId(Markup.TaskType.SUPPLIER_SKU_MAPPING_GENERATION_VALUE)
                .setCategoryId(0)
                .setAutoActivate(true)
                .addConfigParameter(ConfigParameterType.OFFERS_IN_TASK, offersInTask);
        upd.accept(createConfigActionBuilder);

        TaskConfigInfo taskConfigInfo = markupManager.createConfig(createConfigActionBuilder.build());
        TaskInfo taskInfo = taskConfigInfo.getSingleCurrentTask();
        taskProcessManager.processTask(taskInfo.getId());
        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());
    }

    protected YangLogStorage.YangLogStoreRequest getLogStoreRequest(int taskId) {
        return getLogStoreRequest(taskId, 1);
    }

    protected YangLogStorage.YangLogStoreRequest getLogStoreRequest(int taskId, int timesCount) {
        ArgumentCaptor<YangLogStorage.YangLogStoreRequest> requestCaptor =
                ArgumentCaptor.forClass(YangLogStorage.YangLogStoreRequest.class);
        Mockito.verify(yangLogStorageService, times(timesCount)).yangLogStore(requestCaptor.capture());

        Map<Long, YangLogStorage.YangLogStoreRequest> requestMap = requestCaptor.getAllValues().stream()
                .collect(Collectors.toMap(YangLogStorage.YangLogStoreRequest::getHitmanId, r -> r));
        return requestMap.get((long) taskId);
    }

    protected List<String> getSkillIds(Collection<String> skillKeys) {
        return skillKeys.stream().map(tolokaApi::getOrCreateSkillByName)
                .map(Skill::getId)
                .collect(Collectors.toList());
    }

    protected List<AliasMaker.Offer> loadSampleOffers() throws IOException {
        byte[] offers = getResource("tasks/supplier_sku_mapping/supplier_offers.json");
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(AliasMaker.Offer.class, new OfferDeserializer());
        mapper.registerModule(simpleModule);
        return mapper.readValue(offers, TypeFactory.defaultInstance().constructCollectionType(List.class,
                AliasMaker.Offer.class));
    }

    protected byte[] getResource(String filePath) throws IOException {
        InputStream resourceStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(filePath));
        return IOUtils.toByteArray(resourceStream);
    }

    protected void assertSkills(int poolId, Collection<String> skillnames) {
        assertSkills(poolId, skillnames.toArray(new String[0]));
    }

    protected void assertSkills(int poolId, String... skillnames) {
        Pool pool = tolokaApi.getPoolInfo(poolId);
        List<String> filterKeys = pool.getFilter().getAnd().get(0).getOr().stream().map(Filter::getKey)
                .collect(Collectors.toList());
        assertThat(filterKeys.stream().distinct())
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(skillnames)
                        .map(sname -> tolokaApi.getOrCreateSkillByName(sname).getId())
                        .collect(Collectors.toList()));
    }

    // --- blue logs section ---
    protected void finishMappingTask(TaskInfo mappingTask) {
        processMappingTaskInYangOld(mappingTask, BATCH_SIZE);
        clearStepLocks();
        taskProcessManager.processTask(mappingTask.getId());
        assertThat(mappingTask.getProgress().getTaskStatus().getProcessingStatus()).isEqualTo(
                ProgressStatus.FINISHED
        );
    }

    @Deprecated
    protected Task processMappingTaskInYangOld(TaskInfo task, int batchSize) {
        // check sending finished
        assertThat(task.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getSentCount()).isEqualTo(batchSize);

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
        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.COMPLETED);
        tolokaApi.updatePool(pool);
        tolokaApi.addResultsBlueLogsScheme(pool.getId(), generateMappingResults(task), false, TEST_WORKER_ID);
        return tasks.get(0);
    }

    protected List<SupplierSkuMappingResponse> generateMappingResults(TaskInfo task) {
        List<SupplierSkuMappingResponse> responses = new ArrayList<>();
        DataItems<SupplierOfferDataIdentity, SupplierOfferDataItemPayload, SupplierSkuMappingResponse> items =
                task.getProgress().getDataItemsByState(TaskDataItemState.SENT);
        items.getItems().forEach(item -> {
            SupplierSkuMappingResponse resp = new SupplierSkuMappingResponse(item.getId(),
                    item.getInputData().getDataIdentifier().getOfferId(),
                    TEST_WORKER_ID,
                    "MAPPED",
                    TEST_MSKU,
                    null,
                    "qqq",
                    ImmutableList.of(new Comment("Не хватает параметров", ImmutableList.of("цвет", "ширина"))),
                    0L,
                    null);
            responses.add(resp);
        });
        return responses;
    }

    protected List<Task> processInspectionTaskInYang(TaskInfo inspectionTask,
                                                     TaskInfo mappingTask,
                                                     String workerId) {
        // check sending finished
        assertThat(inspectionTask.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(inspectionTask.getTaskStatus().getSentCount()).isEqualTo(1);

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(inspectionTask.getId(), false);
        assertThat(yangPools).hasSize(1);
        List<Task> result = new ArrayList<>();
        for (YangPoolInfo poolInfo : yangPools) {
            assertThat(poolInfo.getTaskId()).isEqualTo(inspectionTask.getId());
            Pool pool = tolokaApi.getPoolInfo(poolInfo.getPoolId());
            assertThat(pool.getPublicDescription()).isNotEmpty();
            assertThat(pool.getPrivateName()).isNotEmpty();
            assertThat(pool.getPrivateComment()).isNotEmpty();
            List<TaskSuite> suites = tolokaApi.getTaskSuites(pool.getId());
            assertThat(suites).hasSize(1);
            List<Task> tasks = suites.get(0).getTasks();
            assertThat(tasks).hasSize(1);
            result.addAll(tasks);
            assertThat(poolInfo.getDataItemIds()).hasSize(1);
            TaskDataItem<MappingInspectionDataPayload, MappingInspectionResponse> inspectionItem =
                    inspectionTask.getProgress().getDataItem(new MappingInspectionDataIdentity(mappingTask.getId()));
            // update pool status and add results
            tolokaApi.addResultsBlueLogsScheme(pool.getId(),
                    generateInspectionResults(mappingTask, inspectionItem, workerId), true, workerId);
        }
        return result;
    }

    protected void finishInspectionTask(TaskInfo inspectionTask, TaskInfo mappingTask, String workerId) {
        processInspectionTaskInYang(inspectionTask, mappingTask, workerId);
        clearStepLocks();
        taskProcessManager.addTask(inspectionTask.getId());
        taskProcessManager.processAll();
    }

    protected List<MappingInspectionResponse> generateInspectionResults(TaskInfo mappingTask,
                                                                        TaskDataItem<MappingInspectionDataPayload,
                                                                                MappingInspectionResponse> inspectionItem,
                                                                        String workerId) {
        log.debug("inspector {} for task {} ", UID_MAP.get(workerId), mappingTask.getId());
        DataItems<SupplierOfferDataIdentity, SupplierOfferDataItemPayload, SupplierSkuMappingResponse> items =
                mappingTask.getProgress().getDataItemsByState(TaskDataItemState.SUCCESSFULLY_PROCEEDED);
        List<SupplierSkuMappingResponse> mappingResponses = items.getItems().stream()
                .map(TaskDataItem::getResponseInfo)
                .collect(Collectors.toList());
        SupplierSkuMappingResponse fixedMapping = getFixedMapping(workerId, mappingResponses.get(0));
        mappingResponses.set(0, fixedMapping);
        MappingInspectionResponse resp = new MappingInspectionResponse(inspectionItem.getId(), mappingResponses);
        return Collections.singletonList(resp);
    }

    private SupplierSkuMappingResponse getFixedMapping(String workerId, SupplierSkuMappingResponse response) {
        return new SupplierSkuMappingResponse(response.getId(), response.getOfferId(),
                workerId, "MAPPED", 2000L,
                response.getNewCategoryId(), response.getComment(), response.getComments(), 0L, null);
    }
    //---
}
