package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
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
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.OfferProcessingStrategiesHolder;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public abstract class OffersProcessingStatusServiceTestBase extends BaseDbTestClass {

    private static final long SEED = 15486;

    protected final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();

    protected static final long CATEGORY_ID = 234;

    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected OfferBatchProcessor offerBatchProcessor;
    @Autowired
    protected SupplierRepository supplierRepository;
    @Autowired
    protected ProcessingTicketInfoRepository processingTicketInfoRepository;
    @Autowired
    protected CategoryInfoRepository categoryInfoRepository;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;

    protected SupplierService supplierService;

    protected NeedContentStatusService needContentStatusService;
    protected CategoryCachingServiceMock categoryCachingServiceMock;
    protected CategoryKnowledgeServiceMock categoryKnowledgeService;

    protected OffersProcessingStatusService offersProcessingStatusService;
    protected OfferMappingActionService offerMappingActionService;
    protected ModelStorageCachingServiceMock modelStorageCachingService;
    protected RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    protected ProcessingTicketInfoService processingTicketInfoService;

    protected final Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
    protected final Supplier whiteSupplier = OfferTestUtils.whiteSupplier().setId(OfferTestUtils.WHITE_SUPPLIER_ID);
    protected final Supplier fmcgSupplier = OfferTestUtils.fmcgSupplier();
    protected final Supplier blueNoModerationSupplier = OfferTestUtils.simpleSupplier()
        .setId(0)
        .setNewContentPipeline(true)
        .setDisableModeration(true);

    @Before
    public void setUpBase() throws Exception {
        supplierService = new SupplierService(supplierRepository);

        categoryInfoRepository.insert(new CategoryInfo(CATEGORY_ID));

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(CATEGORY_ID)
            .setHasKnowledge(true)
            .setAcceptGoodContent(true)
        );

        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService.addCategory(CATEGORY_ID);

        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));

        var legacyOfferMappingActionService = Mockito.mock(LegacyOfferMappingActionService.class);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);

        modelStorageCachingService = new ModelStorageCachingServiceMock();
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false, false, 3, categoryInfoCache);
        supplierRepository.insertBatch(supplier, whiteSupplier, fmcgSupplier, blueNoModerationSupplier);

        TrackerService trackerServiceMock = new TrackerServiceMock();
        var honestMarkClassificationService = Mockito.mock(HonestMarkClassificationService.class);
        Mockito.when(honestMarkClassificationService.getClassificationResult(
                Mockito.any(Offer.class), Mockito.anyLong(), Mockito.any(), Mockito.anySet(), Mockito.anySet()))
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));
        var converterConfig = new OffersToExcelFileConverterConfig(categoryCachingServiceMock);
        var classifierConverter = converterConfig.classifierConverter(categoryCachingServiceMock);
        var processingTicketHelper = new ProcessingTicketHelper("unit-tests", trackerServiceMock,
            Mockito.mock(ManagersService.class), categoryCachingServiceMock, processingTicketInfoService);
        var classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingServiceMock,
            offerMappingActionService,
            offerDestinationCalculator
        );
        var classificationOffersProcessingStrategy = new ClassificationOffersProcessingStrategy(trackerServiceMock,
            offerRepository, supplierRepository, Mockito.mock(MasterDataHelperService.class),
            classifierConverter, categoryKnowledgeService, classificationOffersProcessingService,
            processingTicketHelper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, Mockito.mock(NeedContentStatusService.class),
            Mockito.mock(ApplySettingsService.class), offersProcessingStatusService,
            false);

        var holder = Mockito.mock(OfferProcessingStrategiesHolder.class);
        Mockito.when(holder.getStrategy(Mockito.any()))
            .thenReturn(Mockito.mock(OffersProcessingStrategy.class));
        Mockito.when(holder.getStrategy(TicketType.CLASSIFICATION))
            .thenReturn(classificationOffersProcessingStrategy);
    }

    protected String normalize(String s) {
        return s.trim().replaceAll("\\r", "").replaceAll("\\s+\\n", "\n");
    }

    protected Offer createTestOfferForModeration(long id) {
        return new Offer()
            .setId(id)
            .setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID)
            .setShopSku("Sku-" + id)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("shop_category_name")
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(OfferTestUtils.mapping(15L, Offer.SkuType.MARKET))
            .setTitle("Offer_" + id);
    }

    protected Map<Long, Offer> getOffers(Collection<Long> ids) {
        return offerRepository.findOffers(new OffersFilter().setOfferIds(ids)).stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));
    }

    protected Offer newOffer(Offer.ProcessingStatus status, Offer.BindingKind bindingKind) {
        return newOffer(supplier, status, bindingKind);
    }

    protected Offer newOffer(Supplier supplier, Offer.ProcessingStatus status, Offer.BindingKind bindingKind) {
        return OfferTestUtils.nextOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, bindingKind)
            .updateProcessingStatusIfValid(status);
    }

    protected AntiMapping createAntiMapping(Offer offer, long notSkuId) {
        return new AntiMapping()
            .setOfferId(offer.getId())
            .setNotSkuId(notSkuId)
            .setSourceType(AntiMapping.SourceType.MODERATION_REJECT);
    }
}
