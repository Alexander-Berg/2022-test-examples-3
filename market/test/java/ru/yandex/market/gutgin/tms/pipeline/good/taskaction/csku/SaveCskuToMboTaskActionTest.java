package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Stubber;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.db.dao.pipeline.DataBucketMessagesService;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.service.ResponseProcessingResult;
import ru.yandex.market.gutgin.tms.service.goodcontent.ProcessedStatistic;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcExternalServiceRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestType;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.GcTicketRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcExternalServiceRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcTicketRequest;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.SaveCskuToMboTaskAction.BATCH_SIZE;

public class SaveCskuToMboTaskActionTest extends DBDcpStateGenerator {

    private static final long PARENT_MODEL_ID = 4L;
    private static final long SKU_1_ID = 1L;
    private static final long SKU_2_ID = 2L;
    private static final long SKU_3_ID = 3L;
    private static final long TS = 123456789L;

    private ModelStorageHelper modelStorageHelper;
    private SaveCskuToMboTaskAction saveCskuCardsToMbo;
    private CSKUDataPreparation cskuDataPreparation;
    private CSKURequestCreator cskuRequestCreator;
    private CSKUResponseProcessor cskuResponseProcessor;
    @Autowired
    private GcTicketRequestDao gcTicketRequestDao;
    @Autowired
    private GcExternalServiceRequestDao gcExternalServiceRequestDao;
    @Autowired
    private ProtocolMessageDao protocolMessageDao;
    private DataBucketMessagesService dataBucketMessagesService;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.modelStorageHelper = mock(ModelStorageHelper.class);
        this.cskuDataPreparation = mock(CSKUDataPreparation.class);
        this.cskuRequestCreator = mock(CSKURequestCreator.class);
        this.cskuResponseProcessor = mock(CSKUResponseProcessor.class);
        this.dataBucketMessagesService = new DataBucketMessagesService(
                configuration,
                protocolMessageDao
        );
        this.saveCskuCardsToMbo = new SaveCskuToMboTaskAction(
                gcSkuTicketDao,
                modelStorageHelper,
                gcExternalServiceRequestDao,
                gcSkuValidationDao,
                cskuRequestCreator,
                cskuResponseProcessor,
                cskuDataPreparation,
                BATCH_SIZE);
    }

    @Test
    public void whenFailedModelsThenFailTask() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2 * BATCH_SIZE + 1);
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED));
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setDcpGroupId(null));
        gcSkuTicketDao.update(gcSkuTickets);

        mockDataPreparation(gcSkuTickets);
        doReturn(request()).when(cskuRequestCreator).createRequest(any());
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(true),
                new ResponseProcessingResult(false),
                new ResponseProcessingResult(true)
        ).when(cskuResponseProcessor).processResults(any(), any());
        ProcessTaskResult<ProcessDataBucketData> result =
                saveCskuCardsToMbo.runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));

        assertGcExternalRequest(3);
        assertThat(result.hasProblems()).isTrue();
        assertThat(result.getResult()).isNull();
    }

    @Test
    public void onTaskRestartDontTouchAlreadySavedTickets() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(BATCH_SIZE + 1);
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED));
        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setDcpGroupId(null));
        gcSkuTicketDao.update(gcSkuTickets);

        mockDataPreparation(gcSkuTickets);
        doReturn(request()).when(cskuRequestCreator).createRequestForGroup(any(), any());
        doReturn(request()).when(cskuRequestCreator).createRequest(any());
        doReturn(createSaveGroupResponse())
                .doThrow(new RuntimeException()) //эмулируем падение на 2 батче (500-ка мбо, например)
                .doReturn(createSaveGroupResponse()) // отправка 1 тикета после рестарта таски
                .when(modelStorageHelper).executeSaveModelRequest(any());
        doReturn(new ResponseProcessingResult(true)).when(cskuResponseProcessor)
                .processResults(any(), any());

        Long dataBucket = gcSkuTickets.stream().map(GcSkuTicket::getDataBucketId).findFirst()
                .orElseThrow(IllegalStateException::new);

        //первый запуск с падением
        Assertions.assertThatThrownBy(() -> saveCskuCardsToMbo.doRun(new ProcessDataBucketData(dataBucket)))
                .isInstanceOf(RuntimeException.class);
        verify(cskuResponseProcessor, times(1))
                .processResults(any(), argThat(list -> list.size() == BATCH_SIZE));//обновляем статусы для 1 батча

        List<GcTicketRequest> requestToTicket = gcTicketRequestDao.findAll();
        assertThat(requestToTicket).hasSize(BATCH_SIZE + 1);
        //реквест для сломанного тикета НЕ откатился транзакцией
        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        assertThat(requests).hasSize(2);
        assertThat(requests).extracting(GcExternalServiceRequest::getStatus).containsExactlyInAnyOrder(
                GcExternalRequestStatus.FINISHED, //для первого батча обновили статус реквеста
                GcExternalRequestStatus.CREATED //для второго батча сохранили реквест, но не обновили статус
        );
        System.out.println("requests = " + requests);
    }

    @Test
    public void whenDatabucketHasOffersWithGroupTheyProcessedSeparately() {
        int dcpGroupId = 1000;
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(4);
        gcSkuTickets.forEach(ticket -> ticket.setStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED));
        gcSkuTickets.forEach(ticket -> ticket.setDcpGroupId(ticket.getId() % 2 == 0 ? null : dcpGroupId));
        gcSkuTicketDao.update(gcSkuTickets);
        List<GcSkuTicket> tickets1 =
                gcSkuTickets.stream().filter(ticket -> ticket.getDcpGroupId() == null)
                        .collect(Collectors.toList());
        List<GcSkuTicket> tickets2 =
                gcSkuTickets.stream().filter(ticket -> ticket.getDcpGroupId() != null)
                        .collect(Collectors.toList());

        doReturn(ticketWrappers(tickets1))
                .when(cskuDataPreparation)
                .collectDataForRequest(eq(tickets1));
        doReturn(groupedTicketWrapper(tickets2))
                .when(cskuDataPreparation)
                .collectDataForRequest(eq(tickets2));
        doReturn(request()).when(cskuRequestCreator).createRequest(any());
        doReturn(request()).when(cskuRequestCreator).createRequestForGroup(any(), any());
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(true)).when(cskuResponseProcessor).processResults(any(), any());
        doReturn(new ResponseProcessingResult(true)).when(cskuResponseProcessor).processResultsForGroup(any(), any(),
                anyBoolean());

        ProcessTaskResult<ProcessDataBucketData> result =
                saveCskuCardsToMbo.runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));

        assertGcExternalRequest(2);
        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult()).isNotNull();
    }

    @Test
    public void whenRequestHasConflictTryToResolveIt() {
        List<Long> modelsWithConflict = Arrays.asList(1L, 2L);

        int dcpGroupId = 1000;
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(4);
        tickets.forEach(ticket -> ticket.setDcpGroupId(dcpGroupId));
        gcSkuTicketDao.update(tickets);

        doReturn(groupedTicketWrapper(tickets))
                .when(cskuDataPreparation)
                .collectDataForRequest(any());
        //request with conflict
        ModelCardApi.SaveModelsGroupRequest request = request();
        doReturn(request).when(cskuRequestCreator).createRequestForGroup(any(), argThat(List::isEmpty));
        //conflict free request
        ModelCardApi.SaveModelsGroupRequest conflictFreeRequest = conflictFreeRequest();
        doReturn(conflictFreeRequest).when(cskuRequestCreator).createRequestForGroup(any(), eq(modelsWithConflict));

        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createSaveGroupResponse();


        doReturn(saveGroupResponse).when(modelStorageHelper).executeSaveModelRequest(any());
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(false, new ProcessedStatistic.BasicProcessedStatistic(),
                modelsWithConflict))
                .when(cskuResponseProcessor).processResultsForGroup(any(), any(), anyBoolean());
        doReturn(new ResponseProcessingResult(true))
                .when(cskuResponseProcessor).processResultsForGroup(any(), any());

        ProcessTaskResult<ProcessDataBucketData> result =
                saveCskuCardsToMbo.runOnTickets(tickets, new ProcessDataBucketData(dataBucketId));

        verify(modelStorageHelper, times(1)).executeSaveModelRequest(eq(request));
        verify(modelStorageHelper, times(1)).executeSaveModelRequest(eq(conflictFreeRequest));
        verify(cskuRequestCreator, times(1))
                .createRequestForGroup(argThat(list -> list.size() == 4), argThat(List::isEmpty));
        verify(cskuRequestCreator, times(1))
                .createRequestForGroup(argThat(list -> list.size() == 4), eq(modelsWithConflict));
        verify(cskuResponseProcessor, times(1))
                .processResultsForGroup(any(), argThat(list -> list.size() == 4));
        verify(cskuResponseProcessor, times(1))
                .processResultsForGroup(any(), argThat(list -> list.size() == 4), anyBoolean());

        assertGcExternalRequest(2);
        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult()).isNotNull();
    }

    @Test
    public void onlyCreatedValidationsAreAppliedToTicketStatus() {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(1);
        tickets.forEach(ticket -> ticket.setDcpGroupId(null));
        gcSkuTicketDao.update(tickets);
        Long ticketId = tickets.get(0).getId();

        //сымитируем упавшую, но не критичную валидацию в csku пайпе
        gcSkuValidationDao.createValidations(Collections.singletonList(ticketId),
                GcSkuValidationType.PICTURE_MBO_VALIDATION);
        TicketValidationResult validationResult = TicketValidationResult.invalid(ticketId,
                Messages.get().dcpImageInvalid("", false, true, false, false));
        gcSkuValidationDao.saveValidationResults(Collections.singletonList(validationResult),
                GcSkuValidationType.PICTURE_MBO_VALIDATION);
        gcSkuTicketDao.updateTicketValidationStatus(Collections.singletonList(ticketId), true,
                GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED);
        dataBucketMessagesService.insertValidationMessages(dataBucketId);

        //проверим что тикет ok, но валидация не ок
        GcSkuTicket gcSkuTicket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(gcSkuTicket).extracting(GcSkuTicket::getValid).isEqualTo(true);
        List<GcSkuValidation> validations = gcSkuValidationDao.findAll();
        assertThat(validations).hasSize(1);
        GcSkuValidation validation = validations.get(0);
        assertThat(validation).extracting(GcSkuValidation::getValidationType)
                .isEqualTo(GcSkuValidationType.PICTURE_MBO_VALIDATION);
        assertThat(validation).extracting(GcSkuValidation::getIsOk).isEqualTo(false);

        mockDataPreparation(tickets);
        doReturn(request()).when(cskuRequestCreator).createRequest(any());
        doReturn(createSaveGroupResponse()).when(modelStorageHelper).executeSaveModelRequest(any());
        doReturn(new ResponseProcessingResult(false)).when(cskuResponseProcessor)
                .processResults(any(), any());

        ProcessTaskResult<ProcessDataBucketData> result =
                saveCskuCardsToMbo.apply(new ProcessDataBucketData(dataBucketId));

        assertThat(result.hasProblems()).isTrue();

        GcSkuTicket ticketAfterSaveAttempt = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticketAfterSaveAttempt).extracting(GcSkuTicket::getValid).isEqualTo(true);
        assertThat(ticketAfterSaveAttempt).extracting(GcSkuTicket::getStatus)
                .isEqualTo(GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED);
    }

    @Test
    public void whenOfferWOGroupMappedToSkuWithSiblingsAndRequestHasConflictTryToResolveIt() {
        List<Long> modelsWithConflict = Arrays.asList(SKU_1_ID, SKU_2_ID);

        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(1);
        GcSkuTicket ticket = tickets.get(0);
        ticket.setDcpGroupId(null);
        gcSkuTicketDao.update(ticket);

        //У всех скю один родитель
        doReturn(groupedTicketWrapper(tickets, PARENT_MODEL_ID, List.of(SKU_1_ID, SKU_2_ID, SKU_3_ID)))
                .when(cskuDataPreparation)
                .collectDataForRequest(any());
        //request with conflict
        ModelCardApi.SaveModelsGroupRequest request = request();
        doReturn(request).when(cskuRequestCreator).createRequestForGroup(any(), argThat(List::isEmpty));
        //conflict free request
        ModelCardApi.SaveModelsGroupRequest conflictFreeRequest = conflictFreeRequest();
        doReturn(conflictFreeRequest).when(cskuRequestCreator).createRequestForGroup(any(), eq(modelsWithConflict));

        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createSaveGroupResponse();


        doReturn(saveGroupResponse).when(modelStorageHelper).executeSaveModelRequest(any());
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(false, new ProcessedStatistic.BasicProcessedStatistic(),
                modelsWithConflict))
                .when(cskuResponseProcessor).processResultsForGroup(any(), any(), anyBoolean());
        doReturn(new ResponseProcessingResult(true))
                .when(cskuResponseProcessor).processResultsForGroup(any(), any());

        ProcessTaskResult<ProcessDataBucketData> result =
                saveCskuCardsToMbo.runOnTickets(tickets, new ProcessDataBucketData(dataBucketId));

        verify(modelStorageHelper, times(1)).executeSaveModelRequest(eq(request));
        verify(modelStorageHelper, times(1)).executeSaveModelRequest(eq(conflictFreeRequest));
        verify(cskuRequestCreator, times(1))
                .createRequestForGroup(argThat(list -> list.size() == 1), argThat(List::isEmpty));
        verify(cskuRequestCreator, times(1))
                .createRequestForGroup(argThat(list -> list.size() == 1), eq(modelsWithConflict));
        verify(cskuResponseProcessor, times(1))
                .processResultsForGroup(any(), argThat(list -> list.size() == 1));
        verify(cskuResponseProcessor, times(1))
                .processResultsForGroup(any(), argThat(list -> list.size() == 1), anyBoolean());

        assertGcExternalRequest(2);
        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult()).isNotNull();
    }

    @Test
    public void verifyTsIsSetFromResponse() {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(1);

        List<Long> skuIds = List.of(SKU_1_ID, SKU_2_ID, SKU_3_ID);

        List<TicketWrapper> inputTicketWrappers = groupedTicketWrapper(tickets, PARENT_MODEL_ID, skuIds);
        List<Long> modelIds = new ArrayList<>(skuIds);
        modelIds.add(PARENT_MODEL_ID);
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createSaveGroupResponse(modelIds, TS);
        List<TicketWrapper> ticketWrappers = saveCskuCardsToMbo.updateTsFromResponse(inputTicketWrappers,
                saveGroupResponse);
        TicketWrapper ticketWrapper = ticketWrappers.get(0);
        assertThat(ticketWrapper.getParentModel().getModifiedTs()).isEqualTo(TS);
        ticketWrapper.getAllSkus().forEach(sku -> assertThat(sku.getModifiedTs()).isEqualTo(TS));
    }

    private void mockDataPreparation(List<GcSkuTicket> gcSkuTicket) {
        Stubber stubber = doReturn(ticketWrappers(gcSkuTicket));
        stubber.when(cskuDataPreparation)
                .collectDataForRequest(any());
    }

    private List<TicketWrapper> ticketWrappers(List<GcSkuTicket> tickets) {
        List<TicketWrapper> result = new ArrayList<>();
        int i = 1;
        for (GcSkuTicket ticket : tickets) {
            result.add(new TicketWrapper(null, createModel(i++), true, ticket));
        }
        return result;
    }

    private List<TicketWrapper> groupedTicketWrapper(List<GcSkuTicket> tickets) {
        List<TicketWrapper> result = new ArrayList<>();
        int i = 1;
        for (GcSkuTicket ticket : tickets) {
            result.add(new TicketWrapper(null, createModel(i++), Collections.emptyList(), true, ticket));
        }
        return result;
    }

    private List<TicketWrapper> groupedTicketWrapper(List<GcSkuTicket> tickets, long parentModelId,
                                                     List<Long> allSkuIds) {
        List<TicketWrapper> result = new ArrayList<>();
        List<ModelStorage.Model> allSkus = allSkuIds.stream().map(id -> createModel(id)).collect(Collectors.toList());
        for (GcSkuTicket ticket : tickets) {
            result.add(new TicketWrapper(null, createModel(parentModelId), allSkus, true, ticket));
        }
        return result;
    }

    private ModelStorage.Model createModel(long id) {
        return ModelStorage.Model.newBuilder()
                .setId(id)
                .build();
    }

    private void mockRequestExecution() {
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createSaveGroupResponse();
        doReturn(saveGroupResponse).when(modelStorageHelper).executeSaveModelRequest(any());
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupRequest request() {
        return ModelCardApi.SaveModelsGroupRequest.newBuilder()
                .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                        .addModels(
                                ModelStorage.Model.newBuilder()
                                        .setCategoryId(1234)
                                        .build()
                        )
                        .build())
                .build();
    }

    @NotNull
    private ModelCardApi.SaveModelsGroupRequest conflictFreeRequest() {
        return ModelCardApi.SaveModelsGroupRequest.newBuilder()
                .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                        .build())
                .build();
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createSaveGroupResponse() {
        return createSaveGroupResponse(List.of(777L), 0);
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createSaveGroupResponse(List<Long> modelIds, long modifiedTs) {
        ModelCardApi.SaveModelsGroupOperationResponse.Builder singleResponse =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK);
        modelIds.forEach(modelId ->
                singleResponse
                        .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setStatus(ModelStorage.OperationStatusType.OK)
                                .setType(ModelStorage.OperationType.CREATE)
                                .setModelId(modelId)
                                .setModel(createModel(modelId).toBuilder().setModifiedTs(modifiedTs))
                                .build()
                        )
        );

        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(singleResponse)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }

    private void assertGcExternalRequest(int expected) {
        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        assertThat(requests.size()).isEqualTo(expected);
        Set<Long> requestIds = requests.stream().map(GcExternalServiceRequest::getId).collect(Collectors.toSet());
        assertThat(requestIds).hasSize(expected);
        assertThat(requests).extracting(GcExternalServiceRequest::getRequest).doesNotContainNull();
        assertThat(requests).extracting(GcExternalServiceRequest::getResponse).doesNotContainNull();
        assertThat(requests).extracting(GcExternalServiceRequest::getStatus)
                .containsOnly(GcExternalRequestStatus.FINISHED);
        assertThat(requests).extracting(GcExternalServiceRequest::getType)
                .containsOnly(GcExternalRequestType.MBO_SAVE_CSKU);
    }
}
