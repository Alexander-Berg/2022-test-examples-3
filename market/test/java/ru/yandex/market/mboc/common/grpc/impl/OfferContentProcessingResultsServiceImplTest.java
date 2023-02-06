package ru.yandex.market.mboc.common.grpc.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampValidationResult;
import io.grpc.stub.StreamObserver;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.from.service.ContentProcessingResultHandlerService;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingAuditLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLogFacade;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingPgLog;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.ContentProcessingErrorType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ContentProcessingError;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.modelform.repository.ModelFormCacheRepository;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.ContentProcessingErrorRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.OfferContentProcessingResults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil.extractExternalBusinessSkuKey;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER_SELF;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.AUTO_PROCESSED;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_PROCESS;
import static ru.yandex.market.mboc.http.OfferContentProcessingResults.UpdateContentProcessingTasksRequest.DataCampState.FINISHED;

public class OfferContentProcessingResultsServiceImplTest extends BaseDbTestClass {
    private static final String PARTNER_CONTENT_ERROR_INVALID_STATE = "ir.partner_content.error.invalid_state";
    private static final Integer BUSINESS_ID = 1;
    private final Supplier supplier = OfferTestUtils.simpleSupplier().setId(BUSINESS_ID);
    ContentProcessingResultHandlerService contentProcessingResultHandlerService;
    OfferContentProcessingResultsServiceImpl service;
    @Autowired
    OfferRepository offerRepository;
    @Autowired
    ContentProcessingQueueRepository datacampOfferForContentProcessingRepository;
    @Autowired
    QueueFromContentProcessingRepository queueFromContentProcessingRepository;
    @Autowired
    CategoryCachingService categoryCachingService;
    @Autowired
    SupplierRepository supplierRepository;
    @Autowired
    TransactionHelper transactionHelper;
    @Autowired
    OfferBatchProcessor offerBatchProcessor;
    @Autowired
    AntiMappingRepository antiMappingRepository;
    @Autowired
    ModelFormCacheRepository modelFormCacheRepository;
    @Autowired
    CategoryKnowledgeRepository categoryKnowledgeRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    ContentProcessingQueueRepository contentProcessingQueueRepository;
    @Autowired
    private ContentProcessingErrorRepository contentProcessingErrorRepository;

    ContextedOfferDestinationCalculator contextedOfferDestinationCalculator;

    private SupplierService supplierService;
    private ContentProcessingLog contentProcessingLog;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private ModelStorageCachingServiceMock modelStorageCachingService;
    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private OfferUpdateSequenceService offerUpdateSequenceService;

    @Before
    public void before() {
        supplierRepository.insert(supplier);
        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService,
            offerBatchProcessor, supplierRepository);
        offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);

        contentProcessingLog = new ContentProcessingLogFacade(List.of(
            new ContentProcessingPgLog(jdbcTemplate),
            new ContentProcessingAuditLog(Mockito.mock(AuditWriter.class))
        ));

        offerUpdateSequenceService = Mockito.mock(OfferUpdateSequenceService.class);

        contentProcessingResultHandlerService = new ContentProcessingResultHandlerService(
            transactionHelper,
            offerRepository,
            queueFromContentProcessingRepository,
            offerMappingActionService,
            modelStorageCachingService,
            offersProcessingStatusService,
            contentProcessingLog,
            storageKeyValueService,
            contentProcessingQueueRepository,
            contentProcessingErrorRepository,
            hashCalculator,
            offerUpdateSequenceService
        );
        contextedOfferDestinationCalculator = new ContextedOfferDestinationCalculator(
            categoryInfoCache,
            storageKeyValueService
        );
        service = new OfferContentProcessingResultsServiceImpl(
            contentProcessingResultHandlerService,
            transactionHelper);
        categoryRepository.insertOrUpdate(new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID));
    }

    @Test
    public void shouldResendToGgOfferWithDeletedModelWithNoMapping() {
        var offer = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L).build()
        );

        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer),
                offer,
                false,
                null,
                null
            )
        );

        var results = contentProcessingResultHandlerService.processOffers(offers);
        var contentProcessingOffer = contentProcessingQueueRepository.findAll().get(0);

        assertThat(results)
            .hasSize(1);
        assertThat(results)
            .allMatch(result -> Objects.isNull(result.getErrorInfo()));
        assertNotNull(contentProcessingOffer);
        assertEquals((int) BUSINESS_ID, contentProcessingOffer.getBusinessId());
        assertEquals("offer1", contentProcessingOffer.getShopSku());
    }

    @Test
    public void shouldSkipGgResultOnDeletedModelIfOfferHasApprovedMapping() {
        var offer = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        var mbocOffer = offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L)
            .approvedSkuMapping(new Offer.Mapping(1, LocalDateTime.now()))
            .approvedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .build();
        offerRepository.insertOffers(mbocOffer);

        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer),
                offer,
                false,
                null,
                null
            )
        );

        var results = contentProcessingResultHandlerService.processOffers(offers);
        var contentProcessingOffers = contentProcessingQueueRepository.findAll();
        var resultOffer = offerRepository.findOfferByBusinessSkuKey(mbocOffer.getBusinessSkuKey());

        assertThat(results)
            .hasSize(1);
        assertThat(results)
            .allMatch(result -> Objects.isNull(result.getErrorInfo()));
        assertThat(contentProcessingOffers)
            .isEmpty();
        assertEquals(mbocOffer, resultOffer);
    }

    @Test
    public void errorIsNotThrownForDeletedModelInTotalErrorIncomingRequest() {
        var offerBuilder = dcOffer(BUSINESS_ID, "offer1", 11, 999L)
            .toBuilder();
        offerBuilder
            .getContentBuilder()
            .getPartnerBuilder().getMarketSpecificContentBuilder().getProcessingResponseBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);
        var offer = offerBuilder.build();

        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L).build()
        );

        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer),
                offer,
                false,
                null,
                null
            )
        );

        List<ContentProcessingResultHandlerService.Result> results =
            contentProcessingResultHandlerService.processOffers(offers);

        // no exception is thrown
        assertThat(results).hasSize(1);

        assertThatLogWasWritten(1);
    }

    private void assertThatLogWasWritten(int count) {
        jdbcTemplate.query("select count(*) from mbo_category.offer_content_processing_log", (row) -> {
            assertThat(row.getInt(1)).isEqualTo(count);
        });
    }

    @Test
    public void errorIsNotThrownForZeroModelInTotalErrorIncomingRequest() {
        var offerBuilder = dcOffer(BUSINESS_ID, "offer1", 11, 999L)
            .toBuilder();
        offerBuilder
            .getContentBuilder()
            .getPartnerBuilder().getMarketSpecificContentBuilder().getProcessingResponseBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);
        var offer = offerBuilder.build();

        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L).build()
                .updateApprovedSkuMapping(new Offer.Mapping(0L, LocalDateTime.now()))
        );

        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer),
                offer,
                false,
                null,
                null
            )
        );

        List<ContentProcessingResultHandlerService.Result> results =
            contentProcessingResultHandlerService.processOffers(offers);

        // no exception is thrown
        assertThat(results).hasSize(1);
        assertThatLogWasWritten(1);
    }

    @Test
    public void successfullyProcessIncomingRequests() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(30);
        var timestamp2 = DateTimeUtils.instantNow().minusSeconds(300);
        var offer1 = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        var offer2 = dcOffer(BUSINESS_ID, "offer2", 11, 999L);
        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L).acceptanceStatus(Offer.AcceptanceStatus.OK).build(),
            offer(2, BUSINESS_ID, "offer2", IN_PROCESS, 11L).acceptanceStatus(Offer.AcceptanceStatus.OK).build(),
            offer(3, BUSINESS_ID, "offer3", IN_PROCESS, 11L).acceptanceStatus(Offer.AcceptanceStatus.OK).build(),
            offer(4, BUSINESS_ID, "offer4", IN_PROCESS, 11L).acceptanceStatus(Offer.AcceptanceStatus.OK).build()
        );

        datacampOfferForContentProcessingRepository.insertBatch(
            new ContentProcessingOffer(BUSINESS_ID, "offer1", 1, timestamp2),
            new ContentProcessingOffer(BUSINESS_ID, "offer2", 1, timestamp2),
            new ContentProcessingOffer(BUSINESS_ID, "offer3", 1, timestamp2),
            new ContentProcessingOffer(BUSINESS_ID, "offer4", 1, timestamp2)
        );

        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var request = OfferContentProcessingResults.UpdateContentProcessingTasksRequest.newBuilder()
            .setDatacampContentProcessingState(FINISHED)
            .setRequestTs(DataCampOfferUtil.toTimestamp(timestamp))
            .setDatacampContentProcessingTaskId(111L)
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer1.getIdentifiers().getBusinessId())
                    .setShopSku(offer1.getIdentifiers().getOfferId())
                    .setContentProcessing(offer1)
                    .build()
            )
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer2.getIdentifiers().getBusinessId())
                    .setShopSku(offer2.getIdentifiers().getOfferId())
                    .setContentProcessing(offer2)
                    .build()
            )
            .build();

        List<Triple<OfferContentProcessingResults.UpdateContentProcessingTasksResponse, Throwable, Boolean>> responses =
            new ArrayList<>();
        StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> observer =
            new StreamObserver<>() {
                @Override
                public void onNext(OfferContentProcessingResults.UpdateContentProcessingTasksResponse value) {
                    responses.add(Triple.of(value, null, false));
                }

                @Override
                public void onError(Throwable t) {
                    responses.add(Triple.of(null, t, false));
                }

                @Override
                public void onCompleted() {
                    responses.add(Triple.of(null, null, true));
                }
            };

        service.updateDataCampContentProcessingTasks(request, observer);

        assertEquals(2, responses.size());
        assertEquals("Completed", Triple.of(null, null, true), responses.get(1));
        assertNull(responses.get(0).second);
        assertNotNull(responses.get(0).first);
        assertEquals(
            OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.Status.OK,
            responses.get(0).first.getStatusPerBusinessId(1).getStatus()
        );
        assertEquals(
            Map.of(
                "offer1", Triple.of(AUTO_PROCESSED, 999L, PARTNER_SELF),
                "offer2", Triple.of(AUTO_PROCESSED, 999L, PARTNER_SELF),
                "offer3", Triple.of(IN_PROCESS, null, null),
                "offer4", Triple.of(IN_PROCESS, null, null)
            ),
            offerRepository.findOffers(new OffersFilter()).stream()
                .collect(Collectors.toMap(
                    Offer::getShopSku,
                    o -> Triple.of(
                        o.getProcessingStatus(),
                        o.getApprovedSkuMapping() == null ? null : o.getApprovedSkuMapping().getMappingId(),
                        o.getApprovedSkuMappingConfidence()
                    )
                ))
        );

        assertEquals(
            Map.of(
                "offer1", Triple.of(AUTO_PROCESSED, (Long) null, Offer.MappingStatus.NONE),
                "offer2", Triple.of(AUTO_PROCESSED, (Long) null, Offer.MappingStatus.NONE),
                "offer3", Triple.of(IN_PROCESS, (Long) null, Offer.MappingStatus.NONE),
                "offer4", Triple.of(IN_PROCESS, (Long) null, Offer.MappingStatus.NONE)
            ),
            offerRepository.findOffers(new OffersFilter()).stream()
                .collect(Collectors.toMap(
                    Offer::getShopSku,
                    o -> Triple.of(
                        o.getProcessingStatus(),
                        o.getSupplierSkuMapping() == null ? null : o.getSupplierSkuMapping().getMappingId(),
                        o.getSupplierSkuMappingStatus()
                    )
                ))
        );

        Assertions.assertThat(contentProcessingErrorRepository.findAll()).isEmpty();
    }

    @Test
    public void insertProcessingErrors() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(30);
        var offer1 = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        var offer2 = dcOffer(BUSINESS_ID, "offer2", 11, 999L);

        var request = OfferContentProcessingResults.UpdateContentProcessingTasksRequest.newBuilder()
            .setDatacampContentProcessingState(FINISHED)
            .setRequestTs(DataCampOfferUtil.toTimestamp(timestamp))
            .setDatacampContentProcessingTaskId(111L)
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer1.getIdentifiers().getBusinessId())
                    .setShopSku(offer1.getIdentifiers().getOfferId())
                    .setContentProcessing(offer1)
                    .setProcessingError(OfferContentProcessingResults.UpdateContentProcessingTasksRequest
                        .BusinessIdsProcessingTask.ProcessingError.BAD_CATEGORY)
                    .build()
            )
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer2.getIdentifiers().getBusinessId())
                    .setShopSku(offer2.getIdentifiers().getOfferId())
                    .setProcessingError(OfferContentProcessingResults.UpdateContentProcessingTasksRequest
                        .BusinessIdsProcessingTask.ProcessingError.BAD_CATEGORY)
                    .build()
            )
            .build();

        List<Triple<OfferContentProcessingResults.UpdateContentProcessingTasksResponse, Throwable, Boolean>> responses =
            new ArrayList<>();
        StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> observer =
            new StreamObserver<>() {
                @Override
                public void onNext(OfferContentProcessingResults.UpdateContentProcessingTasksResponse value) {
                    responses.add(Triple.of(value, null, false));
                }

                @Override
                public void onError(Throwable t) {
                    responses.add(Triple.of(null, t, false));
                }

                @Override
                public void onCompleted() {
                    responses.add(Triple.of(null, null, true));
                }
            };

        service.updateDataCampContentProcessingTasks(request, observer);

        assertEquals(2, responses.size());
        assertEquals("Completed", Triple.of(null, null, true), responses.get(1));
        assertNull(responses.get(0).second);
        assertNotNull(responses.get(0).first);
        assertEquals(
            OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.Status.OK,
            responses.get(0).first.getStatusPerBusinessId(1).getStatus()
        );

        Map<BusinessSkuKey, ContentProcessingError> collect = contentProcessingErrorRepository.findAll().stream()
            .collect(Collectors.toMap(c -> new BusinessSkuKey(c.getBusinessId(), c.getShopSku()), c -> c));
        Assertions.assertThat(collect.keySet()).containsExactlyInAnyOrder(
            new BusinessSkuKey(BUSINESS_ID, "offer1"), new BusinessSkuKey(BUSINESS_ID, "offer2"));
    }

    @Test
    public void insertProcessingErrorsDoNotUpdateTs() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(30);
        var offer1 = dcOffer(BUSINESS_ID, "offer1", 11, 999L);

        Instant instant = Instant.now().minusSeconds(10000);
        contentProcessingErrorRepository.save(new ContentProcessingError(
            BUSINESS_ID, "offer1", ContentProcessingErrorType.BAD_CATEGORY, instant));

        var request = OfferContentProcessingResults.UpdateContentProcessingTasksRequest.newBuilder()
            .setDatacampContentProcessingState(FINISHED)
            .setRequestTs(DataCampOfferUtil.toTimestamp(timestamp))
            .setDatacampContentProcessingTaskId(111L)
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer1.getIdentifiers().getBusinessId())
                    .setShopSku(offer1.getIdentifiers().getOfferId())
                    .setContentProcessing(offer1)
                    .setProcessingError(OfferContentProcessingResults.UpdateContentProcessingTasksRequest
                        .BusinessIdsProcessingTask.ProcessingError.BAD_CATEGORY)
                    .build()
            )
            .build();

        List<Triple<OfferContentProcessingResults.UpdateContentProcessingTasksResponse, Throwable, Boolean>> responses =
            new ArrayList<>();
        StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> observer =
            new StreamObserver<>() {
                @Override
                public void onNext(OfferContentProcessingResults.UpdateContentProcessingTasksResponse value) {
                    responses.add(Triple.of(value, null, false));
                }

                @Override
                public void onError(Throwable t) {
                    responses.add(Triple.of(null, t, false));
                }

                @Override
                public void onCompleted() {
                    responses.add(Triple.of(null, null, true));
                }
            };

        service.updateDataCampContentProcessingTasks(request, observer);

        List<ContentProcessingError> all = contentProcessingErrorRepository.findAll();
        Assertions.assertThat(all).hasSize(1);
        Assert.assertEquals(instant, all.get(0).getInsertedTs());
    }

    @Test
    public void partnerContentErrorInvalidState() {
        int prevCount = contentProcessingQueueRepository.findAll().size();
        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        DataCampOffer.Offer.Builder offerBuilder = dcOffer(BUSINESS_ID, "offer1", 11, 999L).toBuilder();
        offerBuilder
            .setResolution(DataCampResolution.Resolution.newBuilder()
                .addBySource(DataCampResolution.Verdicts.newBuilder()
                    .addVerdict(DataCampResolution.Verdict.newBuilder()
                        .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                            .addMessages(DataCampExplanation.Explanation.newBuilder()
                                .setCode(ContentProcessingResultHandlerService.PARTNER_CONTENT_ERROR_INVALID_STATE)
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .getContentBuilder()
            .getPartnerBuilder().getMarketSpecificContentBuilder().getProcessingResponseBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);

        var offer = offerBuilder.build();

        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L).build()
        );

        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer),
                offer,
                false,
                null,
                null
            )
        );

        contentProcessingResultHandlerService.processOffers(offers);

        assertThat(contentProcessingQueueRepository.findAll().size()).isEqualTo(prevCount + 1);
    }

    @Test
    public void mappingOnFcButOfferMappingPskuTest() {
        int count = contentProcessingQueueRepository.findAll().size();
        var timestamp = DateTimeUtils.instantNow().minusSeconds(30);
        var timestamp2 = DateTimeUtils.instantNow().minusSeconds(300);
        var offer1 = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        Offer offer = offer(1, BUSINESS_ID, "offer1", IN_PROCESS, 11L)
            .acceptanceStatus(Offer.AcceptanceStatus.OK).build().updateApprovedSkuMapping(new Offer.Mapping(1L,
                LocalDateTime.now(), Offer.SkuType.PARTNER20), PARTNER_SELF);
        offerRepository.insertOffers(offer);

        datacampOfferForContentProcessingRepository.insertBatch(
            new ContentProcessingOffer(BUSINESS_ID, "offer1", 1, timestamp2)
        );

        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.FAST_SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var request = OfferContentProcessingResults.UpdateContentProcessingTasksRequest.newBuilder()
            .setDatacampContentProcessingState(FINISHED)
            .setRequestTs(DataCampOfferUtil.toTimestamp(timestamp))
            .setDatacampContentProcessingTaskId(111L)
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offer1.getIdentifiers().getBusinessId())
                    .setShopSku(offer1.getIdentifiers().getOfferId())
                    .setContentProcessing(offer1)
                    .build()
            )
            .build();

        List<Triple<OfferContentProcessingResults.UpdateContentProcessingTasksResponse, Throwable, Boolean>> responses =
            new ArrayList<>();
        StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> observer =
            new StreamObserver<>() {
                @Override
                public void onNext(OfferContentProcessingResults.UpdateContentProcessingTasksResponse value) {
                    responses.add(Triple.of(value, null, false));
                }

                @Override
                public void onError(Throwable t) {
                    responses.add(Triple.of(null, t, false));
                }

                @Override
                public void onCompleted() {
                    responses.add(Triple.of(null, null, true));
                }
            };

        service.updateDataCampContentProcessingTasks(request, observer);
        assertEquals(count, contentProcessingQueueRepository.findAll().size() - 1);
        assertTrue(offer.hasApprovedSkuMapping());
    }

    @Test
    public void successfullyProcessIncomingRequestsWithDeletedOffer() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(30);
        var timestamp2 = DateTimeUtils.instantNow().minusSeconds(300);
        var offerPresent = dcOffer(BUSINESS_ID, "offerPresent", 11, 999L);
        var offerDeleted = dcOffer(BUSINESS_ID, "offerDeleted", 11, 999L);
        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offerPresent", IN_PROCESS, 11L)
                .acceptanceStatus(Offer.AcceptanceStatus.OK)
                .build()
        );

        datacampOfferForContentProcessingRepository.insertBatch(
            new ContentProcessingOffer(BUSINESS_ID, "offerPresent", 1, timestamp2),
            new ContentProcessingOffer(BUSINESS_ID, "offerDeleted", 1, timestamp2)
        );

        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var request = OfferContentProcessingResults.UpdateContentProcessingTasksRequest.newBuilder()
            .setDatacampContentProcessingState(FINISHED)
            .setRequestTs(DataCampOfferUtil.toTimestamp(timestamp))
            .setDatacampContentProcessingTaskId(111L)
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offerPresent.getIdentifiers().getBusinessId())
                    .setShopSku(offerPresent.getIdentifiers().getOfferId())
                    .setContentProcessing(offerPresent)
                    .build()
            )
            .addContentProcessingTask(
                OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask.newBuilder()
                    .setBusinessId(offerDeleted.getIdentifiers().getBusinessId())
                    .setShopSku(offerDeleted.getIdentifiers().getOfferId())
                    .setContentProcessing(offerDeleted)
                    .build()
            )
            .build();

        List<Triple<OfferContentProcessingResults.UpdateContentProcessingTasksResponse, Throwable, Boolean>> responses =
            new ArrayList<>();
        StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> observer =
            new StreamObserver<>() {
                @Override
                public void onNext(OfferContentProcessingResults.UpdateContentProcessingTasksResponse value) {
                    responses.add(Triple.of(value, null, false));
                }

                @Override
                public void onError(Throwable t) {
                    responses.add(Triple.of(null, t, false));
                }

                @Override
                public void onCompleted() {
                    responses.add(Triple.of(null, null, true));
                }
            };

        service.updateDataCampContentProcessingTasks(request, observer);

        assertEquals(2, responses.size());
        assertEquals("Completed", Triple.of(null, null, true), responses.get(1));
        assertNull(responses.get(0).second);
        assertThat(responses.get(0).first).isNotNull();
        assertThat(responses.get(0).first.getStatusPerBusinessIdList())
            .extracting(OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus::getStatus)
            .containsExactly(
                OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.Status.OK,
                OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.Status.OK
            );

        List<Offer> offersInRepo = offerRepository.findOffers(new OffersFilter());

        assertThat(offersInRepo)
            .extracting(o -> Tuple.tuple(
                o.getProcessingStatus(),
                o.getApprovedSkuMapping() == null ? null : o.getApprovedSkuMapping().getMappingId(),
                o.getApprovedSkuMappingConfidence(),
                o.getSupplierSkuMapping() == null ? null : o.getSupplierSkuMapping().getMappingId(),
                o.getSupplierSkuMappingStatus()
            ))
            .containsExactly(
                Tuple.tuple(
                    AUTO_PROCESSED,
                    999L, PARTNER_SELF,
                    null, Offer.MappingStatus.NONE
                )
            );
    }

    @Test
    public void successfullyUpdateOffers() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(300);
        var offer1 = dcOffer(BUSINESS_ID, "offer1", 11, 999L);
        var offer2 = dcOffer(BUSINESS_ID, "offer2", null, null);
        var offer3 = dcOffer(BUSINESS_ID, "offer3", null, null);

        datacampOfferForContentProcessingRepository.insertBatch(
            new ContentProcessingOffer(BUSINESS_ID, "offer1", 1, timestamp),
            new ContentProcessingOffer(BUSINESS_ID, "offer2", 1, timestamp),
            new ContentProcessingOffer(BUSINESS_ID, "offer3", 1, timestamp)
        );
        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));
        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer1), offer1, false, null, null
            ),
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer2), offer2, false, null, null
            ),
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer3), offer3, false, null, null
            )
        );
        var categoryId = 11L;
        categoryKnowledgeService.addCategory(categoryId);
        categoryRepository.insertOrUpdate(new Category().setCategoryId(categoryId));
        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer1", IN_PROCESS, categoryId)
                .acceptanceStatus(Offer.AcceptanceStatus.OK)
                .marketSpecificContentHashSent(hashCalculator.marketSpecificContentHash(offer1,
                        new Offer(),
                        contextedOfferDestinationCalculator)
                    .orElse(null))
                .build(),
            offer(2, BUSINESS_ID, "offer2", IN_PROCESS, categoryId)
                .approvedSkuMapping(new Offer.Mapping(998, LocalDateTime.now()))
                .approvedSkuMappingConfidence(PARTNER)
                .marketSpecificContentHashSent(hashCalculator.marketSpecificContentHash(offer2,
                        new Offer(),
                        contextedOfferDestinationCalculator)
                    .orElse(null))
                .acceptanceStatus(Offer.AcceptanceStatus.OK)
                .offerDestination(Offer.MappingDestination.BLUE)
                .bindingKind(Offer.BindingKind.APPROVED)
                .build(),
            offer(3, BUSINESS_ID, "offer3", IN_PROCESS, categoryId)
                .acceptanceStatus(Offer.AcceptanceStatus.OK)
                .marketSpecificContentHashSent(123314123L).build()
        );

        contentProcessingResultHandlerService.processOffers(offers);

        assertEquals(
            Map.of(
                "offer1", Triple.of(AUTO_PROCESSED, 999L, PARTNER_SELF),
                "offer2", Triple.of(IN_PROCESS, 998L, PARTNER),
                "offer3", Triple.of(IN_PROCESS, null, null)
            ),
            offerRepository.findOffers(new OffersFilter()).stream()
                .collect(Collectors.toMap(
                    Offer::getShopSku,
                    o -> Triple.of(
                        o.getProcessingStatus(),
                        o.getApprovedSkuMapping() == null ? null : o.getApprovedSkuMapping().getMappingId(),
                        o.getApprovedSkuMappingConfidence()
                    )
                ))
        );

        assertThatLogWasWritten(3);
    }

    @Test
    public void doNotSetPartnerErrorsForForcefullySentOffers() {
        var timestamp = DateTimeUtils.instantNow().minusSeconds(300);
        var offerBuilder = dcOffer(BUSINESS_ID, "offer", 11, 999L).toBuilder();
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder().getProcessingResponseBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR);
        var offer = offerBuilder.build();

        datacampOfferForContentProcessingRepository.insertBatch(
            new ContentProcessingOffer(BUSINESS_ID, "offer", 1, timestamp)
        );
        modelStorageCachingService.addModel(
            new Model().setId(999L).setCategoryId(11)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));
        var offers = List.of(
            new ContentProcessingResultHandlerService.OfferToProcess(
                extractExternalBusinessSkuKey(offer), offer, true, null, null
            )
        );
        var categoryId = 11L;
        categoryKnowledgeService.addCategory(categoryId);
        categoryRepository.insertOrUpdate(new Category().setCategoryId(categoryId));
        offerRepository.insertOffers(
            offer(1, BUSINESS_ID, "offer", IN_PROCESS, categoryId)
                .acceptanceStatus(Offer.AcceptanceStatus.OK)
                .marketSpecificContentHashSent(hashCalculator.marketSpecificContentHash(
                        offer, new Offer(), contextedOfferDestinationCalculator)
                    .orElse(null))
                .build()
        );

        contentProcessingResultHandlerService.processOffers(offers);

        var currentOffer = offerRepository.findOfferByBusinessSkuKey(extractExternalBusinessSkuKey(offer));
        assertThat(currentOffer.getProcessingStatus()).isEqualTo(IN_PROCESS);
        assertThat(currentOffer.getApprovedSkuMapping()).isEqualTo(null);
        assertThat(currentOffer.getContentStatusActiveError()).isEqualTo(null);

        assertThatLogWasWritten(1);
    }

    static DataCampOffer.Offer dcOffer(int businessId, String offerId, Integer marketCategoryId, Long marketSkuId) {
        var b = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(businessId)
                .setOfferId(offerId)
                .build()
            ).setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(100500L)
                        .build())
                    .build())
                .build());
        var content = DataCampOfferContent.OfferContent.newBuilder();
        if (marketCategoryId != null && marketSkuId != null) {
            content.setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                .setPartner(DataCampOfferMapping.Mapping.newBuilder()
                    .setMarketCategoryId(marketCategoryId)
                    .setMarketSkuId(marketSkuId)
                    .setMarketModelName("model" + marketSkuId)
                    .build()
                )
                .build()
            );
        }
        content.getPartnerBuilder().getMarketSpecificContentBuilder().getProcessingResponseBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK);
        b.setContent(content.build());
        return b.build();
    }

    static Offer.OfferBuilder offer(long id, int businessId, String shopSku, Offer.ProcessingStatus status,
                                    Long categoryId) {
        return Offer.builder()
            .id(id)
            .title("title")
            .shopCategoryName("shop category")
            .businessId(businessId)
            .shopSku(shopSku)
            .processingStatus(status)
            .categoryId(categoryId);
    }

}
