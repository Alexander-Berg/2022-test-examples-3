package ru.yandex.market.mboc.common.services.dsbs;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.ReimportFashionLogRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedMappingLogRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator.DEFAULT_THRESHOLD;
import static ru.yandex.market.mboc.common.offers.model.Offer.DsbsAssortmentStatus.FROZEN;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

/**
 * @author apluhin
 * @created 2/16/22
 */
public class YqlFreezeHidingDsbsTest extends BaseDbTestClass {


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


    private YqlFreezeHidingDsbs yqlFreezeHidingDsbs;


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


        yqlFreezeHidingDsbs = new YqlFreezeHidingDsbs(
            null,
            storageKeyValueService,
            spyOfferRepository,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
            Mockito.mock(SendDataCampOfferStatesService.class), offersProcessingStatusService, transactionHelper);

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
        yqlFreezeHidingDsbs.prepareRead();
    }

    @Test
    public void testFreezeOffer() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.DSBS)
            .setSupplierSkuMapping(null)
            .setSuggestSkuMapping(null)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
            .setCreated(DEFAULT_THRESHOLD.minusDays(1).atTime(0, 0))
            .setCategoryIdInternal(TEST_CATEGORY_INFO_ID)
            .setAutomaticClassification(true)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setGroupId(1)
            .setApprovedSkuMappingInternal(null)
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);

        List<Offer.DsbsAssortmentHiding> hidings = List.of(Offer.DsbsAssortmentHiding.DISABLED_PARTNER,
            Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT);

        yqlFreezeHidingDsbs.handleBatch(List.of(new FreezeRow(14L, hidings)));

        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.OPEN);
        Assertions.assertThat(offerById.getDsbsAssortmentStatus())
            .isEqualTo(FROZEN);
        Assertions.assertThat(offerById.getAssortmentHidings()).containsExactlyInAnyOrderElementsOf(hidings);
        Assertions.assertThat(offerById.getOfferErrors()).containsExactly(
            MbocErrors.get().frozenOffer(offerById.getShopSku())
        );
    }

    @Test
    public void testExtractRow() throws Exception {
        ResultSet mock = Mockito.mock(ResultSet.class);
        Mockito.when(mock.getBoolean(Mockito.eq("disabled_partner"))).thenReturn(false);
        Mockito.when(mock.getBoolean(Mockito.eq("price_error"))).thenReturn(false);
        Mockito.when(mock.getBoolean(Mockito.eq("not_has_stocks"))).thenReturn(true);
        Mockito.when(mock.getBoolean(Mockito.eq("disabled_shop"))).thenReturn(false);
        Mockito.when(mock.getBoolean(Mockito.eq("has_not_content"))).thenReturn(true);
        Mockito.when(mock.getLong(Mockito.eq("offer_id"))).thenReturn(1L);
        FreezeRow freezeRow = yqlFreezeHidingDsbs.extractRow(mock);
        Assertions.assertThat(freezeRow).isEqualTo(
            new FreezeRow(1L,
                List.of(
                    Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS,
                    Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT))
        );
    }
}
