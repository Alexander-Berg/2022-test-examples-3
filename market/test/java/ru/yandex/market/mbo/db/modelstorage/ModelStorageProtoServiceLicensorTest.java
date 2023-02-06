package ru.yandex.market.mbo.db.modelstorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.image.ModelImageService;

/**
 * @author ayratgdl
 * @since 28.09.18
 */
public class ModelStorageProtoServiceLicensorTest {
    private static final long CATEGORY_ID = 101;
    private static final long AUTO_MODEL_ID = 201;
    private static final long GURU_MODEL_ID = 202;
    private static final long USER_ID = 301;
    private static final long NAME_PARAMETER_ID = 401;
    private static final long LICENSOR_OPTION_ID_1 = 511;
    private static final long LICENSOR_OPTION_ID_2 = 512;
    private static final long FRANCHISE_OPTION_ID_1 = 521;
    private static final long PERSONAGE_OPTION_ID_1 = 531;

    private ModelStorageProtoService protoService;
    private StatsModelStorageServiceStub modelStorageService;
    private boolean validationErrorOnLicensorParameters;

    @Before
    public void setUp() throws Exception {
        modelStorageService = createModelStorageService();

        protoService = new ModelStorageProtoService();
        protoService.setStorageService(modelStorageService);
        protoService.setModelStorageHealthService(Mockito.mock(ModelStorageHealthService.class));
        protoService.setModelImageService(Mockito.mock(ModelImageService.class));

        GeneratedSkuService generatedSkuService = Mockito.mock(GeneratedSkuService.class);
        Mockito
            .when(generatedSkuService.createOrUpdateSku(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(
                new GroupOperationStatus(
                    new OperationStatus(OperationStatusType.OK, null)
                )
            );
        protoService.setGeneratedSkuService(generatedSkuService);

        validationErrorOnLicensorParameters = false;
    }

    @Test
    public void createModelFromGeneratedModelWithBadLicensorParameters() {
        ModelCardApi.SyncGuruModelsRequest request = buildCreateModelRequest();

        CommonModel model = buildAutoModel();
        modelStorageService.initializeWithModels(model);

        validationErrorOnLicensorParameters = true;

        ModelStorage.OperationResponse response = protoService.createGuruModels(request);
        ModelStorage.OperationStatus createStatus = response.getStatusesList().get(0);

        Assert.assertEquals(ModelStorage.OperationStatusType.OK, createStatus.getStatus());
        Mockito.verify(modelStorageService, Mockito.times(2)).saveModels(Mockito.any(ModelSaveGroup.class),
            Mockito.any(ModelSaveContext.class)
        );

        long guruModelId = createStatus.getRelatedModelId();
        CommonModel guruModel = modelStorageService.getModel(CATEGORY_ID, guruModelId).get();
        Assert.assertFalse(containsValue(guruModel, KnownIds.LICENSOR_PARAM_ID));
        Assert.assertFalse(containsValue(guruModel, KnownIds.FRANCHISE_PARAM_ID));
        Assert.assertFalse(containsValue(guruModel, KnownIds.PERSONAGE_PARAM_ID));
    }

    @Test
    public void createModelFromGeneratedModelWithGoodLicensorParameters() {
        ModelCardApi.SyncGuruModelsRequest request = buildCreateModelRequest();

        CommonModel model = buildAutoModel();
        modelStorageService.initializeWithModels(model);

        validationErrorOnLicensorParameters = false;

        ModelStorage.OperationResponse response = protoService.createGuruModels(request);
        ModelStorage.OperationStatus createStatus = response.getStatusesList().get(0);

        Assert.assertEquals(ModelStorage.OperationStatusType.OK, createStatus.getStatus());
        Mockito.verify(modelStorageService, Mockito.times(1)).saveModels(Mockito.any(ModelSaveGroup.class),
            Mockito.any(ModelSaveContext.class)
        );

        long guruModelId = createStatus.getRelatedModelId();
        CommonModel guruModel = modelStorageService.getModel(CATEGORY_ID, guruModelId).get();

        Assert.assertEquals(Arrays.asList(LICENSOR_OPTION_ID_1),
            getOptions(guruModel, KnownIds.LICENSOR_PARAM_ID));
        Assert.assertEquals(Arrays.asList(FRANCHISE_OPTION_ID_1),
            getOptions(guruModel, KnownIds.FRANCHISE_PARAM_ID));
        Assert.assertEquals(Arrays.asList(PERSONAGE_OPTION_ID_1),
            getOptions(guruModel, KnownIds.PERSONAGE_PARAM_ID));
    }

    @Test
    public void updateModelFromGeneratedModelWithBadLicensorParameters() {
        ModelCardApi.SyncGuruModelsRequest request = buildUpdateModelRequest();

        CommonModel guruModel = buildGuruModel();

        CommonModel autoModel = buildAutoModel();
        autoModel.addRelation(new ModelRelation(GURU_MODEL_ID, CATEGORY_ID, RelationType.SYNC_TARGET));
        modelStorageService.initializeWithModels(guruModel, autoModel);

        validationErrorOnLicensorParameters = true;

        ModelStorage.OperationResponse response = protoService.updateGuruModels(request);
        ModelStorage.OperationStatus createStatus = response.getStatusesList().get(0);

        Assert.assertEquals(ModelStorage.OperationStatusType.OK, createStatus.getStatus());
        Mockito.verify(modelStorageService, Mockito.times(2)).saveModel(Mockito.any(), Mockito.any());

        CommonModel updatedGuruModel = modelStorageService.getModel(CATEGORY_ID, GURU_MODEL_ID).get();
        Assert.assertFalse(containsValue(updatedGuruModel, KnownIds.LICENSOR_PARAM_ID));
        Assert.assertFalse(containsValue(updatedGuruModel, KnownIds.FRANCHISE_PARAM_ID));
        Assert.assertFalse(containsValue(updatedGuruModel, KnownIds.PERSONAGE_PARAM_ID));
    }

    @Test
    public void updateModelFromGeneratedModelWithGoodLicensorParameters() {
        ModelCardApi.SyncGuruModelsRequest request = buildUpdateModelRequest();

        CommonModel guruModel = buildGuruModel();
        CommonModel autoModel = buildAutoModel();
        autoModel.addRelation(new ModelRelation(GURU_MODEL_ID, CATEGORY_ID, RelationType.SYNC_TARGET));
        modelStorageService.initializeWithModels(guruModel, autoModel);

        validationErrorOnLicensorParameters = false;

        ModelStorage.OperationResponse response = protoService.updateGuruModels(request);
        ModelStorage.OperationStatus createStatus = response.getStatusesList().get(0);

        Assert.assertEquals(ModelStorage.OperationStatusType.OK, createStatus.getStatus());
        Mockito.verify(modelStorageService, Mockito.times(1)).saveModel(Mockito.any(), Mockito.any());

        CommonModel updatedGuruModel = modelStorageService.getModel(CATEGORY_ID, GURU_MODEL_ID).get();
        Assert.assertEquals(Arrays.asList(LICENSOR_OPTION_ID_1),
            getOptions(updatedGuruModel, KnownIds.LICENSOR_PARAM_ID));
        Assert.assertEquals(Arrays.asList(FRANCHISE_OPTION_ID_1),
            getOptions(updatedGuruModel, KnownIds.FRANCHISE_PARAM_ID));
        Assert.assertEquals(Arrays.asList(PERSONAGE_OPTION_ID_1),
            getOptions(updatedGuruModel, KnownIds.PERSONAGE_PARAM_ID));
    }

    private StatsModelStorageServiceStub createModelStorageService() {
        StatsModelStorageServiceStub baseModelService = new StatsModelStorageServiceStub();
        StatsModelStorageServiceStub modelService = Mockito.spy(baseModelService);
        Mockito.doReturn(Collections.emptySet())
            .when(modelService)
            .getFieldValues(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer(invocation -> {
                CommonModel model = invocation.getArgument(0);
                if (validationErrorOnLicensorParameters &&
                    (containsValue(model, KnownIds.LICENSOR_PARAM_ID)
                        || containsValue(model, KnownIds.FRANCHISE_PARAM_ID)
                        || containsValue(model, KnownIds.PERSONAGE_PARAM_ID))
                ) {
                    OperationStatus singleStatus =
                        new OperationStatus(OperationStatusType.VALIDATION_ERROR, OperationType.CHANGE, model.getId());
                    singleStatus.addValidationErrors(Collections.singletonList(
                        new ModelValidationError(model.getId(), ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                    ));
                    return new GroupOperationStatus(singleStatus);
                } else {
                    return baseModelService.saveModel(invocation.getArgument(0), invocation.getArgument(1));
                }
            }
        ).when(modelService).saveModel(Mockito.any(), Mockito.any());

        Mockito.doAnswer(invocation -> {
                ModelSaveGroup modelsGroup = invocation.getArgument(0);
                ModelSaveContext context = invocation.getArgument(1);
                List<CommonModel> guruModels =
                    modelsGroup.getModels().stream()
                        .filter(model -> model.getCurrentType() == CommonModel.Source.GURU)
                        .collect(Collectors.toList());
                for (CommonModel guru : guruModels) {
                    if (validationErrorOnLicensorParameters &&
                        (containsValue(guru, KnownIds.LICENSOR_PARAM_ID)
                            || containsValue(guru, KnownIds.FRANCHISE_PARAM_ID)
                            || containsValue(guru, KnownIds.PERSONAGE_PARAM_ID))
                    ) {
                        OperationStatus singleStatus = new OperationStatus(OperationStatusType.VALIDATION_ERROR,
                            OperationType.CHANGE, guru.getId());
                        singleStatus.addValidationErrors(Collections.singletonList(
                            new ModelValidationError(guru.getId(), ModelValidationError.ErrorType.ILLEGAL_LICENSOR)
                        ));
                        return new GroupOperationStatus(singleStatus);
                    }
                }
                return baseModelService.saveModels(modelsGroup, context);
            }
        ).when(modelService)
            .saveModels(Mockito.any(ModelSaveGroup.class), Mockito.any(ModelSaveContext.class));

        return modelService;
    }

    private ModelCardApi.SyncGuruModelsRequest buildCreateModelRequest() {
        return ModelCardApi.SyncGuruModelsRequest.newBuilder()
            .addSourceModelId(AUTO_MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .setSourceModelType(ModelStorage.ModelType.GENERATED)
            .setUserId(USER_ID)
            .build();
    }

    private ModelCardApi.SyncGuruModelsRequest buildUpdateModelRequest() {
        return ModelCardApi.SyncGuruModelsRequest.newBuilder()
            .addSourceModelId(AUTO_MODEL_ID)
            .setCategoryId(CATEGORY_ID)
            .setSourceModelType(ModelStorage.ModelType.GENERATED)
            .setUserId(USER_ID)
            .build();
    }

    private static CommonModel buildAutoModel() {
        CommonModel model = new CommonModel();
        model.setId(AUTO_MODEL_ID);
        model.setCategoryId(CATEGORY_ID);
        model.setCurrentType(CommonModel.Source.GENERATED);
        model.setSource(CommonModel.Source.GENERATED);
        model.addParameterValue(
            new ParameterValue(NAME_PARAMETER_ID, XslNames.NAME, Param.Type.STRING,
                ParameterValue.ValueBuilder.newBuilder()
                    .setStringValue(new Word(Language.RUSSIAN.getId(), "Model " + AUTO_MODEL_ID))
            )
        );
        model.addParameterValue(
            new ParameterValue(KnownIds.LICENSOR_PARAM_ID, "xsl_name_licensor", Param.Type.ENUM,
                ParameterValue.ValueBuilder.newBuilder()
                    .setOptionId(LICENSOR_OPTION_ID_1)
            )
        );
        model.addParameterValue(
            new ParameterValue(KnownIds.FRANCHISE_PARAM_ID, "xsl_name_franchise", Param.Type.ENUM,
                ParameterValue.ValueBuilder.newBuilder()
                    .setOptionId(FRANCHISE_OPTION_ID_1)
            )
        );
        model.addParameterValue(
            new ParameterValue(KnownIds.PERSONAGE_PARAM_ID, "xsl_name_personage", Param.Type.ENUM,
                ParameterValue.ValueBuilder.newBuilder()
                    .setOptionId(PERSONAGE_OPTION_ID_1)
            )
        );
        return model;
    }

    private CommonModel buildGuruModel() {
        CommonModel guruModel = new CommonModel();
        guruModel.setId(GURU_MODEL_ID);
        guruModel.setCategoryId(CATEGORY_ID);
        guruModel.setCurrentType(CommonModel.Source.GURU);
        guruModel.setSource(CommonModel.Source.GURU);
        guruModel.addParameterValue(
            new ParameterValue(NAME_PARAMETER_ID, XslNames.NAME, Param.Type.STRING,
                ParameterValue.ValueBuilder.newBuilder()
                    .setStringValue(new Word(Language.RUSSIAN.getId(), "Model " + GURU_MODEL_ID))
            )
        );
        guruModel.addParameterValue(
            new ParameterValue(KnownIds.LICENSOR_PARAM_ID, "xsl_name_licensor", Param.Type.ENUM,
                ParameterValue.ValueBuilder.newBuilder()
                    .setOptionId(LICENSOR_OPTION_ID_2)
            )
        );
        return guruModel;
    }

    private static boolean containsValue(CommonModel model, long paramId) {
        ParameterValues values = model.getParameterValues(paramId);
        return values != null && !values.isEmpty();
    }

    private static List<Long> getOptions(CommonModel model, long paramId) {
        ParameterValues values = model.getParameterValues(paramId);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().map(ParameterValue::getOptionId).collect(Collectors.toList());
    }
}
