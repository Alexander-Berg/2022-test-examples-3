package ru.yandex.market.mboc.tms.service.manual;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.OfferManualProcessingQueueRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferManualProcessingServiceTest extends BaseDbTestClass {

    @Autowired
    private OfferManualProcessingQueueRepository queue;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;

    private CategoryCachingServiceMock categoryCache;
    private CategoryKnowledgeServiceMock categoryKnowledgeServiceMock;

    @Autowired
    private CategoryRuleService categoryRuleService;

    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;

    private NeedContentStatusService needContentStatusService;
    private BooksService booksService;
    private CompositeAutoApproveService autoApproveService;
    private OfferMappingActionService offerMappingActionService;
    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;

    private ApplySettingsService applySettingsService;
    private OffersProcessingStatusService processingStatusService;
    private final TransactionHelper transactionHelper = TransactionHelper.MOCK;

    private OfferManualProcessingService service;

    private final LocalDate sinceDate = LocalDate.of(2022, 5, 5);

    @Before
    public void setUp() throws Exception {
        categoryCache = new CategoryCachingServiceMock();
        supplierService = new SupplierService(supplierRepository);
        booksService = new BooksService(categoryCache, Set.of(90829L, 15797254L, 15708154L));
        needContentStatusService = new NeedContentStatusService(categoryCache, supplierService, booksService);
        modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        categoryKnowledgeServiceMock = new CategoryKnowledgeServiceMock();
        categoryKnowledgeServiceMock.enableAllCategories();
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        processingStatusService = new OffersProcessingStatusService(offerBatchProcessor, needContentStatusService,
            supplierService, categoryKnowledgeServiceMock, retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            new StorageKeyValueServiceMock(), new FastSkuMappingsService(needContentStatusService), true, true, 3,
            categoryInfoCache);
        var supplierAutoApproveService = new SupplierAutoApproveServiceImpl(
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepository
        );
        var suggestAutoApproveService = new SuggestAutoApproveServiceImpl(
            categoryInfoRepository,
            modelStorageCachingServiceMock, offerMappingActionService, antiMappingRepository
        );
        autoApproveService = new CompositeAutoApproveService(
            antiMappingRepository, supplierAutoApproveService, suggestAutoApproveService
        );
        var acceptanceService = new AcceptanceService(categoryInfoRepository, categoryCache, supplierService,
            false, categoryRuleService, true, offerDestinationCalculator);
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        applySettingsService = new ApplySettingsService(supplierService,
            acceptanceService, autoApproveService, processingStatusService, fastSkuMappingsService);

        service = new OfferManualProcessingService(queue, storageKeyValueService, offerRepository,
            categoryInfoCache, applySettingsService, offerDestinationCalculator, transactionHelper);
        storageKeyValueService.putValue(
            ContextedOfferDestinationCalculator.ENABLED_TAGS,
            "{\"FASHION\":\"" + sinceDate + "\",\"MEDICINE\":\"" + sinceDate + "\"}");
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void applyFashionToCategoryTest() {
        var supplier = OfferTestUtils.simpleSupplier().setDropshipBySeller(true).setType(MbocSupplierType.DSBS);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory();
        categoryInfoRepository.insert(categoryInfo);

        var offers = IntStream.range(0, 1000)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .map(offer -> offer.updateAcceptanceStatus(offerDestinationCalculator, Offer.AcceptanceStatus.OK))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
        });

        var categoryInfo1 = categoryInfoRepository.findById(categoryInfo.getCategoryId())
            .addTag(CategoryInfo.CategoryTag.FASHION);
        categoryInfoRepository.update(categoryInfo1);

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
        });
    }

    @Test
    public void undoFashionFromCategoryTest() {
        var supplier = OfferTestUtils.simpleSupplier().setDropshipBySeller(true).setType(MbocSupplierType.DSBS);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory()
            .addTag(CategoryInfo.CategoryTag.FASHION);
        categoryInfoRepository.insert(categoryInfo);

        var offers = IntStream.range(0, 100)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .map(offer -> offer.updateAcceptanceStatus(offerDestinationCalculator, Offer.AcceptanceStatus.OK))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
        });

        var categoryInfo1 = categoryInfoRepository.findById(categoryInfo.getCategoryId())
            .removeTag(CategoryInfo.CategoryTag.FASHION);
        categoryInfoRepository.update(categoryInfo1);

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
        });
    }

    @Test
    public void applyAndUndoMedicineToCategoryTest() {
        var supplier = OfferTestUtils.simpleSupplier().setDropshipBySeller(true).setType(MbocSupplierType.DSBS);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory();
        categoryInfoRepository.insert(categoryInfo);

        var offers = IntStream.range(0, 1000)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .map(offer -> offer.updateAcceptanceStatus(offerDestinationCalculator, Offer.AcceptanceStatus.OK))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
        });

        var categoryInfo1 = categoryInfoRepository.findById(categoryInfo.getCategoryId())
            .addTag(CategoryInfo.CategoryTag.MEDICINE);
        categoryInfoRepository.update(categoryInfo1);

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
        });

        var categoryInfo2 = categoryInfoRepository.findById(categoryInfo.getCategoryId())
            .removeTag(CategoryInfo.CategoryTag.MEDICINE);
        categoryInfoRepository.update(categoryInfo2);

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
        });
    }

    @Test
    public void applyAcceptanceTest() {
        var supplier = OfferTestUtils.simpleSupplier().setFulfillment(true).setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory();
        categoryInfo.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfo.setCategorySuggestAutoApprove(true);
        categoryInfoRepository.insert(categoryInfo);

        var offers = IntStream.range(0, 100)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .collect(Collectors.toList());

        offerRepository.insertOffers(offers);
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        });

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        });
    }

    @Test
    public void applyProcessingTest() {
        var supplier = OfferTestUtils.simpleSupplier().setFulfillment(true).setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory();
        categoryInfo.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfo.setCategorySuggestAutoApprove(true);
        categoryInfoRepository.insert(categoryInfo);

        modelStorageCachingServiceMock.addModel(
            new Model().setId(100L)
                .setCategoryId(category.getCategoryId())
                .setTitle("model title")
        );

        var offers = IntStream.range(0, 100)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .map(offer -> offer.setSuggestSkuMapping(
                    new Offer.Mapping(100, LocalDateTime.now(), Offer.SkuType.MARKET))
                .setSuggestSkuMappingType(SkuBDApi.SkutchType.BARCODE_SKUTCH)
            )
            .collect(Collectors.toList());

        offerRepository.insertOffers(offers);
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.NEW);
        });

        processAllOffers();

        assertThat(queue.hasOffersToProcess()).isFalse();
        offerRepository.findAll().forEach(offer -> {
            assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
            assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
            assertThat(offer.hasApprovedSkuMapping()).isTrue();
            assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        });
    }

    @Test
    public void testZeroAndOneManualOffersRange() {
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
        generateManualOffersRange(1);
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    @Test
    public void test1999ManualOffersRange() {
        generateManualOffersRange(1999);
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    @Test
    public void test2000ManualOffersRange() {
        generateManualOffersRange(2000);
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    @Test
    public void test2001ManualOffersRange() {
        generateManualOffersRange(2001);
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    @Test
    public void test9999ManualOffersRange() {
        generateManualOffersRange(9999);
        processAllOffers();
        assertThat(queue.hasOffersToProcess()).isFalse();
    }

    private void processAllOffers() {
        var ids = offerRepository.findAll().stream().map(Offer::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            queue.enqueue(ids);
        }
        service.processOffers();
    }

    private Offer generateOfferWithId(Supplier supplier, Category category, int id) {
        return OfferTestUtils.simpleOffer(supplier)
            .setShopSku("ssku-" + id)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.ACTIVE)
            .setCategoryIdInternal(category.getCategoryId());
    }

    private void generateManualOffersRange(int range) {
        var supplier = OfferTestUtils.simpleSupplier().setFulfillment(true).setType(MbocSupplierType.THIRD_PARTY);
        supplierRepository.insert(supplier);

        var category = OfferTestUtils.defaultCategory();
        categoryCache.addCategory(category);

        var categoryInfo = allManualCategory();
        categoryInfo.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT);
        categoryInfo.setCategorySuggestAutoApprove(true);
        categoryInfoRepository.insert(categoryInfo);

        var offers = IntStream.range(0, range)
            .mapToObj(i -> generateOfferWithId(supplier, category, i))
            .collect(Collectors.toList());
        offerRepository.insertOffers(offers);
    }

    private CategoryInfo allManualCategory() {
        var categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance();
        categoryInfo.setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfo.setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfo.setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);
        categoryInfo.setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL);

        return categoryInfo;
    }
}
