package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.Mapping;
import ru.yandex.market.mboc.common.offers.model.Offer.MappingStatus;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.managers.ManagersService;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.OfferProcessingStrategiesHolder;
import ru.yandex.market.mboc.common.services.offers.ReopenNeedInfoService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.MatchingOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedSizeMeasureFilter;
import ru.yandex.market.mboc.common.services.offers.processing.NoSizeMeasureValueStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.ProcessingTicketHelper;
import ru.yandex.market.mboc.common.services.offers.processing.ReclassificationProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.WaitContentProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.queue.OfferQueueService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.SizeMeasureHelper;
import ru.yandex.market.mboc.common.services.users.StaffServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;

import static org.mockito.ArgumentMatchers.anyCollection;

/**
 * @author yuramalinov
 * @created 18.12.18
 */
public class ReopenNeedInfoExecutorTest extends BaseDbTestClass {
    private static final LocalDateTime NOW = DateTimeUtils.dateTimeNow();

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private CategoryInfoRepository categoryInfoRepository;
    @Autowired
    private MboUsersRepository mboUsersRepository;
    @Autowired
    private ProcessingTicketInfoRepository processingTicketInfoRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    @Qualifier("reopenNeedInfoQueueService")
    private OfferQueueService offerQueue;

    private TrackerServiceMock trackerService;
    private ReopenNeedInfoService service;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;
    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private CategorySizeMeasureService sizeMeasureService;
    private SizeMeasureHelper sizeMeasureHelper;
    private GlobalVendorsCachingService globalVendorsCachingService;
    private NeedSizeMeasureFilter needSizeMeasureFilter;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private ProcessingTicketInfoService processingTicketInfoService;
    private SupplierService supplierService;
    private ApplySettingsService applySettingsService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;

    @Before
    public void setUp() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);
        sizeMeasureService = new CategorySizeMeasureServiceStub();
        sizeMeasureHelper = new SizeMeasureHelper(sizeMeasureService, sizeMeasureService);

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        var config = new OffersToExcelFileConverterConfig(categoryCachingServiceMock);

        trackerService = new TrackerServiceMock();
        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        ManagersService managersService = new ManagersServiceMock();
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);

        HonestMarkClassificationService honestMarkClassificationService =
            Mockito.mock(HonestMarkClassificationService.class);
        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.anyLong(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));

        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();

        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingServiceMock, offerBatchProcessor, supplierRepository);

        var helper = new ProcessingTicketHelper(Environments.TESTING, trackerService,
            managersService, categoryCachingServiceMock, processingTicketInfoService);
        var clsConverter = config.classifierConverter(categoryCachingServiceMock);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor, needContentStatusService, supplierService, categoryKnowledgeService,
            retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);
        var classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingServiceMock,
            offerMappingActionService,
            offerDestinationCalculator
        );
        applySettingsService = Mockito.mock(ApplySettingsService.class);
        var classificationStrategy = new ClassificationOffersProcessingStrategy(
            trackerService, offerRepository, supplierRepository, masterDataHelperService,
            clsConverter, categoryKnowledgeService, classificationOffersProcessingService, helper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);
        var reclassificationProcessingStrategy = new ReclassificationProcessingStrategy(
            trackerService, offerRepository, supplierRepository, masterDataHelperService,
            clsConverter, categoryKnowledgeService, classificationOffersProcessingService, helper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);

        var trackerServiceMock = Mockito.mock(TrackerServiceMock.class);
        var staffService = new StaffServiceMock();
        var waitContentProcessingStrategy = new WaitContentProcessingStrategy(trackerServiceMock, offerRepository,
            supplierRepository, categoryInfoRepository, mboUsersRepository, categoryCachingServiceMock,
            staffService, offersProcessingStatusService);

        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        var noSizeMeasureValueStrategy = new NoSizeMeasureValueStrategy(offerRepository,
            trackerService, supplierRepository, globalVendorsCachingService, sizeMeasureHelper,
            offersProcessingStatusService);

        var matchingConverter = config.matchingConverter(
            modelStorageCachingServiceMock, offerRepository);
        needSizeMeasureFilter = Mockito.mock(NeedSizeMeasureFilter.class);
        Mockito.when(needSizeMeasureFilter.createNeedSizeMeasureTickets(anyCollection()))
            .thenAnswer(invocation -> invocation.getArguments()[0]);
        var matchingStrategy = new MatchingOffersProcessingStrategy(
            trackerService, offerRepository, supplierRepository, masterDataHelperService,
            managersService, offerMappingActionService, matchingConverter, needSizeMeasureFilter,
            helper, applySettingsService, offersProcessingStatusService);

        var holder = new OfferProcessingStrategiesHolder(Arrays.asList(
            classificationStrategy,
            matchingStrategy,
            reclassificationProcessingStrategy,
            waitContentProcessingStrategy,
            noSizeMeasureValueStrategy)
        );


        service = new ReopenNeedInfoService(offerRepository, transactionHelper, offerQueue::enqueueByIds,
            offersProcessingStatusService);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        categoryKnowledgeService.addCategory(90401L);
    }

    private void execute() {
        offerQueue.handleQueueBatch(service::processChunkFromQueue);
    }

    @Test
    public void testItDoesntReopenTooEarly() {
        // just one minute, there might be more updates
        offerRepository.insertOffer(createOffer().setContentChangedTs(NOW.minusMinutes(1)));

        execute();

        // Nothing is created
        Assertions.assertThat(trackerService.getAllTickets()).isEmpty();
    }

    @Test
    public void testReopenForDifferentTimespanConditions() {
        var tooRecentUpdate = createOffer().setContentChangedTs(NOW.minusMinutes(1));
        offerRepository.insertOffer(tooRecentUpdate);
        var oldUpdate = createOffer()
            .setContentChangedTs(NOW.minus(ReopenNeedInfoService.SEND_IF_OLD_ACTIVITY).minusMinutes(1));
        offerRepository.insertOffer(oldUpdate);

        execute();

        Assertions.assertThat(offerRepository.getOfferById(tooRecentUpdate.getId()).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_INFO);

        Assertions.assertThat(offerRepository.getOfferById(oldUpdate.getId()).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
    }

    @Test
    public void testItReopensAndSendsToModerationInOldPipline() {
        Offer offer1 = createOffer() // This should go to classification
            .setBindingKind(Offer.BindingKind.SUGGESTED)
            .setContentChangedTs(NOW.minusHours(1));
        Offer offer2 = createOffer() // This should go to matching
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setContentChangedTs(NOW.minusHours(1));
        categoryKnowledgeService.addCategory(1L);
        categoryCachingServiceMock.addCategory(1L);
        Offer offer3 = createOffer() // This should be sent to moderation
            .setContentChangedTs(NOW.minusHours(1))
            .setSupplierSkuMappingStatus(MappingStatus.NEW)
            .setSupplierSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.MARKET))
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer4 = createOffer() // This also should be sent to moderation
            .setContentChangedTs(NOW.minusHours(1))
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L).copyWithSkuType(Offer.SkuType.MARKET))
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer dontTouchIt = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED);

        offerRepository.insertOffers(offer1, offer2, offer3, offer4, dontTouchIt);
        categoryInfoRepository.insert(new CategoryInfo(1).setModerationInYang(true));

        execute();

        // Classification offer
        Offer updated1 = offerRepository.getOfferById(offer1.getId());
        Assertions.assertThat(updated1.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(updated1.getLastPrimaryProcessingStatus()).isEqualTo(Offer.ProcessingStatus.REOPEN);

        // Matching offer
        Offer updated2 = offerRepository.getOfferById(offer2.getId());
        Assertions.assertThat(updated2.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(updated1.getLastPrimaryProcessingStatus()).isEqualTo(Offer.ProcessingStatus.REOPEN);

        // Moderation offers
        Assertions.assertThat(offerRepository.getOfferById(offer3.getId()).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_MODERATION);
        Assertions.assertThat(offerRepository.getOfferById(offer4.getId()).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_MODERATION);

        // not updated offer
        Offer notUpdated = offerRepository.getOfferById(dontTouchIt.getId());
        MbocAssertions.assertThat(notUpdated).isEqualTo(dontTouchIt);
    }

    @Test
    public void testNewContentPipelineOffers() {
        Supplier newPipeSupplier = supplierRepository.insert(new Supplier()
            .setName("new pipeline")
            .setNewContentPipeline(true));
        Category category = new Category().setCategoryId(1L).setAcceptGoodContent(false);
        Category goodCategory = new Category().setCategoryId(2L).setAcceptGoodContent(true);
        categoryCachingServiceMock.addCategory(category);
        categoryInfoRepository.insert(new CategoryInfo(category.getCategoryId()).setModerationInYang(true));
        categoryCachingServiceMock.addCategory(goodCategory);
        categoryInfoRepository.insert(new CategoryInfo(goodCategory.getCategoryId()).setModerationInYang(true));

        Model sku = new Model()
            .setId(1L)
            .setTitle("d")
            .setCategoryId(category.getCategoryId())
            .setModelType(Model.ModelType.SKU);
        Model skuGood = new Model()
            .setId(2L)
            .setTitle("d")
            .setCategoryId(goodCategory.getCategoryId())
            .setModelType(Model.ModelType.SKU);
        modelStorageCachingServiceMock
            .addModel(sku)
            .addModel(skuGood);

        Offer offer1 = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .setBusinessId(newPipeSupplier.getId())
            .setCategoryIdForTests(goodCategory.getCategoryId(), Offer.BindingKind.SUPPLIER)
            .setSupplierSkuMapping(Mapping.fromSku(skuGood))
            .setSupplierSkuMappingStatus(MappingStatus.NEW);
        Offer offer2 = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .setBusinessId(newPipeSupplier.getId())
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(Mapping.fromSku(sku));
        Offer offer3 = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .setBusinessId(newPipeSupplier.getId())
            .setCategoryIdForTests(category.getCategoryId(), Offer.BindingKind.SUGGESTED);
        Offer dontTouchIt1 = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .setBusinessId(newPipeSupplier.getId())
            .setCategoryIdForTests(goodCategory.getCategoryId(), Offer.BindingKind.SUPPLIER)
            .setSupplierSkuMapping(Mapping.fromSku(skuGood))
            .setSupplierSkuMappingStatus(MappingStatus.RE_SORT);

        offerRepository.insertOffer(offer1);
        offerRepository.insertOffer(offer2);
        offerRepository.insertOffer(offer3);
        offerRepository.insertOffer(dontTouchIt1);

        Stream.of(offer1, offer2, offer3, dontTouchIt1)
            .filter(Offer::hasCategoryId)
            .map(Offer::getCategoryId)
            .distinct()
            .filter(Objects::nonNull)
            .forEach(categoryKnowledgeService::addCategory);

        execute();

        // Moderation offer
        Offer updated1 = offerRepository.getOfferById(offer1.getId());
        Offer updated2 = offerRepository.getOfferById(offer2.getId());
        Offer updated3 = offerRepository.getOfferById(offer3.getId());
        Assertions.assertThat(updated1.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_MODERATION);
        Assertions.assertThat(updated2.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_MODERATION);
        Assertions.assertThat(updated3.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);

        Offer notUpdated1 = offerRepository.getOfferById(dontTouchIt1.getId());
        MbocAssertions.assertThat(notUpdated1).isEqualTo(dontTouchIt1);
    }

    @Test
    public void testReopenForContentLabOffers() {
        Offer offer = createOffer()
            .setContentChangedTs(NOW.minusHours(1)).setBindingKind(Offer.BindingKind.APPROVED);
        Offer contentLabOffer = createOffer()
            .setContentChangedTs(NOW.minusHours(1)).setBindingKind(Offer.BindingKind.APPROVED)
            .setThroughContentLab(true);
        if (contentLabOffer.hasCategoryId()) {
            categoryKnowledgeService.addCategory(contentLabOffer.getCategoryId());
        }

        offerRepository.insertOffers(offer, contentLabOffer);

        execute();

        contentLabOffer = offerRepository.getOfferById(contentLabOffer.getId());
        Assertions.assertThat(contentLabOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(contentLabOffer.getLastPrimaryProcessingStatus()).isEqualTo(Offer.ProcessingStatus.REOPEN);
    }

    @Test
    public void retrieveSkuType() {
        Offer offer = createOffer()
            .setContentChangedTs(NOW.minusHours(1))
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.MARKET).copyWithSkuType(null))
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        offerRepository.insertOffers(offer);
        categoryInfoRepository.insert(new CategoryInfo(1).setModerationInYang(true));
        categoryCachingServiceMock.addCategory(1);
        modelStorageCachingServiceMock.addModel(new Model().setId(1)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setModelQuality(Model.ModelQuality.OPERATOR));

        execute();

        var updated = offerRepository.getOfferById(offer.getId());
        Assert.assertEquals(Offer.SkuType.MARKET, updated.getSuggestSkuMapping().getSkuType());
    }

    @Test
    public void testItDoesntReopenAlreadyReopened() {
        var offer = createOffer()
            .setContentChangedTs(NOW.minus(ReopenNeedInfoService.SEND_IF_OLD_ACTIVITY).minusMinutes(1));
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());
        offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);
        offerRepository.updateOffer(offer);

        execute();

        offer = offerRepository.getOfferById(offer.getId());
        // status is not set to RE_OPEN although offer has been enqueued
        MbocAssertions.assertThat(offer)
            .hasProcessingStatus(Offer.ProcessingStatus.IN_PROCESS);
    }

    private Offer createOffer() {
        return OfferTestUtils.nextOffer()
            .setCategoryIdInternal(90401L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_INFO)
            .setProcessingStatusModifiedInternal(NOW.minusMonths(1))
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
    }
}
