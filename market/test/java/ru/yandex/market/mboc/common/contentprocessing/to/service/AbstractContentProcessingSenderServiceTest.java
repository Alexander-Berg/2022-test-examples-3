package ru.yandex.market.mboc.common.contentprocessing.to.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.ir.http.OfferContentProcessing;
import ru.yandex.market.ir.http.OfferContentProcessing.OfferContentProcessingResponse;
import ru.yandex.market.ir.http.OfferContentProcessing.OfferContentProcessingResponse.Status;
import ru.yandex.market.ir.http.OfferContentProcessingServiceGrpc;
import ru.yandex.market.ir.http.OfferContentProcessingServiceGrpc.OfferContentProcessingServiceBlockingStub;
import ru.yandex.market.ir.http.OfferContentProcessingServiceGrpc.OfferContentProcessingServiceImplBase;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingAuditLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLogFacade;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingPgLog;
import ru.yandex.market.mboc.common.contentprocessing.to.ContentProcessingObserver;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.FailureType;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampServiceMock;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuQualityEnum;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartmentService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.AVAILABLE;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.FAILED;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.REMOVED;
import static Market.DataCamp.DataCampOfferPictures.MarketPicture.Status.UNDEFINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepositoryImpl.MAX_INSERTED_DELAY_MINUTES;
import static ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepositoryImpl.MIN_CHANGED_DELAY_MINUTES;
import static ru.yandex.market.mboc.common.contentprocessing.to.service.AbstractContentProcessingSenderService.SEND_ONLY_MAIN_OFFERS_ENABLED;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.PROCESSED;
import static ru.yandex.market.mboc.common.utils.DateTimeUtils.instantNow;

public abstract class AbstractContentProcessingSenderServiceTest extends BaseDbTestClass {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Autowired
    protected ContentProcessingQueueRepository queue;
    @Autowired
    protected ContentProcessingObserver observer;
    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected IMasterDataRepository masterDataFor1pRepository;
    @Autowired
    protected OfferBatchProcessor offerBatchProcessor;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected SupplierRepository supplierRepository;
    @Autowired
    protected StorageKeyValueService keyValueService;
    @Autowired
    protected DatacampImportService datacampImportService;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    protected MigrationModelRepository migrationModelRepository;

    protected NeedContentStatusService needContentStatusService;
    protected OffersProcessingStatusService offersProcessingStatusService;

    protected OfferContentProcessingServiceImplBase agServiceImpl;
    protected OfferContentProcessingServiceBlockingStub agService;

    protected CategoryCachingServiceMock categoryService;
    protected DataCampConverterService dataCampConverterService;
    protected DatacampServiceMock dataCampService;
    protected ContentProcessingLog contentProcessingLog;

    protected AbstractContentProcessingSenderService service;

    protected Supplier simpleSupplier;
    protected Supplier businessSupplier;
    protected Supplier cskuSupplier;
    protected Supplier fulfillmentSupplier;

    protected Category allowedCategory;
    protected Category allowedCategoryWithBarcodes;
    protected Category restrictedCategory;

    @Before
    @SneakyThrows
    public void setUp() {
        // It's easier than to mock final class, trust me
        var grpcSrvName = InProcessServerBuilder.generateName();
        agServiceImpl = mock(OfferContentProcessingServiceImplBase.class);
        grpcCleanup.register(InProcessServerBuilder.forName(grpcSrvName)
            .directExecutor().addService(agServiceImpl).build().start());
        var channel = grpcCleanup.register(InProcessChannelBuilder.forName(grpcSrvName).directExecutor().build());
        agService = OfferContentProcessingServiceGrpc.newBlockingStub(channel);

        categoryService = new CategoryCachingServiceMock().enableAuto();

        var dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID,
            SupplierConverterServiceMock.BERU_BUSINESS_ID,
            new SupplierConverterServiceMock()
        );
        var offerCategoryRestrictionCalculator = new OfferCategoryRestrictionCalculator(
            Mockito.mock(HonestMarkDepartmentService.class), new CategoryInfoCacheImpl(categoryInfoRepository));

        dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            offerCategoryRestrictionCalculator,
            storageKeyValueService,
            true
        );
        dataCampService = new DatacampServiceMock();

        // DC version
        // add to retry queue

        simpleSupplier = OfferTestUtils.simpleSupplier().setId(333);
        businessSupplier = OfferTestUtils.businessSupplier().setId(444).setNewContentPipeline(true);
        cskuSupplier = OfferTestUtils.businessSupplier().setId(100500).setNewContentPipeline(true);
        fulfillmentSupplier = OfferTestUtils.fulfillmentSupplier().setId(555).setFulfillment(true).setCrossdock(true);
        supplierRepository.insertBatch(simpleSupplier, businessSupplier, cskuSupplier, fulfillmentSupplier);

        allowedCategory = new Category()
            .setCategoryId(11L)
            .setAllowPskuWithoutBarcode(true)
            .setAcceptContentFromWhiteShops(true)
            .setAllowFastSkuCreation(true)
            .setHasKnowledge(true)
            .setLeaf(true);
        allowedCategoryWithBarcodes = new Category()
            .setCategoryId(100L)
            .setAllowPskuWithoutBarcode(false)
            .setAcceptContentFromWhiteShops(true)
            .setAllowFastSkuCreation(true)
            .setHasKnowledge(true)
            .setLeaf(true);
        restrictedCategory = new Category()
            .setCategoryId(22L)
            .setAllowPskuWithoutBarcode(true)
            .setAcceptContentFromWhiteShops(false)
            .setAllowFastSkuCreation(false)
            .setHasKnowledge(true)
            .setLeaf(true);
        categoryService.addCategories(allowedCategory, allowedCategoryWithBarcodes, restrictedCategory);

        contentProcessingLog = new ContentProcessingLogFacade(List.of(
            new ContentProcessingPgLog(jdbcTemplate),
            new ContentProcessingAuditLog(Mockito.mock(AuditWriter.class))
        ));

        var supplierService = new SupplierService(supplierRepository);

        needContentStatusService = new NeedContentStatusService(categoryService, supplierService,
            new BooksService(categoryService, Collections.emptySet()));

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock().enableAllCategories();
        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor, needContentStatusService, supplierService,
            categoryKnowledgeServiceMock, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false,
            3, categoryInfoCache);
    }

    public abstract void testErrorWrongProcessingStatus();

    protected void testErrorWrongProcessingStatus(Offer offer, Config config) {
        offerRepository.insertOffers(offer);

        assertThat(queue.findAll()).hasSize(0);
        // Manual insert is needed because observer won't add with wrong status
        var queueItem = new ContentProcessingOffer(
            offer.getBusinessId(), offer.getShopSku(), offer.getGroupId(), instantNow());
        queue.insert(queueItem);
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        offer.setMarketSpecificContentHashSent(1L);
        offerRepository.updateOffers(offer);

        makeReadyInQueue(offer);
        configureDatacampResponse(offer, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(config.expectedQueueSize);
        assertThat(result.getOffersFailed()).extractingByKey(offer.getBusinessSkuKey()).matches(fr -> !fr.hasError()
            && fr.getType() == FailureType.WRONG_STATUS);
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_RE_SORT)
            .doesNotHaveContentStatusActiveError();

        assertThatLogWasWritten(0);
    }

    public abstract void testBarcodeError();

    protected void testBarcodeError(Offer offerWithError, Offer offerGood, Config config) {
        offerRepository.insertOffers(offerWithError, offerGood);
        offerWithError = offerRepository.getOfferById(offerWithError.getId());
        offerGood = offerRepository.getOfferById(offerGood.getId());

        assertThat(queue.findAll()).hasSize(2);

        makeReadyInQueue(offerWithError, offerGood);
        configureDatacampResponse(offerWithError, 1L, builder -> {
            builder.getBasicBuilder().getContentBuilder().getPartnerBuilder()
                .getActualBuilder().getBarcodeBuilder().clearValue();
        });
        offerRepository.updateOffer(offerWithError);

        configureDatacampResponse(offerGood, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(config.expectedQueueSize);
        assertThat(result.getOffersFailed()).extractingByKey(offerWithError.getBusinessSkuKey())
            .matches(fr -> fr.getType() == FailureType.VALIDATION)
            .matches(fr -> fr.hasError())
            .extracting(fr -> fr.getError().getErrorCode())
            .isEqualTo("mboc.error.dc-offer-content-processing.barcode-required");
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        Offer offerWithErrorAfter = offerRepository.findOfferByBusinessSkuKey(offerWithError.getBusinessSkuKey());
        MbocAssertions.assertThat(offerWithErrorAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT)
            .hasContentStatusActiveError(MbocErrors.get()
                .contentProcessingBarcodeRequiredForCategory(
                    offerWithErrorAfter.getShopSku(), offerWithErrorAfter.getCategoryId())
                // hack: param is deserialized as Integer when reading from DB
                .copyWithParam("categoryId", offerWithErrorAfter.getCategoryId().intValue())
            );

        assertThatLogWasWritten(1);
    }

    public abstract void testFirstPicError();

    public void testFirstPicError(Offer offerWithFirstPicRemoved, Offer offerWithFirstPicFailed) {
        offerRepository.insertOffers(offerWithFirstPicRemoved, offerWithFirstPicFailed);

        offerWithFirstPicFailed = offerRepository.getOfferById(offerWithFirstPicFailed.getId());
        offerWithFirstPicRemoved = offerRepository.getOfferById(offerWithFirstPicRemoved.getId());

        configureDatacampResponse(offerWithFirstPicRemoved, 1L, offer -> {
            offer.setBasic(OfferBuilder.create(offer.getBasic())
                .withDefaultProcessedSpecification()
                .withPictures(List.of(
                    Pair.of("pic1", REMOVED),
                    Pair.of("pic2", AVAILABLE)
                ))
                .build()
            );
        });
        configureDatacampResponse(offerWithFirstPicFailed, 1L, offer -> {
            offer.setBasic(OfferBuilder.create(offer.getBasic())
                .withDefaultProcessedSpecification()
                .withPictures(List.of(
                    Pair.of("pic1", FAILED),
                    Pair.of("pic2", AVAILABLE)
                ))
                .build()
            );
        });

        offerRepository.updateOffers(offerWithFirstPicRemoved, offerWithFirstPicFailed);

        configureAgResponses(0);

        assertThat(queue.findAll()).hasSize(2);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed())
            .hasSize(2)
            .allSatisfy((key, reason) -> {
                assertThat(reason.hasError()).isTrue();
                assertThat(reason.getError().getErrorCode())
                    .isEqualTo(MbocErrors.get().firstPictureNotDownloaded("stub", "stub").getErrorCode());
            });
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        List<Offer> offersAfter = offerRepository.findOffersByBusinessSkuKeys(
            offerWithFirstPicRemoved.getBusinessSkuKey(), offerWithFirstPicFailed.getBusinessSkuKey());
        MbocAssertions.assertThat(offersAfter).allSatisfy(o -> MbocAssertions.assertThat(o)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT)
            .hasContentStatusActiveError(MbocErrors.get()
                .firstPictureNotDownloaded(o.getShopSku(), "pic1"))
        );

        assertThatLogWasWritten(0);
    }

    public abstract void testNoPicturesNoError();

    protected void testNoPicturesNoError(Offer offer) {
        offerRepository.insertOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());
        configureDatacampResponse(offer, 1L, basic -> {
            basic.setBasic(OfferBuilder.create(basic.getBasic())
                .withDefaultProcessedSpecification()
                .withPictures(List.of())
                .build()
            );
        });
        offerRepository.updateOffer(offer);
        configureAgResponses(0);

        assertThat(queue.findAll()).hasSize(1);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(0);
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .doesNotHaveContentStatusActiveError();

        assertThatLogWasWritten(1);
    }

    public abstract void testDequeueIfNotAllPicturesDownloaded();

    protected void testDequeueIfNotAllPicturesDownloaded(Offer offer) {
        offerRepository.insertOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());
        configureDatacampResponse(offer, 1L, dcOffer -> {
            dcOffer.setBasic(OfferBuilder.create(dcOffer.getBasic())
                .withDefaultProcessedSpecification()
                .withPictures(List.of(
                    Pair.of("pic1", UNDEFINED),
                    Pair.of("pic2", AVAILABLE)
                ))
                .build()
            );
        });
        offerRepository.updateOffer(offer);
        configureAgResponses(0);

        assertThat(queue.findAll()).hasSize(1);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(1)
            .allSatisfy((key, reason) -> assertThat(reason.getType()).isEqualTo(FailureType.EMPTY_DC_HASH));
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);

        assertThatLogWasWritten(0);
    }

    public abstract void testDequeueWhenNotAllPicturesDownloadedEmptyActual();

    protected void testDequeueWhenNotAllPicturesDownloadedEmptyActual(Offer offer) {
        offerRepository.insertOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());
        configureDatacampResponse(offer, 1L, dcOffer -> {
            var basic = OfferBuilder.create(dcOffer.getBasic())
                .withDefaultProcessedSpecification()
                .build().toBuilder();

            var pictures = dcOffer.getBasic().getPictures().toBuilder();
            var partnerPictures = DataCampOfferPictures.PartnerPictures.newBuilder();
            partnerPictures.getOriginalBuilder()
                .addSource(DataCampOfferPictures.SourcePicture.newBuilder().setUrl("pic1"))
                .addSource(DataCampOfferPictures.SourcePicture.newBuilder().setUrl("pic2"));

            pictures.setPartner(partnerPictures);
            basic.setPictures(pictures);

            dcOffer.setBasic(basic);
        });
        offerRepository.updateOffer(offer);
        configureAgResponses(0);

        assertThat(queue.findAll()).hasSize(1);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(1)
            .allSatisfy((key, reason) -> assertThat(reason.getType()).isEqualTo(FailureType.EMPTY_DC_HASH));
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);

        assertThatLogWasWritten(0);
    }

    public abstract void testDontSendAndIgnoreWhenHasContentMapping();

    protected void testDontSendAndIgnoreWhenHasContentMapping(Offer offerBad, Offer offerGood) {
        offerRepository.insertOffers(offerBad, offerGood);

        assertThat(queue.findAll()).hasSize(1);
        // Manual insert is needed because observer won't add with wrong status
        var queueItem = new ContentProcessingOffer(
            offerBad.getBusinessId(), offerBad.getShopSku(), offerBad.getGroupId(), instantNow());
        queue.insert(queueItem);
        offerBad = offerRepository.findOfferByBusinessSkuKey(offerBad.getBusinessSkuKey());
        offerBad.setMarketSpecificContentHashSent(1L);
        offerRepository.updateOffers(offerBad);

        makeReadyInQueue(offerBad, offerGood);
        configureDatacampResponse(offerBad, 1L);
        configureDatacampResponse(offerGood, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(1)
            .allSatisfy((key, reason) -> assertThat(reason.hasError()).isFalse());
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offerBad.getBusinessSkuKey());
        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.PROCESSED)
            .doesNotHaveContentStatusActiveError();

        assertThatLogWasWritten(1);
    }

    public abstract void testChooseValidationCheckWithHigherLevel();

    protected void testChooseValidationCheckWithHigherLevelBody(Offer offerGood,
                                                                Offer offerNoHashButMapped) {
        offerRepository.insertOffers(offerGood, offerNoHashButMapped);
        assertThat(queue.findAll())
            .extracting(ContentProcessingOffer::getKey)
            .containsExactlyInAnyOrder(offerGood.getBusinessSkuKey());

        // Manual insert is needed because observer won't add with wrong status
        var queueItem = new ContentProcessingOffer(
            offerNoHashButMapped.getBusinessId(), offerNoHashButMapped.getShopSku(),
            offerNoHashButMapped.getGroupId(), instantNow());
        queue.insert(queueItem);
        assertThat(queue.findAll())
            .extracting(ContentProcessingOffer::getKey)
            .containsExactlyInAnyOrder(offerGood.getBusinessSkuKey(), offerNoHashButMapped.getBusinessSkuKey());

        makeReadyInQueue(offerGood);
        makeReadyInQueue(offerNoHashButMapped);
        configureDatacampResponse(offerGood.getBusinessSkuKey(), 1, 1L);
        configureDatacampResponse(offerNoHashButMapped.copy(), 1L, __ -> {
        }, false);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).isEmpty();

        assertThat(result.getOffersFailed().values())
            .extracting(AbstractContentProcessingSenderService.FailureReason::getType)
            .containsExactlyInAnyOrder(FailureType.HAS_MSKU_MAPPING);
        // none of them sent
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        var offersAfter = offerRepository.findOffersByBusinessSkuKeys(
            offerGood.getBusinessSkuKey(),
            offerNoHashButMapped.getBusinessSkuKey()
        );
        var offersStatuses = offersAfter.stream()
            .collect(Collectors.toMap(Offer::getBusinessSkuKey, Offer::getProcessingStatus));
        assertThat(offersStatuses)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                offerGood.getBusinessSkuKey(), CONTENT_PROCESSING,
                offerNoHashButMapped.getBusinessSkuKey(), PROCESSED
            ));

        assertThatLogWasWritten(1);
    }

    public abstract void testErrorCardCreationNotAllowed();

    protected void testErrorCardCreationNotAllowed(Offer offerBad, Offer offerGood) {
        offerRepository.insertOffers(offerGood, offerBad);

        assertThat(queue.findAll()).hasSize(2);
        makeReadyInQueue(offerBad, offerGood);
        configureDatacampResponse(offerBad, 1L);
        configureDatacampResponse(offerGood, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).extractingByKey(offerBad.getBusinessSkuKey()).matches(fr -> !fr.hasError()
            && fr.getType() == FailureType.CARD_CREATION_NOT_ALLOWED);

        Offer offerGoodAfter = offerRepository.findOfferByBusinessSkuKey(offerGood.getBusinessSkuKey());
        MbocAssertions.assertThat(offerGoodAfter)
            .hasProcessingStatus(CONTENT_PROCESSING)
            .doesNotHaveContentStatusActiveError();

        assertThat(result.getOffersSentCount()).isEqualTo(1);
        assertThatLogWasWritten(1);
    }

    public abstract void testSuccessfullyProcessDeduplicated();

    protected void testSuccessfullyProcessDeduplicated(List<Offer> offers, boolean shouldSend) {
        offerRepository.insertOffers(offers);

        var queueItems = offers.stream()
            .map(offer -> new ContentProcessingOffer(offer.getBusinessId(),
                offer.getShopSku(), offer.getGroupId(), instantNow(), false, true)
            )
            .collect(Collectors.toList());
        queue.insertOrUpdateAll(queueItems);
        assertThat(queue.findAll()).hasSize(offers.size());

        makeReadyInQueue(offers.toArray(Offer[]::new));
        offers.forEach(offer -> configureDatacampResponse(offer, 1L));
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);

        if (shouldSend) {
            assertThat(result.getOffersFailed()).isEmpty();
            assertThat(result.getOffersSentCount()).isEqualTo(offers.size());
        } else {
            assertThat(result.getOffersFailedCount()).isEqualTo(offers.size());
            assertThat(result.getOffersSent()).isEmpty();
        }
    }

    public abstract void testCorrectHandlingSecondaryOffers();

    protected void testCorrectHandlingSecondaryOffers(List<Offer> offers, Offer mainOffer) {
        keyValueService.putValue(SEND_ONLY_MAIN_OFFERS_ENABLED, true);
        keyValueService.invalidateCache();
        assertThat(offers.contains(mainOffer)).isTrue();

        offerRepository.insertOffers(offers);

        var queueItems = offers.stream()
            .map(offer -> new ContentProcessingOffer(offer.getBusinessId(),
                offer.getShopSku(), offer.getGroupId(), instantNow(), false, false)
            )
            .collect(Collectors.toList());
        queue.insertOrUpdateAll(queueItems);
        assertThat(queue.findAll()).hasSize(offers.size());

        makeReadyInQueue(offers.toArray(Offer[]::new));
        offers.forEach(offer -> configureDatacampResponse(offer, 1L));
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        var failureTypes = result.getOffersFailed().values().stream()
            .map(AbstractContentProcessingSenderService.FailureReason::getType)
            .collect(Collectors.toSet());
        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(offers.size() - 1);
        assertThat(failureTypes).containsExactly(FailureType.SECONDARY_OFFER);
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        keyValueService.putValue(SEND_ONLY_MAIN_OFFERS_ENABLED, false);
        keyValueService.invalidateCache();
    }

    public abstract void testSendAnyCsku();

    protected void testSendAnyCsku(Offer offerGood) {
        offerRepository.insertOffers(offerGood);

        assertThat(queue.findAll()).hasSize(1);

        makeReadyInQueue(offerGood);
        configureDatacampResponse(offerGood, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(0);
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        ArgumentCaptor<OfferContentProcessing.OfferContentProcessingRequest> requestCaptor =
            ArgumentCaptor.forClass(OfferContentProcessing.OfferContentProcessingRequest.class);
        Mockito.verify(agServiceImpl).startContentProcessing(requestCaptor.capture(), Mockito.any());
        Mockito.clearInvocations(agServiceImpl);

        OfferContentProcessing.OfferContentProcessingRequest sentRequest = requestCaptor.getValue();
        Assertions.assertThat(sentRequest.getOffersWithFlagsList()).hasSize(1);
        DataCampOffer.Offer sentOffer = sentRequest.getOffersWithFlags(0).getOffer();
        var contentStatus = sentOffer.getContent().getStatus().getContentSystemStatus();

        assertThat(contentStatus.getAllowModelCreateUpdate()).isTrue();

        assertThatLogWasWritten(1);
    }

    public abstract void testSendMskuMarkedAllowPartnerContent();

    protected void testSendMskuMarkedAllowPartnerContent(Offer offerGood) {
        offerRepository.insertOffers(offerGood);

        assertThat(queue.findAll()).hasSize(1);

        makeReadyInQueue(offerGood);
        configureDatacampResponse(offerGood, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFailed()).hasSize(0);
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        ArgumentCaptor<OfferContentProcessing.OfferContentProcessingRequest> requestCaptor =
            ArgumentCaptor.forClass(OfferContentProcessing.OfferContentProcessingRequest.class);
        Mockito.verify(agServiceImpl).startContentProcessing(requestCaptor.capture(), Mockito.any());
        Mockito.clearInvocations(agServiceImpl);

        OfferContentProcessing.OfferContentProcessingRequest sentRequest = requestCaptor.getValue();
        Assertions.assertThat(sentRequest.getOffersWithFlagsList()).hasSize(1);
        DataCampOffer.Offer sentOffer = sentRequest.getOffersWithFlags(0).getOffer();
        var contentStatus = sentOffer.getContent().getStatus().getContentSystemStatus();

        assertThat(contentStatus.getAllowModelCreateUpdate()).isTrue();

        assertThatLogWasWritten(1);
    }

    public abstract void testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue();

    protected void testSendCorrectAllowCreateUpdateForCskuWithManualInsertToQueue(Offer offer,
                                                                                  boolean allowUpdateCreate) {
        // avoid automatic transition to CONTENT_PROCESSING:
        offer.setMarketSpecificContentHashSent(offer.getMarketSpecificContentHash());
        offerRepository.insertOffers(offer);

        assertThat(queue.findAll()).hasSize(0);
        var queueItem = new ContentProcessingOffer(
            offer.getBusinessId(), offer.getShopSku(), offer.getGroupId(), instantNow());
        queue.insert(queueItem);

        makeReadyInQueue(offer);
        configureDatacampResponse(offer, 1L);
        configureAgResponses(0);

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersSentCount()).isEqualTo(1);

        ArgumentCaptor<OfferContentProcessing.OfferContentProcessingRequest> requestCaptor =
            ArgumentCaptor.forClass(OfferContentProcessing.OfferContentProcessingRequest.class);
        Mockito.verify(agServiceImpl).startContentProcessing(requestCaptor.capture(), Mockito.any());
        Mockito.clearInvocations(agServiceImpl);

        OfferContentProcessing.OfferContentProcessingRequest sentRequest = requestCaptor.getValue();
        Assertions.assertThat(sentRequest.getOffersWithFlagsList()).hasSize(1);
        DataCampOffer.Offer sentOffer = sentRequest.getOffersWithFlags(0).getOffer();
        var contentStatus = sentOffer.getContent().getStatus().getContentSystemStatus();

        assertThat(contentStatus.getAllowModelCreateUpdate()).isEqualTo(allowUpdateCreate);
        assertThatLogWasWritten(1);
    }

    protected void testShouldSkipFrozenModelsOffers(List<Offer> offers) {

        offerRepository.insertOffers(offers);
        var forUpdate = offerRepository.findAll().stream()
            .peek(offer -> {
                configureDatacampResponse(offer, 1L, dcOffer -> {
                    dcOffer.setBasic(OfferBuilder.create(dcOffer.getBasic())
                        .withDefaultProcessedSpecification()
                        .build()
                    );
                });
            })
            .collect(Collectors.toList());

        offerRepository.updateOffers(forUpdate);
        configureAgResponses(0);
        var queueItems = offers.stream()
            .map(offer -> new ContentProcessingOffer(offer.getBusinessId(),
                offer.getShopSku(), offer.getGroupId(), instantNow(), false, false)
            )
            .collect(Collectors.toList());
        queue.insertOrUpdateAll(queueItems);

        assertThat(queue.findAll()).hasSize(offers.size());

        var result = service.executeTask(
            service.new SenderTaskContext(queue.findAll())
        );

        assertThat(queue.findAll()).hasSize(0);
        assertThat(result.getOffersFrozen()).hasSize(offers.size());
        assertThat(result.getOffersSentCount()).isEqualTo(0);

        offerRepository.findAll().forEach(offerAfter -> {
            MbocAssertions.assertThat(offerAfter)
                .hasProcessingStatus(Offer.ProcessingStatus.NEED_CONTENT);
        });

        assertThatLogWasWritten(0);
    }

    // helpers

    protected void configureAgResponses(int failAfter) {
        Answer<?> okResponse = createAgResponse(Status.OK);
        Stubber stubber = doAnswer(okResponse);

        if (failAfter > 0) {
            for (int i = 0; i < failAfter - 1; i++) {
                stubber = stubber.doAnswer(okResponse);
            }
            stubber = stubber.doAnswer(createAgResponse(Status.ERROR));
        }

        stubber.when(agServiceImpl).startContentProcessing(any(), any());
    }

    private Answer<?> createAgResponse(Status status) {
        return invocation -> {
            @SuppressWarnings("unchecked")
            var streamObserver = (StreamObserver<OfferContentProcessingResponse>) invocation.getArgument(1);
            streamObserver.onNext(OfferContentProcessingResponse.newBuilder().setStatus(status).build());
            streamObserver.onCompleted();
            return null;
        };
    }

    protected Offer offer(Supplier supplier, String shopSku, Offer.ProcessingStatus status, Integer groupId,
                          Long dcVer, LocalDateTime created) {
        return OfferTestUtils.nextOffer(supplier)
            .setDataCampOffer(true)
            .setBusinessId(supplier.getId())
            .setShopSku(shopSku)
            .setTitle(shopSku + " title")
            .setShopCategoryName("Test cat name")
            .setProcessingStatusInternal(status)
            .setCategoryIdForTests(allowedCategory.getCategoryId(), Offer.BindingKind.APPROVED)
            .setDataCampContentVersion(dcVer)
            .setGroupId(groupId)
            .setBarCode(String.valueOf(Math.random()))
            .setMarketSpecificContentHash(1L)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCreated(created);
    }

    protected Offer offer(Supplier supplier, String shopSku, Offer.ProcessingStatus status, Integer groupId,
                          Long dcVer) {
        return offer(supplier, shopSku, status, groupId, dcVer, DateTimeUtils.dateTimeNow());
    }

    protected void configureDatacampResponse(Map<Offer, Long> offers) {
        offers.forEach(this::configureDatacampResponse);
    }

    protected void configureDatacampResponse(BusinessSkuKey skuKey, Integer groupId, Long dcVersion) {
        configureDatacampResponse(
            new Offer()
                .setBusinessId(skuKey.getBusinessId())
                .setShopSku(skuKey.getShopSku())
                .setGroupId(groupId),
            dcVersion
        );
    }

    protected void configureDatacampResponse(Offer offer, Long dcVersion) {
        var offer_ = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        if (offer_ != null) {
            configureDatacampResponse(offer_, dcVersion, b -> {
            });
            offerRepository.updateOffer(offer_);
        } else {
            configureDatacampResponse(offer, dcVersion, b -> {
            });
        }
    }

    protected void configureDatacampResponse(
        Offer offer, Long dcVersion,
        Consumer<DataCampUnitedOffer.UnitedOffer.Builder> configure
    ) {
        configureDatacampResponse(offer, dcVersion, configure, true);
    }

    protected void configureDatacampResponse(
        Offer offer, Long dcVersion,
        Consumer<DataCampUnitedOffer.UnitedOffer.Builder> configure,
        boolean resetHash
    ) {
        var groupId = DataCampOfferMeta.Ui32Value.newBuilder();
        if (offer.getGroupId() != null) {
            groupId.setValue(offer.getGroupId());
        }

        var unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(offer.getBusinessId())
                    .setOfferId(offer.getShopSku())
                    .build()
                )
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                    .setVersion(DataCampOfferStatus.VersionStatus.newBuilder()
                        .setActualContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                            .setCounter(dcVersion)
                            .build()
                        )
                        .build()
                    )
                    .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                            .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue("title").build())
                            .build()
                        )
                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                            .setGroupId(groupId)
                            .build()
                        )
                        .build()
                    )
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(OfferBuilder.mapping(
                            offer.getCategoryId(),
                            offer.getModelId(),
                            offer.getApprovedSkuId(),
                            null, null, null
                        )).build())
                    .build()
                )
                .build()
            );
        configure.accept(unitedOffer);
        dataCampService.putOffer(unitedOffer.build());

        if (resetHash) {
            var hash = hashCalculator.marketSpecificContentHash(
                unitedOffer.build().getBasic(),
                offer,
                offerDestinationCalculator);
            if (offer.getMarketSpecificContentHashSent() != null) {
                offer.setMarketSpecificContentHashSent(hash.orElse(1L));
            }
            offer.setMarketSpecificContentHash(hash.orElse(1L));
        }
    }

    protected void makeReadyInQueue(Offer... offers) {
        makeReadyInQueue(Arrays.stream(offers).map(Offer::getBusinessSkuKey).toArray(BusinessSkuKey[]::new));
    }

    protected void makeReadyInQueue(BusinessSkuKey... keys) {
        var set = new HashSet<>(Arrays.asList(keys));
        queue.updateBatch(queue.findAll().stream()
            .filter(it -> set.contains(it.getKey()))
            .map(it -> it.setInserted(it.getInserted().minus(MAX_INSERTED_DELAY_MINUTES + 1, ChronoUnit.MINUTES)))
            .map(it -> it.setChanged(it.getChanged().minus(MIN_CHANGED_DELAY_MINUTES + 1, ChronoUnit.MINUTES)))
            .collect(Collectors.toList()));
    }

    protected void assertThatLogWasWritten(int count) {
        jdbcTemplate.query("select count(*) from mbo_category.offer_content_processing_log", (row) -> {
            assertThat(row.getInt(1)).isEqualTo(count);
        });
    }

    protected Msku createMsku(long id) {
        return new Msku()
            .setMarketSkuId(id)
            .setTitle("title")
            .setParentModelId(0L)
            .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setVendorId((long) OfferTestUtils.TEST_VENDOR_ID)
            .setSkuType(SkuTypeEnum.SKU)
            .setSkuQuality(SkuQualityEnum.OPERATOR)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
    }

    @Value
    @Builder
    protected static class Config {
        int expectedQueueSize;
    }
}
