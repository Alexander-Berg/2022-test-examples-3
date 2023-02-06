package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampExplanation.Explanation.Level;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.UltraControllerServiceData.UltraController;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.ir.http.OfferContentProcessing;
import ru.yandex.market.ir.http.OfferContentProcessingServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisherMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.pgupdateseq.PgUpdateSeqRow;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.app.offers.OfferContentStateController;
import ru.yandex.market.mboc.app.pipeline.BasePipelineTest;
import ru.yandex.market.mboc.app.pipeline.GenericScenario;
import ru.yandex.market.mboc.common.contentprocessing.from.service.ContentProcessingResultHandlerService;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingAuditLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLogFacade;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingPgLog;
import ru.yandex.market.mboc.common.contentprocessing.to.ContentProcessingObserver;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AdaptedForTestingGroupedContentProcessingSenderServiceImpl;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AdaptedForTestingUngroupedContentProcessingSenderServiceImpl;
import ru.yandex.market.mboc.common.contentprocessing.to.service.ContentProcessingSenderService;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampServiceMock;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.logbroker.events.ThrottlingLogbrokerEventPublisher;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.upload.OfferUploadQueueItem;
import ru.yandex.market.mboc.common.offers.repository.ContentProcessingErrorRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.services.datacamp.ImportMessageHandlerContext;
import ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandler;
import ru.yandex.market.mboc.common.services.datacamp.OfferGenerationHelper;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.services.migration.MigrationModelService;
import ru.yandex.market.mboc.common.services.offers.processing.RemoveOfferService;
import ru.yandex.market.mboc.common.services.offers.upload.ErpOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.offers.upload.MdmOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.offers.upload.OfferChangeForUploadObserver;
import ru.yandex.market.mboc.common.services.offers.upload.OfferUploadQueueService;
import ru.yandex.market.mboc.common.services.offers.upload.YtOfferUploadQueueService;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil.Lens.content;
import static ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil.Lens.contentStatus;
import static ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil.Lens.contentSystemStatus;
import static ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil.getVerdictExplanations;
import static ru.yandex.market.mboc.common.services.datacamp.OfferGenerationHelper.toUnitedOffersBatch;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.PARAM_NAME;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.PARAM_VAL;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.whiteSupplierUnderBiz;

/**
 * Base class for DataCamp pipeline integration tests.
 */
public abstract class BaseDatacampPipelineTest extends BasePipelineTest {

    protected static final String SHOP_SKU_DCP = "shop-sku-dcp-1";

    protected static final int CSKU_WHITE_LIST_BIZ = 123234;

    @Autowired
    protected ContentProcessingQueueRepository contentProcessingQueueRepository;
    @Autowired
    protected RemovedOfferRepository removedOfferRepository;
    @Autowired
    protected ContentProcessingObserver observer;
    @Autowired
    protected OfferChangeForUploadObserver offerChangeForUploadObserver;
    @Autowired
    protected YtOfferUploadQueueService ytOfferUploadQueueService;
    @Autowired
    protected ErpOfferUploadQueueService erpOfferUploadQueueService;
    @Autowired
    protected MdmOfferUploadQueueService mdmOfferUploadQueueService;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    private ContentProcessingErrorRepository contentProcessingErrorRepository;
    @Autowired
    protected MigrationModelRepository migrationModelRepository;

    protected SupplierConverterServiceMock supplierConverterService;
    protected DataCampIdentifiersService dataCampIdentifiersService;
    protected DataCampConverterService dataCampConverterService;
    protected DatacampImportService datacampImportService;
    protected LogbrokerEventPublisherMock<DataCampUnitedOffersEvent> logbrokerEventPublisherMock;
    protected LogbrokerEventPublisherMock<DataCampUnitedOffersEvent> logbroker1pEventPublisherMock;
    protected DatacampServiceMock datacampServiceMock;
    protected ManagedChannel grpcChannel;
    protected Server grpcServer;
    protected OfferContentProcessingServiceGrpc.OfferContentProcessingServiceImplBase mockServer;
    protected UltraControllerServiceMock ultraControllerServiceMock;
    protected LogbrokerDatacampOfferMessageHandler logbrokerDatacampOfferMessageHandler;
    protected ContentProcessingSenderService ungroupedContentProcessingSenderService;
    protected ContentProcessingSenderService groupedContentProcessingSenderService;
    protected ContentProcessingResultHandlerService contentProcessingResultHandlerService;
    protected SendDataCampOfferStatesService sendDataCampOfferStatesService;
    protected OfferContentStateController offerContentStateController;
    protected ContentProcessingLog contentProcessingLog;
    protected RemoveOfferService removeOfferService;
    protected MigrationModelService migrationModelService;

    @Before
    public void setUpDc() throws Exception {
        mockMigrationModelService();
        mockDcOfferMessageHandler();
        mockDcOfferForCpService();
        mockDcOfferForCpResultsService();
        mockSendDataCampOfferStatesService();
        mockOfferContentStateController();
    }

    private void mockMigrationModelService() {
        migrationModelService = new MigrationModelService(
            migrationModelRepository,
            mskuRepository
        );
    }

    private void mockOfferContentStateController() {
        ultraControllerServiceMock = new UltraControllerServiceMock();
        offerContentStateController = new OfferContentStateController(ultraControllerServiceMock,
            offerRepository, supplierRepository, mskuRepository, categoryCachingService, offersEnrichmentService,
            dataCampConverterService, dataCampIdentifiersService,
            offerMappingActionService,
            modelStorageCachingService, applySettingsService, antiMappingRepository, migrationModelService);
    }

    private void mockDcOfferMessageHandler() {
        logbrokerEventPublisherMock = new LogbrokerEventPublisherMock<>();
        logbroker1pEventPublisherMock = new LogbrokerEventPublisherMock<>();
        supplierConverterService = new SupplierConverterServiceMock();
        dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID,
            SupplierConverterServiceMock.BERU_BUSINESS_ID,
            supplierConverterService
        );
        datacampImportService = Mockito.mock(DatacampImportService.class);

        dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            offerCategoryRestrictionCalculator,
            storageKeyValueService,
            true);
        removeOfferService = new RemoveOfferService(
            removedOfferRepository,
            offerRepository,
            datacampImportService,
            migrationService,
            transactionHelper,
            offerDestinationCalculator
        );
        logbrokerDatacampOfferMessageHandler = new LogbrokerDatacampOfferMessageHandler(
            addProductInfoHelperService,
            new ThrottlingLogbrokerEventPublisher<>(logbrokerEventPublisherMock),
            categoryCachingService,
            supplierService,
            dataCampIdentifiersService,
            dataCampConverterService,
            datacampImportService,
            offerRepository,
            mskuRepository,
            migrationService,
            removeOfferService,
            globalVendorsCachingService,
            storageKeyValueService,
            antiMappingRepository,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
            migrationModelService,
            SupplierConverterServiceMock.BERU_BUSINESS_ID
        );
    }

    private void mockDcOfferForCpService() throws Exception {
        OfferContentProcessingServiceGrpc.OfferContentProcessingServiceImplBase rpcServiceMock
            = new OfferContentProcessingServiceGrpc.OfferContentProcessingServiceImplBase() {
            @Override
            public void startContentProcessing(OfferContentProcessing.OfferContentProcessingRequest request,
            StreamObserver<OfferContentProcessing.OfferContentProcessingResponse> responseObserver) {
                responseObserver.onNext(OfferContentProcessing.OfferContentProcessingResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
        mockServer = Mockito.mock(
            OfferContentProcessingServiceGrpc.OfferContentProcessingServiceImplBase.class,
            AdditionalAnswers.delegatesTo(rpcServiceMock)
        );
        String grpcServerName = InProcessServerBuilder.generateName();
        grpcServer = InProcessServerBuilder.forName(grpcServerName)
            .directExecutor()
            .addService(mockServer)
            .build()
            .start();
        grpcChannel = InProcessChannelBuilder.forName(grpcServerName).directExecutor().build();
        var agGrpc = OfferContentProcessingServiceGrpc.newBlockingStub(grpcChannel);
        datacampServiceMock = new DatacampServiceMock();

        contentProcessingLog = new ContentProcessingLogFacade(List.of(
            new ContentProcessingPgLog(jdbcTemplate),
            new ContentProcessingAuditLog(Mockito.mock(AuditWriter.class))
        ));

        ungroupedContentProcessingSenderService =
            new AdaptedForTestingUngroupedContentProcessingSenderServiceImpl(
                contentProcessingQueueRepository,
                agGrpc,
                transactionHelper,
                offerRepository,
                mskuRepository,
                categoryCachingService,
                supplierService,
                offersProcessingStatusService,
                needContentStatusService,
                dataCampConverterService,
                datacampServiceMock,
                datacampImportService,
                storageKeyValueService,
                contentProcessingLog,
                new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
                hashCalculator,
                migrationModelRepository
            );
        groupedContentProcessingSenderService =
            new AdaptedForTestingGroupedContentProcessingSenderServiceImpl(
                contentProcessingQueueRepository,
                agGrpc,
                transactionHelper,
                offerRepository,
                mskuRepository,
                categoryCachingService,
                supplierService,
                offersProcessingStatusService,
                needContentStatusService,
                datacampServiceMock,
                dataCampConverterService,
                datacampImportService,
                storageKeyValueService,
                contentProcessingLog,
                new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
                hashCalculator,
                migrationModelRepository
            );
    }

    private void mockDcOfferForCpResultsService() {
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
    }

    private void mockSendDataCampOfferStatesService() {
        sendDataCampOfferStatesService = new SendDataCampOfferStatesService(
            transactionHelper,
            offerRepository,
            mskuRepository,
            antiMappingRepository,
            supplierRepository,
            queueFromContentProcessingRepository,
            offerMetaRepository,
            categoryCachingService,
            dataCampConverterService,
            migrationService,
            masterDataFor1pRepository,
            new ThrottlingLogbrokerEventPublisher<>(logbrokerEventPublisherMock),
            new ThrottlingLogbrokerEventPublisher<>(logbroker1pEventPublisherMock),
            globalVendorsCachingService,
            acceptanceService,
            storageKeyValueService,
            migrationModelService
        );
    }

    @After
    public void after() throws InterruptedException {
        grpcServer.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
        grpcChannel.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
    }

    protected <T> Consumer<T> tmsSendDataCampOfferStates() {
        return task(() -> {
            Long lastModifiedSequenceId = offerUpdateSequenceService.getLastModifiedSequenceId();
            Long count = offerUpdateSequenceService.getNewModifiedSequenceCount();
            offerUpdateSequenceService.copyOfferChangesFromStaging();
            List<PgUpdateSeqRow<Long>> logIdRows =
                offerUpdateSequenceService.getModifiedRecordsIdBatch(lastModifiedSequenceId, count.intValue());
            List<Long> offerIds = logIdRows.stream().map(PgUpdateSeqRow::getKey).collect(Collectors.toList());
            sendDataCampOfferStatesService.sendStates(offerIds);
        });
    }

    protected <T> Consumer<T> importOfferFromDC(DatacampMessageOuterClass.DatacampMessage datacampMessage) {
        return ignored -> {
            try {
                logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(datacampMessage));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected <T> Consumer<T> importOfferFromDCWithExhaustedRetry(
        DatacampMessageOuterClass.DatacampMessage datacampMessage) {
        DataCampOffer.Offer basic = datacampMessage.getUnitedOffers(0).getOffer(0).getBasic();
        BusinessSkuKey businessSkuKey = dataCampConverterService.createBusinessSkuKey(basic);
        return ignored -> {
            try {
                logbrokerDatacampOfferMessageHandler.processAs(
                    Collections.singletonList(datacampMessage),
                    new ImportMessageHandlerContext(
                        Map.of(businessSkuKey, 6),
                        ImportMessageHandlerContext.DataSourceType.DEFAULT
                    )
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected BiConsumer<String, DCPipelineState> verifyOfferIsNotInContentProcessingQueue(int businessId,
                                                                                           String shopSku) {
        return (description, state) -> {
            var inQueue = contentProcessingQueueRepository
                .findAllByBusinessSkuKeys(new BusinessSkuKey(businessId, shopSku));
            Assertions.assertThat(inQueue).isEmpty();
        };
    }

    protected void makeContentProcessingOfferChangedDayAgo(int businessId, String shopSku) {
        ContentProcessingOffer offer = contentProcessingQueueRepository
            .findAllByBusinessSkuKeys(new BusinessSkuKey(businessId, shopSku))
            .stream().findFirst().orElseThrow();
        offer.setChanged(Instant.now().minus(1, ChronoUnit.DAYS));
        contentProcessingQueueRepository.update(offer);
    }

    protected <T> Consumer<T> agSendsDCMapping(DataCampOffer.Offer dcOffer, Long modelId, long skuId) {
        return offer -> {
            DataCampOffer.Offer.Builder builder = dcOffer.toBuilder();

            DataCampOfferMapping.Mapping.Builder mappingBuilder = DataCampOfferMapping.Mapping.newBuilder();
            if (modelId != null) {
                mappingBuilder.setMarketModelId(modelId);
            }
            mappingBuilder.setMarketSkuId(skuId);
            builder.getContentBuilder().getBindingBuilder().setPartner(mappingBuilder);

            DataCampOfferMarketContent.MarketContentProcessing.Builder processingResponse =
                DataCampOfferMarketContent.MarketContentProcessing.newBuilder();
            processingResponse.setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK);

            builder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
                .setProcessingResponse(processingResponse);

            contentProcessingResultHandlerService.processOffers(List.of(
                new ContentProcessingResultHandlerService.OfferToProcess(
                    DataCampOfferUtil.extractExternalBusinessSkuKey(dcOffer),
                    builder.build(),
                    false,
                    null,
                    null
                )
            ));
        };
    }

    protected static DatacampMessageOuterClass.DatacampMessage toMessage(int shopId, DataCampOffer.Offer... offers) {
        return toMessage(Collections.singleton(shopId), offers);
    }

    protected static DatacampMessageOuterClass.DatacampMessage toMessage(Set<Integer> shopIds,
                                                                         DataCampOffer.Offer... offers) {
        return DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addUnitedOffers(toUnitedOffersBatch(shopIds, offers))
            .build();
    }

    protected void putOfferToDatacampService(DataCampOffer.Offer dcOffer) {
        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(dcOffer)
            .build();
        datacampServiceMock.putOffer(unitedOffer);
    }

    protected BiConsumer<String, DCPipelineState> assertNoOfferInRepo() {
        return (description, state) ->
            Assertions.assertThat(offerRepository.findOfferByBusinessSkuKey(state.getOffer().getBusinessSkuKey()))
                .as("Step: %s", description)
                .isNull();
    }

    protected BiConsumer<String, DCPipelineState> assertLastDCServiceOffers() {
        return (description, state) -> {
            Offer offer = state.getOffer();
            Map<Integer, DataCampOffer.Offer> expectedServiceOffers = state.getDatacampServiceOffers();
            Map<Integer, DataCampOffer.Offer> serviceOffers = getLastSentDCOffer(offer)
                .map(DataCampUnitedOffer.UnitedOffer::getServiceMap)
                .orElse(Collections.emptyMap());
            if (CollectionUtils.isEmpty(serviceOffers) && CollectionUtils.isEmpty(expectedServiceOffers)) {
                return;
            }
            Assertions.assertThat(serviceOffers)
                .as("Step: %s", description)
                .hasSameSizeAs(expectedServiceOffers)
                .containsKeys(expectedServiceOffers.keySet().toArray(new Integer[0]))
                .containsOnlyKeys(expectedServiceOffers.keySet().toArray(new Integer[0]));
            SoftAssertions.assertSoftly(softly -> {
                serviceOffers.forEach((supplierId, serviceOffer) -> {
                    Function<DataCampOffer.Offer, DataCampOffer.Offer> testDataExtractor = sourceOffer -> {
                        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();
                        builder
                            .setIdentifiers(sourceOffer.getIdentifiers())
                            .getStatusBuilder()
                            .addAllDisabled(sourceOffer.getStatus().getDisabledList().stream()
                                .map(flag -> DataCampOfferMeta.Flag.newBuilder()
                                    .setFlag(flag.getFlag())
                                    .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setSource(flag.getMeta().getSource())))
                                .map(DataCampOfferMeta.Flag.Builder::build)
                                .collect(Collectors.toList()));
                        content.then(contentStatus).then(contentSystemStatus).apply(sourceOffer)
                            .ifPresent(status -> builder.getContentBuilder()
                                .getStatusBuilder()
                                .getContentSystemStatusBuilder()
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                    .setSource(status.getMeta().getSource()))
                                .setServiceOfferState(status.getServiceOfferState()));
                        return builder.build();
                    };

                    softly.assertThat(testDataExtractor.apply(serviceOffer))
                        .as("Step: %s", description)
                        .isEqualToComparingFieldByFieldRecursively(
                            testDataExtractor.apply(expectedServiceOffers.get(supplierId))
                        );
                });
            });
        };
    }

    protected BiConsumer<String, DCPipelineState> assertLastDCBasicOffer() {
        return (description, state) -> {
            Offer offer = state.getOffer();
            DataCampOffer.Offer expectedBasicOffer = state.getDatacampOfferCopy()
                .map(DataCampUnitedOffer.UnitedOffer::getBasic)
                .orElse(null);
            DataCampOffer.Offer sentBasicOffer = getLastSentDCOffer(offer)
                .map(DataCampUnitedOffer.UnitedOffer::getBasic)
                .orElse(null);
            if (expectedBasicOffer == null || sentBasicOffer == null) {
                return;
            }

            List<Pair<String, Level>> expectedVerdicts = getVerdictExplanations(
                expectedBasicOffer,
                explanation -> Pair.of(explanation.getCode(), explanation.getLevel()));

            List<Pair<String, Level>> sentExplanation = getVerdictExplanations(
                sentBasicOffer,
                explanation -> Pair.of(explanation.getCode(), explanation.getLevel()));

            assertThat(sentExplanation)
                .containsExactlyElementsOf(expectedVerdicts);
        };
    }

    protected BiConsumer<String, DCPipelineState> assertLastDCContentSystemStatus() {
        return (description, state) -> {
            var status = state.getDatacampStatus();
            var offer = state.getOffer();
            DataCampContentStatus.ContentSystemStatus contentSystemStatus = getLastSentDCOffer(offer)
                .map(DataCampUnitedOffer.UnitedOffer::getBasic)
                .flatMap(DataCampOfferUtil.Lens.content
                    .then(DataCampOfferUtil.Lens.contentStatus)
                    .then(DataCampOfferUtil.Lens.contentSystemStatus))
                .orElse(null);
            if (contentSystemStatus == null && status == null) {
                return;
            }
            Assertions.assertThat(contentSystemStatus)
                .as("Step: %s", description)
                .isNotNull();
            Assertions.assertThat(contentSystemStatus)
                .as("Step: %s", description)
                .isEqualToComparingOnlyGivenFields(
                    status,
                    "allowCategorySelection",
                    "allowModelSelection",
                    "allowModelCreateUpdate",
                    "modelBarcodeRequired",
                    "cpcState",
                    "cpaState",
                    "statusContentVersion"
                );
            Assertions.assertThat(contentSystemStatus.getActiveErrorList())
                .as("Step: %s", description)
                .extracting(DataCampExplanation.Explanation::getCode)
                .containsExactlyInAnyOrderElementsOf(
                    status.getActiveErrorList().stream()
                        .map(DataCampExplanation.Explanation::getCode)
                        .collect(Collectors.toList())
                );
        };
    }

    protected Optional<DataCampUnitedOffer.UnitedOffer> getLastSentDCOffer(Offer offer) {
        return getLastSentDCOffer(offer.getBusinessSkuKey());
    }

    protected Optional<DataCampUnitedOffer.UnitedOffer> getLastSentDCOffer(BusinessSkuKey key) {
        return logbrokerEventPublisherMock.getSendEvents().stream()
            .map(DataCampUnitedOffersEvent::getPayload)
            .map(DatacampMessageOuterClass.DatacampMessage::getUnitedOffersList)
            .flatMap(Collection::stream)
            .map(DataCampUnitedOffer.UnitedOffersBatch::getOfferList)
            .flatMap(Collection::stream)
            .filter(dcOffer -> dataCampIdentifiersService.createBusinessSkuKey(dcOffer.getBasic())
                .equals(key))
            .reduce((first, second) -> second);
    }

    protected Offer testDCWhiteOffer() {
        return testDCOffer(whiteSupplierUnderBiz());
    }

    protected Offer testDCOffer(Supplier supplier) {
        return OfferTestUtils.simpleOffer(supplier)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setShopSku(SHOP_SKU_DCP)
            .setBarCode(BAR_CODE)
            .setVendorCode(OfferTestUtils.DEFAULT_VENDORCODE)
            .storeOfferContent(OfferContent.builder()
                .urls(Collections.singletonList(URL))
                .picUrls("pic1\npic2\npic3")
                .extraShopFields(Map.of(PARAM_NAME, Objects.toString(PARAM_VAL))
                ).build())
            .setDataCampOffer(true)
            .setDataCampContentVersion(0L)
            .updateAcceptanceStatusForTests(supplier.getType().isCpa() ?
                Offer.AcceptanceStatus.NEW : Offer.AcceptanceStatus.OK)
            .setClassifierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, 0.1d)
            .setMappingDestination(Offer.MappingDestination.WHITE);
    }

    protected DataCampOffer.Offer initialOffer() {
        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();
        builder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setOfferId(SHOP_SKU_DCP))
            .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                    .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(0L))))
            .setPictures(OfferBuilder.pictures("pic1", "pic2", "pic3"))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setUcMapping(DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                        .setMarketCategoryName(OfferTestUtils.DEFAULT_CATEGORY_NAME)))
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                        .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                            .setName(OfferTestUtils.DEFAULT_SHOP_CATEGORY_NAME))
                        .setTitle(OfferGenerationHelper.stringValue(OfferTestUtils.DEFAULT_TITLE))
                        .setVendorCode(OfferGenerationHelper.stringValue(OfferTestUtils.DEFAULT_VENDORCODE))
                        .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                            .addValue(BAR_CODE))
                        .setUrl(OfferGenerationHelper.stringValue(URL))
                        .setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
                            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                                .setName(PARAM_NAME)
                                .setValue(Objects.toString(PARAM_VAL))
                                .build())
                            .build())
                    ))
                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                    .setIrData(DataCampOfferContent.EnrichedOfferSubset.newBuilder()
                        .setClassifierCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID)
                        .setClassifierConfidentTopPercision(0.1)
                        .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN)))
            );
        return builder.build();
    }

    protected GenericScenario<DCPipelineState> createScenario() {
        return new GenericScenario<>(
            (description, state) -> {
                offerInRepoIsValid().accept(description, state.getOffer());
                assertLastDCContentSystemStatus().accept(description, state);
                assertLastDCServiceOffers().accept(description, state);
                assertLastDCBasicOffer().accept(description, state);
            },
            DCPipelineState::copy
        );
    }

    protected Consumer<DCPipelineState> run(Consumer<Offer> step) {
        return state -> step.accept(state == null ? null : state.getOffer());
    }

    protected BiConsumer<String, DCPipelineState> verifyOfferSentToAg() {
        return verifyOfferSentToAg((x, y) -> {
        });
    }

    protected BiConsumer<String, DCPipelineState> verifyOfferSentToAg(
        BiConsumer<String, DataCampOffer.Offer> requestAssertion
    ) {
        return (description, st) -> {
            // offer should be deleted from content processing
            Assertions.assertThat(contentProcessingQueueRepository
                .findAllByBusinessSkuKeys(st.getOffer().getBusinessSkuKey()))
                .as("Step: %s", description)
                .isEmpty();
            // offer should be sent to AG
            ArgumentCaptor<OfferContentProcessing.OfferContentProcessingRequest> requestCaptor =
                ArgumentCaptor.forClass(OfferContentProcessing.OfferContentProcessingRequest.class);
            Mockito.verify(mockServer, Mockito.times(1)
                .description(String.format("Step: %s", description)))
                .startContentProcessing(requestCaptor.capture(), Mockito.any());
            Mockito.clearInvocations(mockServer);

            OfferContentProcessing.OfferContentProcessingRequest sentRequest = requestCaptor.getValue();
            Assertions.assertThat(sentRequest.getOffersWithFlagsList()).as(description).hasSize(1);
            DataCampOffer.Offer sentOffer = sentRequest.getOffersWithFlags(0).getOffer();
            requestAssertion.accept(description, sentOffer);
        };
    }

    protected BiConsumer<String, DCPipelineState> verifyContentSystemStatusNoDiff(DataCampOffer.Offer dcOffer) {
        return verifyContentSystemStatusHandle(dcOffer, status -> {
        });
    }

    protected BiConsumer<String, DCPipelineState> verifyContentSystemStatusHandle(
        DataCampOffer.Offer dcOffer,
        Consumer<DataCampContentStatus.ContentSystemStatus.Builder> handleStatusDiff
    ) {
        return (description, st) -> {
            var builder = st.getDatacampStatus() != null ?
                st.getDatacampStatus().toBuilder()
                : DataCampContentStatus.ContentSystemStatus.newBuilder();
            handleStatusDiff.accept(builder);
            var expectedStatus = builder.build();
            var batch = DataCampOffer.OffersBatch.newBuilder()
                .addOffer(dcOffer)
                .build();
            var calculated = offerContentStateController.calculateContentStatus(batch);
            Assert.equals(1, calculated.getOfferCount());
            var status = calculated.getOffer(0).getContent().getStatus();
            Assertions.assertThat(status.getContentSystemStatus())
                .as("Step: %s", description)
                .isEqualToComparingOnlyGivenFields(expectedStatus,
                    "allowCategorySelection",
                    "allowModelSelection",
                    "allowModelCreateUpdate",
                    "modelBarcodeRequired",
                    "cpcState",
                    "cpaState");
        };
    }

    protected BiConsumer<String, DCPipelineState> verifyNotEnqueued(OfferUploadQueueService queueService) {
        return (description, state) -> {
            var offerDb = offerRepository.findOfferByBusinessSkuKey(state.getOffer().getBusinessSkuKey());
            Assertions.assertThat(queueService.getForUpload(10))
                .extracting(OfferUploadQueueItem::getOfferId)
                .doesNotContain(offerDb.getId());
        };
    }

    protected BiConsumer<String, DCPipelineState> verifyEnqueued(OfferUploadQueueService queueService) {
        return (description, state) -> {
            var offerDb = offerRepository.findOfferByBusinessSkuKey(state.getOffer().getBusinessSkuKey());
            Assertions.assertThat(queueService.getForUpload(10))
                .extracting(OfferUploadQueueItem::getOfferId)
                .contains(offerDb.getId());
        };
    }

    protected Map<DataCampValidationResult.RecommendationStatus, List<DataCampValidationResult.Feature>>
    unwrapAcceptanceRecommendations(Collection<DataCampResolution.Verdicts> verdicts) {
        return verdicts.stream()
            .map(DataCampResolution.Verdicts::getVerdictList)
            .flatMap(Collection::stream)
            .map(DataCampResolution.Verdict::getResultsList)
            .flatMap(Collection::stream)
            .filter(DataCampValidationResult.ValidationResult::hasRecommendationStatus)
            .collect(
                Collectors.toMap(
                    DataCampValidationResult.ValidationResult::getRecommendationStatus,
                    DataCampValidationResult.ValidationResult::getApplicationsList)
            );
    }

}
