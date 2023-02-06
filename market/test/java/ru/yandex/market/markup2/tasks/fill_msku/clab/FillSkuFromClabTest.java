package ru.yandex.market.markup2.tasks.fill_msku.clab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clab.api.http.ContentLabApi;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.api.CreateConfigAction;
import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.TaskTypeContainerParams;
import ru.yandex.market.markup2.tasks.fill_msku.BaseFillMskuTypeContainerFactory;
import ru.yandex.market.markup2.tasks.fill_msku.FSFCGenerationRequestGenerator;
import ru.yandex.market.markup2.tasks.fill_msku.FSFCGenerationTypeContainerFactory;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuDataAttributes;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuDataItemPayload;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuDataItemsProcessor;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuFromClabStatisticSaver;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuResponse;
import ru.yandex.market.markup2.tasks.fill_msku.FillSkuStatus;
import ru.yandex.market.markup2.tasks.fill_msku.ModelDataIdentity;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuFromClabInspectionRequestGenerator;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuFromClabInspectionResultCollector;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuFromClabInspectionTaskPropertiesGetter;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuInspectionDataIdentity;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuInspectionDataItemsProcessor;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuInspectionDataPayload;
import ru.yandex.market.markup2.tasks.fill_msku.clab_inspection.FillSkuInspectionResponse;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.Comment;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.utils.clab.ContentLabApiServiceMock;
import ru.yandex.market.markup2.utils.mboc.MboCategoryMappingsServiceMock;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.model.Pool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
public class FillSkuFromClabTest extends MockMarkupTestBase {
    private static final int BASE_POOL = 1;
    private static final String WORKER_ID = "worker_id";
    private static final String INSPECTOR_ID = "inspector_id";
    private static final long RANDOM_SEED = 238589897423894723L;
    private static final long CATEGORY_ID = 91991L;
    private TovarTreeProvider tovarTreeProvider;
    private MarkupManager markupManager;
    private TaskProcessManagerStub taskProcessManager;
    private MboCategoryMappingsServiceMock mboCategoryMappingsService;
    private ContentLabApiServiceMock contentLabApiService;
    private TasksCache tasksCache;
    private TolokaApiStub tolokaApi;
    private FillSkuDataItemsProcessor processor;
    private FillSkuFromClabTaskPropertiesGetter propertiesGetter;
    private FillSkuFromClabInspectionTaskPropertiesGetter inspectionPropertiesGetter;
    private EnhancedRandom enhancedRandom;
    private ObjectMapper objectMapper;
    private YangLogStorageService yangLogStorageService;
    private TaskDataUniqueCache taskDataUniqueCache;

    @Before
    public void init() throws Exception {
        objectMapper = new ObjectMapper();
        enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .build();

        mboCategoryMappingsService = new MboCategoryMappingsServiceMock();
        contentLabApiService = new ContentLabApiServiceMock();
        tovarTreeProvider = mock(TovarTreeProvider.class);
        when(tovarTreeProvider.getCategoryName(anyInt())).thenAnswer(invocation -> {
            int categoryId = invocation.getArgument(0);
            return "cat" + categoryId;
        });
        AllBeans allBeans = createNew();
        markupManager = allBeans.get(MarkupManager.class);
        taskProcessManager = (TaskProcessManagerStub) allBeans.get(ITaskProcessManager.class);
        tasksCache = allBeans.get(TasksCache.class);
        tolokaApi = (TolokaApiStub) allBeans.get(TolokaApi.class);
        tolokaApi.createPool(new Pool().setId(BASE_POOL));
        propertiesGetter.setTolokaApi(tolokaApi);
        inspectionPropertiesGetter.setTolokaApi(tolokaApi);

        JsonSerializer reqSerializer = processor.getRequestSerializer();
        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(ModelDataIdentity.class, new JsonUtils.DefaultJsonSerializer<>())
            .addDeserializer(ModelDataIdentity.class,
                new JsonUtils.DefaultJsonDeserializer<>(ModelDataIdentity.class))
            .addSerializer(FillSkuDataAttributes.class, new JsonUtils.DefaultJsonSerializer<>())
            .addDeserializer(FillSkuDataAttributes.class,
                new JsonUtils.DefaultJsonDeserializer<>(FillSkuDataAttributes.class))
            .addSerializer(FillSkuDataItemPayload.class, new JsonUtils.DefaultJsonSerializer<>())
            .addDeserializer(FillSkuDataItemPayload.class,
                new JsonUtils.DefaultJsonDeserializer<>(FillSkuDataItemPayload.class))
            .addSerializer(TaskDataItem.class, reqSerializer)
            .addSerializer(FillSkuResponse.class, new JsonUtils.DefaultJsonSerializer<>())
            .addDeserializer(FillSkuResponse.class, processor.getResponseDeserializer());
        objectMapper.registerModule(serializerModule);
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        TaskTypeContainer container = BaseFillMskuTypeContainerFactory
            .createFillSkuFromClabTypeContainer(createTaskContainerParams(), tasksCache);

        TaskTypeContainer inspectionContainer = BaseFillMskuTypeContainerFactory
            .createFillSkuInspectionTypeContainer(createInspectionTaskContainerParams(), tasksCache);

        FSFCGenerationRequestGenerator fsfcGenerationRequestGenerator = new FSFCGenerationRequestGenerator(
            contentLabApiService,
            new TaskDataUniqueCache());

        TaskTypeContainer generationContainer = FSFCGenerationTypeContainerFactory
            .createTypeContainer("", fsfcGenerationRequestGenerator);

        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        Map<Integer, TaskTypeContainer> containersMap = new HashMap<>();
        containersMap.put(Markup.TaskType.FILL_MSKU_FROM_CLAB_VALUE, container);
        containersMap.put(Markup.TaskType.FILL_MSKU_FROM_CLAB_INSPECTION_VALUE, inspectionContainer);
        containersMap.put(Markup.TaskType.FILL_MSKU_FROM_CLAB_GENERATION_VALUE, generationContainer);
        taskTypesContainers.setTaskTypeContainers(containersMap);
        return taskTypesContainers;
    }

    private TaskTypeContainerParams<ModelDataIdentity, FillSkuDataItemPayload,
        FillSkuResponse> createTaskContainerParams() {

        FillSkuFromClabRequestGenerator requestGenerator =
            new FillSkuFromClabRequestGenerator();
        requestGenerator.setContentLabService(contentLabApiService);
        requestGenerator.setMboCategoryMappingsService(mboCategoryMappingsService);
        requestGenerator.setTovarTreeProvider(tovarTreeProvider);
        MboUsers.MboUser worker = MboUsers.MboUser.newBuilder().setStaffLogin("workerLogin").build();
        UsersService usersService = mock(UsersService.class);
        when(usersService.getUidByWorkerIdOrDefault(eq(WORKER_ID))).thenReturn(1L);
        when(usersService.getUserByWorkerId(eq(WORKER_ID))).thenReturn(worker);
        yangLogStorageService = mock(YangLogStorageService.class);
        when(yangLogStorageService.yangLogStore(any())).thenReturn(YangLogStorage.YangLogStoreResponse.newBuilder()
            .setSuccess(true)
            .build());
        FillSkuFromClabStatisticSaver statisticsSaver =
            new FillSkuFromClabStatisticSaver(yangLogStorageService, usersService);

        FillSkuFromClabResultCollector resultMaker = new FillSkuFromClabResultCollector(contentLabApiService,
            statisticsSaver,
            yangLogStorageService,
            usersService);

        propertiesGetter = new FillSkuFromClabTaskPropertiesGetter();

        processor = new FillSkuDataItemsProcessor();
        processor.setRequestGenerator(requestGenerator);
        processor.setResultMaker(resultMaker);
        processor.setTaskPropertiesGetter(propertiesGetter);

        TaskTypeContainerParams<ModelDataIdentity, FillSkuDataItemPayload,
            FillSkuResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName("fill sku from clab");
        params.setDataItemsProcessor(processor);
        params.setYangBasePoolId(BASE_POOL);
        params.setMaxBatchCount(10);
        params.setMaxBatchCount(10);

        return params;
    }

    private TaskTypeContainerParams<FillSkuInspectionDataIdentity, FillSkuInspectionDataPayload,
        FillSkuInspectionResponse> createInspectionTaskContainerParams() {

        FillSkuFromClabInspectionRequestGenerator requestGenerator =
            new FillSkuFromClabInspectionRequestGenerator();
        MboUsers.MboUser worker = MboUsers.MboUser.newBuilder().setStaffLogin("workerLogin").build();
        MboUsers.MboUser inspector = MboUsers.MboUser.newBuilder().setStaffLogin("inspectorLogin").build();
        UsersService usersService = mock(UsersService.class);
        when(usersService.getUidByWorkerIdOrDefault(eq(WORKER_ID))).thenReturn(1L);
        when(usersService.getUidByWorkerIdOrDefault(eq(INSPECTOR_ID))).thenReturn(2L);
        when(usersService.getUserByWorkerId(eq(WORKER_ID))).thenReturn(worker);
        when(usersService.getUserByWorkerId(eq(INSPECTOR_ID))).thenReturn(inspector);
        FillSkuFromClabStatisticSaver statisticsSaver =
            new FillSkuFromClabStatisticSaver(yangLogStorageService, usersService);

        FillSkuFromClabInspectionResultCollector resultMaker = new FillSkuFromClabInspectionResultCollector(
            contentLabApiService,
            statisticsSaver,
            usersService);

        inspectionPropertiesGetter = new FillSkuFromClabInspectionTaskPropertiesGetter();

        FillSkuInspectionDataItemsProcessor inspectionProcessor = new FillSkuInspectionDataItemsProcessor();
        inspectionProcessor.setRequestGenerator(requestGenerator);
        inspectionProcessor.setResultMaker(resultMaker);
        inspectionProcessor.setTaskPropertiesGetter(inspectionPropertiesGetter);

        TaskTypeContainerParams<FillSkuInspectionDataIdentity, FillSkuInspectionDataPayload,
            FillSkuInspectionResponse> params = new TaskTypeContainerParams<>();
        params.setTaskName("fill sku from clab inspection");
        params.setDataItemsProcessor(inspectionProcessor);
        params.setYangBasePoolId(BASE_POOL);
        params.setMaxBatchCount(1);
        params.setMaxBatchCount(1);

        return params;
    }

    @Test
    public void successfullNoInspectionTask() throws Exception {
        ContentLabApi.YangTaskModelDetails modelDetails = createModelDetails(createModel());
        ContentLabApi.YangTaskModelDetails modelDetails1 = createModelDetails(
            createModel(modelDetails.getModel().getCategoryId()));
        contentLabApiService.addModel(modelDetails);
        contentLabApiService.addModel(modelDetails1);

        mboCategoryMappingsService.putOffer(createOffer(modelDetails.getSku(0)));
        mboCategoryMappingsService.putOffer(createOffer(modelDetails.getSku(1)));
        mboCategoryMappingsService.putOffer(createOffer(modelDetails1.getSku(0)));

        startGeneration();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.FILL_MSKU_FROM_CLAB_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();

        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());
        assertThat(taskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(2);
        assertThat(taskInfo.getTaskStatus().getSentCount()).isEqualTo(2);

        TaskDataItem item = taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        FillSkuDataItemPayload payload = (FillSkuDataItemPayload) item.getInputData();
        assertThat(payload.getAttributes().getFillSkus()).hasSize(2);

        Comment forManagerComment = new Comment("FOR_MANAGER", Collections.singletonList("bad"));
        FillSkuResponse resp = new FillSkuResponse(
            item.getId(),
            WORKER_ID,
            ImmutableList.of(
                createStatus(modelDetails.getSku(0),
                    FillSkuStatus.FillSkuResult.PUBLISHED,
                    ""),
                createStatus(modelDetails.getSku(1),
                    FillSkuStatus.FillSkuResult.RETURN_TO_INPUT_MANAGER,
                    "Bad")
            )
        );
        mockNeedInspectionCall(false);
        processTaskInYang(taskInfo, resp, false);

        Assert.assertEquals(TaskState.COMPLETED, taskInfo.getState());
        assertThat(taskInfo.getTaskStatus().getReceivedCount()).isEqualTo(1);
        assertThat(taskInfo.getTaskStatus().getProcessedCount()).isEqualTo(1);

        List<ContentLabApi.YangTaskStatus> statuses =
            contentLabApiService.getStatuses(modelDetails.getModel().getModelId());

        assertThat(statuses).containsExactlyInAnyOrder(
            ContentLabApi.YangTaskStatus.newBuilder()
                .setSku(modelDetails.getSku(0).toBuilder().clearLabImages().build())
                .setComment("")
                .setResult(ContentLabApi.YangTaskResult.PUBLISHED)
                .build(),
            ContentLabApi.YangTaskStatus.newBuilder()
                .setSku(modelDetails.getSku(1).toBuilder().clearLabImages().build())
                .setComment("Bad")
                .setResult(ContentLabApi.YangTaskResult.RETURN_TO_INPUT_MANAGER)
                .build()
        );
    }

    @Test
    public void testNoOffers() throws Exception {
        ContentLabApi.YangTaskModelDetails modelDetails = createModelDetails(createModel());
        contentLabApiService.addModel(modelDetails);

        startGeneration();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.FILL_MSKU_FROM_CLAB_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();

        assertThat(taskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(0);
    }

    @Test
    public void testSerialization() throws Exception {
        ContentLabApi.YangTaskModelDetails modelDetails = createModelDetails(createModel());
        contentLabApiService.addModel(modelDetails);

        mboCategoryMappingsService.putOffer(createOffer(modelDetails.getSku(0)));

        startGeneration();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.FILL_MSKU_FROM_CLAB_VALUE, (x) -> true);
        assertThat(configInfoSet).hasSize(1);

        TaskConfigInfo configInfo = configInfoSet.iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();

        TaskDataItem taskDataItem = taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        FillSkuDataItemPayload payload = (FillSkuDataItemPayload) taskDataItem.getInputData();

        // Identifier
        String val = objectMapper.writeValueAsString(payload.getDataIdentifier());
        System.out.println(val);
        ModelDataIdentity identCpy = objectMapper.readValue(val, ModelDataIdentity.class);
        Assert.assertEquals(payload.getDataIdentifier(), identCpy);

        // Attributes
        val = objectMapper.writeValueAsString(payload.getAttributes());
        System.out.println(val);
        FillSkuDataAttributes attrCpy = objectMapper.readValue(val, FillSkuDataAttributes.class);
        Assert.assertEquals(payload.getAttributes(), attrCpy);

        // Payload
        val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        FillSkuDataItemPayload payloadCpy = objectMapper.readValue(val, FillSkuDataItemPayload.class);
        Assert.assertEquals(payload, payloadCpy);

        //Request
        val = objectMapper.writeValueAsString(taskDataItem);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        List<String> requiredFields = new ArrayList<>(
            Arrays.asList("id", "model_id", "category_id", "category_name", "unique_category_name", "fill_skus"));
        requiredFields.removeAll(keys);

        Assert.assertEquals("Absent " + requiredFields.toString(), 0, requiredFields.size());

        String responseFromHitman = "{\"req_id\":\"100\", \"worker_id\":\"qwerty\", \"fill_sku_statuses\":[" +
            "{\"msku_id\":1, \"fill_sku_result\":\"PUBLISHED\", \"comment\":\"\"}," +
            "{\"msku_id\":2, \"fill_sku_result\":\"CLAB_NEED_INFO\", \"comment\":\"blablabla\"}" +
            "]}";
        FillSkuResponse response = objectMapper.readValue(responseFromHitman, FillSkuResponse.class);
        FillSkuResponse expected = new FillSkuResponse(100L, "qwerty", ImmutableList.of(
            new FillSkuStatus(1L, "PUBLISHED", ""),
            new FillSkuStatus(2L, "CLAB_NEED_INFO", "blablabla")
        ));
        assertThat(response).isEqualTo(expected);
    }

    @Test
    public void successfullWithInspectionTask() throws Exception {
        ContentLabApi.YangTaskModelDetails modelDetails = createModelDetails(createModel());
        ContentLabApi.YangTaskModelDetails modelDetails1 = createModelDetails(
            createModel(modelDetails.getModel().getCategoryId()));
        contentLabApiService.addModel(modelDetails);
        contentLabApiService.addModel(modelDetails1);

        mboCategoryMappingsService.putOffer(createOffer(modelDetails.getSku(0)));
        mboCategoryMappingsService.putOffer(createOffer(modelDetails.getSku(1)));
        mboCategoryMappingsService.putOffer(createOffer(modelDetails1.getSku(0)));

        startGeneration();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByTypeId(
            Markup.TaskType.FILL_MSKU_FROM_CLAB_VALUE, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();

        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());
        assertThat(taskInfo.getTaskStatus().getGeneratedCount()).isEqualTo(2);
        assertThat(taskInfo.getTaskStatus().getSentCount()).isEqualTo(2);

        TaskDataItem item = taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        FillSkuDataItemPayload payload = (FillSkuDataItemPayload) item.getInputData();
        assertThat(payload.getAttributes().getFillSkus()).hasSize(2);

        FillSkuResponse resp = new FillSkuResponse(
            item.getId(),
            WORKER_ID,
            ImmutableList.of(
                createStatus(modelDetails.getSku(0),
                    FillSkuStatus.FillSkuResult.PUBLISHED,
                    ""),
                createStatus(modelDetails.getSku(1),
                    FillSkuStatus.FillSkuResult.RETURN_TO_INPUT_MANAGER,
                    "Bad")
            )
        );
        mockNeedInspectionCall(true);
        processTaskInYang(taskInfo, resp, false);
        // task is running because of inspection
        Assert.assertEquals(TaskState.RUNNING, taskInfo.getState());

        // checking inspection
        Assert.assertTrue(configInfo.hasDependConfigId());
        int dependConfigId = configInfo.getDependConfigId();
        TaskConfigInfo inspectionConfig = tasksCache.getTaskConfig(dependConfigId);
        TaskInfo inspectionTask = inspectionConfig.getSingleCurrentTask();
        Assert.assertEquals(TaskState.RUNNING, inspectionTask.getState());

        TaskDataItem inspectionItem = inspectionTask.getProgress().getDataItemsByState(TaskDataItemState.SENT)
            .getItems().iterator().next();

        FillSkuInspectionResponse inspectionResponse = new FillSkuInspectionResponse(
            inspectionItem.getId(),
            new FillSkuResponse(
                item.getId(),
                INSPECTOR_ID,
                ImmutableList.of(
                    createStatus(modelDetails.getSku(0),
                        FillSkuStatus.FillSkuResult.PUBLISHED,
                        ""),
                    createStatus(modelDetails.getSku(1),
                        FillSkuStatus.FillSkuResult.PUBLISHED,
                        "")
                )
            )
        );

        // process inspection
        processTaskInYang(inspectionTask, inspectionResponse, true);

        Assert.assertEquals(TaskState.COMPLETED, inspectionTask.getState());
        Assert.assertTrue(inspectionConfig.getState().isFinalState());

        List<ContentLabApi.YangTaskStatus> statuses =
            contentLabApiService.getStatuses(modelDetails.getModel().getModelId());

        assertThat(statuses).containsExactlyInAnyOrder(
            ContentLabApi.YangTaskStatus.newBuilder()
                .setSku(modelDetails.getSku(0).toBuilder().clearLabImages().build())
                .setComment("")
                .setResult(ContentLabApi.YangTaskResult.PUBLISHED)
                .build(),
            ContentLabApi.YangTaskStatus.newBuilder()
                .setSku(modelDetails.getSku(1).toBuilder().clearLabImages().build())
                .setComment("")
                .setResult(ContentLabApi.YangTaskResult.PUBLISHED)
                .build()
        );
    }

    private void startGeneration() throws Exception {
        CreateConfigAction createConfigAction = new CreateConfigAction.Builder()
            .setCount(Integer.MAX_VALUE)
            .setTypeId(Markup.TaskType.FILL_MSKU_FROM_CLAB_GENERATION_VALUE)
            .setCategoryId(0)
            .setAutoActivate(true)
            .addConfigParameter(ConfigParameterType.OFFERS_IN_TASK, 10)
            .build();
        markupManager.createConfig(createConfigAction);
        taskProcessManager.processNextTask();
    }

    private ContentLabApi.YangTaskModel createModel() {
        return ContentLabApi.YangTaskModel.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setModelId(enhancedRandom.nextLong())
            .build();
    }

    private ContentLabApi.YangTaskModel createModel(long categoryId) {
        return ContentLabApi.YangTaskModel.newBuilder()
            .setCategoryId(categoryId)
            .setModelId(enhancedRandom.nextLong())
            .build();
    }

    private ContentLabApi.YangTaskSku createSku(ContentLabApi.YangTaskModel model) {
        return ContentLabApi.YangTaskSku.newBuilder()
            .setCategoryId(model.getCategoryId())
            .setGoodId(enhancedRandom.nextLong())
            .setMskuId(enhancedRandom.nextLong())
            .setSupplierId(enhancedRandom.nextLong())
            .setSupplierSkuId(enhancedRandom.nextObject(String.class))
            .addLabImages(createPicture())
            .addLabImages(createPicture())
            .build();
    }

    private SupplierOffer.Offer createOffer(ContentLabApi.YangTaskSku sku) {
        return SupplierOffer.Offer.newBuilder()
            .setSupplierId(sku.getSupplierId())
            .setShopSkuId(sku.getSupplierSkuId())
            .setBarcode(enhancedRandom.nextObject(String.class))
            .setVendorCode(enhancedRandom.nextObject(String.class))
            .setShopVendor(enhancedRandom.nextObject(String.class))
            .setTitle(enhancedRandom.nextObject(String.class))
            .setDescription(enhancedRandom.nextObject(String.class))
            .build();
    }

    private ModelStorage.Picture createPicture() {
        return ModelStorage.Picture.newBuilder()
            .setUrl(enhancedRandom.nextObject(String.class))
            .setUrlOrig(enhancedRandom.nextObject(String.class))
            .setWidth(enhancedRandom.nextInt())
            .setHeight(enhancedRandom.nextInt())
            .build();
    }

    private FillSkuStatus createStatus(ContentLabApi.YangTaskSku sku,
                                       FillSkuStatus.FillSkuResult result,
                                       String comment) {
        return new FillSkuStatus(sku.getMskuId(), result.name(), comment);
    }


    private ContentLabApi.YangTaskModelDetails createModelDetails(ContentLabApi.YangTaskModel model) {
        return ContentLabApi.YangTaskModelDetails.newBuilder()
            .setModel(model)
            .addSku(createSku(model))
            .addSku(createSku(model))
            .build();
    }

    protected void processTaskInYang(TaskInfo task, IResponseItem response, boolean isInspection) {
        // check sending finished
        assertThat(task.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        List<YangPoolInfo> yangPools = YangPoolsCache.getInstance().getYangPools(task.getId(), false);
        assertThat(yangPools).hasSize(1);
        YangPoolInfo poolInfo = yangPools.get(0);
        assertThat(poolInfo.getTaskId()).isEqualTo(task.getId());

        if (isInspection) {
            tolokaApi.addResultsBlueLogsScheme(poolInfo.getPoolId(), Collections.singletonList(response),
                true, INSPECTOR_ID);
        } else {
            tolokaApi.addResultsBlueLogsScheme(poolInfo.getPoolId(), Collections.singletonList(response),
                false, WORKER_ID);
        }
        processAllTasksWithUnlock(taskProcessManager);
    }

    private void mockNeedInspectionCall(boolean needInspection) {
        when(yangLogStorageService.yangResolveNeedInspection(any())).thenReturn(
            YangLogStorage.YangResolveNeedInspectionResponse.newBuilder()
                .setNeedInspection(needInspection)
                .setDebugInformation("Debug info")
                .build());
    }
}
