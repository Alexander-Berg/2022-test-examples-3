package ru.yandex.market.gutgin.tms.service.fast;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.service.ResponseProcessingResult;
import ru.yandex.market.gutgin.tms.service.goodcontent.ProcessedStatistic;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import static org.assertj.core.api.Assertions.assertThat;

public class FastCardResponseProcessorTest extends DBDcpStateGenerator {

    private FastCardResponseProcessor fastCardResponseProcessor;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.fastCardResponseProcessor = new FastCardResponseProcessor(gcSkuTicketDao);
    }

    @Test
    public void createMultipleModels() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2);
        long newModelId = 100L;
        ModelStorageHelper.SaveGroupResponse multipleModelsResponse = createMultipleModelsResponse(newModelId);

        ResponseProcessingResult result = fastCardResponseProcessor.processResults(multipleModelsResponse, gcSkuTickets);

        ProcessedStatistic processedStatistic = result.getProcessedStatistic();
        assertThat(processedStatistic.creation()).isEqualTo(2);
        assertThat(processedStatistic.updates()).isEqualTo(0);

        List<GcSkuTicket> all = gcSkuTicketDao.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(newModelId);
        assertThat(all).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
    }

    @Test
    public void createMultipleModelsWithOneModelFailed() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3);
        List<Long> ticketIds = gcSkuTickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList());
        long newModelId = 100L;
        ModelStorageHelper.SaveGroupResponse multipleModelsResponse = createMultipleModelsResponseWithFailure(
                newModelId, -2);

        ResponseProcessingResult result = fastCardResponseProcessor.processResults(multipleModelsResponse, gcSkuTickets);

        List<GcSkuTicket> all = gcSkuTicketDao.findAll();
        assertThat(all).hasSize(3);
        List<GcSkuTicket> successful = gcSkuTicketDao.fetchById(Arrays.asList(ticketIds.get(0), ticketIds.get(2)));
        assertThat(successful).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(newModelId);
        assertThat(successful).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
        GcSkuTicket failed = gcSkuTicketDao.findById(ticketIds.get(1));
        assertThat(failed).extracting(GcSkuTicket::getResultMboPskuId).isNull();
        assertThat(failed).extracting(GcSkuTicket::getStatus).isEqualTo(GcSkuTicketStatus.NEW);
        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
    }

    @Test
    public void createSingleModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1);
        long newModelId = 100L;
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createResponse(newModelId);

        ResponseProcessingResult result = fastCardResponseProcessor.processResults(saveGroupResponse, gcSkuTickets);

        List<GcSkuTicket> all = gcSkuTicketDao.findAll();
        assertThat(all).hasSize(1);
        GcSkuTicket gcSkuTicket = all.get(0);
        assertThat(gcSkuTicket).extracting(GcSkuTicket::getResultMboPskuId).isEqualTo(newModelId);
        assertThat(gcSkuTicket).extracting(GcSkuTicket::getStatus).isEqualTo(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
    }

    @Test
    public void failedToCreateSingleModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1);
        long newModelId = -1L; // mbo возвращает отрицательные id для моделей без id (не созданные ранее)
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createFailedResponse(newModelId);

        ResponseProcessingResult result = fastCardResponseProcessor.processResults(saveGroupResponse, gcSkuTickets);

        List<GcSkuTicket> all = gcSkuTicketDao.findAll();
        assertThat(all).hasSize(1);
        GcSkuTicket gcSkuTicket = all.get(0);
        assertThat(gcSkuTicket).extracting(GcSkuTicket::getResultMboPskuId).isNull();
        assertThat(gcSkuTicket).extracting(GcSkuTicket::getStatus).isEqualTo(GcSkuTicketStatus.NEW);
        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createResponse(long newModelId) {
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Operation completed successfully")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setCategoryId(100)
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build())
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createMultipleModelsResponse(long newModelId) {
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Operation completed successfully")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setCategoryId(100)
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setCategoryId(100)
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build())
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createMultipleModelsResponseWithFailure(long newModelId,
                                                                                         long failedModelId) {
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.PARTIALLY_OK)
                        .setStatusMessage("Saved partially: there are failed models")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setCategoryId(100)
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(failedModelId)
                                .addValidationError(
                                        ModelStorage.ValidationError.newBuilder()
                                                .setType(ModelStorage.ValidationErrorType.EMPTY_VENDOR)
                                                .setCritical(true)
                                                .setModelId(failedModelId)
                                                .build()
                                )
                                .setFailureModelId(0)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setCategoryId(100)
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build())
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createFailedResponse(long modelId) {
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setStatusMessage("Validation error occurred")
                        .addFailedModelId(modelId)
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(modelId)
                                .addValidationError(
                                        ModelStorage.ValidationError.newBuilder()
                                                .setType(ModelStorage.ValidationErrorType.EMPTY_VENDOR)
                                                .setCritical(true)
                                                .setModelId(modelId)
                                                .build()
                                )
                                .setFailureModelId(0)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .build())
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }
}