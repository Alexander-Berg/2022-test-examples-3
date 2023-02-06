package ru.yandex.market.psku.postprocessor.service.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.bazinga.dna.ProcessModelsTask;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ExternalRequestResponseDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.RequestStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ExternalRequestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModelSaveServiceTest extends BaseDBTest {
    private static final long EXISTING_MODEL_ID = 200501L;
    private static final long EXISTING_PSKU_ID1 = 100501L;
    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;
    private static final long PARAM_ID_3 = 3L;
    private static final long PARAM_ID_4 = 4L;

    private static final long OWNER_ID_1 = 1L;
    private static final long OWNER_ID_2 = 2L;

    private static final long QUEUE_ID_1 = 33L;

    private final Timestamp ts = Timestamp.from(Instant.now());
    private DeletedMappingModels deletedMappingModel;
    private Map<Long, ModelStorage.Model> modelsMap;

    private ModelStorageHelper modelStorageHelper;
    private ModelSaveService modelSaveService;
    @Autowired
    private ExternalRequestResponseDao externalRequestResponseDao;

    @Before
    public void setUp() throws Exception {
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        modelSaveService = new ModelSaveService(modelStorageHelper, externalRequestResponseDao);

        deletedMappingModel = new DeletedMappingModels(EXISTING_MODEL_ID, CleanupStatus.READY_FOR_PROCESSING, ts, ts,
                QUEUE_ID_1);

        modelsMap = new HashMap<>();
        List<ModelStorage.ParameterValue> parameterValueList = List.of(generatePV(PARAM_ID_1, OWNER_ID_1));
        List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList =
                List.of(generateHypothesis(PARAM_ID_2, OWNER_ID_2));

        ModelStorage.Model model =
            generateModel(EXISTING_MODEL_ID, parameterValueList, parameterValueHypothesisList).toBuilder()
                .addRelations(ModelStorage.Relation.newBuilder()
                    .setId(EXISTING_PSKU_ID1)
                    .setType(ModelStorage.RelationType.SKU_MODEL).build()).build();

        ModelStorage.Model psku = generateModel(EXISTING_PSKU_ID1,
            List.of(generatePV(PARAM_ID_3, OWNER_ID_2)),
            List.of(generateHypothesis(PARAM_ID_4, OWNER_ID_1)));

        modelsMap.put(EXISTING_MODEL_ID, model);
        modelsMap.put(EXISTING_PSKU_ID1, psku);
        when(modelStorageHelper.findModelsWithChildrenMap(Set.of(EXISTING_MODEL_ID)))
            .thenReturn(Map.of(EXISTING_MODEL_ID, model, EXISTING_PSKU_ID1, psku));
    }

    @Test
    public void whenModelsAreSavedOkExpectResponseReceivedStatus() {

        mockSuccessfulCardApiAnswer();

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        modelSaveService.saveModels(context, models, (isSuccessful, response) -> {
            List<ExternalRequestResponse> responseStatuses =
                    externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
            assertThat(responseStatuses).hasSize(1);
            assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.RESPONSE_RECEIVED);
        });
    }

    @Test
    public void whenModelsAreSavedOkAndModelStatusUpdatedExpectFinishedStatusAndResultOk() {

        mockSuccessfulCardApiAnswer();

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        assertTrue(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    @Test
    public void whenModelsValidationErrorExpectFinishedStatusAndResultFalse() {

        mockBadCardApiAnswer();

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        assertFalse(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    @Test
    public void whenModelSaveRequestSentButStateIsUnknownExpectCreatedOnlyStatusAndResultFalse() {

        mockDisastrousCardApiResponse();

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        assertFalse(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.CREATED);
    }

    @Test
    public void whenFailSomeModelsExpectSaveBrokenAndOtherUnchanged() {
        // должно быть два вызова сохранения, в первом у всех моделей broken == false,
        // во втором у упавшей == true, у остальных остается false
        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, false,
                ModelStorage.OperationStatusType.VALIDATION_ERROR);

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> argumentCaptor =
                ArgumentCaptor.forClass(ModelCardApi.SaveModelsGroupRequest.class);

        verify(modelStorageHelper, times(2)).executeSaveModelRequest(argumentCaptor.capture());
        List<ModelCardApi.SaveModelsGroupRequest> capturesRequests = argumentCaptor.getAllValues();
        // в первом вызове сохранения broken не задан ни у одной модели
        assertThat(getModelFromRequest(capturesRequests.get(0), EXISTING_PSKU_ID1))
            .extracting(ModelStorage.Model::hasBroken)
            .isEqualTo(false);
        assertThat(getModelFromRequest(capturesRequests.get(1), EXISTING_MODEL_ID))
                .extracting(ModelStorage.Model::hasBroken)
                .isEqualTo(false);
        // в втором вызове заданы broken и published для упавшей модели
        // и не заданы для модели с FAILED_MODEL_IN_GROUP
        assertThat(getModelFromRequest(capturesRequests.get(1), EXISTING_PSKU_ID1))
                .extracting(ModelStorage.Model::getBroken)
                .isEqualTo(true);
        assertThat(getModelFromRequest(capturesRequests.get(1), EXISTING_PSKU_ID1))
                .extracting(ModelStorage.Model::getPublished)
                .isEqualTo(false);
        assertThat(getModelFromRequest(capturesRequests.get(1), EXISTING_MODEL_ID))
                .extracting(ModelStorage.Model::hasBroken)
                .isEqualTo(false);
        assertThat(getModelFromRequest(capturesRequests.get(1), EXISTING_MODEL_ID))
                .extracting(ModelStorage.Model::hasPublished)
                .isEqualTo(false);

        //в конечном итоге все успешно сохранилось
        assertTrue(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    @Test
    public void whenFailSomeModelsMoreThanThreeTimesExpectFinishedStatusAndResultFalse() {
        // должно быть три вызова сохранения, в конце концов все неуспешно
        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, true,
                ModelStorage.OperationStatusType.VALIDATION_ERROR);

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        // на все запросы ошибка -> делаем три попытки
        verify(modelStorageHelper, times(3))
                .executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class));

        //в конечном итоге как при исключении неуспех, только статус ответа в FINISHED
        assertFalse(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    @Test
    public void whenSomeModelsHasStatusToFailExpectFinishedStatusAndResultFalse() {
        // один из fail статусов => один вызов + как при исключении результат неуспешен, но статус ответа в FINISHED
        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, true,
                ModelStorage.OperationStatusType.MODEL_NOT_FOUND);

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        // на все запросы фатальная ошибка -> делаем 1 попытку
        verify(modelStorageHelper, times(1))
                .executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class));

        //в конечном итоге как при исключении неуспех, только статус ответа в FINISHED
        assertFalse(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    @Test
    public void whenModelModifiedStatusExpectSaveModelUnchanged() {
        // если сохранение вернуло статус MODEL_MODIFIED - повторая попытка без broken и изменения published
        mockBadCardApiAnswerOnModel(EXISTING_PSKU_ID1, true,
                ModelStorage.OperationStatusType.MODEL_MODIFIED);

        ProcessModelsTask.ProcessModelContext context = new ProcessModelsTask.ProcessModelContext(EXISTING_MODEL_ID,
                deletedMappingModel.getQueueId());
        ArrayList<ModelStorage.Model> models = new ArrayList<>(modelsMap.values());
        boolean result = modelSaveService.saveModels(context, models, null);

        ArgumentCaptor<ModelCardApi.SaveModelsGroupRequest> argumentCaptor =
                ArgumentCaptor.forClass(ModelCardApi.SaveModelsGroupRequest.class);

        verify(modelStorageHelper, times(3)).executeSaveModelRequest(argumentCaptor.capture());
        List<ModelCardApi.SaveModelsGroupRequest> capturesRequests = argumentCaptor.getAllValues();
        List<ModelStorage.Model> requestedModels = capturesRequests.stream()
                .map(r -> getModelFromRequest(r, EXISTING_PSKU_ID1))
                .collect(Collectors.toList());
        // во всех попытках broken не задан у модели с MODEL_MODIFIED
        assertThat(requestedModels)
            .extracting(ModelStorage.Model::hasBroken)
            .containsOnly(false);
        // во всех попытках published не задан у модели с MODEL_MODIFIED
        assertThat(requestedModels)
            .extracting(ModelStorage.Model::getPublished)
            .containsOnly(false);

        //в конечном итоге как при исключении неуспех (тк за 3 раза не сохранили), только статус ответа в FINISHED
        assertFalse(result);
        List<ExternalRequestResponse> responseStatuses = externalRequestResponseDao.fetchByModelId(EXISTING_MODEL_ID);
        assertThat(responseStatuses).hasSize(1);
        assertThat(responseStatuses.get(0).getStatus()).isEqualTo(RequestStatus.FINISHED);
    }

    private void mockSuccessfulCardApiAnswer() {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
            .thenAnswer(invocationOnMock ->
                new ModelStorageHelper.SaveGroupResponse(
                    invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                    ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                            .setStatus(ModelStorage.OperationStatusType.OK))
                        .build()
                )
            );
    }

    private void mockBadCardApiAnswer() {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenAnswer(invocationOnMock ->
                        new ModelStorageHelper.SaveGroupResponse(
                                invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                                ModelCardApi.SaveModelsGroupResponse.newBuilder()
                                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR))
                                        .build()
                        )
                );
    }

    private void mockDisastrousCardApiResponse() {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenThrow(new RuntimeException("Terrible things happened"));
    }

    private void mockBadCardApiAnswerOnModel(long modelId, boolean alwaysError,
                                             ModelStorage.OperationStatusType modelStatus) {
        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
            .thenAnswer(invocationOnMock -> {
                ModelCardApi.SaveModelsGroupRequest request =
                        invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class);

                if (request.getModelsRequestCount() != 1) {
                    throw new IllegalStateException("Expecting exactly 1 request for a group save");
                }

                // будет возвращать ошибку если есть модели с заданным id И
                // если alwaysError = true ИЛИ если интересующая модель еще не broken == true
                Predicate<ModelStorage.Model> brokenModelTest = m -> (m.getId() == modelId)
                        && (alwaysError || !m.hasBroken() || !m.getBroken());

                boolean isFailed = request.getModelsRequest(0).getModelsList()
                        .stream()
                        .anyMatch(brokenModelTest);
                List<ModelStorage.OperationStatus> modelStatuses = request.getModelsRequest(0).getModelsList()
                        .stream()
                        .map(m -> ModelStorage.OperationStatus.newBuilder()
                            .setModelId(m.getId())
                            .setModel(m)
                            // для ошибочных моделей ставим заданный статус
                            // для остальных OK если не было ошибок, FAILED_MODEL_IN_GROUP если ошибки в других моделях
                            .setStatus(brokenModelTest.test(m)
                                ? modelStatus
                                : (isFailed
                                    ? ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP
                                    : ModelStorage.OperationStatusType.OK)
                            )
                            .setType(ModelStorage.OperationType.CHANGE)
                            .build()
                        )
                        .collect(Collectors.toList());
                ModelStorage.OperationStatusType overallStatus = isFailed
                        ? ModelStorage.OperationStatusType.VALIDATION_ERROR
                        : ModelStorage.OperationStatusType.OK;

                ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                .setStatus(overallStatus)
                                .addAllRequestedModelsStatuses(modelStatuses)
                        )
                        .build();

                return new ModelStorageHelper.SaveGroupResponse(request, response);
            });
    }

    private ModelStorage.Model generateModel(long id,
                                             List<ModelStorage.ParameterValue> parameterValueList,
                                             List<ModelStorage.ParameterValueHypothesis> parameterValueHypothesisList) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .addAllParameterValues(parameterValueList)
                .addAllParameterValueHypothesis(parameterValueHypothesisList)
                .build();
    }

    private ModelStorage.ParameterValue generatePV(Long paramId, Long ownerId) {
        return ModelStorage.ParameterValue.newBuilder()
                .setParamId(paramId)
                .setOwnerId(ownerId)
                .build();
    }

    private ModelStorage.ParameterValueHypothesis generateHypothesis(Long paramId, Long ownerId) {
        return ModelStorage.ParameterValueHypothesis.newBuilder()
                .setParamId(paramId)
                .setOwnerId(ownerId)
                .build();
    }

    private ModelStorage.Model getModelFromRequest(ModelCardApi.SaveModelsGroupRequest request, long modelId) {
        return request.getModelsRequest(0).getModelsList().stream()
                .filter(m -> m.getId() == modelId)
                .findAny()
                .orElse(null);
    }
}
