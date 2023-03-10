package ru.yandex.market.mbo.db.modelstorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusConverter;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.merge.ModelMergeServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.params.ModelParamsService;
import ru.yandex.market.mbo.export.MboParameters.Category;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersConfig;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author ayratgdl
 * @since 25/12/2018
 */
public class ModelStorageProtoServiceCreateGuruModelsTest {
    private static final long CATEGORY_ID = 101;
    private static final long GENERATED_MODEL_ID = 201;
    private static final long GENERATED_SKU_ID = 202;
    private static final long EXISTENT_GURU_MODEL_ID = 203;
    private static final long VENDOR_MODEL_ID = 204;
    private static final long UID = 1;

    private ModelStorageProtoService modelService;

    private StatsModelStorageServiceStub modelStorageService;

    @Before
    public void setUp() throws Exception {
        modelService = new ModelStorageProtoService();

        modelStorageService = Mockito.spy(new StatsModelStorageServiceStub());
        modelService.setStorageService(modelStorageService);

        modelService.setModelStorageHealthService(Mockito.mock(ModelStorageHealthService.class));

        GeneratedSkuService skuService = new GeneratedSkuService();
        skuService.setStorageService(modelStorageService);
        CategoryParametersServiceClient parametersClient = Mockito.mock(CategoryParametersServiceClient.class);
        skuService.setParametersServiceClient(parametersClient);
        ModelMergeServiceImpl mergeService = new ModelMergeServiceImpl(new ModelParamsService(), null);
        skuService.setMergeService(mergeService);
        modelService.setGeneratedSkuService(skuService);

        Mockito.doReturn(Collections.emptySet())
            .when(modelStorageService).getFieldValues(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.when(parametersClient.getCategoryParameters(CATEGORY_ID)).thenAnswer(invocation ->
            new CategoryParametersConfig(
                Category.newBuilder()
                    .setHid(CATEGORY_ID)
                    .addAllParameter(Collections.emptyList())
                    .build()
            )
        );
    }

    // region 1. ?????????? ???? ???????????????? ?????????? ???????? ???????????? (?? ?????? ?????? ??????)
    // ?????????? createGuruModels ???????? ???????????????????? ???????? ???????????? (???? relation ?????? title).
    // ?? ???????? ???????????? ???????? ???????????? ???? ??????????????????
    // syncGuruModelsRequest.hasApplyForAllModels()  == false

    /**
     * ????????:
     * 1. ???????????????????? ?????????? createGuruModels ?? id generated-???????????? ?????????????? ???? ????????????????????
     * 2. ???????????????????? ?????????? ???? ???????????????? MODEL_NOT_FOUND
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: MODEL_NOT_FOUND
     *   type: CREATE
     *   model_id: 201
     *   failure_model_id: 201
     *   status_message: "Model not found"
     * }
     */
    @Test
    public void ifGeneratedModelHasNotFoundThenResponseStatusModelNotFound() {
        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .build();

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createFailureStatus(OperationStatusType.MODEL_NOT_FOUND, OperationType.CREATE, GENERATED_MODEL_ID,
                        GENERATED_MODEL_ID)
                )
                .build();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

    /**
     * ????????:
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? generated ?????????????? ?? ???????????? only_link_models=true
     * 2. ???????????????????????? ???????? ???????????? ???????????????? ???? ?????????????????????????????? generated ???????????? ???? ??????????????????
     * 3. ?????? ?????? ???????? ???????????? ???? ?????????????? ?? only_link_models=true, ???? ???????????????????? ?????????? ???? ???????????????? NO_OP
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     * only_link_models: true
     *
     * Response:
     * statuses {
     *   status: NO_OP
     *   type: CREATE
     *   model_id: 201
     *   status_message: "Related model not found"
     * }
     */
    @Test
    public void ifGuruDoesNotExistAndFlagOnlyLinkModelsIsTrueThenResponseRelatedModelNotFound() {
        CommonModel generated = CommonModelBuilder.newBuilder(GENERATED_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED)
            .title("GENERATED MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated);

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .setOnlyLinkModels(true)
                .build();

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.NO_OP, OperationType.CREATE, GENERATED_MODEL_ID,
                        null, "Related model not found")
                )
                .build();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

    /**
     * ????????:
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? generated ??????????????
     * 2. ???????????????????????? ???????? ???????????? ???????????????? ???? ?????????????????????????????? generated ???????????? ???? ??????????????????
     * 3. ?????????????????? ???????? ????????????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: {NEW_GURU_ID}
     *   status_message: "Model created and linked successfully."
     * }
     */
    @Test
    public void ifGuruDoesNotExistThenCreateGuruAndResponseOk() {
        CommonModel generated = CommonModelBuilder.newBuilder(GENERATED_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED)
            .title("GENERATED MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated);

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .build();

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);
        long guruId = getRelation(loadModel(GENERATED_MODEL_ID), RelationType.SYNC_TARGET);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, GENERATED_MODEL_ID,
                        guruId, "Model created and linked successfully.")
                )
                .build();
        Assert.assertEquals(expectedResponse, actualResponse);

        Assert.assertNotNull(loadModel(guruId));
    }

    /**
     * ????????:
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? generated ?????????????? ?????????????? ??????
     * 2. ???????????????????????? ???????? ???????????? ???????????????? ???? ?????????????????????????????? generated ???????????? ???? ??????????????????
     * 3. ?????????????????? ???????? ???????????? ?? ??????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: {NEW_GURU_ID}
     *   status_message: "Model created and linked successfully."
     * }
     */
    @Test
    public void ifGuruDoesNotExistThenCreateGuruWithSkuAndResponseOk() {
        CommonModel generated = CommonModelBuilder.newBuilder(GENERATED_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED)
            .title("GENERATED MODEL TITLE")
            .modelRelation(GENERATED_SKU_ID, CATEGORY_ID, RelationType.SKU_MODEL)
            .getModel();

        CommonModel generatedSku = CommonModelBuilder.newBuilder(GENERATED_SKU_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .modelRelation(GENERATED_MODEL_ID, CATEGORY_ID, RelationType.SKU_PARENT_MODEL)
            .getModel();

        modelStorageService.initializeWithModels(generated, generatedSku);

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .build();
        System.out.println("Request:\n" + request);

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);
        System.out.println("Response:\n" + actualResponse);
        long guruId = getRelation(loadModel(GENERATED_MODEL_ID), RelationType.SYNC_TARGET);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, GENERATED_MODEL_ID,
                        guruId, "Model created and linked successfully.")
                )
                .build();
        Assert.assertEquals(expectedResponse, actualResponse);

        Assert.assertNotNull(loadModel(guruId));

        long skuId = getRelation(loadModel(guruId), RelationType.SKU_MODEL);
        Assert.assertNotNull(loadModel(skuId));
    }
    //endregion

    // region 2. ?????????? ???? ???????????????????? ???????????? ?? ???????????????????????? ???????? ??????????????

    /**
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? generated ??????????????
     * 2. generated ???????????? ?????????? relation ???? ???????????????????????? ???????? ????????????
     * 3. ?? guru ???????????? ?????????????????????????????? relation ???? generated ????????????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: 203
     *   status_message: "Model linked successfully."
     * }
     */
    @Test
    public void ifGuruHaveFoundByRelationThenSetRelationFromGuruToGenerated() {
        CommonModel generated = CommonModelBuilder.newBuilder(GENERATED_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED)
            .title("GENERATED MODEL TITLE")
            .modelRelation(EXISTENT_GURU_MODEL_ID, CATEGORY_ID, RelationType.SYNC_TARGET)
            .getModel();

        CommonModel guru = CommonModelBuilder.newBuilder(EXISTENT_GURU_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title("GURU MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated, guru);

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .build();
        System.out.println("Request:");
        System.out.println(request);

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, GENERATED_MODEL_ID,
                        EXISTENT_GURU_MODEL_ID, "Model linked successfully.")
                )
                .build();

        Assert.assertEquals(expectedResponse, actualResponse);

        ModelRelation actualRelation = loadModel(EXISTENT_GURU_MODEL_ID).getRelation(GENERATED_MODEL_ID).get();
        ModelRelation expectedRelation = new ModelRelation(GENERATED_MODEL_ID, CATEGORY_ID, RelationType.SYNC_SOURCE);
        Assert.assertEquals(expectedRelation, actualRelation);
    }

    /**
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? generated ??????????????
     * 2. generated ???????????? ???? ?????????? relation ???? ???????????????????????? ???????? ????????????
     * 3. ?????????????? guru ???????????? ???? ??????????
     * 3. ?? guru ???????????? ?????????????????????????????? relation ???? generated ????????????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: GENERATED
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: 203
     *   status_message: "Model linked successfully."
     * }
     */
    @Test
    public void ifGuruHaveFoundByTitleThenSetRelationFromGuruToGenerated() {
        CommonModel generated = CommonModelBuilder.newBuilder(GENERATED_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GENERATED)
            .title("MODEL TITLE")
            .getModel();

        CommonModel guru = CommonModelBuilder.newBuilder(EXISTENT_GURU_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title("MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated, guru);

        Mockito.doReturn(Collections.singleton(guru.getId()))
            .when(modelStorageService).getFieldValues(
                 Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setUserId(UID)
                .build();
        System.out.println("Request:");
        System.out.println(request);

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, GENERATED_MODEL_ID,
                        EXISTENT_GURU_MODEL_ID, "Model linked successfully.")
                )
                .build();

        Assert.assertEquals(expectedResponse, actualResponse);

        ModelRelation actualRelation = loadModel(EXISTENT_GURU_MODEL_ID).getRelation(GENERATED_MODEL_ID).get();
        ModelRelation expectedRelation = new ModelRelation(GENERATED_MODEL_ID, CATEGORY_ID, RelationType.SYNC_SOURCE);
        Assert.assertEquals(expectedRelation, actualRelation);
    }
    // endregion

    // region 3. ?????????? ???? ???????????????????? ???????????? ????????????
    /**
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? VENDOR ??????????????
     * 2. VENDOR ???????????? ?????????? relation ???? ???????????????????????? ???????? ????????????
     * 3. ?? guru ???????????? ?????????????????????????????? relation ???? VENDOR ????????????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: VENDOR
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: 203
     *   status_message: "Model linked successfully."
     * }
     */
    @Test
    public void ifGuruHaveFoundByRelationThenSetRelationFromGuruToVendor() {
        CommonModel generated = CommonModelBuilder.newBuilder(VENDOR_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.VENDOR)
            .title("MODEL TITLE")
            .modelRelation(EXISTENT_GURU_MODEL_ID, CATEGORY_ID, RelationType.SYNC_TARGET)
            .getModel();

        CommonModel guru = CommonModelBuilder.newBuilder(EXISTENT_GURU_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title("MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated, guru);

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(VENDOR_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .setUserId(UID)
                .build();
        System.out.println("Request:");
        System.out.println(request);

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, VENDOR_MODEL_ID,
                        EXISTENT_GURU_MODEL_ID, "Model linked successfully.")
                )
                .build();

        Assert.assertEquals(expectedResponse, actualResponse);

        ModelRelation actualRelation = loadModel(EXISTENT_GURU_MODEL_ID).getRelation(VENDOR_MODEL_ID).get();
        ModelRelation expectedRelation = new ModelRelation(VENDOR_MODEL_ID, CATEGORY_ID, RelationType.SYNC_SOURCE);
        Assert.assertEquals(expectedRelation, actualRelation);
    }

    /**
     * ????????:
     * 1. ???????????????????? ?????????? createGuruModels ?? ???????????????????????? VENDOR ??????????????
     * 2. ???????? ????????????, ?????????????? ???? ?????????????????????????????? VENDOR ???????????? ????????????????????, ???? ?????????? ??????
     * 3. ???????? ???????????? ???? ?????????????????? ???? ??????????
     * 4. ?????????????????? ???????? ????????????
     *
     * Request:
     * category_id: 101
     * user_id: 1
     * source_model_type: VENDOR
     * source_model_id: 201
     *
     * Response:
     * statuses {
     *   status: OK
     *   type: CREATE
     *   model_id: 201
     *   related_model_id: {NEW_GURU_ID}
     *   status_message: "Model created and linked successfully."
     * }
     */
    @Test
    public void ifGuruExistsButDoesNotFoundByTitleThenCreateGuruAndResponseOk() {
        CommonModel generated = CommonModelBuilder.newBuilder(VENDOR_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.VENDOR)
            .title("MODEL TITLE")
            .getModel();

        CommonModel guru = CommonModelBuilder.newBuilder(EXISTENT_GURU_MODEL_ID, CATEGORY_ID)
            .currentType(CommonModel.Source.GURU)
            .title("MODEL TITLE")
            .getModel();

        modelStorageService.initializeWithModels(generated, guru);

        Mockito.doReturn(Collections.singleton(guru.getId()))
            .when(modelStorageService).getFieldValues(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        ModelCardApi.SyncGuruModelsRequest request =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(VENDOR_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .setUserId(UID)
                .build();

        ModelStorage.OperationResponse actualResponse = modelService.createGuruModels(request);
        long guruId = getRelation(loadModel(VENDOR_MODEL_ID), RelationType.SYNC_TARGET);

        ModelStorage.OperationResponse expectedResponse =
            ModelStorage.OperationResponse.newBuilder()
                .addStatuses(
                    createStatus(OperationStatusType.OK, OperationType.CREATE, VENDOR_MODEL_ID,
                        guruId, "Model created and linked successfully.")
                )
                .build();
        Assert.assertEquals(expectedResponse, actualResponse);

        Assert.assertNotNull(loadModel(guruId));
    }
    // endregion

    @Test
    public void checkSkipFirstPictureValidationFlagPassed() {
        ArgumentCaptor<ModelSaveContext> contextCaptor = ArgumentCaptor.forClass(ModelSaveContext.class);
        final int totalInteractions = 6;
        // with absent forceFirstPictureBackground (should be default false)
        ModelCardApi.SyncGuruModelsRequest requestWithoutFlag =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .setUserId(UID)
                .build();
        modelService.createGuruModels(requestWithoutFlag);
        modelService.updateGuruModels(requestWithoutFlag);
        // with forceFirstPictureBackground = false
        ModelCardApi.SyncGuruModelsRequest requestWithNotForced =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .setUserId(UID)
                .setSkipFirstPictureValidation(false)
                .build();
        modelService.createGuruModels(requestWithNotForced);
        modelService.updateGuruModels(requestWithNotForced);
        // with forceFirstPictureBackground = true
        ModelCardApi.SyncGuruModelsRequest requestWithForced =
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .addSourceModelId(GENERATED_MODEL_ID)
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .setUserId(UID)
                .setSkipFirstPictureValidation(true)
                .build();
        modelService.createGuruModels(requestWithForced);
        modelService.updateGuruModels(requestWithForced);

        // checking
        Mockito.verify(modelStorageService, Mockito.times(totalInteractions))
            .processModelsOfType(Mockito.anyLong(), Mockito.any(CommonModel.Source.class),
                Mockito.any(), Mockito.anyCollection(),
                contextCaptor.capture(), Mockito.any());
        List<Boolean> collectedCalls = contextCaptor.getAllValues().stream()
            .map(ModelSaveContext::isSkipFirstPictureValidation)
            .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(false, false, false, false, true, true), collectedCalls);
    }

    private ModelStorage.OperationStatus createStatus(OperationStatusType statusType, OperationType type,
                                                      long modelId, Long relatedModelId, String statusMessage) {
        OperationStatus status = new OperationStatus(statusType, type, modelId);
        status.setStatusMessage(statusMessage);
        if (relatedModelId != null) {
            status.setRelatedModelId(relatedModelId);
        }
        return OperationStatusConverter.convert(status);
    }

    private ModelStorage.OperationStatus createFailureStatus(OperationStatusType statusType, OperationType type,
                                                             long modelId, long failedModelId) {
        OperationStatus status = new OperationStatus(statusType, type, modelId);
        status.setFailureModelId(failedModelId);
        return OperationStatusConverter.convert(status);
    }

    private CommonModel loadModel(long modelId) {
        return modelStorageService.getModel(CATEGORY_ID, modelId).get();
    }

    private static long getRelation(CommonModel model, RelationType relationType) {
        return model.getRelations(relationType).get(0).getId();
    }
}

