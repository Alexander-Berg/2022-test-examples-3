package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc.OfferContentProcessingResultsServiceBlockingStub;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MappingConfidenceType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_FAST;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.tables.GcSkuValidation.GC_SKU_VALIDATION;

public class SendResultsToDatacampTaskActionTest extends DBDcpStateGenerator {
    public static final long PARAM_ID = 100500L;
    public static final String PARAM_NAME = "100500";
    public static final GcSkuValidationType VALIDATION_TYPE_1 = GcSkuValidationType.PARAMETER_TYPE_CONFORMITY;
    public static final GcSkuValidationType VALIDATION_TYPE_2 = GcSkuValidationType.HTML_TAGS_EXISTENCE;
    public static final MessageInfo NOT_MULTIPLE_PARAM_MESSAGE_INFO = Messages.get()
            .notMultivalueParamDcp(PARAM_ID, PARAM_NAME, "");
    public static final MessageInfo INCONSISTENT_STATE_MESSAGE_INFO = Messages.get().inconsistentOfferState();

    @Autowired
    GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    DatacampOfferDao datacampOfferDao;

    private DefaultRatingEvaluator pskuRatingEvaluator;
    private ModelStorageHelper modelStorageHelper;
    private SendResultsToDatacampTaskAction sendResultsToDatacampTaskAction;

    @Before
    public void setUp() {
        super.setUp();

        ModelStorageServiceMock modelStorageServiceMock = new ModelStorageServiceMock();
        this.modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        this.pskuRatingEvaluator = new DefaultRatingEvaluator(new CategoryDataKnowledgeMock());
    }

    @Test
    public void whenPskuRunThenActualizeDatacampOffer() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTickets();
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        //Шлём вердикты вместо messages
        assertThat(itemList).hasSize(0);

        List<DataCampOfferMarketContent.MarketContentProcessing.TotalResult> totalResults = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .map(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getResult())
                .collect(Collectors.toList());
        assertThat(totalResults).containsOnly(
                DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK,
                DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK);
        gcSkuTickets.forEach(gcSkuTicket ->
                gcSkuTicket.getDatacampOffer().getResolution().getBySourceList().forEach(resolution -> {
                    assertThat(resolution.getMeta().getSource()).isEqualTo(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
                    assertThat(resolution.getVerdictList()).hasSize(1);
                    assertThat(resolution.getVerdictList().get(0).getResults(0).getIsBanned()).isFalse();
                    assertThat(resolution.hasMeta()).isTrue();
                })
        );
    }

    @Test
    public void dontRemoveBindingParameters() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTickets();
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        //старые поля остались
        assertThat(gcSkuTickets)
                .extracting(t -> t.getDatacampOffer().getContent().getBinding().getPartner().getMarketCategoryId())
                .containsOnly(1000);

        //новые поля записались
        gcSkuTickets.forEach(gcSkuTicket -> {
            long expectedSkuId = gcSkuTicket.getId() * 1000;
            assertThat(gcSkuTicket).extracting(
                            t -> t.getDatacampOffer().getContent().getBinding().getPartner().getMarketSkuId())
                    .isEqualTo(expectedSkuId);
        });
    }

    @Test
    public void whenFastRunThenActualizeDatacampOffer() {
        setUpOfferEnricher(SendFastResultsToMbocTaskAction::new);

        // Проверим на актуализацию только впервые пришедшие офферы
        // Инициализируем
        List<GcSkuTicket> gcSkuTickets = generateGcSkuTickets();
        List<Long> skuIds = new ArrayList<>(gcSkuTickets.size());
        List<Long> modelIds = new ArrayList<>(gcSkuTickets.size());
        enrichWithRandomSkuIdsModelIdsAndValidations(gcSkuTickets, skuIds, modelIds);

        // Выполняем актуализацию offerEnricher-ом
        gcSkuTickets = processAndCheckActualizingResultReturningNewTickets();

        // Проверяем результат
        checkSkuIds(skuIds, modelIds, gcSkuTickets);
        checkSkuTypes(gcSkuTickets);
        assertThat(gcSkuTickets)
                .extracting(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getResolution().getBySourceList())
                .flatExtracting(verdicts ->
                        verdicts.stream().map(DataCampResolution.Verdicts::getVerdictList)
                                .flatMap(Collection::stream).map(DataCampResolution.Verdict::getResultsList)
                                .flatMap(Collection::stream).collect(Collectors.toList())
                )
                .allMatch(
                        verdictResult -> verdictResult.getIsBanned() && !verdictResult.getMessagesList().isEmpty()
                );

        // Проверяем что при enrich-е оффера с имеющимеся валидациями, они удалятся, если были пройдены
        makeValidationsValid(gcSkuTickets);
        gcSkuTickets = processAndCheckActualizingResultReturningNewTickets();

        checkSkuIds(skuIds, modelIds, gcSkuTickets);
        checkSkuTypes(gcSkuTickets);

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        //Когда отправляем вердикты, не шлём messages
        assertThat(itemList).hasSize(0);

        assertThat(gcSkuTickets)
                .extracting(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getResolution().getBySourceList())
                .flatExtracting(verdicts ->
                        verdicts.stream().map(DataCampResolution.Verdicts::getVerdictList).collect(Collectors.toList())
                )
                .flatExtracting(verdictList ->
                        verdictList.stream().flatMap(v -> v.getResultsList().stream()).collect(Collectors.toList())
                )
                .allMatch(validationResult -> !validationResult.getIsBanned());
        assertThat(gcSkuTickets)
                .extracting(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getResolution().getBySourceList())
                .flatExtracting(verdicts ->
                        verdicts.stream().map(DataCampResolution.Verdicts::getVerdictList).collect(Collectors.toList())
                )
                .flatExtracting(verdictList ->
                        verdictList.stream().flatMap(v -> v.getResultsList().stream()).collect(Collectors.toList())
                ).flatExtracting(DataCampValidationResult.ValidationResult::getMessagesList)
                .isEmpty();
    }

    @Test
    public void whenParamBoundValidationFailureThenFieldPosition() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTickets();
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        gcSkuTickets.forEach(ticket -> {
            List<DataCampExplanation.Explanation> validationResult =
                    ticket.getDatacampOffer().getResolution().getBySourceList().get(0).getVerdictList().get(0)
                            .getResultsList().get(0).getMessagesList();

            validationResult.forEach(explanation -> {
                assertThat(explanation)
                        .extracting(expl -> expl.getContentMessageDetails().getPosition())
                        .isEqualTo(DataCampExplanation.ContentMessageDetails.MessagePosition.FIELD);
                assertThat(explanation)
                        .extracting(expl -> expl.getContentMessageDetails().getParamId())
                        .isEqualTo(PARAM_ID);
            });
        });
    }

    @Test
    public void whenNotParamBoundValidationFailureThenCommonPosition() {

        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTickets();
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, INCONSISTENT_STATE_MESSAGE_INFO);

        gcSkuTickets.forEach(ticket -> {
            List<DataCampExplanation.Explanation> validationResult =
                    ticket.getDatacampOffer().getResolution().getBySourceList().get(0).getVerdictList().get(0)
                            .getResultsList().get(0).getMessagesList();

            validationResult.forEach(explanation -> {
                assertThat(explanation)
                        .extracting(expl -> expl.getContentMessageDetails().getPosition())
                        .isEqualTo(DataCampExplanation.ContentMessageDetails.MessagePosition.COMMON);
            });
        });
    }

    @Test
    public void whenPskuFromFCAndValidationFailuresThenSendVerdictsWithWarnings() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTicketsWithFCInThePast(2);
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        MessageInfo messageInfo = Messages.get().notMultivalueParamDcp(PARAM_ID, PARAM_NAME, "0");

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        //Когда отправляем вердикты, не шлём messages
        assertThat(itemList).hasSize(0);
        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    //verify that incoming mdm verdict is cleaned up
                    List<DataCampResolution.Verdicts> bySourceList = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList();
                    assertThat(bySourceList).hasSize(1);

                    DataCampValidationResult.ValidationResult validationResult = bySourceList.get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isTrue();
                    assertThat(validationResult.getMessagesList()).hasSize(1);
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getCode).isEqualTo(messageInfo.getCode());
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getLevel)
                            .isEqualTo(DataCampExplanation.Explanation.Level.WARNING);

                });
    }

    @Test
    public void whenCskuMappedSelfAndValidationFailuresThenSendVerdictMessages() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, GcSkuTicketType.CSKU, (offers) -> {
        });
        gcSkuTickets = updateTicketsWithMappingConfidence(gcSkuTickets, MappingConfidenceType.PARTNER_SELF);
        gcSkuTickets = updateTicketsWithValidationsAndNoSkus(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        MessageInfo messageInfo = Messages.get().notMultivalueParamDcp(PARAM_ID, PARAM_NAME, "0");
        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    List<DataCampResolution.Verdicts> bySourceList = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList();
                    assertThat(bySourceList).hasSize(1);

                    DataCampValidationResult.ValidationResult validationResult = bySourceList.get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).hasSize(1);
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getCode).isEqualTo(messageInfo.getCode());
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getLevel)
                            .isEqualTo(DataCampExplanation.Explanation.Level.ERROR);
                    assertThat(gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent()
                            .getProcessingResponse().getResult())
                            .isEqualTo(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);
                });
    }

    @Test
    public void whenCskuMappedOtherAndValidationFailuresThenSendNegativeVerdictMessages() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, GcSkuTicketType.CSKU, (offers) -> {
        });
        gcSkuTickets = updateTicketsWithMappingConfidence(gcSkuTickets, MappingConfidenceType.PARTNER);
        gcSkuTickets = updateTicketsWithValidationsAndNoSkus(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    List<DataCampResolution.Verdicts> bySourceList = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList();
                    assertThat(bySourceList).hasSize(1);

                    DataCampValidationResult.ValidationResult validationResult = bySourceList.get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).isNotEmpty();
                    assertThat(gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent()
                            .getProcessingResponse().getResult())
                            .isEqualTo(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);
                });
    }

    @Test
    public void whenPskuWithNoMappingsAndValidationFailuresThenSendVerdictMessagesWithWarning() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, GcSkuTicketType.DATA_CAMP, (offers) -> {
        });
        gcSkuTickets = updateTicketsWithMappingConfidence(gcSkuTickets, null);
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        MessageInfo messageInfo = Messages.get().notMultivalueParamDcp(PARAM_ID, PARAM_NAME, "0");

        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    List<DataCampResolution.Verdicts> bySourceList = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList();
                    assertThat(bySourceList).hasSize(1);

                    DataCampValidationResult.ValidationResult validationResult = bySourceList.get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).hasSize(1);
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getCode).isEqualTo(messageInfo.getCode());
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getLevel)
                            .isEqualTo(DataCampExplanation.Explanation.Level.WARNING);

                });
    }

    @Test
    public void whenPskuFromFCAndValidationWarningThenSendPositiveVerdicts() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTicketsWithFCInThePast(2);
        MessageInfo messageInfo =
                Messages.get(MessageInfo.Level.WARNING).dcpImageInvalid("url", false, true, false, false);
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, true, messageInfo);

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        //Когда отправляем вердикты, не шлём messages
        assertThat(itemList).hasSize(0);
        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    //verify that incoming mdm verdict is cleaned up
                    List<DataCampResolution.Verdicts> bySourceList = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList();
                    assertThat(bySourceList).hasSize(1);

                    DataCampValidationResult.ValidationResult validationResult = bySourceList.get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).hasSize(1);
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getCode).isEqualTo(messageInfo.getCode());
                    assertThat(validationResult.getMessagesList().get(0))
                            .extracting(DataCampExplanation.Explanation::getLevel)
                            .isEqualTo(DataCampExplanation.Explanation.Level.WARNING);
                });
    }

    private List<GcSkuTicket> updateTicketsWithValidationsAndNoSkus(List<GcSkuTicket> gcSkuTickets, boolean isValid,
                                                                    MessageInfo messageInfo) {
        List<TicketValidationResult> validationResults = new ArrayList<>(gcSkuTickets.size());
        gcSkuTickets.forEach(gcSkuTicket -> {
            Long id = gcSkuTicket.getId();
            if (isValid) {
                if (messageInfo == null) {
                    validationResults.add(TicketValidationResult.valid(id));
                } else {
                    validationResults.add(
                            TicketValidationResult.validWithWarnings(id, Collections.singletonList(messageInfo)));
                }
            } else {
                validationResults.add(TicketValidationResult.invalid(id, messageInfo));
            }
        });

        List<Long> ticketIds = gcSkuTickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList());
        gcSkuValidationDao.createValidations(ticketIds, VALIDATION_TYPE_1);
        gcSkuValidationDao.saveValidationResults(validationResults, VALIDATION_TYPE_1);

        return processAndCheckActualizingResultReturningNewTickets();
    }

    private List<GcSkuTicket> updateTicketsWithValidations(List<GcSkuTicket> gcSkuTickets, boolean isValid,
                                                           MessageInfo messageInfo) {
        List<Long> skuIds = new ArrayList<>(gcSkuTickets.size());
        List<Long> modelIds = new ArrayList<>(gcSkuTickets.size());
        List<TicketValidationResult> validationResults = new ArrayList<>(gcSkuTickets.size());
        gcSkuTickets.forEach(gcSkuTicket -> {
            Long id = gcSkuTicket.getId();
            long skuId = id * 1000L;
            gcSkuTicketDao.setResultSkuModelId(id, skuId, id);
            skuIds.add(skuId);
            modelIds.add(id);
            if (isValid) {
                if (messageInfo == null) {
                    validationResults.add(TicketValidationResult.valid(id));
                } else {
                    validationResults.add(
                            TicketValidationResult.validWithWarnings(id, Collections.singletonList(messageInfo)));
                }
            } else {
                validationResults.add(TicketValidationResult.invalid(id, messageInfo));
            }
        });

        List<Long> ticketIds = gcSkuTickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList());
        gcSkuValidationDao.createValidations(ticketIds, VALIDATION_TYPE_1);
        gcSkuValidationDao.saveValidationResults(validationResults, VALIDATION_TYPE_1);

        gcSkuTickets = processAndCheckActualizingResultReturningNewTickets();


        checkSkuIds(skuIds, modelIds, gcSkuTickets);
        return gcSkuTickets;
    }

    private List<GcSkuTicket> updateTicketsWithMappingConfidence(List<GcSkuTicket> gcSkuTickets,
                                                                 MappingConfidenceType mappingConfidenceType) {
        gcSkuTickets.forEach(ticket -> {
            ticket.setMappingConfidence(mappingConfidenceType);
            Long id = ticket.getId();
            long skuId = id * 1000L;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
        });
        return gcSkuTickets;
    }

    @Test
    public void whenPskuFromFCAndNoValidationFailuresThenSendPositiveVerdicts() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTicketsWithFCInThePast(2);
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, true, null);

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        assertThat(itemList).isEmpty();
        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    DataCampValidationResult.ValidationResult validationResult = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList().get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).isEmpty();
                });
    }

    @Test
    public void whenPskuIsProcessedThenCleanVerdicts() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTicketsWithFCInThePastAndNoMapping(2);
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, NOT_MULTIPLE_PARAM_MESSAGE_INFO);

        gcSkuTickets.forEach(gcSkuTicket ->
                gcSkuTicket.getDatacampOffer().getResolution().getBySourceList().forEach(resolution -> {
                    assertThat(resolution.getMeta().getSource()).isEqualTo(DataCampOfferMeta.DataSource.MARKET_GUTGIN);
                    assertThat(resolution.getVerdictList()).hasSize(1);
                    assertThat(resolution.getVerdictList().get(0).getResults(0).getIsBanned()).isFalse();
                    assertThat(resolution.hasMeta()).isTrue();
                })
        );
    }

    @Test
    public void whenRaceConditionThenSendVerdictsWithWarning() {
        setUpOfferEnricher(SendFastResultsToMbocTaskAction::new);

        List<GcSkuTicket> gcSkuTickets = generateGcSkuTicketsWithFCInThePast(1);
        gcSkuTickets.get(0).setType(GcSkuTicketType.FAST_CARD);
        gcSkuTicketDao.update(gcSkuTickets.get(0));
        gcSkuTickets = updateTicketsWithValidations(gcSkuTickets, false, INCONSISTENT_STATE_MESSAGE_INFO);

        List<DataCampOfferMarketContent.MarketContentProcessing.Item> itemList = gcSkuTickets.stream()
                .map(gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent().getPartner().getMarketSpecificContent())
                .flatMap(marketSpecificContent -> marketSpecificContent.getProcessingResponse().getItemsList().stream())
                .collect(Collectors.toList());

        assertThat(itemList).isEmpty();
        gcSkuTickets
                .forEach(gcSkuTicket -> {
                    DataCampValidationResult.ValidationResult validationResult = gcSkuTicket.getDatacampOffer()
                            .getResolution()
                            .getBySourceList().get(0)
                            .getVerdictList().get(0)
                            .getResultsList().get(0);
                    assertThat(validationResult.hasIsBanned()).isTrue();
                    assertThat(validationResult.getIsBanned()).isFalse();
                    assertThat(validationResult.getMessagesList()).hasSize(1);
                    assertThat(validationResult.getMessagesList().get(0).getCode())
                            .isEqualTo(INCONSISTENT_STATE_MESSAGE_INFO.getCode());
                    assertThat(validationResult.getMessagesList().get(0).getLevel())
                            .isEqualTo(DataCampExplanation.Explanation.Level.WARNING);
                });
    }


    private void setUpOfferEnricher(SendResultsToDatacampTaskActionConstructor constructor) {
        this.sendResultsToDatacampTaskAction = constructor.construct(
                gcSkuTicketDao, datacampOfferDao, null,
                pskuRatingEvaluator, modelStorageHelper
        );
    }

    @Test
    public void whenArrayOrIterableThenNormalExplanation() {
        setUpOfferEnricher(SendResultsToDatacampTaskAction::new);

        assertExplanation("array", new int[]{1, 2, 3},
                explanationParam("array", "1"),
                explanationParam("array", "2"),
                explanationParam("array", "3")
        );

        assertExplanation("array", new Integer[]{1, 2, 3},
                explanationParam("array", "1"),
                explanationParam("array", "2"),
                explanationParam("array", "3")
        );

        assertExplanation("mixed", Arrays.asList("one", new String[]{"two", "free"}),
                explanationParam("mixed", "one"),
                explanationParam("mixed", "two"),
                explanationParam("mixed", "free")
        );

        assertExplanation("simple", 100500,
                explanationParam("simple", "100500")
        );
    }

    private List<GcSkuTicket> generateGcSkuTickets() {
        return generateDBDcpInitialState(2, offers -> {
            for (int i = 0; i < offers.size(); i++) {
                DatacampOffer datacampOffer = offers.get(i);
                datacampOffer.setData(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(
                                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setShopId(Math.toIntExact(PARTNER_SHOP_ID))
                                                .setOfferId("offer_" + i)
                                )
                                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                        .setBinding(
                                                DataCampOfferMapping.ContentBinding.newBuilder()
                                                        .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                                                                .setMarketCategoryId(1000)
                                                                .build())
                                                        .build()
                                        )
                                        .build()
                                )
                                .build());
            }
        });
    }

    private List<GcSkuTicket> generateGcSkuTicketsWithFCInThePastAndNoMapping(int amount) {
        return generateDBDcpInitialState(amount, offers -> {
            for (int i = 0; i < offers.size(); i++) {
                DatacampOffer datacampOffer = offers.get(i);
                datacampOffer.setData(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(
                                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setShopId(Math.toIntExact(PARTNER_SHOP_ID))
                                                .setOfferId("offer_" + i)
                                )
                                .setResolution(DataCampResolution.Resolution
                                        .newBuilder()
                                        .addAllBySource(
                                                Arrays.asList(
                                                        DataCampResolution.Verdicts
                                                                .newBuilder()
                                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                        .setSource(
                                                                                DataCampOfferMeta.DataSource.MARKET_MDM)
                                                                        .build())
                                                                .addVerdict(DataCampResolution.Verdict
                                                                        .newBuilder().addResults(
                                                                                DataCampValidationResult.ValidationResult
                                                                                        .newBuilder()
                                                                                        .setIsBanned(true)
                                                                                        .build())
                                                                        .build())
                                                                .build(),
                                                        DataCampResolution.Verdicts
                                                                .newBuilder()
                                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                        .setSource(
                                                                                DataCampOfferMeta.DataSource.MARKET_GUTGIN)
                                                                        .build())
                                                                .addVerdict(DataCampResolution.Verdict
                                                                        .newBuilder().addResults(
                                                                                DataCampValidationResult.ValidationResult
                                                                                        .newBuilder()
                                                                                        .setIsBanned(true)
                                                                                        .build())
                                                                        .build())
                                                                .build()
                                                )
                                        )
                                        .build()
                                )
                                .setContent(DataCampOfferContent.OfferContent
                                        .newBuilder()
                                        .setBinding(DataCampOfferMapping.ContentBinding
                                                .newBuilder()
                                                .setApproved(Market.DataCamp.DataCampOfferMapping.Mapping
                                                        .newBuilder()
                                                        .setMarketSkuType(MARKET_SKU_TYPE_PSKU)
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                );
            }
        });
    }

    private List<GcSkuTicket> generateGcSkuTicketsWithFCInThePast(int amount) {
        return generateDBDcpInitialState(amount, offers -> {
            for (int i = 0; i < offers.size(); i++) {
                DatacampOffer datacampOffer = offers.get(i);
                datacampOffer.setData(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(
                                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setShopId(Math.toIntExact(PARTNER_SHOP_ID))
                                                .setOfferId("offer_" + i)
                                )
                                .setResolution(DataCampResolution.Resolution
                                        .newBuilder()
                                        .addAllBySource(Collections.singletonList(
                                                DataCampResolution.Verdicts
                                                        .newBuilder()
                                                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                .setSource(DataCampOfferMeta.DataSource.MARKET_MDM)
                                                                .build())
                                                        .addVerdict(DataCampResolution.Verdict
                                                                .newBuilder()
                                                                .addResults(DataCampValidationResult.ValidationResult
                                                                        .newBuilder()
                                                                        .setIsBanned(true)
                                                                        .build())
                                                                .build())
                                                        .build())
                                        )
                                        .build()
                                )
                                .setContent(DataCampOfferContent.OfferContent
                                        .newBuilder()
                                        .setBinding(DataCampOfferMapping.ContentBinding
                                                .newBuilder()
                                                .setApproved(Market.DataCamp.DataCampOfferMapping.Mapping
                                                        .newBuilder()
                                                        .setMarketSkuType(MARKET_SKU_TYPE_FAST)
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                );
            }
        });
    }

    private void enrichWithRandomSkuIdsModelIdsAndValidations(List<GcSkuTicket> gcSkuTickets, List<Long> skuIds,
                                                              List<Long> modelIds) {
        List<TicketValidationResult> validationResults1 = new ArrayList<>(gcSkuTickets.size());
        List<TicketValidationResult> validationResults2 = new ArrayList<>(gcSkuTickets.size());

        gcSkuTickets.forEach(gcSkuTicket -> {
            Long id = gcSkuTicket.getId();
            long skuId = id * 1000L;
            gcSkuTicketDao.setResultSkuModelId(id, skuId, id);
            skuIds.add(skuId);
            modelIds.add(id);
            validationResults1.add(
                    TicketValidationResult.invalid(
                            id,
                            Messages.get().notMultivalueParamDcp(PARAM_ID, PARAM_NAME, gcSkuTicket.getShopSku())
                    )
            );
            validationResults2.add(
                    TicketValidationResult.invalid(
                            id,
                            Messages.get().invalidHtmlTagInSku(
                                    gcSkuTicket.getShopSku(),
                                    Math.toIntExact(skuId) % 1231,
                                    PARAM_NAME,
                                    (long) PARAM_ID
                            )
                    )
            );
        });

        List<Long> ticketIds = gcSkuTickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList());

        gcSkuValidationDao.createValidations(ticketIds, VALIDATION_TYPE_1);
        gcSkuValidationDao.createValidations(ticketIds, VALIDATION_TYPE_2);
        gcSkuValidationDao.saveValidationResults(validationResults1, VALIDATION_TYPE_1);
        gcSkuValidationDao.saveValidationResults(validationResults2, VALIDATION_TYPE_2);
    }

    private void makeValidationsValid(List<GcSkuTicket> gcSkuTickets) {
        List<Long> ticketIds = gcSkuTickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList());
        gcSkuValidationDao.deleteAllValidationInfo(ticketIds, VALIDATION_TYPE_1);
        gcSkuValidationDao.deleteAllValidationInfo(ticketIds, VALIDATION_TYPE_2);

        dsl().update(GC_SKU_VALIDATION)
                .set(GC_SKU_VALIDATION.IS_OK, true)
                .execute();
    }

    private void checkSkuIds(List<Long> skuIds, List<Long> modelIds, List<GcSkuTicket> gcSkuTickets) {
        assertThat(gcSkuTickets)
                .extracting(gcSkuTicket ->
                        gcSkuTicket.getDatacampOffer().getContent().getBinding().getPartner().getMarketSkuId()
                )
                .containsExactlyInAnyOrderElementsOf(skuIds);

        assertThat(gcSkuTickets)
                .extracting(gcSkuTicket ->
                        gcSkuTicket.getDatacampOffer().getContent().getBinding().getPartner().getMarketModelId()
                )
                .containsExactlyInAnyOrderElementsOf(modelIds);
    }

    private void checkSkuTypes(List<GcSkuTicket> gcSkuTickets) {
        assertThat(gcSkuTickets)
                .extracting(
                        gcSkuTicket -> gcSkuTicket.getDatacampOffer().getContent()
                                .getBinding().getPartner().getMarketSkuType()
                ).allMatch(MARKET_SKU_TYPE_FAST::equals);
    }

    private List<GcSkuTicket> processAndCheckActualizingResultReturningNewTickets() {
        processDataBucketData = new ProcessDataBucketData();
        processDataBucketData.setDataBucketId(dataBucketId);
        ProcessTaskResult<ProcessDataBucketData> result = processActualizing(dataBucketId);
        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);
        return gcSkuTicketDao.findAll();
    }

    private ProcessTaskResult<ProcessDataBucketData> processActualizing(long dataBucketId) {
        List<GcSkuTicket> tickets = gcSkuTicketDao.getTicketsByDataBucket(dataBucketId);
        Map<Long, ModelStorage.Model> savedSkuMap = sendResultsToDatacampTaskAction
                .getSavedSkuMap(tickets);
        Map<Long, List<SendResultsToDatacampTaskAction.MessageInfosIsOk>> messageInfoMap =
                sendResultsToDatacampTaskAction.collectMessageInfos(tickets, savedSkuMap);
        sendResultsToDatacampTaskAction.enrichOffers(tickets, savedSkuMap, messageInfoMap);
        gcSkuTicketDao.updateDataCampOffers(tickets);
        return ProcessTaskResult.success(processDataBucketData);
    }

    @NotNull
    private DataCampExplanation.Explanation.Param explanationParam(String mixed, String one) {
        return DataCampExplanation.Explanation.Param.newBuilder().setName(mixed).setValue(one).build();
    }

    private void assertExplanation(String name, Object value, DataCampExplanation.Explanation.Param... expected) {
        List<DataCampExplanation.Explanation.Param> params = new ArrayList<>();
        sendResultsToDatacampTaskAction.createExplanationParam(name, value, params::add);
        assertThat(params)
                .containsExactlyInAnyOrder(expected);
    }

    @FunctionalInterface
    private interface SendResultsToDatacampTaskActionConstructor {
        SendResultsToDatacampTaskAction construct(
                GcSkuTicketDao gcSkuTicketDao,
                DatacampOfferDao datacampOfferDao,
                OfferContentProcessingResultsServiceBlockingStub offerContentProcessingResultsServiceBlockingStub,
                SkuRatingEvaluator skuRatingEvaluator,
                ModelStorageHelper modelStorageHelper
        );
    }
}
