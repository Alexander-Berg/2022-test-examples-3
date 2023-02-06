package ru.yandex.market.aliasmaker.cache.models;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.AliasMakerUtils;
import ru.yandex.market.aliasmaker.cache.CategoryReloadingCache;
import ru.yandex.market.aliasmaker.cache.vendors.GlobalVendorsCache;
import ru.yandex.market.aliasmaker.cache.vendors.GlobalVendorsCacheImpl;
import ru.yandex.market.aliasmaker.models.User;
import ru.yandex.market.mbo.export.CategoryParametersAddOptionsServiceStub;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.GlobalVendorsService;
import ru.yandex.market.mbo.http.GuruCategoryService;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboGuruService;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.SaveModelsRequest;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.mbo.http.YangTaskEnums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static ru.yandex.market.aliasmaker.TestFileUtils.load;

/**
 * @author york
 * @since 07.06.2018
 */
public class ModelsShadowReloadingCacheTest {
    private static final int HID = 91491;
    private static final String TASK_ID = "task_id";
    private static final YangTaskEnums.YangTask TASK_TYPE = YangTaskEnums.YangTask.YANG_TASK_BLUE_LOGS;
    private static final List<String> MODEL_FILES = Arrays.asList("models.json", "skus.json");
    private static AtomicInteger newParamIdSequence = new AtomicInteger(-1);

    private Long idSeq = 100000L;
    private Map<String, Map<Long, ModelStorage.Model>> allModels = new HashMap<>();
    private ModelStorageServiceStub modelStorageService;
    private ModelsShadowReloadingCache cache;
    private ModelService modelService;

    @Before
    public void init() throws Exception {
        for (String fileName : MODEL_FILES) {
            ModelStorage.GetModelsResponse.Builder builder = ModelStorage.GetModelsResponse.newBuilder();
            JsonFormat.merge(
                    new InputStreamReader(
                            getClass().getResourceAsStream("/" + fileName)
                    ),
                    builder
            );
            for (ModelStorage.Model model : builder.getModelsList()) {
                Map<Long, ModelStorage.Model> map = allModels.computeIfAbsent(model.getCurrentType(),
                        k -> new HashMap<>());

                map.put(model.getId(), model);
            }
        }

        modelStorageService = Mockito.mock(ModelStorageServiceStub.class);
        Mockito.when(modelStorageService.findModels(any())).thenAnswer(
                invocation -> {
                    ModelStorage.FindModelsRequest request = invocation.getArgument(0,
                            ModelStorage.FindModelsRequest.class);

                    ModelStorage.GetModelsResponse.Builder responseBuilder =
                            ModelStorage.GetModelsResponse.newBuilder();

                    Map<Long, ModelStorage.Model> map;
                    if (request.hasModelType()) {
                        map = getByType(request.getModelType().name());
                    } else {
                        map = new HashMap<>(getByType("SKU"));
                        map.putAll(getByType("GURU"));
                    }
                    Collection<ModelStorage.Model> models;
                    if (request.getModelIdsCount() > 0) {
                        models = findByIds(map, request.getModelIdsList());
                    } else {
                        models = map.values();
                    }
                    if (request.hasCategoryId()) {
                        Long categoryId = request.getCategoryId();
                        models = models.stream()
                                .filter(m -> m.getCategoryId() == categoryId)
                                .collect(Collectors.toList());
                    }
                    models.forEach(responseBuilder::addModels);
                    return responseBuilder.build();
                }
        );

        Mockito.when(modelStorageService.getModels(any())).thenAnswer(
                invocation -> {
                    ModelStorage.GetModelsRequest request = invocation.getArgument(0,
                            ModelStorage.GetModelsRequest.class);
                    ModelStorage.GetModelsResponse.Builder responseBuilder =
                            ModelStorage.GetModelsResponse.newBuilder();

                    Map<Long, ModelStorage.Model> map = new HashMap<>(getByType("SKU"));
                    map.putAll(getByType("GURU"));

                    map.values().stream()
                            .filter(m -> m.getCategoryId() == request.getCategoryId() &&
                                    request.getModelIdsList().contains(m.getId()))
                            .forEach(responseBuilder::addModels);

                    return responseBuilder.build();
                }
        );

        Mockito.when(modelStorageService.saveModels(any())).thenAnswer(
                invocation -> {
                    ModelStorage.SaveModelsRequest request = invocation.getArgument(
                            0, ModelStorage.SaveModelsRequest.class
                    );
                    ModelStorage.OperationResponse.Builder response = ModelStorage.OperationResponse.newBuilder();
                    for (ModelStorage.Model model : request.getModelsList()) {
                        ModelStorage.Model.Builder updated = updateModel(model);
                        response.addStatuses(
                                ModelStorage.OperationStatus.newBuilder()
                                        .setStatus(ModelStorage.OperationStatusType.OK)
                                        .setModel(updated)
                                        .setModelId(updated.getId())
                                        .setType(model.getId() <= 0 ? ModelStorage.OperationType.CREATE :
                                                ModelStorage.OperationType.CHANGE)
                        );
                    }
                    return response.build();
                }
        );

        Mockito.when(modelStorageService.saveModelsGroup(any())).thenAnswer(
                invocation -> {
                    ModelCardApi.SaveModelsGroupRequest request = invocation.getArgument(
                            0, ModelCardApi.SaveModelsGroupRequest.class
                    );
                    ModelCardApi.SaveModelsGroupOperationResponse.Builder groupResponse =
                            ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                    .setStatus(ModelStorage.OperationStatusType.OK);

                    for (ModelStorage.Model model : request.getModelsRequest(0).getModelsList()) {
                        ModelStorage.Model.Builder modelBuilder = updateModel(model);
                        groupResponse.addRequestedModelsStatuses(
                                ModelStorage.OperationStatus.newBuilder()
                                        .setStatus(ModelStorage.OperationStatusType.OK)
                                        .setModel(modelBuilder)
                                        .setModelId(modelBuilder.getId())
                                        .setType(model.getId() <= 0 ? ModelStorage.OperationType.CREATE :
                                                ModelStorage.OperationType.CHANGE)
                                        .build()
                        );
                    }
                    return ModelCardApi.SaveModelsGroupResponse.newBuilder()
                            .addResponse(groupResponse)
                            .build();
                }
        );

        CategoryReloadingCache categoryCache = new CategoryReloadingCache();
        GuruCategoryService guruCategoryService = Mockito.mock(GuruCategoryService.class);
        Mockito.when(guruCategoryService.updateGuruCategoryInternals(any())).thenAnswer(invocation ->
                MboGuruService.UpdateGuruCategoryInternalsResponse.newBuilder()
                        .setStatus(MboGuruService.OperationStatusType.OK)
                        .build()
        );
        categoryCache.setGuruCategoryService(guruCategoryService);
        GlobalVendorsService globalVendorsService = Mockito.mock(GlobalVendorsService.class);
        Mockito.when(globalVendorsService.searchVendors(any(MboVendors.SearchVendorsRequest.class)))
                .thenAnswer(invocation -> {
                    MboVendors.SearchVendorsRequest searchVendorsRequest =
                            invocation.getArgument(0, MboVendors.SearchVendorsRequest.class);
                    MboVendors.SearchVendorsResponse.Builder builder =
                            load("/vendors.json", MboVendors.SearchVendorsResponse.newBuilder());
                    int offset = searchVendorsRequest.getOffset();
                    if (offset >= builder.getVendorsCount()) {
                        builder.clearVendors();
                    } else {
                        List<MboVendors.GlobalVendor> vendors = builder.getVendorsList().subList(
                                offset,
                                Math.min(builder.getVendorsCount(), offset + searchVendorsRequest.getLimit())
                        );
                        builder.clearVendors();
                        builder.addAllVendors(vendors);
                    }
                    return builder.build();
                });
        GlobalVendorsCache globalVendorsCache = new GlobalVendorsCacheImpl(globalVendorsService);

        CategoryParametersServiceStub categoryParametersService = Mockito.mock(CategoryParametersServiceStub.class);
        Mockito.when(categoryParametersService.getParameters(any())).thenAnswer(
                invocation -> load(
                        "/params.json", MboParameters.GetCategoryParametersResponse.newBuilder()
                ).build()
        );
        CategoryParametersAddOptionsServiceStub categoryParametersAddOptionsService = Mockito
                .mock(CategoryParametersAddOptionsServiceStub.class);
        Mockito.when(categoryParametersAddOptionsService.addOptions(any())).thenAnswer(
                invocation -> {
                    MboParameters.AddOptionsRequest request = invocation.getArgument(
                            0, MboParameters.AddOptionsRequest.class
                    );
                    List<MboParameters.ProcessedOption> processedOptions = new ArrayList<>();
                    for (int i = 0; i < request.getOptionCount(); i++) {
                        processedOptions.add(
                                MboParameters.ProcessedOption.newBuilder()
                                        .setOptionId(newParamIdSequence.decrementAndGet())
                                        .setParameterId(request.getParamId())
                                        .setVersionId(0)
                                        .build()
                        );
                    }
                    return MboParameters.AddOptionsResponse.newBuilder()
                            .setOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addAllOptions(processedOptions)
                            )
                            .build();
                }
        );
        Mockito.when(categoryParametersService.overrideOptions(any())).thenAnswer(
                invocation -> {
                    MboParameters.OverrideOptionsRequest request = invocation.getArgument(
                            0, MboParameters.OverrideOptionsRequest.class
                    );
                    return MboParameters.OverrideOptionsResponse.newBuilder()
                            .addOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addOptions(
                                                    MboParameters.ProcessedOption.newBuilder()
                                                            .setVersionId(1)
                                                            .setParameterId(request.getParamId())
                                                            .setOptionId(request.getOptions(0).getOptionId())
                                            )
                            )
                            .build();
                }
        );
        Mockito.when(categoryParametersService.updateOption(any())).thenAnswer(
                invocation -> {
                    MboParameters.UpdateOptionRequest request = invocation.getArgument(
                            0, MboParameters.UpdateOptionRequest.class
                    );
                    return MboParameters.UpdateOptionResponse.newBuilder()
                            .setOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addOptions(
                                                    MboParameters.ProcessedOption.newBuilder()
                                                            .setOptionId(request.getOptionId())
                                                            .setParameterId(request.getParamId())
                                                            .setVersionId(request.getExpectedVersionId() + 1)
                                            )
                            )
                            .build();
                }
        );
        categoryCache.setGlobalVendorsCache(globalVendorsCache);
        categoryCache.setCategoryParametersService(categoryParametersService);
        categoryCache.setCategoryParametersAddOptionsService(categoryParametersAddOptionsService);
        cache = new ModelsShadowReloadingCache();
        cache.setModelStorageService(modelStorageService);
        cache.setCategoryCache(categoryCache);
        cache.setCacheFactory(new CategoryModelsCacheFactory(Collections.emptyList(), null, null));
        modelService = new ModelService();
        modelService.setModelStorageService(modelStorageService);
        modelService.setModelsCache(cache);
    }

    @Test
    public void testLoadOnlyOperatorModelsMode() {
        cache.setLoadOnlyOperatorModels(false);
        cache.loadCategoryModels(HID);

        ArgumentCaptor<ModelStorage.FindModelsRequest> argument =
                ArgumentCaptor.forClass(ModelStorage.FindModelsRequest.class);
        Mockito.verify(modelStorageService, Mockito.atLeastOnce()).findModels(argument.capture());
        ModelStorage.FindModelsRequest findModelsRequest = argument.getValue();
        Assertions.assertThat(findModelsRequest.getOnlyOperatorQuality()).isFalse();

        cache.setLoadOnlyOperatorModels(true);
        cache.loadCategoryModels(HID);

        Mockito.verify(modelStorageService, Mockito.atLeastOnce()).findModels(argument.capture());
        findModelsRequest = argument.getValue();
        Assertions.assertThat(findModelsRequest.getOnlyOperatorQuality()).isTrue();
        Assertions.assertThat(cache.isOperatorQualityModel(HID, 1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testUpdateGroup() {
        int categoryId = HID;
        CategoryModelsCache guruCache = cache.getGuruModelsCache(categoryId);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(categoryId);
        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());
        List<ModelStorage.Model> skuModels = skuCache.getModelsStream()
                .collect(Collectors.toList());

        ModelStorage.Model guruModel = guruModels.get(0);
        ModelStorage.Model skuModel = skuModels.get(0);
        ModelStorage.Model newSku = skuModel.toBuilder()
                .clearId()
                .clearModifiedTs()
                .build();

        List<ModelStorage.Model> models = Arrays.asList(guruModel, skuModel, newSku);

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);

        Assert.assertEquals(AliasMaker.OperationStatus.SUCCESS, builder.getResult().getStatus());
        cache.updateModelsInKnowledge(categoryId, builder.getModelList());

        //assert new sku saved
        Assert.assertEquals(skuModels.size() + 1, skuCache.getModelsStream().count());

        checkNewTimestamp(guruCache, guruModel);
        checkNewTimestamp(skuCache, skuModel);

        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> captor = ArgumentCaptor
                .forClass(ModelCardApi.SaveModelsGroupRequest.class);
        Mockito.verify(modelStorageService, Mockito.times(1))
                .saveModelsGroup(captor.capture());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = captor.getValue().getModelsRequestList();
        modelsRequestList.forEach(r -> {
            Assert.assertEquals(r.getSource(), MboAudit.Source.YANG_TASK);
            Assert.assertTrue(r.hasBilledOperation());
            Assert.assertTrue(r.getBilledOperation());
        });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testZeroIdsConvertedToNegativeIds() {
        int categoryId = HID;
        CategoryModelsCache guruCache = cache.getGuruModelsCache(categoryId);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(categoryId);

        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());
        List<ModelStorage.Model> skuModels = skuCache.getModelsStream()
                .collect(Collectors.toList());

        ModelStorage.Model guruModel = guruModels.get(0);
        ModelStorage.Model skuModel = skuModels.get(0);
        ModelStorage.Model newSku1 = skuModel.toBuilder().setId(0).clearModifiedTs().build();
        ModelStorage.Model newSku2 = skuModel.toBuilder().setId(0).clearModifiedTs().build();
        ModelStorage.Model newSku3 = skuModel.toBuilder().setId(0).clearModifiedTs().build();

        List<ModelStorage.Model> models = Arrays.asList(guruModel, skuModel, newSku1, newSku2, newSku3);

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);
        cache.updateModelsInKnowledge(categoryId, builder.getModelList());

        //assert new sku saved
        Assert.assertEquals(AliasMaker.OperationStatus.SUCCESS, builder.getResult().getStatus());
        Assert.assertEquals(skuModels.size() + 3, skuCache.getModelsStream().count());

        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> captor = ArgumentCaptor
                .forClass(ModelCardApi.SaveModelsGroupRequest.class);
        Mockito.verify(modelStorageService, Mockito.times(1))
                .saveModelsGroup(captor.capture());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = captor.getValue().getModelsRequestList();
        List<Long> sentIds = modelsRequestList.stream()
                .map(SaveModelsRequest::getModelsList)
                .flatMap(Collection::stream)
                .map(Model::getId)
                .collect(Collectors.toList());
        assertThat(sentIds).containsExactly(guruModel.getId(), skuModel.getId(), -1L, -2L, -3L);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNegativeIdsReusedIds() {
        int categoryId = HID;
        CategoryModelsCache guruCache = cache.getGuruModelsCache(categoryId);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(categoryId);

        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());
        List<ModelStorage.Model> skuModels = skuCache.getModelsStream()
                .collect(Collectors.toList());

        ModelStorage.Model guruModel = guruModels.get(0);
        ModelStorage.Model skuModel = skuModels.get(0);
        ModelStorage.Model newSku1 = skuModel.toBuilder().setId(-10).clearModifiedTs().build();
        ModelStorage.Model newSku2 = skuModel.toBuilder().setId(0).clearModifiedTs().build();
        ModelStorage.Model newSku3 = skuModel.toBuilder().setId(-2).clearModifiedTs().build();

        List<ModelStorage.Model> models = Arrays.asList(guruModel, skuModel, newSku1, newSku2, newSku3);

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);
        cache.updateModelsInKnowledge(categoryId, builder.getModelList());

        //assert new sku saved
        Assert.assertEquals(AliasMaker.OperationStatus.SUCCESS, builder.getResult().getStatus());
        Assert.assertEquals(skuModels.size() + 3, skuCache.getModelsStream().count());

        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> captor = ArgumentCaptor
                .forClass(ModelCardApi.SaveModelsGroupRequest.class);
        Mockito.verify(modelStorageService, Mockito.times(1))
                .saveModelsGroup(captor.capture());

        List<ModelStorage.SaveModelsRequest> modelsRequestList = captor.getValue().getModelsRequestList();
        List<Long> sentIds = modelsRequestList.stream()
                .map(SaveModelsRequest::getModelsList)
                .flatMap(Collection::stream)
                .map(Model::getId)
                .collect(Collectors.toList());
        assertThat(sentIds).containsExactly(guruModel.getId(), skuModel.getId(), -10L, -11L, -2L);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testSourceAndBilledOperationPassedToSaveRequest() {
        CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(HID);

        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());
        List<ModelStorage.Model> skuModels = skuCache.getModelsStream()
                .collect(Collectors.toList());

        ModelStorage.Model guruModel = guruModels.get(0);
        ModelStorage.Model skuModel = skuModels.get(0);
        ModelStorage.Model newSku = skuModel.toBuilder()
                .clearId()
                .clearModifiedTs()
                .build();

        List<ModelStorage.Model> models = Arrays.asList(guruModel, skuModel, newSku);

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testMDMParamWithNoModificationDate() {
        CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);

        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());

        Model.Builder guruModel = guruModels.get(0).toBuilder();

        ModelStorage.ParameterValue mdmSomethingNoModificationDate = ModelStorage.ParameterValue.newBuilder()
                .setParamId(1232343L)
                .setXslName("mdm_something")
                .setValueSource(ModelStorage.ModificationSource.MDM)
                .setBoolValue(true)
                .build();

        guruModel.addParameterValues(mdmSomethingNoModificationDate);

        List<ModelStorage.Model> models = Collections.singletonList(guruModel.build());

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);
        List<Model> modelList = builder.getModelList();
        Model model = modelList.get(0);
        model.getParameterValuesList()
                .forEach(p -> {
                    if (p.getXslName().equals("mdm_something")) {
                        Assert.assertEquals(ModelStorage.ModificationSource.MDM, p.getValueSource());
                    }
                });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNewModelsAreReturnedIfConcurrentModification() {
        CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(HID);

        List<ModelStorage.Model> guruModels = guruCache.getModelsStream()
                .collect(Collectors.toList());
        List<ModelStorage.Model> skuModels = skuCache.getModelsStream()
                .collect(Collectors.toList());

        ModelStorage.Model guruModel = guruModels.get(0);
        ModelStorage.Model skuModel = skuModels.get(0);
        ModelStorage.Model newSku = skuModel.toBuilder()
                .clearId()
                .clearModifiedTs()
                .build();

        List<ModelStorage.Model> models = Arrays.asList(guruModel, newSku);
        modelStorageService = Mockito.mock(ModelStorageServiceStub.class);
        Mockito.when(modelStorageService.saveModelsGroup(any())).thenAnswer(
                invocation -> {
                    ModelCardApi.SaveModelsGroupRequest request = invocation.getArgument(
                            0, ModelCardApi.SaveModelsGroupRequest.class
                    );
                    ModelCardApi.SaveModelsGroupOperationResponse.Builder groupResponse =
                            ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                    .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED);
                    for (Model model : request.getModelsRequest(0).getModelsList()) {
                        ModelStorage.OperationStatus.Builder status = ModelStorage.OperationStatus.newBuilder()
                                .setModelId(model.getId())
                                .setType(ModelStorage.OperationType.CHANGE);
                        if (model.getId() == guruModel.getId()) {
                            status.setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                                    .setModel(model);
                        } else {
                            status.setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                    .setFailureModelId(guruModel.getId());
                        }
                        groupResponse.addRequestedModelsStatuses(status);
                    }

                    return ModelCardApi.SaveModelsGroupResponse.newBuilder()
                            .addResponse(groupResponse)
                            .build();
                }
        );
        modelService.setModelStorageService(modelStorageService);

        AliasMaker.UpdateModelsGroupResponse.Builder builder = modelService.updateModelsGroup(AliasMaker.Action.UPDATE,
                models, true, new User(), TASK_TYPE, TASK_ID, true, true);
        assertThat(builder.getModelList()).extracting(ModelStorage.Model::getId)
                .containsExactlyInAnyOrder(0L, guruModel.getId());
    }

    @Test
    public void testRemoveDuplicates() {
        CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(HID);

        ModelStorage.Model model = guruCache.getModelsStream().findFirst().get();
        assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isEqualTo(model);
        model = model.toBuilder()
                .setVendorId(model.getVendorId() + 1)
                .setModifiedTs(model.getModifiedTs() + 1)
                .build();
        allModels.get(model.getCurrentType()).put(model.getId(), model);

        ModelStorage.Model sku = skuCache.getModelsStream().findFirst().get();
        assertThat(skuCache.getModel(sku.getVendorId(), sku.getId())).isEqualTo(sku);
        sku = sku.toBuilder()
                .setVendorId(sku.getVendorId() + 1)
                .setModifiedTs(sku.getModifiedTs() + 1)
                .build();
        allModels.get(sku.getCurrentType()).put(sku.getId(), sku);

        cache.loadCategoryModels(HID);

        assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isNotNull();
        assertThat(skuCache.getModel(sku.getVendorId(), sku.getId())).isNotNull();
        assertThat(guruCache.getModel(model.getVendorId() - 1, model.getId())).isNull();
        assertThat(skuCache.getModel(sku.getVendorId() - 1, sku.getId())).isNull();
    }

    @Test
    public void testRemoveDeletedModel() {
        CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);
        CategoryModelsCache skuCache = cache.getSkuModelsCache(HID);

        ModelStorage.Model model = guruCache.getModelsStream().findFirst().get();
        assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isEqualTo(model);
        allModels.get(model.getCurrentType()).remove(model.getId());

        ModelStorage.Model sku = skuCache.getModelsStream().findFirst().get();
        assertThat(skuCache.getModel(sku.getVendorId(), sku.getId())).isEqualTo(sku);
        allModels.get(sku.getCurrentType()).remove(sku.getId());

        cache.loadCategoryModels(HID);

        assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isNull();
        assertThat(skuCache.getModel(sku.getVendorId(), sku.getId())).isNull();
    }

    @Test
    public void testUsingDiskCache() {
        String baseDir = System.getProperty("java.io.tmpdir") + "/" + System.currentTimeMillis();
        CategoryModelsCacheFactory factory = new CategoryModelsCacheFactory(Collections.singleton(HID),
                baseDir + "/prod", baseDir + "/tmp");
        cache.setCacheFactory(factory);
        try {
            Map<Long, ModelStorage.Model> storageGuruModels = allModels.get(ModelStorage.ModelType.GURU.name());
            Supplier<List<ModelStorage.Model>> byHid = () -> storageGuruModels.values().stream()
                    .filter(m -> m.getCategoryId() == HID)
                    .collect(Collectors.toList());
            cache.loadCategoryModels(HID);
            CategoryModelsCache guruCache = cache.getGuruModelsCache(HID);
            Assertions.assertThat(guruCache).isInstanceOf(DiskCategoryModelsCache.class);
            int cnt = (int) guruCache.getModelsStream().count();

            Assertions.assertThat(cnt).isEqualTo(byHid.get().size());

            ModelStorage.Model anyModel = byHid.get().get(0);
            anyModel = anyModel.toBuilder().setModifiedTs(anyModel.getModifiedTs() + 100).build();
            storageGuruModels.put(anyModel.getId(), anyModel);
            ModelStorage.Model newLocalModel = ModelStorage.Model.newBuilder()
                    .setId(100000)
                    .setVendorId(234234)
                    .setCategoryId(HID)
                    .setCurrentType("GURU")
                    .setModifiedTs(System.currentTimeMillis())
                    .build();
            ModelStorage.Model newExternalModel = ModelStorage.Model.newBuilder()
                    .setId(100001)
                    .setVendorId(134)
                    .setCategoryId(HID)
                    .setCurrentType("GURU")
                    .setModifiedTs(System.currentTimeMillis())
                    .build();

            storageGuruModels.put(newExternalModel.getId(), newExternalModel);
            cache.updateModelsInKnowledge(HID, Collections.singletonList(newLocalModel));
            assertThat(guruCache.getModel(newLocalModel.getVendorId(), newLocalModel.getId())).isEqualTo(newLocalModel);

            storageGuruModels.put(newLocalModel.getId(), newLocalModel);
            cache.loadCategoryModels(HID);
            Assertions.assertThat(guruCache.getModelsStream().count()).isEqualTo(
                    byHid.get().size()
            );
            for (Model model : byHid.get()) {
                assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isNotNull();
                assertThat(guruCache.getModel(model.getVendorId(), model.getId()).getModifiedTs())
                        .isEqualTo(model.getModifiedTs());
            }
        } finally {
            factory.tearDown();
        }
    }

    @Test
    public void testCacheReloadingAndInheritance() {
        Map<Long, ModelStorage.Model> guru = allModels.get(ModelStorage.ModelType.GURU.name());

        Map<Long, ModelStorage.Model> skus = allModels.get(ModelStorage.ModelType.SKU.name());

        ModelStorage.Model modelWithSkus = guru.values().stream()
                .filter(m -> AliasMakerUtils.getModelSKUIds(m).size() > 0)
                .findFirst().get();

        int hid = (int) modelWithSkus.getCategoryId();

        List<ModelStorage.ParameterValue> parameterValuesList = new ArrayList<>(modelWithSkus.getParameterValuesList());
        parameterValuesList.removeIf(p -> p.getXslName().equals("BarCode"));
        modelWithSkus = modelWithSkus.toBuilder().clearParameterValues()
                .addAllParameterValues(parameterValuesList)
                .build();
        updateModel(modelWithSkus);

        long skuId = AliasMakerUtils.getModelSKUIds(modelWithSkus).iterator().next();
        ModelStorage.Model sku = skus.get(skuId);
        if (sku == null) {
            sku = ModelStorage.Model.newBuilder()
                    .setCategoryId(hid)
                    .setCurrentType("SKU")
                    .build();
        }
        sku = addBarcode(sku);
        updateModel(sku);

        CategoryModelsCache guruCache = cache.getGuruModelsCache(hid);
        Model curModel = guruCache.getModel(modelWithSkus.getVendorId(), modelWithSkus.getId());
        assertThat(hasBarcode(curModel)).isTrue();
        updateModel(curModel);

        cache.loadCategoryModels(hid);

        guruCache = cache.getGuruModelsCache(hid);
        Model curModel2 = guruCache.getModel(modelWithSkus.getVendorId(), modelWithSkus.getId());
        assertThat(curModel2.getModifiedTs() > curModel.getModifiedTs());
        assertThat(hasBarcode(curModel2)).isTrue();
    }

    @Test
    public void testUpdateModelsInKnowledge() {
        Map<Long, ModelStorage.Model> guru = allModels.get(ModelStorage.ModelType.GURU.name());
        Map<Long, ModelStorage.Model> skus = allModels.get(ModelStorage.ModelType.SKU.name());

        ModelStorage.Model modelWithSkus = guru.values().stream()
                .filter(m -> AliasMakerUtils.getModelSKUIds(m).size() > 0)
                .findFirst().get();

        int hid = (int) modelWithSkus.getCategoryId();

        List<ModelStorage.ParameterValue> parameterValuesList = new ArrayList<>(modelWithSkus.getParameterValuesList());
        parameterValuesList.removeIf(p -> p.getXslName().equals("BarCode"));
        modelWithSkus = modelWithSkus.toBuilder().clearParameterValues()
                .addAllParameterValues(parameterValuesList)
                .build();
        modelWithSkus = updateModel(modelWithSkus).build();

        List<ModelStorage.Model> modelsToUpdate = new ArrayList<>();
        modelsToUpdate.add(modelWithSkus);

        for (long skuId : AliasMakerUtils.getModelSKUIds(modelWithSkus)) {
            ModelStorage.Model sku = skus.get(skuId);
            if (sku == null) {
                sku = ModelStorage.Model.newBuilder()
                        .setCategoryId(hid)
                        .setCurrentType("SKU")
                        .build();
            }
            sku = addBarcode(sku);
            modelsToUpdate.add(updateModel(sku).build());
        }

        modelsToUpdate = modelsToUpdate.stream().map(m -> m.toBuilder().setModifiedTs(m.getModifiedTs() + 1).build())
                .collect(Collectors.toList());

        cache.updateModelsInKnowledge(hid, modelsToUpdate);

        modelsToUpdate.forEach(mdl -> {
            Model model = getCurrentState(mdl);
            assertThat(model.getModifiedTs()).isEqualTo(mdl.getModifiedTs());
            if (model.getCurrentType().equals(ModelStorage.ModelType.GURU.name())) {
                assertThat(hasBarcode(model)).isTrue();
            }
        });
    }

    @Test
    public void testShrinkingModelOnUpdate() {
        int hid = Integer.MAX_VALUE;
        ModelStorage.Model guruModel = ModelStorage.Model.newBuilder()
                .setId(Integer.MAX_VALUE)
                .setCurrentType("GURU")
                .setCategoryId(hid)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("ahaha"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("XLPictureColorness"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("XL-Picture_10"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("search_aliases"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("search_aliases"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("aliases"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("aliases"))
                .addParameterValues(ModelStorage.ParameterValue.newBuilder().setXslName("aliases"))
                .addPictures(ModelStorage.Picture.getDefaultInstance())
                .addPictures(ModelStorage.Picture.getDefaultInstance())
                .build();

        ModelStorage.Model skuModel = guruModel.toBuilder()
                .setCurrentType("SKU")
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setId(guruModel.getId())
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .setCategoryId(hid)
                        .build())
                .build();

        Map<Long, ModelStorage.Model> guru = allModels.get(ModelStorage.ModelType.GURU.name());
        Map<Long, ModelStorage.Model> skus = allModels.get(ModelStorage.ModelType.SKU.name());
        guru.put(guruModel.getId(), guruModel);
        skus.put(skuModel.getId(), skuModel);
        cache.loadCategoryModels(hid);

        ModelStorage.Model newVersionGuru = getCurrentState(guruModel);

        assertThat(newVersionGuru.getPicturesList()).hasSize(1);
        assertThat(newVersionGuru.getParameterValuesList()).extracting(pv -> pv.getXslName())
                .containsExactlyInAnyOrder("ahaha", "aliases", "aliases", "aliases");

        ModelStorage.Model newVersionSku = getCurrentState(skuModel);

        assertThat(newVersionSku.getPicturesList()).hasSize(1);
        assertThat(newVersionSku.getParameterValuesList()).extracting(pv -> pv.getXslName())
                .containsExactlyInAnyOrder("ahaha");
    }

    private Model getCurrentState(Model model) {
        return cache.getModelsCache((int) model.getCategoryId(), model.getCurrentType())
                .getModel(model.getVendorId(), model.getId());
    }

    private boolean hasBarcode(ModelStorage.Model m) {
        return m.getParameterValuesList().stream().filter(p -> p.getXslName().equals("BarCode"))
                .findFirst().isPresent();
    }

    private ModelStorage.Model addBarcode(ModelStorage.Model m) {
        List<ModelStorage.ParameterValue> params = new ArrayList<>(m.getParameterValuesList());
        params.removeIf(p -> p.getXslName().equals("BarCode"));
        params.add(ModelStorage.ParameterValue.newBuilder()
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("01234").build())
                .setParamId(14202862)
                .setXslName("BarCode")
                .build()
        );
        return m.toBuilder().clearParameterValues().addAllParameterValues(params).build();
    }

    private void checkNewTimestamp(CategoryModelsCache zCache, ModelStorage.Model zModel) {
        ModelStorage.Model newVersion = zCache.getModel(Math.toIntExact(zModel.getVendorId()), zModel.getId());
        Assert.assertNotNull(newVersion);
        Assert.assertTrue(newVersion.getModifiedTs() > zModel.getModifiedTs());
    }

    private Map<Long, ModelStorage.Model> getByType(String type) {
        return allModels.computeIfAbsent(
                type, t -> new HashMap<>());
    }

    private ModelStorage.Model.Builder updateModel(ModelStorage.Model model) {
        long newModelId = model.getId();
        if (newModelId <= 0) {
            newModelId = idSeq++;
        }
        ModelStorage.Model.Builder modelBuilder = model.toBuilder();
        modelBuilder.setModifiedTs(model.getModifiedTs() + 1);
        modelBuilder.setId(newModelId);

        getByType(model.getCurrentType()).put(newModelId, modelBuilder.build());
        return modelBuilder;
    }

    private Collection<ModelStorage.Model> findByIds(Map<Long, ModelStorage.Model> models, List<Long> ids) {
        return ids.stream()
                .map(id -> models.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
