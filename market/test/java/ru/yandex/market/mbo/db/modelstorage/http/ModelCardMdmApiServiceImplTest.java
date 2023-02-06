package ru.yandex.market.mbo.db.modelstorage.http;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContext;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContextProvider;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditService;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.params.ModelParamsService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.db.params.guru.BaseGuruService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.MboAssertions;

/**
 * @author dmserebr
 * @date 05/12/2019
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class ModelCardMdmApiServiceImplTest {
    protected static final OperationStats STATS = new OperationStats();
    private static final Long SOME_USER = 2L;

    private ModelAuditService modelAuditService;
    private ModelAuditContextProvider modelAuditContextProvider;
    private ModelStorageServiceStub modelStorageServiceStub;
    private ModelStoreInterface modelStore;
    private AutoUser autoUser = new AutoUser(1L);
    private BaseGuruService guruService;
    private ModelStorageHealthService modelStorageHealthService;

    private ModelCardMdmApiServiceImpl modelCardMdmApiService;

    @Before
    public void before() {
        modelAuditService = Mockito.mock(ModelAuditService.class);
        modelAuditContextProvider = Mockito.mock(ModelAuditContextProvider.class);

        modelStorageServiceStub = new ModelStorageServiceStub();
        modelStore = new ModelStoreInterfaceStub(modelStorageServiceStub, autoUser);
        guruService = Mockito.mock(BaseGuruService.class);
        modelStorageHealthService = Mockito.mock(ModelStorageHealthService.class);

        ModelParamsService paramsService = Mockito.mock(ModelParamsService.class);
        Mockito.when(paramsService.syncModel(Mockito.any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        ModelAuditContext auditContext = Mockito.mock(ModelAuditContext.class);
        Mockito.when(auditContext.getStats()).thenReturn(STATS.getSaveStats());
        Mockito.when(modelAuditContextProvider.createContext()).thenReturn(auditContext);

        modelCardMdmApiService = new ModelCardMdmApiServiceImpl(
            modelAuditService, modelAuditContextProvider, modelStore,
            autoUser, guruService, modelStorageHealthService);
    }

    @Test
    public void testEmptyRequest() {
        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder().build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesCount()).isZero();
    }

    @Test
    public void testEmptyModelNoDifference() {
        ModelStorage.Model.Builder emptyModel = emptyModel();
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(emptyModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel)
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(emptyModel.getId())
                .setStatus(ModelStorage.OperationStatusType.NO_OP)
                .setStatusMessage("Model not changed: " + emptyModel.getId())
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).isEmpty();
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);
    }

    @Test
    public void testModelWithoutCategoryId() {
        ModelStorage.Model.Builder emptyModel = emptyModel();
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(emptyModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel.clearCategoryId())
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(emptyModel.getId())
                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                .setStatusMessage("Model does not have category_id")
                .setFailureModelId(emptyModel.getId())
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).isEmpty();
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);
    }

    @Test
    public void testMissingModel() {
        ModelStorage.Model.Builder emptyModel = emptyModel();

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel)
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(emptyModel.getId()).setFailureModelId(emptyModel.getId())
                .setStatus(ModelStorage.OperationStatusType.MODEL_NOT_FOUND).setType(ModelStorage.OperationType.CHANGE)
                .setStatusMessage("Failed to find before model: " + emptyModel.getId()).build()
        );
    }

    @Test
    public void testMissingModelWithMdmParameters() {
        ModelStorage.Model.Builder emptyModel = emptyModel()
            .addParameterValues(mdmParamValue(60, 600));

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel)
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60)).when(guruService).getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(emptyModel.getId()).setFailureModelId(emptyModel.getId())
                .setStatus(ModelStorage.OperationStatusType.MODEL_NOT_FOUND).setType(ModelStorage.OperationType.CHANGE)
                .setStatusMessage("Failed to find before model: " + emptyModel.getId()).build()
        );
    }

    @Test
    public void testNonMdmParametersInNewModelIgnored() {
        ModelStorage.Model.Builder emptyModel = emptyModel();
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(emptyModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel
                .addParameterValues(nonMdmParamValue(50, 500))
                .addParameterValues(nonMdmParamValue(60, 600)))
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(emptyModel.getId())
                .setStatus(ModelStorage.OperationStatusType.NO_OP)
                .setStatusMessage("Model not changed: " + emptyModel.getId())
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(0);
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);
    }

    @Test
    public void testNonMdmParametersInOldModelNotUpdated() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(nonMdmParamValue(60, 600));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(70, 700))
                .addParameterValues(nonMdmParamValue(80, 800)))
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.NO_OP)
                .setStatusMessage("Model not changed: " + origModel.getId())
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // not updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);
    }

    @Test
    public void testPictureParamsPreserved() {
        // Ранее существовал сценарий, при котором для мержа моделек использовался ModelMergeService. В нём есть
        // особенность, что он не мёржит картиночные параметры и теряет их. Данный тест исторический и убеждается, что
        // проблема не повторится.
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(nonMdmParamValue(60, 600).setXslName(XslNames.XL_PICTURE)); // картиночный параметр
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(mdmParamValue(70, 700)))
            .build();

        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());
        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // preserved
        MboAssertions.assertThat(modelFromStorage, 70).values(700L); // added
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMdmParamsUpdated() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601))
                .addParameterValues(mdmParamValue(70, 701)))
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(601L); // updated
        MboAssertions.assertThat(modelFromStorage, 70).values(701L); // updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMdmParamsWithNonMdmSourceInNewModelNotUpdated() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601)
                    .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB))
                .addParameterValues(mdmParamValue(70, 701)))
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // not updated
        MboAssertions.assertThat(modelFromStorage, 70).values(701L); // updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMdmParamsWithNonMdmSourceInOrigModelAndNewModelNotUpdated() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600)
                .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601)
                    .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB))
                .addParameterValues(mdmParamValue(70, 701)))
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // not updated
        MboAssertions.assertThat(modelFromStorage, 70).values(701L); // updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMultipleModels() {
        // some are updated, some are not (with errors / no ops)
        ModelStorage.Model.Builder model1 = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600));
        ModelStorage.Model.Builder model2 = emptyModel().setId(11L)
            .addParameterValues(nonMdmParamValue(50, 1500))
            .addParameterValues(mdmParamValue(60, 1600))
            .addParameterValues(mdmParamValue(70, 1700));
        ModelStorage.Model.Builder model3 = emptyModel().setId(12L)
            .addParameterValues(nonMdmParamValue(50, 2500))
            .addParameterValues(mdmParamValue(60, 2600))
            .addParameterValues(mdmParamValue(70, 2700));

        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(model1.build()), SOME_USER);
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(model2.build()), SOME_USER);
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(model3.build()), SOME_USER);

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        // preparations done, then do request
        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 500))
                .addParameterValues(mdmParamValue(60, 600)))
            .addModels(emptyModel().setId(11L)
                .addParameterValues(nonMdmParamValue(50, 1501))
                .addParameterValues(mdmParamValue(60, 1601))
                .addParameterValues(mdmParamValue(70, 1701)
                    .clearValueSource()))
            .addModels(emptyModel().setId(12L)
                .addParameterValues(nonMdmParamValue(50, 2501))
                .addParameterValues(mdmParamValue(60, 2600))
                .addParameterValues(mdmParamValue(70, 2701)))
            .addModels(emptyModel().setId(13L)
                .addParameterValues(nonMdmParamValue(50, 3501))
                .addParameterValues(mdmParamValue(60, 3600)))
            .build();

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(model1.getId())
                .setStatus(ModelStorage.OperationStatusType.NO_OP)
                .setStatusMessage("Model not changed: " + model1.getId())
                .setType(ModelStorage.OperationType.CHANGE).build(),
            ModelStorage.OperationStatus.newBuilder().setModelId(model2.getId())
                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                .setStatusMessage("Values of parameters [param70] do not have modification source")
                .setFailureModelId(model2.getId())
                .setType(ModelStorage.OperationType.CHANGE).build(),
            ModelStorage.OperationStatus.newBuilder().setModelId(model3.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build(),
            ModelStorage.OperationStatus.newBuilder().setModelId(13L)
                .setStatus(ModelStorage.OperationStatusType.MODEL_NOT_FOUND)
                .setType(ModelStorage.OperationType.CHANGE)
                .setFailureModelId(13L)
                .setStatusMessage("Failed to find before model: 13").build()
        );

        // assert storage
        Assertions.assertThat(modelStorageServiceStub.getAllModels().size()).isEqualTo(3);

        CommonModel modelFromStorage = modelStorageServiceStub.getModel(100L, 10L).get();
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // not updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);

        CommonModel modelFromStorage2 = modelStorageServiceStub.getModel(100L, 11L).get();
        Assertions.assertThat(modelFromStorage2.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage2, 50).values(1500L); // not updated
        MboAssertions.assertThat(modelFromStorage2, 60).values(1600L); // not updated
        MboAssertions.assertThat(modelFromStorage2, 70).values(1700L); // not updated
        Assertions.assertThat(modelFromStorage2.getModifiedUserId()).isEqualTo(SOME_USER);

        CommonModel modelFromStorage3 = modelStorageServiceStub.getModel(100L, 12L).get();
        Assertions.assertThat(modelFromStorage3.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage3, 50).values(2500L); // not updated
        MboAssertions.assertThat(modelFromStorage3, 60).values(2600L); // not updated (and should not have been)
        MboAssertions.assertThat(modelFromStorage3, 70).values(2701L); // updated
        Assertions.assertThat(modelFromStorage3.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testInvalidXslName() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601)))
            .build();

        ThinCategoryParam param50 = ParameterBuilder.builder().id(50).xsl("xsl50").endParameter();
        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).xsl("xsl60").mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param50, param60)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                .setFailureModelId(origModel.getId())
                .setStatusMessage("Model 10 has parameter values with incorrect XSL names: " +
                    "param 60: expected xsl60, actual param60")
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(600L); // not updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(SOME_USER);
    }

    @Test
    public void testMdmParamsNotDeleted() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601)))
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(3);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(601L); // updated
        MboAssertions.assertThat(modelFromStorage, 70).values(700L); // not deleted
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMdmParamsDeletedInMergeReplaceMode() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700))
            .addParameterValues(mdmParamValue(80, 800)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601)))
            .setMergeType(ModelStorage.MergeType.MERGE_REPLACE)
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        ThinCategoryParam param80 = ParameterBuilder.builder().id(80).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70, param80)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        // param values 700 & 800 are deleted
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(2);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        MboAssertions.assertThat(modelFromStorage, 60).values(601L); // updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testLastMdmParameterDeletedInMergeReplaceMode() {
        ModelStorage.Model.Builder origModel = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel.build()), SOME_USER);

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 500)))
            .setMergeType(ModelStorage.MergeType.MERGE_REPLACE)
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).containsExactly(
            ModelStorage.OperationStatus.newBuilder().setModelId(origModel.getId())
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE).build()
        );

        CommonModel modelFromStorage = modelStorageServiceStub.getAllModels().get(0);
        // param values 600 and 700 are deleted
        Assertions.assertThat(modelFromStorage.getParameterValues()).hasSize(1);
        MboAssertions.assertThat(modelFromStorage, 50).values(500L); // not updated
        Assertions.assertThat(modelFromStorage.getModifiedUserId()).isEqualTo(autoUser.getId());
    }

    @Test
    public void testMdmParamsUpdateModifiedError() {
        ModelStorage.Model.Builder origModel1 = emptyModel()
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel1.build()), SOME_USER);

        ModelStorage.Model.Builder origModel2 = emptyModel(20L)
            .addParameterValues(nonMdmParamValue(50, 500))
            .addParameterValues(mdmParamValue(60, 600))
            .addParameterValues(mdmParamValue(70, 700));
        modelStorageServiceStub.saveModel(ModelProtoConverter.convert(origModel2.build()), SOME_USER);

        modelStorageServiceStub.getAllModels().stream()
            .filter(m -> m.getId() == 20L)
            .forEach(m -> m.setModificationDate(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))));

        ModelStorage.SaveModelsRequest request = ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(emptyModel()
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601))
                .addParameterValues(mdmParamValue(70, 701)))
            .addModels(emptyModel(20L)
                .addParameterValues(nonMdmParamValue(50, 501))
                .addParameterValues(mdmParamValue(60, 601))
                .addParameterValues(mdmParamValue(70, 701)))
            .build();

        ThinCategoryParam param60 = ParameterBuilder.builder().id(60).mdmParameter(true).endParameter();
        ThinCategoryParam param70 = ParameterBuilder.builder().id(70).mdmParameter(true).endParameter();
        Mockito.doReturn(List.of(param60, param70)).when(guruService)
            .getModelThinPropertyTemplatesByHid(Mockito.anyLong());

        ModelStorage.OperationResponse response = modelCardMdmApiService.updateMdmParameters(request);

        Assertions.assertThat(response.getStatusesList()).contains(
            ModelStorage.OperationStatus.newBuilder()
                .setModelId(origModel1.getId())
                .setFailureModelId(origModel1.getId())
                .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                .setType(ModelStorage.OperationType.CHANGE)
                .setStatusMessage("Failed due to errors in other models of save group")
                .build()
        );
    }

    private static ModelStorage.Model.Builder emptyModel() {
        return emptyModel(10L);
    }

    private static ModelStorage.Model.Builder emptyModel(long id) {
        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setCategoryId(100L);
    }

    private static ModelStorage.ParameterValue.Builder nonMdmParamValue(int paramId, int optionId) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId).setXslName("param" + paramId).setOptionId(optionId)
            .setValueType(MboParameters.ValueType.ENUM).setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
    }

    private static ModelStorage.ParameterValue.Builder mdmParamValue(int paramId, int optionId) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(paramId).setXslName("param" + paramId).setOptionId(optionId)
            .setValueType(MboParameters.ValueType.ENUM).setValueSource(ModelStorage.ModificationSource.MDM);
    }
}
