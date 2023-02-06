package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.service.ResponseProcessingResult;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.SkuDuplicatesDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CSKUResponseProcessorTest extends DBDcpStateGenerator {

    private CSKUResponseProcessor cskuResponseProcessor;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.cskuResponseProcessor = new CSKUResponseProcessor(gcSkuTicketDao, mock(SkuDuplicatesDao.class));
    }

    @Test
    public void processOkResponse() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(3);

        ModelStorageHelper.SaveGroupResponse responce = createResponse(100);
        ResponseProcessingResult result = cskuResponseProcessor.processResults(responce, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(3);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(101L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @Test
    public void processOkResponseWithExtraModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        gcSkuTickets.get(0).setExistingMboPskuId(100L);

        ModelStorageHelper.SaveGroupResponse response = createResponseWithExtraModel(100, 10);
        ResponseProcessingResult result = cskuResponseProcessor.processResults(response, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(1);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(0);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(10L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(11L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @Test
    public void processOffersWithoutMappingAndWithoutGroup() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(3);
        gcSkuTickets.forEach(ticket -> ticket.setExistingMboPskuId(null));
        gcSkuTicketDao.update(gcSkuTickets);

        ModelStorageHelper.SaveGroupResponse responce = createResponse(100);
        ResponseProcessingResult result = cskuResponseProcessor.processResults(responce, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(3);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(101L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @Test
    public void processOffersWithBadResponseWithNegativeIds() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.forEach(ticket -> ticket.setExistingMboPskuId(null));
        gcSkuTicketDao.update(gcSkuTickets);

        Long ticketIdWithoutMapping = gcSkuTickets.get(0).getId();
        Long goodTicketId = gcSkuTickets.get(1).getId();
        ModelStorageHelper.SaveGroupResponse responce =
                createBadResponseWithNegativeModelsIds(ticketIdWithoutMapping, 100);
        ResponseProcessingResult result = cskuResponseProcessor.processResults(responce, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(1);

        List<GcSkuTicket> badTickets = gcSkuTicketDao.fetchById(ticketIdWithoutMapping);
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.NEW);

        List<GcSkuTicket> goodTickets = gcSkuTicketDao.fetchById(goodTicketId);
        assertThat(goodTickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertThat(goodTickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(101L);
        assertThat(goodTickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @Test
    public void processFailedResponse() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(3);
        Long ticket1 = gcSkuTickets.get(0).getId();
        Long badTicket = gcSkuTickets.get(1).getId();
        Long ticket3 = gcSkuTickets.get(2).getId();

        ModelStorageHelper.SaveGroupResponse responce = createBadResponse(100, 110);

        ResponseProcessingResult result = cskuResponseProcessor.processResults(responce, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(2);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(Arrays.asList(ticket1, ticket3));
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(101L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);

        List<GcSkuTicket> badTickets = gcSkuTicketDao.fetchById(Collections.singletonList(badTicket));
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.NEW);
    }


    @Test
    public void processGroupWithSuccessfulResponse() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(10L);
        gcSkuTickets.get(1).setExistingMboPskuId(11L);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = generateResponseForGroup(100, 10, 11);

        ResponseProcessingResult result =
                cskuResponseProcessor.processResultsForGroup(saveGroupResponse, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(2);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(0);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L, 11L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }


    @Test
    public void processGroupWithSuccessfulResponseAndDifferentParentModels() {
        List<GcSkuTicket> ticketsBefore = generateDBDcpInitialStateNew(2);
        ticketsBefore.get(0).setExistingMboPskuId(10L);
        ticketsBefore.get(1).setExistingMboPskuId(11L);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = generateResponseForGroupWithTwoModels(100, 110, 10,
                11);

        ResponseProcessingResult result =
                cskuResponseProcessor.processResultsForGroup(saveGroupResponse, ticketsBefore);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(2);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(0);

        List<GcSkuTicket> ticketsAfter = gcSkuTicketDao.findAll();
        assertThat(ticketsAfter).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L, 110L);
        assertModelIdBySkuID(ticketsAfter, 10, 100);
        assertModelIdBySkuID(ticketsAfter, 11, 110);
        assertThat(ticketsAfter).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L, 11L);
        assertThat(ticketsAfter).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @Test
    public void processGroupWithAllNewSkus() {
        List<GcSkuTicket> ticketsBefore = generateDBDcpInitialStateNew(2);
        ticketsBefore.get(0).setExistingMboPskuId(null);
        ticketsBefore.get(1).setExistingMboPskuId(null);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = generateResponseForGroup(100, 10, 11);

        ResponseProcessingResult result =
                cskuResponseProcessor.processResultsForGroup(saveGroupResponse, ticketsBefore);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);
        assertThat(result.getProcessedStatistic().creation()).isEqualTo(2);

        List<GcSkuTicket> ticketsAfter = gcSkuTicketDao.findAll();
        assertThat(ticketsAfter).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);
        assertModelIdBySkuID(ticketsAfter, 10, 100);
        assertModelIdBySkuID(ticketsAfter, 11, 100);
        assertThat(ticketsAfter).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L, 11L);
        assertThat(ticketsAfter).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);

    }

    private void assertModelIdBySkuID(List<GcSkuTicket> gcSkuTickets, int skuId, int modelId) {
        List<Long> models = gcSkuTickets.stream()
                .filter(gcSkuTicket -> gcSkuTicket.getResultMboPskuId() == skuId)
                .map(GcSkuTicket::getResultMboPmodelId)
                .collect(Collectors.toList());
        if (models.size() != 1) {
            throw new IllegalStateException("Expecting exactly one sku with id " + skuId);
        }
        assertThat(models.get(0)).isEqualTo(modelId);
    }

    @Test
    public void processGroupWithErrorResponse() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(10L);
        gcSkuTickets.get(1).setExistingMboPskuId(11L);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = generateBadResponseForGroup(100, 10, 11);

        ResponseProcessingResult result = cskuResponseProcessor.processResultsForGroup(saveGroupResponse, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);

        List<GcSkuTicket> badTickets = gcSkuTicketDao.findAll();
        //если в запросе для групп есть ошибка не трогаем тикеты (только все или ничего)
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.NEW);
    }

    @Test
    public void whenResponseHasSkuDefiningConflictThenReturnSpecialCase() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(3);
        gcSkuTickets.get(0).setExistingMboPskuId(10L);
        gcSkuTickets.get(1).setExistingMboPskuId(11L);
        gcSkuTickets.get(2).setExistingMboPskuId(12L);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse =
                generateBadResponseForGroupWithSkuDefiningParamsConflict(100, 10, 11, 12);

        ResponseProcessingResult result = cskuResponseProcessor.processResultsForGroup(saveGroupResponse, gcSkuTickets);

        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
        assertThat(result.hasSkuDefiningParamsConflict()).isTrue();
        assertThat(result.getModelsWithConflict()).containsOnly(10L, 11L);
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);

        List<GcSkuTicket> badTickets = gcSkuTicketDao.findAll();
        //если в запросе для групп есть ошибка не трогаем тикеты (только все или ничего)
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();
        assertThat(badTickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.NEW);
    }

    @Test
    public void whenMskuWithErrorDontUpdateAnyTicket() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(10L);
        gcSkuTickets.get(1).setExistingMboPskuId(11L);
        gcSkuTickets.forEach(ticket -> ticket.setType(GcSkuTicketType.CSKU_MSKU));
        gcSkuTicketDao.update(gcSkuTickets);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse =
                generateBadMskuGroupResponse(100, 200, 10, 11);

        ResponseProcessingResult result = cskuResponseProcessor.processResultsForGroup(saveGroupResponse,
                gcSkuTickets, true);

        assertThat(result.isAllTicketsInBatchSuccessful()).isFalse();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getModelsWithConflict()).isEmpty();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(0);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnlyNulls();
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.NEW);
    }

    @Test
    public void whenMskuDontExpectTargetModel() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2);
        gcSkuTickets.get(0).setExistingMboPskuId(10L);
        gcSkuTickets.get(1).setExistingMboPskuId(11L);
        gcSkuTickets.forEach(ticket -> ticket.setType(GcSkuTicketType.CSKU_MSKU));
        gcSkuTicketDao.update(gcSkuTickets);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse =
                generateMskuGroupResponse(100, 200, 10, 11);

        ResponseProcessingResult result = cskuResponseProcessor.processResultsForGroup(saveGroupResponse,
                gcSkuTickets, true);

        assertThat(result.isAllTicketsInBatchSuccessful()).isTrue();
        assertThat(result.hasSkuDefiningParamsConflict()).isFalse();
        assertThat(result.getModelsWithConflict()).isEmpty();
        assertThat(result.getProcessedStatistic().updates()).isEqualTo(2);

        List<GcSkuTicket> tickets = gcSkuTicketDao.findAll();
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L, 200L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L, 11L);
        assertThat(tickets).filteredOn(ticket -> ticket.getExistingMboPskuId() == 10L)
                .extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L);
        assertThat(tickets).filteredOn(ticket -> ticket.getExistingMboPskuId() == 10L)
                .extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(100L);

        assertThat(tickets).filteredOn(ticket -> ticket.getExistingMboPskuId() == 11L)
                .extracting(GcSkuTicket::getResultMboPskuId).containsOnly(11L);
        assertThat(tickets).filteredOn(ticket -> ticket.getExistingMboPskuId() == 11L)
                .extracting(GcSkuTicket::getResultMboPmodelId).containsOnly(200L);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(10L, 11L);
        assertThat(tickets).extracting(GcSkuTicket::getStatus).containsOnly(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createResponse(long modelId) {
        ModelCardApi.SaveModelsGroupResponse.Builder builder = ModelCardApi.SaveModelsGroupResponse.newBuilder();
        ModelCardApi.SaveModelsGroupOperationResponse responce1 = generateResponseForOneTicket(modelId);
        ModelCardApi.SaveModelsGroupOperationResponse responce2 = generateResponseForOneTicket(modelId);
        ModelCardApi.SaveModelsGroupOperationResponse responce3 = generateResponseForOneTicket(modelId);
        builder.addResponse(responce1)
                .addResponse(responce2)
                .addResponse(responce3)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, builder.build());
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createResponseWithExtraModel(long oldModelId, long newModelId) {
        ModelCardApi.SaveModelsGroupResponse.Builder builder = ModelCardApi.SaveModelsGroupResponse.newBuilder();
        builder.addResponse(responseWithExtraModel(oldModelId, newModelId))
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, builder.build());
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createBadResponseWithNegativeModelsIds(long ticketId1,
                                                                                        long skuId) {
        ModelCardApi.SaveModelsGroupResponse.Builder builder = ModelCardApi.SaveModelsGroupResponse.newBuilder();
        ModelCardApi.SaveModelsGroupOperationResponse responce1 = generateFailedResponseForNewSku(ticketId1);
        ModelCardApi.SaveModelsGroupOperationResponse responce2 = generateResponseForOneTicket(skuId);
        builder
                .addResponse(responce1)
                .addResponse(responce2)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, builder.build());
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createBadResponse(long newModelId, long badModelId) {
        ModelCardApi.SaveModelsGroupResponse.Builder builder = ModelCardApi.SaveModelsGroupResponse.newBuilder();
        ModelCardApi.SaveModelsGroupOperationResponse response1 = generateResponseForOneTicket(newModelId);
        ModelCardApi.SaveModelsGroupOperationResponse response2 = generateFailedResponseForOneTicket(badModelId);
        ModelCardApi.SaveModelsGroupOperationResponse response3 = generateResponseForOneTicket(newModelId);
        builder.addResponse(response1)
                .addResponse(response2)
                .addResponse(response3)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, builder.build());
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupOperationResponse generateResponseForOneTicket(long newModelId) {
        ModelCardApi.SaveModelsGroupOperationResponse build = ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setStatusMessage("Operation completed successfully")
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(newModelId)
                        .setModel(ModelStorage.Model.newBuilder()
                                .setId(newModelId)
                                .setCategoryId(100)
                                .addRelations(ModelStorage.Relation.newBuilder()
                                        .setId(newModelId + 1)
                                        .build())
                                .build())
                        .setStatusMessage("Operation completed successfully")
                        .build())
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(newModelId + 1)
                        .setModel(ModelStorage.Model.newBuilder()
                                .setId(newModelId + 1)
                                .setCategoryId(100)
                                .addRelations(ModelStorage.Relation.newBuilder()
                                        .setId(newModelId)
                                        .build())
                                .build())
                        .setStatusMessage("Operation completed successfully")
                        .build())
                .build();
        return build;
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupOperationResponse responseWithExtraModel(long oldModelId, long newModelId) {
        long skuId = newModelId + 1;
        ModelCardApi.SaveModelsGroupOperationResponse build = ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setStatusMessage("Operation completed successfully")
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(oldModelId)
                        .setModel(ModelStorage.Model.newBuilder()
                                .setId(oldModelId)
                                .setCategoryId(100)
                                .addRelations(ModelStorage.Relation.newBuilder()
                                        .setId(1000000)
                                        .build())
                                .build())
                        .setStatusMessage("Operation completed successfully")
                        .build())
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(skuId)
                        .setModel(ModelStorage.Model.newBuilder()
                                .setId(skuId)
                                .setCategoryId(100)
                                .addRelations(ModelStorage.Relation.newBuilder()
                                        .setId(newModelId)
                                        .build())
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
                                .addRelations(ModelStorage.Relation.newBuilder()
                                        .setId(skuId)
                                        .build())
                                .build())
                        .setStatusMessage("Operation completed successfully")
                        .build())
                .build();
        return build;
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateResponseForGroup(long newModelId,
                                                                          long sku1,
                                                                          long sku2) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Operation completed successfully")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(newModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(newModelId)
                                        .setSourceType(ModelStorage.ModelType.PARTNER.name())
                                        .setCurrentType(ModelStorage.ModelType.GURU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku1)
                                                .build())
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku2)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku1)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(sku1)
                                        .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(newModelId)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku2)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setId(sku2)
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(newModelId)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateResponseForGroupWithTwoModels(
            long targetModelId,
            long modelId2,
            long sku1,
            long sku2) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Operation completed successfully")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(targetModelId)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(targetModelId)
                                        .setCategoryId(100)
                                        .setSourceType(ModelStorage.ModelType.PARTNER.name())
                                        .setCurrentType(ModelStorage.ModelType.GURU.name())
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku1)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku1)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(sku1)
                                        .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(targetModelId)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku2)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(sku2)
                                        .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(modelId2)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(modelId2)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(modelId2)
                                        .setSourceType(ModelStorage.ModelType.PARTNER.name())
                                        .setCurrentType(ModelStorage.ModelType.GURU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku2)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateMskuGroupResponse(
            long modelId1,
            long modelId2,
            long sku1,
            long sku2) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Operation completed successfully")
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku1)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(sku1)
                                        .setSourceType(ModelStorage.ModelType.SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(modelId1)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(sku2)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(sku2)
                                        .setSourceType(ModelStorage.ModelType.SKU.name())
                                        .setCurrentType(ModelStorage.ModelType.SKU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(modelId2)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(modelId1)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(modelId1)
                                        .setCategoryId(100)
                                        .setSourceType(ModelStorage.ModelType.GURU.name())
                                        .setCurrentType(ModelStorage.ModelType.GURU.name())
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku1)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(modelId2)
                                .setModel(ModelStorage.Model.newBuilder()
                                        .setId(modelId2)
                                        .setSourceType(ModelStorage.ModelType.GURU.name())
                                        .setCurrentType(ModelStorage.ModelType.GURU.name())
                                        .setCategoryId(100)
                                        .addRelations(ModelStorage.Relation.newBuilder()
                                                .setId(sku2)
                                                .build())
                                        .build())
                                .setStatusMessage("Operation completed successfully")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateBadMskuGroupResponse(
            long modelId1,
            long modelId2,
            long sku1,
            long sku2) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setStatusMessage("Operation not completed successfully")
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                                .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                                .setSubtype(ModelStorage.ValidationErrorSubtype.DEFINITION_MISMATCH)
                                .setModelId(sku1)
                                .setCritical(true)
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setModelId(sku2)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(modelId1)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(modelId2)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateBadResponseForGroup(long modelId,
                                                                             long okSku,
                                                                             long errorSku) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setStatusMessage("Operation completed successfully")
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                                .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                                .setSubtype(ModelStorage.ValidationErrorSubtype.DEFINITION_MISMATCH)
                                .setModelId(errorSku)
                                .setCritical(true)
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setModelId(errorSku)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(okSku)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(modelId)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse generateBadResponseForGroupWithSkuDefiningParamsConflict(
            long modelId,
            long errorSku1,
            long errorSku2,
            long okSku
    ) {
        ModelCardApi.SaveModelsGroupOperationResponse response =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setStatusMessage("Operation completed successfully")
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                                .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                                .setSubtype(ModelStorage.ValidationErrorSubtype.DUPLICATE_SKU_PARAM_VALUE)
                                .setModelId(errorSku1)
                                .setCritical(true)
                                .build())
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                                .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                                .setSubtype(ModelStorage.ValidationErrorSubtype.DUPLICATE_SKU_PARAM_VALUE)
                                .setModelId(errorSku2)
                                .setCritical(true)
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setModelId(errorSku1)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                .setModelId(errorSku2)
                                .setStatusMessage("Validation error occurred")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(okSku)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                                .setModelId(modelId)
                                .setStatusMessage("There are failed models in group")
                                .build())
                        .build();

        ModelCardApi.SaveModelsGroupResponse result = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(response)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, result);
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupOperationResponse generateFailedResponseForOneTicket(long newModelId) {
        ModelCardApi.SaveModelsGroupOperationResponse build = ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(newModelId)
                        .build())
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setModelId(newModelId)
                        .setModel(ModelStorage.Model.newBuilder()
                                .setId(newModelId)
                                .setCategoryId(100)
                                .build())
                        .build())
                .build();
        return build;
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupOperationResponse generateFailedResponseForNewSku(long ticketId) {
        long skuId = -ticketId;
        long modelId = -ticketId * 10;
        ModelCardApi.SaveModelsGroupOperationResponse build = ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                .setStatusMessage("Validation error occurred")
                .addFailedModelId(skuId)
                .addFailedModelId(modelId)
                .addValidationError(
                        ModelStorage.ValidationError.newBuilder()
                        .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                        .setSubtype(ModelStorage.ValidationErrorSubtype.DUPLICATE_SKU_PARAM_VALUE)
                        .setModelId(modelId)
                        .setCritical(true)
                        .build())
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                        .setModelId(modelId)
                        .setStatusMessage("Validation error occurred")
                        .setFailureModelId(modelId)
                        .addValidationError(
                                ModelStorage.ValidationError.newBuilder()
                                        .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                                        .setSubtype(ModelStorage.ValidationErrorSubtype.DUPLICATE_SKU_PARAM_VALUE)
                                        .setModelId(modelId)
                                        .setCritical(true)
                                        .build())
                        .build())
                .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                        .setModelId(skuId)
                        .setFailureModelId(skuId)
                        .setStatusMessage("There are failed models in group")
                        .build())
                .build();
        return build;
    }
}
