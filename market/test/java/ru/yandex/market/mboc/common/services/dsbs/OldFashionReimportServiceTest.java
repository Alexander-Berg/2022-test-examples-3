package ru.yandex.market.mboc.common.services.dsbs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.ReimportFashionLogRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedMappingLogRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.fashion.FashionClassificationCheckImpl;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator.DEFAULT_THRESHOLD;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

/**
 * @author apluhin
 * @created 2/10/22
 */
@Slf4j
public class OldFashionReimportServiceTest extends BaseDbTestClass {

    public static String KEY = "OldFashionReimportService.handled_offset";

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository spyOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    private AntiMappingService antiMappingService;
    private OfferMappingActionService offerMappingActionService;
    @Autowired
    private RemovedMappingLogRepository removedMappingLogRepository;
    private RemovedMappingLogRepository spyRemovedMappingLogRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ContentProcessingQueueRepository contentProcessingQueueRepository;
    private OffersProcessingStatusService spyOffersProcessingStatusService;
    @Autowired
    private QueueFromContentProcessingRepository queueFromContentProcessingRepository;
    @Autowired
    private ReimportFashionLogRepository reimportFashionLogRepository;

    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;


    private OldFashionReimportService oldFashionReimportService;

    private OldFashionReimportService oldFashionReimportServiceMock;


    @Before
    public void setUp() {
        spyOfferRepository = Mockito.spy(offerRepository);
        spyRemovedMappingLogRepository = Mockito.spy(removedMappingLogRepository);
        categoryCachingService = new CategoryCachingServiceMock();
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var supplierService = new SupplierService(supplierRepository);

        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService,
            offerBatchProcessor, supplierRepository);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache);
        spyOffersProcessingStatusService = Mockito.spy(offersProcessingStatusService);


        oldFashionReimportService = new OldFashionReimportService(
            offersProcessingStatusService,
            spyOfferRepository,
            transactionHelper,
            storageKeyValueService,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
            new FashionClassificationCheckImpl(storageKeyValueService, offersProcessingStatusService,
                queueFromContentProcessingRepository, spyOfferRepository, offerDestinationCalculator),
            reimportFashionLogRepository);

        List<Supplier> suppliers = IntStream.of(
            42, 43, 44, 45, 50, 99, 100
        )
            .mapToObj(id -> OfferTestUtils.simpleSupplier().setId(id))
            .collect(Collectors.toList());
        supplierRepository.insertOrUpdateAll(suppliers);

        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(offers);

        var category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);
        categoryRepository.insert(OfferTestUtils.defaultCategory().setCategoryId(90401L));

        var categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance()
            .addTag(CategoryInfo.CategoryTag.FASHION)
            .addTag(CategoryInfo.CategoryTag.SIZED_FASHION);
        categoryInfoRepository.insert(categoryInfo);
        categoryCachingService.addCategory(category);

    }

    @Test
    public void testReimportOldFashionToClassification() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.DSBS)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setCreated(DEFAULT_THRESHOLD.minusDays(1).atTime(0, 0))
            .setCategoryIdInternal(TEST_CATEGORY_INFO_ID)
            .setAutomaticClassification(true)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setGroupId(1)
            .setApprovedSkuMappingInternal(null)
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);
        oldFashionReimportService.reimportOfferKeys(List.of(14L));
        Assertions.assertThat(offerRepository.getOfferById(14L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(reimportFashionLogRepository.findById(14L).isCompleteReimport()).isFalse();
    }

    @Test
    public void testReimportOldFashionToModeration() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.DSBS)
            .setSuggestSkuMapping(null)
            .setCreated(DEFAULT_THRESHOLD.minusDays(1).atTime(0, 0))
            .setCategoryIdInternal(TEST_CATEGORY_INFO_ID)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setGroupId(1)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.PARTNER20))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.AUTO_ACCEPTED)
            .setApprovedSkuMappingInternal(null)
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);
        oldFashionReimportService.reimportOfferKeys(List.of(14L));
        Assertions.assertThat(offerRepository.getOfferById(14L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_MODERATION);
        Assertions.assertThat(reimportFashionLogRepository.findById(14L).isCompleteReimport()).isFalse();
    }

    @Test
    public void testCheckByEmptyGroupId() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.DSBS)
            .setSuggestSkuMapping(null)
            .setCreated(DEFAULT_THRESHOLD.minusDays(1).atTime(0, 0))
            .setCategoryIdInternal(TEST_CATEGORY_INFO_ID)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT)
            .setApprovedSkuMappingInternal(null)
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);
        oldFashionReimportService.reimportOfferKeys(List.of(14L));
        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        Assertions.assertThat(offerById.getContentStatusActiveError()).isNotNull();
        Collection<ContentProcessingOffer> failedKeys =
            contentProcessingQueueRepository.findAllByBusinessSkuKeys(
                new BusinessSkuKey(offerById.getBusinessId(), offerById.getShopSku())
            );
        Assertions.assertThat(failedKeys).isEmpty();
        Assertions.assertThat(reimportFashionLogRepository.findById(14L).isCompleteReimport()).isFalse();
    }

    @Test
    public void testReimportOldFashionToContentProcessing() {
        Offer offer = getOffer().setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        offerRepository.updateOffers(offer);
        oldFashionReimportService.reimportOfferKeys(List.of(14L));
        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(reimportFashionLogRepository.findById(14L).isCompleteReimport()).isFalse();
    }

    private Offer getOffer() {
        return offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.DSBS)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setCreated(DEFAULT_THRESHOLD.minusDays(1).atTime(0, 0))
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setGroupId(1)
            .setCategoryIdInternal(TEST_CATEGORY_INFO_ID)
            .setApprovedSkuMappingInternal(null)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
    }

    @Test
    public void testBatchSavepoint() {
        Offer offer = getOffer();
        List<Long> collect1 = LongStream.range(1000, 1099).boxed().collect(Collectors.toList());
        List<Offer> collect =
            LongStream.range(1000, 1101).boxed()
                .map(it -> offer.copy().setId(it).setShopSku("qwerty" + it)).collect(Collectors.toList());
        offerRepository.insertOffers(collect);
        oldFashionReimportService.reimportOfferKeys(collect1);
        Assertions.assertThat(
            storageKeyValueService.getLong(KEY, 0L))
            .isEqualTo(collect1.size());
    }

    @Test
    public void testIgnoreBySavepoint() {
        Offer offer = getOffer();
        List<Long> collect1 = LongStream.range(1000, 1101).boxed().collect(Collectors.toList());
        List<Offer> collect =
            LongStream.range(1000, 1101).boxed()
                .map(it -> offer.copy().setId(it).setShopSku("qwerty" + it)).collect(Collectors.toList());
        offerRepository.insertOffers(collect);
        storageKeyValueService.putValue(KEY, 100L);
        oldFashionReimportService.reimportOfferKeys(collect1);

        ArgumentCaptor<OffersFilter> captor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(spyOfferRepository, Mockito.times(2))
            .findOffers(captor.capture(), Mockito.anyBoolean());

        Assertions.assertThat(captor.getValue().getOfferIds().size()).isEqualTo(1);
    }
}
