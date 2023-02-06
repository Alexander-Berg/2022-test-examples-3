package ru.yandex.market.mboc.common.contentprocessing.from.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingAuditLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLog;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLogFacade;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingPgLog;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.HashCalculator;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.AuditWriter;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.ContentProcessingErrorRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mboc.common.contentprocessing.config.ContentProcessingConfig.FAST_SKU_FEATURE_FLAG;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_SKU_ID;

public class ContentProcessingResultHandlerServiceTest extends BaseDbTestClass {

    @Autowired
    OfferRepository offerRepository;
    @Autowired
    QueueFromContentProcessingRepository queueFromContentProcessingRepository;
    @Autowired
    SupplierRepository supplierRepository;
    @Autowired
    TransactionHelper transactionHelper;
    @Autowired
    OfferBatchProcessor offerBatchProcessor;
    @Autowired
    AntiMappingRepository antiMappingRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    ContentProcessingQueueRepository contentProcessingQueueRepository;
    @Autowired
    private ContentProcessingErrorRepository contentProcessingErrorRepository;

    ContextedOfferDestinationCalculator calculator;

    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private ModelStorageCachingServiceMock modelStorageCachingService;
    private SupplierService supplierService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private ContentProcessingLog contentProcessingLog;
    private CategoryCachingServiceMock categoryCachingService;
    private OfferUpdateSequenceService offerUpdateSequenceService;

    private ContentProcessingResultHandlerService contentProcessingResultHandlerService;

    private final Supplier supplier = OfferTestUtils.simpleSupplier();

    @Before
    public void before() {
        supplierService = new SupplierService(supplierRepository);
        categoryCachingService = new CategoryCachingServiceMock();
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
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService, categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache);
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        DataCampIdentifiersService dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, SupplierConverterServiceMock.BERU_BUSINESS_ID,
            supplierConverterService);
        hashCalculator = new HashCalculator(storageKeyValueService, dataCampIdentifiersService, categoryInfoCache);

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

        storageKeyValueService.putValue(FAST_SKU_FEATURE_FLAG, true);

        categoryRepository.insertOrUpdateAll(List.of(
            new Category().setCategoryId(CategoryTree.ROOT_CATEGORY_ID),
            new Category()
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setAcceptContentFromWhiteShops(true)
                .setAllowFastSkuCreation(true)
                .setLeaf(true)
        ));

        calculator = new ContextedOfferDestinationCalculator(
            categoryInfoCache,
            storageKeyValueService
        );
        supplierRepository.insertBatch(
            supplier,
            OfferTestUtils.businessSupplier(),
            OfferTestUtils.whiteSupplierUnderBiz()
        );
    }

    @Test
    public void whenErrorResultAndForcedOfferAndNullHashSentThenContentProcessingStatusChanges() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);

        var dcOffer = new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .build())
            .build();

        offer.setMarketSpecificContentHash(null)
            .setMarketSpecificContentHashSent(null);
        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer,
            true,
            null,
            null
        );

        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        var offerAfter = offerRepository.getOfferById(offer.getId());

        // the point is that CONTENT_PROCESSING status has changed to smth else
        assertThat(offerAfter.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
        assertThat(offerAfter.getContentProcessingStatus()).
            isNotEqualTo(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        MbocAssertions.assertThat(offerAfter).doesNotHaveContentStatusActiveError();
    }

    @Test
    public void whenErrorResultAndForcedOfferAndAnotherHashSentThenContentProcessingStatusStays() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);

        var dcOffer = new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .build())
            .build();

        var notDcHash = hashCalculator.marketSpecificContentHash(
                dcOffer, new Offer(), calculator)
            .map(hash -> hash / 2 + 1)
            .orElse(123L);
        offer.setMarketSpecificContentHash(null)
            .setMarketSpecificContentHashSent(notDcHash);

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer,
            true,
            null,
            null
        );

        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        var offerAfter = offerRepository.getOfferById(offer.getId());

        MbocAssertions.assertThat(offerAfter)
            .hasProcessingStatus(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .hasContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .doesNotHaveContentStatusActiveError();
    }

    @Test
    public void whenErrorResultAndForcedOfferAndSameHashSentThenContentProcessingStatusChanges() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);

        var dcOffer = new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .build())
            .build();

        var dcHash =
            hashCalculator.marketSpecificContentHash(dcOffer, offer,
                calculator).get();
        offer.setMarketSpecificContentHash(null)
            .setMarketSpecificContentHashSent(dcHash);

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer,
            true,
            null,
            null
        );

        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        var offerAfter = offerRepository.getOfferById(offer.getId());

        // the point is that CONTENT_PROCESSING status has changed to smth else
        assertThat(offerAfter.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
        assertThat(offerAfter.getContentProcessingStatus())
            .isNotEqualTo(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        MbocAssertions.assertThat(offerAfter).doesNotHaveContentStatusActiveError();
    }

    @Test
    public void modelShouldBeReloadedIfTypeChanged() {
        storageKeyValueService.putValue(FAST_SKU_FEATURE_FLAG, true);
        storageKeyValueService.invalidateCache();
        long skuId = 999L;
        long categoryId = 11;
        categoryCachingService.addCategory(categoryId);
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .updateApprovedSkuMapping(new Offer.Mapping(skuId, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_FAST)
            .setCategoryIdForTests(categoryId, Offer.BindingKind.APPROVED);

        var dcOffer = new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK)
                .build())
            .build().toBuilder();
        var partner = dcOffer.getContent().getBinding().getPartner().toBuilder();
        partner.setMarketSkuType(DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU)
            .setMarketSkuId(skuId)
            .setMarketCategoryId(Math.toIntExact(categoryId));
        dcOffer.getContentBuilder().getBindingBuilder().setPartner(partner);

        modelStorageCachingService.addModel(new Model().setId(skuId).setCategoryId(categoryId)
            .setModelType(Model.ModelType.FAST_SKU));

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer.build(),
            true,
            null,
            null
        );

        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        Assertions.assertThat(modelStorageCachingService.getReloaded()).containsExactly(skuId);

        modelStorageCachingService.addModel(new Model().setId(skuId).setCategoryId(categoryId)
            .setModelType(Model.ModelType.PARTNER_SKU));
        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));
        Assertions.assertThat(modelStorageCachingService.getReloaded()).isEmpty();

        var newOffer = offerRepository.getOfferById(offer.getId());
        Assert.assertEquals(Offer.MappingConfidence.PARTNER_SELF, newOffer.getApprovedSkuMappingConfidence());
    }

    @Test
    public void whenAgSendsResultsForOfferWithNullCheckedHash() {
        var offer = OfferTestUtils.nextOffer(OfferTestUtils.whiteSupplierUnderBiz())
            .setBusinessId(OfferTestUtils.businessSupplier().getId())
            .setCreated(LocalDateTime.now())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING);

        var dcOffer = new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .build())
            .build();

        var checkedDcHash = hashCalculator.marketSpecificContentHash(dcOffer, new Offer(),
            calculator);
        assertThat(checkedDcHash).isEmpty();

        var dcHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        offer.setMarketSpecificContentHash(dcHash)
            .setMarketSpecificContentHashSent(dcHash);

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer,
            false,
            null,
            null
        );

        contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        var offerAfter = offerRepository.getOfferById(offer.getId());

        // the point is that CONTENT_PROCESSING status has changed to smth else
        assertThat(offerAfter.getProcessingStatus())
            .isNotEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
        assertThat(offerAfter.getContentProcessingStatus())
            .isNotEqualTo(Offer.ContentProcessingStatus.CONTENT_PROCESSING);
        MbocAssertions.assertThat(offerAfter).hasContentStatusActiveError();
    }

    @Test
    public void testAgOverridesPskuMappingWithAnotherPsku() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var results = agSendsAnotherMapping(
            offer,
            OfferTestUtils.mapping(TEST_SKU_ID, Offer.SkuType.PARTNER20), Offer.MappingConfidence.PARTNER_SELF
        );

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.PROCESSED);
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isNotNull();
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isEqualTo(offerAfter.getMarketSpecificContentHash());
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID + 1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);
    }

    @Test
    public void testAgOverridesFastMappingWithAnotherFast() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.FAST_SKU));

        var results = agSendsAnotherMapping(
            offer,
            OfferTestUtils.mapping(TEST_SKU_ID, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_FAST
        );

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.NEED_CONTENT);
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isNotNull();
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isEqualTo(offerAfter.getMarketSpecificContentHash());
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID + 1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_FAST);
    }

    @Test
    public void testAgOverridesFastMappingWithAnotherPsku() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var results = agSendsAnotherMapping(
            offer,
            OfferTestUtils.mapping(TEST_SKU_ID, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_FAST
        );

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.PROCESSED);
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isNotNull();
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isEqualTo(offerAfter.getMarketSpecificContentHash());
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID + 1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);
    }

    @Test
    public void testAgCannotOverrideContentMapping() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var results = agSendsAnotherMapping(
            offer,
            OfferTestUtils.mapping(TEST_SKU_ID, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT
        );

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.NONE);
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isNotNull();
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isEqualTo(offerAfter.getMarketSpecificContentHash());
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void whenAgSendsSameMappingWithLowerConfidenceAgResponseIsSaved() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        var results = agSendsAnotherMapping(
            offer,
            OfferTestUtils.mapping(TEST_SKU_ID + 1, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT
        );

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.NONE);
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isNotNull();
        assertThat(offerAfter.getMarketSpecificContentHashSent()).isEqualTo(offerAfter.getMarketSpecificContentHash());
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID + 1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        var savedResponses = queueFromContentProcessingRepository.findByFilter(
            QueueFromContentProcessingRepository.Filter.builder()
                .offerIds(List.of(offerAfter.getId()))
                .build()
        );
        assertThat(savedResponses).hasSize(1);
        assertThat(savedResponses.get(0).getOfferId()).isEqualTo(offerAfter.getId());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(offerUpdateSequenceService, times(1)).markOffersModified(captor.capture());
        var markedModifiedOfferIds = captor.getValue();
        assertThat(markedModifiedOfferIds).containsExactlyInAnyOrder(offerAfter.getId());
    }

    @Test
    public void dontCalculateHashIfPresentHashFromAg() {
        var offer = OfferTestUtils.nextOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING)
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.CONTENT_PROCESSING)
            .setCategoryIdForTests(TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setCreated(LocalDateTime.now());

        modelStorageCachingService.addModel(
            new Model().setId(TEST_SKU_ID + 1)
                .setCategoryId(TEST_CATEGORY_INFO_ID)
                .setModelType(Model.ModelType.SKU)
                .setModelQuality(Model.ModelQuality.PARTNER));

        offer = offer.updateApprovedSkuMapping(
            OfferTestUtils.mapping(TEST_SKU_ID, Offer.SkuType.MARKET),
            Offer.MappingConfidence.PARTNER);

        var dcOffer = buildDcOffer(offer).build();
        var dcOfferChanged = buildDcOffer(offer).withPartnerTitle("new title").build();

        var dcHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        offer.setMarketSpecificContentHash(dcHash)
            .setMarketSpecificContentHashSent(dcHash);

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOfferChanged,
            false,
            //send old hash
            hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer),
            null
        );

        var results =
            contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));

        assertThat(results)
            .allMatch(result -> result.getErrorInfo() == null)
            .extracting(ContentProcessingResultHandlerService.Result::getBusinessSkuKey)
            .containsExactlyInAnyOrder(offer.getBusinessSkuKey());

        var offerAfter = offerRepository.getOfferById(offer.getId());

        assertThat(offerAfter.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.AUTO_PROCESSED);
        assertThat(offerAfter.getContentProcessingStatus()).isEqualTo(Offer.ContentProcessingStatus.PROCESSED);
        MbocAssertions.assertThat(offerAfter)
            .hasApprovedMapping(TEST_SKU_ID + 1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF);
    }

    public List<ContentProcessingResultHandlerService.Result> agSendsAnotherMapping(
        Offer offer,
        Offer.Mapping currentMapping,
        Offer.MappingConfidence currentConfidence
    ) {
        offer = offer.updateApprovedSkuMapping(currentMapping, currentConfidence);

        var dcOffer = buildDcOffer(offer).build();

        var dcHash = hashCalculator.marketSpecificContentHashUnchecked(dcOffer, offer);
        offer.setMarketSpecificContentHash(dcHash)
            .setMarketSpecificContentHashSent(dcHash);

        offerRepository.insertOffer(offer);

        var offerToProcess = new ContentProcessingResultHandlerService.OfferToProcess(
            offer.getBusinessSkuKey(),
            dcOffer,
            false,
            null,
            null
        );

        return contentProcessingResultHandlerService.processOffers(List.of(offerToProcess));
    }

    private OfferBuilder buildDcOffer(Offer offer) {
        return new OfferBuilder()
            .withIdentifiers(offer.getBusinessSkuKey())
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK)
                .build())
            .withPartnerMapping(OfferBuilder.skuMapping(TEST_SKU_ID + 1));
    }
}
