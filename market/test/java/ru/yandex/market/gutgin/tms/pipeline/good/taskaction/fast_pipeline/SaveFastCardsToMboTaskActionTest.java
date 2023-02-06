package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.fast_pipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.service.fast.FastCardRequestCreator;
import ru.yandex.market.gutgin.tms.service.fast.FastCardResponseProcessor;
import ru.yandex.market.gutgin.tms.service.ResponseProcessingResult;
import ru.yandex.market.partner.content.common.entity.goodcontent.ValidationWithMessages;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcExternalServiceRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcExternalRequestType;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.GcTicketRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcExternalServiceRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcTicketRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.ProtocolMessage;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED;

public class SaveFastCardsToMboTaskActionTest extends DBDcpStateGenerator {

    private ModelStorageHelper modelStorageHelper;
    private SaveFastCardsToMboTaskAction saveFastCardsToMbo;
    @Autowired
    private GcExternalServiceRequestDao gcExternalServiceRequestDao;
    private FastCardRequestCreator fastCardRequestCreator;
    private FastCardResponseProcessor fastCardResponseProcessor;
    @Autowired
    private GcTicketRequestDao gcTicketRequestDao;


    @Override
    @Before
    public void setUp() {
        super.setUp();
        modelStorageHelper = mock(ModelStorageHelper.class);
        fastCardRequestCreator = mock(FastCardRequestCreator.class);
        fastCardResponseProcessor = mock(FastCardResponseProcessor.class);
        saveFastCardsToMbo = new SaveFastCardsToMboTaskAction(gcSkuTicketDao,
                modelStorageHelper,
                fastCardRequestCreator,
                fastCardResponseProcessor,
                gcExternalServiceRequestDao,
                gcSkuValidationDao
        );
        //when(fastCardRequestCreator.getConsistentTickets(any(), any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void whenFailedModelsThenFailTask() {
        List<GcSkuTicket> gcSkuTickets = generateTickets(201);
        mockGeneratedRequest();
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(true),
                new ResponseProcessingResult(false),
                new ResponseProcessingResult(true)
        ).when(fastCardResponseProcessor)
                .processResults(any(), any());
        ProcessTaskResult<ProcessDataBucketData> result =
                saveFastCardsToMbo.runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertGcExternalRequest(3);
        assertThat(result.hasProblems()).isTrue();
        assertThat(result.getResult()).isNull();
    }

    @Test
    public void testThatRequestIsGeneratedSendAndSavedToDb() {
        mockGeneratedRequest();
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(true)).when(fastCardResponseProcessor).processResults(any(), any());
        List<GcSkuTicket> gcSkuTickets = generateTickets(1);
        Long dataBucketId = gcSkuTickets.get(0).getDataBucketId();

        ProcessTaskResult<ProcessDataBucketData> result = saveFastCardsToMbo.runOnTickets(gcSkuTickets,
                new ProcessDataBucketData(dataBucketId));

        assertGcExternalRequest(1);
        verify(modelStorageHelper, times(1)).executeSaveModelRequest(any());
        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult()).extracting(ProcessDataBucketData::getDataBucketId).isEqualTo(dataBucketId);
    }

    @Test
    public void testBuckets() {
        List<GcSkuTicket> gcSkuTickets = generateTickets(201);
        mockRequestExecution();
        mockGeneratedRequest();
        doReturn(new ResponseProcessingResult(true)).when(fastCardResponseProcessor).processResults(any(), any());

        saveFastCardsToMbo.runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));

        int expectedNumberOfRequests = 3; // 100 + 100 + 1
        verify(modelStorageHelper, times(expectedNumberOfRequests)).executeSaveModelRequest(any());
    }

    @Test
    public void onTaskRestartDontTouchAlreadySavedTickets() {
        SaveFastCardsToMboTaskAction saveWithProcessor = new SaveFastCardsToMboTaskAction(gcSkuTicketDao,
                modelStorageHelper,
                fastCardRequestCreator,
                new FastCardResponseProcessor(gcSkuTicketDao),
                gcExternalServiceRequestDao,
                gcSkuValidationDao
        );

        List<GcSkuTicket> gcSkuTickets = generateTickets(101);

        mockGeneratedRequest();
        doReturn(createSaveGroupResonse(777, 100))
                .doThrow(new RuntimeException()) //эмулируем падение на 2 батче (500-ка мбо, например)
                .doReturn(createSaveGroupResonse(777, 1)) // отправка 1 тикета после рестарта таски
                .when(modelStorageHelper).executeSaveModelRequest(any());
        doReturn(new ResponseProcessingResult(true)).when(fastCardResponseProcessor).processResults(any(), any());

        Long dataBucket = gcSkuTickets.stream().map(GcSkuTicket::getDataBucketId).findFirst()
                .orElseThrow(IllegalStateException::new);

        //первый запуск с падением
        Assertions.assertThatThrownBy(() -> saveWithProcessor.doRun(new ProcessDataBucketData(dataBucket)))
                .isInstanceOf(RuntimeException.class);

        List<GcTicketRequest> requestToTicket = gcTicketRequestDao.findAll();
        assertThat(requestToTicket).hasSize(101);
        //реквест для сломанного тикета НЕ откатился транзакцией

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchByStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
        assertThat(tickets).hasSize(100);
        assertThat(tickets).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(777L);

        //после 1 запуска 1 тикет остался не обработанным
        List<GcSkuTicket> extraTicket = gcSkuTicketDao.fetchByStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_STARTED);
        assertThat(extraTicket).hasSize(1);
        assertThat(extraTicket).extracting(GcSkuTicket::getResultMboPskuId).containsOnlyNulls();

        //рестарт
        ProcessTaskResult<ProcessDataBucketData> result =
                saveWithProcessor.doRun(new ProcessDataBucketData(dataBucket));

        GutginAssertions.assertThat(result).doesntHaveProblems();

        // 100 успешных после первой попытки + 1 успешный после повтора
        List<GcTicketRequest> requestToTicketAfterRestart = gcTicketRequestDao.findAll();
        assertThat(requestToTicketAfterRestart).hasSize(101);

        //после 2 запуска все тикеты завершены
        List<GcSkuTicket> ticketsAfterRestart = gcSkuTicketDao.fetchByStatus(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK);
        assertThat(ticketsAfterRestart).hasSize(101);
        assertThat(ticketsAfterRestart).extracting(GcSkuTicket::getResultMboPskuId).containsOnly(777L);
    }

    @Test
    public void whenExistingModelHasIncorrectMappingCreateValidationError() {
        long badModelId = 500L;
        long goodModelId = 501L;
        ModelStorage.Model badModel = generateExistingModelWithWrongType(badModelId);
        ModelStorage.Model goodModel = generateExistingModelWithCorrectType(goodModelId);
        List<GcSkuTicket> tickets = generateTickets(badModelId, goodModelId);
        List<ModelStorage.Model> toBeReturned = Arrays.asList(badModel, goodModel);
        doReturn(toBeReturned).when(modelStorageHelper).findModels(any());
        mockGeneratedRequest();
        mockRequestExecution();
        doReturn(new ResponseProcessingResult(true)).when(fastCardResponseProcessor)
                .processResults(any(), any());

        ProcessTaskResult<ProcessDataBucketData> result =
                saveFastCardsToMbo.runOnTickets(tickets, new ProcessDataBucketData(dataBucketId));

        //реквест генерируется только для 1 запроса с "хорошим" типом модели
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GcSkuTicket>> captor = ArgumentCaptor.forClass(List.class);
        verify(fastCardRequestCreator).createRequest(captor.capture(), any());
        List<GcSkuTicket> value = captor.getValue();
        assertThat(value).hasSize(1);
        assertThat(value.get(0)).extracting(GcSkuTicket::getExistingMboPskuId).isEqualTo(goodModelId);

        GutginAssertions.assertThat(result).doesntHaveProblems();

        //тикет обновился
        List<GcSkuTicket> ticketWithValidation = gcSkuTicketDao.fetchByStatus(GcSkuTicketStatus.RESULT_UPLOAD_STARTED);
        assertThat(ticketWithValidation).hasSize(1);
        assertThat(ticketWithValidation).extracting(GcSkuTicket::getValid).containsOnly(false);

        //добавилась валидация
        GcSkuTicket gcSkuTicket = ticketWithValidation.get(0);
        List<GcSkuValidation> validations =
                gcSkuValidationDao.getGcSkuValidations(GcSkuValidationType.MAPPING_ON_VALID_MODEL_VALIDATION,
                        gcSkuTicket.getId());
        assertThat(validations).hasSize(1);
        assertThat(validations).extracting(GcSkuValidation::getIsOk).containsOnly(false);

        //добавилось сообщение
        Map<Long, List<ValidationWithMessages>> messages =
                gcSkuTicketDao.getTicketsValidationMessagesMap(Collections.singletonList(gcSkuTicket.getId()));
        assertThat(messages).hasSize(1);
        List<ValidationWithMessages> validationsWithMessages = messages.get(gcSkuTicket.getId());
        assertThat(validationsWithMessages).hasSize(1);
        List<ProtocolMessage> protocolMessages = validationsWithMessages.get(0).getMessages();
        assertThat(protocolMessages).hasSize(1);
        assertThat(protocolMessages).extracting(ProtocolMessage::getCode)
                .containsOnly("ir.partner_content.error.invalid_state");
    }

    private void assertGcExternalRequest(int expected) {
        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        assertThat(requests).hasSize(expected);
        assertThat(requests).extracting(GcExternalServiceRequest::getRequest).doesNotContainNull();
        assertThat(requests).extracting(GcExternalServiceRequest::getResponse).doesNotContainNull();
        assertThat(requests).extracting(GcExternalServiceRequest::getStatus)
                .containsOnly(GcExternalRequestStatus.FINISHED);
        assertThat(requests).extracting(GcExternalServiceRequest::getType)
                .containsOnly(GcExternalRequestType.MBO_SAVE_FAST_CARD);
    }

    private void mockGeneratedRequest() {
        ModelCardApi.SaveModelsGroupRequest build = ModelCardApi.SaveModelsGroupRequest.newBuilder()
                .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                        .addModels(
                                ModelStorage.Model.newBuilder()
                                        .setCategoryId(1234)
                                        .build()
                        )
                        .build())
                .build();
        doReturn(build).when(fastCardRequestCreator).createRequest(any(), any());
    }

    @NotNull
    private List<GcSkuTicket> generateTickets(int numberOfTickets) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(numberOfTickets, datacampOffers -> {

            DatacampOffer datacampOffer = datacampOffers.get(0);
            DcpOfferBuilder dcpOfferBuilder = new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId()
            );
            dcpOfferBuilder.build();
        });

        gcSkuTickets.forEach(gcSkuTicket -> gcSkuTicket.setStatus(SAVE_AND_PUBLISH_STARTED));
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }

    private void mockRequestExecution() {
        ModelStorageHelper.SaveGroupResponse saveGroupResponse = createSaveGroupResonse(777, 1);
        doReturn(saveGroupResponse).when(modelStorageHelper).executeSaveModelRequest(any());
    }

    private List<GcSkuTicket> generateTickets(long goodModelId, long badModelId) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            datacampOffer.setData(new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId())
                    .build());
        });
        for (int i = 0; i < gcSkuTickets.size(); i++) {
            GcSkuTicket gcSkuTicket = gcSkuTickets.get(i);
            if (i == 0) {
                gcSkuTicket.setExistingMboPskuId(goodModelId);
            } else {
                gcSkuTicket.setExistingMboPskuId(badModelId);
            }
        }
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets;
    }

    private ModelStorage.Model generateExistingModelWithWrongType(long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(modelId)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCategoryId(0)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .setSupplierId(10)
                .setPublished(true)
                .setModifiedTs(100)
                .build();
    }

    private ModelStorage.Model generateExistingModelWithCorrectType(long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(modelId)
                .addTitles(LocalizedStringUtils.defaultString("старое имя"))
                .setCategoryId(0)
                .setCurrentType(ModelStorage.ModelType.FAST_SKU.name())
                .setSourceType(ModelStorage.ModelType.FAST_SKU.name())
                .setSupplierId(10)
                .setPublished(true)
                .setModifiedTs(100)
                .build();
    }

    @NotNull
    private ModelStorageHelper.SaveGroupResponse createSaveGroupResonse(int modelId, int modelsCount) {
        ModelCardApi.SaveModelsGroupOperationResponse.Builder builder =
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK);
        for (int i = 0; i < modelsCount; i++) {
            builder.addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.OK)
                    .setType(ModelStorage.OperationType.CREATE)
                    .setModelId(modelId)
                    .build());
        }

        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(builder)
                .build();
        return new ModelStorageHelper.SaveGroupResponse(null, response);
    }
}
