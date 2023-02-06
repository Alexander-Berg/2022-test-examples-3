package ru.yandex.market.mboc.common.services.dsbs;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.MbocErrors;
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

import static ru.yandex.market.mboc.common.offers.model.Offer.DsbsAssortmentStatus.ACTIVE;
import static ru.yandex.market.mboc.common.offers.model.Offer.DsbsAssortmentStatus.FROZEN;

/**
 * @author apluhin
 * @created 2/18/22
 */
public class YqlUnfreezeHidingDsbsTest extends BaseDbTestClass {


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
    private OffersProcessingStatusService spyOffersProcessingStatusService;

    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private SendDataCampOfferStatesService sendDataCampOfferStatesService;


    private YqlUnfreezeHidingDsbs yqlUnfreezeHidingDsbs;


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

        sendDataCampOfferStatesService = Mockito.mock(SendDataCampOfferStatesService.class);
        yqlUnfreezeHidingDsbs = new YqlUnfreezeHidingDsbs(
            null,
            storageKeyValueService,
            spyOfferRepository,
            new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService),
            sendDataCampOfferStatesService,
            offersProcessingStatusService,
            storageKeyValueService,
            transactionHelper);

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
        yqlUnfreezeHidingDsbs.prepareRead();

    }

    @Test
    public void testUnFreezeOfferByAliveHiding() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.BLUE)
            .setDsbsAssortmentStatus(FROZEN)
            .setAssortmentHidings(Set.of(Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS,
                Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT))
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);

        List<Offer.DsbsAssortmentHiding> hidings = List.of(Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS);

        yqlUnfreezeHidingDsbs.handleBatch(List.of(new UnfreezeRow(14L, hidings)));

        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(offerById.getDsbsAssortmentStatus()).isEqualTo(ACTIVE);
        Assertions.assertThat(offerRepository.getOfferById(14L).getAssortmentHidings()).containsExactly(
            Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT
        );
    }

    @Test
    public void testNonCompleteUnFreezeOfferByAliveHiding() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.BLUE)
            .setDsbsAssortmentStatus(FROZEN)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setAssortmentHidings(Set.of(Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS,
                Offer.DsbsAssortmentHiding.PRICE_ERROR))
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);

        List<Offer.DsbsAssortmentHiding> hidings = List.of(Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS);

        yqlUnfreezeHidingDsbs.handleBatch(List.of(new UnfreezeRow(14L, hidings)));

        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.OPEN);
        Assertions.assertThat(offerById.getDsbsAssortmentStatus()).isEqualTo(FROZEN);
        Assertions.assertThat(offerRepository.getOfferById(14L).getAssortmentHidings()).isNotEmpty();
    }

    @Test
    public void unfreezeByContent() {
        Offer offer = offerRepository.getOfferById(14L).setOfferDestination(Offer.MappingDestination.BLUE)
            .setDsbsAssortmentStatus(FROZEN)
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setProcessingStatusInternal(Offer.ProcessingStatus.NEED_CONTENT)
            .setOfferErrors(
                List.of(
                    MbocErrors.get().frozenOffer("test")
                )
            )
            .setAssortmentHidings(Set.of(Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS,
                Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT))
            .setServiceOffers(new Offer.ServiceOffer(44, MbocSupplierType.DSBS, Offer.AcceptanceStatus.OK));
        offerRepository.updateOffers(offer);

        List<Offer.DsbsAssortmentHiding> hidings = List.of(Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT);

        yqlUnfreezeHidingDsbs.handleBatch(List.of(new UnfreezeRow(14L, hidings)));

        Offer offerById = offerRepository.getOfferById(14L);
        Assertions.assertThat(offerById.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_PROCESS);
        Assertions.assertThat(offerById.getDsbsAssortmentStatus()).isEqualTo(ACTIVE);
        Assertions.assertThat(offerById.getAssortmentHidings()).containsExactly(
            Offer.DsbsAssortmentHiding.NOT_HAS_STOCKS
        );
        Assertions.assertThat(offerById.getOfferErrors()).isEmpty();
    }

    @Test
    public void testExtractRow() throws Exception {
        ResultSet mock = Mockito.mock(ResultSet.class);
        Mockito.when(mock.getBoolean(Mockito.eq("disabled_partner"))).thenReturn(true);
        Mockito.when(mock.getBoolean(Mockito.eq("price_errors"))).thenReturn(false);
        Mockito.when(mock.getBoolean(Mockito.eq("not_has_stocks"))).thenReturn(true);
        Mockito.when(mock.getBoolean(Mockito.eq("disabled_shop"))).thenReturn(true);
        Mockito.when(mock.getBoolean(Mockito.eq("has_not_content"))).thenReturn(false);
        Mockito.when(mock.getLong(Mockito.eq("offer_id"))).thenReturn(1L);
        UnfreezeRow unfreezeRow = yqlUnfreezeHidingDsbs.extractRow(mock);
        Assertions.assertThat(unfreezeRow).isEqualTo(
            new UnfreezeRow(
                1L,
                List.of(
                    Offer.DsbsAssortmentHiding.PRICE_ERROR,
                    Offer.DsbsAssortmentHiding.HAS_NOT_CONTENT
                )
            )
        );
    }
}
