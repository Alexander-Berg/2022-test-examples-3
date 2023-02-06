package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferMappingHistory;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMappingHistoryRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static ru.yandex.market.mboc.common.offers.model.Offer.RecheckMappingStatus.ON_RECHECK;

public class OfferMappingRollbackServiceTest extends BaseDbTestClass {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryInfoRepository categoryInfoRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferMappingHistoryRepository offerMappingHistoryRepository;

    private OfferMappingRollbackService service;
    private OffersProcessingStatusService offersProcessingStatusService;

    private Supplier supplier;
    private Supplier business;
    private Offer offer;
    private Category category;
    private CategoryInfo categoryInfo;

    private final List<Offer.Mapping> mappings = List.of(
        new Offer.Mapping(101L, LocalDateTime.now(), Offer.SkuType.FAST_SKU),
        new Offer.Mapping(102L, LocalDateTime.now().plusDays(1), Offer.SkuType.PARTNER20),
        new Offer.Mapping(103L, LocalDateTime.now().plusDays(2), Offer.SkuType.MARKET)
    );

    @Before
    public void setUp() throws Exception {
        var categoryCachingServiceMock = new CategoryCachingServiceMock();
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock()
            .enableAllCategories();
        var legacyActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            storageKeyValueService);
        var mappingActionService = new OfferMappingActionService(legacyActionService);
        var supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(
            categoryCachingServiceMock, supplierService, Mockito.mock(BooksService.class));
        var fastSkuMappingsService = new FastSkuMappingsService(needContentStatusService);
        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        offersProcessingStatusService = new OffersProcessingStatusService(
            null,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            mappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            fastSkuMappingsService,
            true,
            true,
            3, categoryInfoCache);

        service = new OfferMappingRollbackService(
            offerRepository,
            offerMappingHistoryRepository,
            offersProcessingStatusService
        );

        category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);

        categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance();
        categoryInfoRepository.insert(categoryInfo);

        supplier = OfferTestUtils.simpleSupplier()
            .setFulfillment(true)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setMbiBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        business = OfferTestUtils.businessSupplier();
        supplierRepository.insertBatch(business, supplier);

        offer = OfferTestUtils.simpleOkOffer()
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setCategoryId(offerDestinationCalculator, category.getCategoryId(), Offer.BindingKind.APPROVED)
            .addNewServiceOfferIfNotExists(offerDestinationCalculator, supplier);
        offerRepository.insertOffer(offer);
        offer = offerRepository.getOfferById(offer.getId());

        offer.updateApprovedSkuMapping(mappings.get(0), Offer.MappingConfidence.PARTNER_FAST);
        offerRepository.updateOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());

        offer.updateApprovedSkuMapping(mappings.get(1), Offer.MappingConfidence.PARTNER_SELF);
        offerRepository.updateOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());

        offer.updateApprovedSkuMapping(mappings.get(2), Offer.MappingConfidence.DEDUPLICATION);
        offerRepository.updateOffers(offer);
        offer = offerRepository.getOfferById(offer.getId());
    }

    @Test
    public void testRollback() {
        service.startRollback(List.of(offer), Map.of());

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(offer.hasRecheckSkuMapping()).isTrue();
        assertThat(offer.getRecheckSkuMapping().getMappingId()).isEqualTo(mappings.get(1).getMappingId());

        var events = offerMappingHistoryRepository.findByOfferId(offer.getId());
        assertThat(events).hasSize(3);
        events.stream()
            .filter(event -> event.getSkuIdAfter() == 103L)
            .forEach(event ->
                assertThat(event.getRecoverStatus()).isEqualTo(OfferMappingHistory.RecoverStatus.IN_RECOVER)
            );
        events.stream()
            .filter(event -> event.getSkuIdAfter() != 103L)
            .forEach(event ->
                assertThat(event.getRecoverStatus()).isNotEqualTo(OfferMappingHistory.RecoverStatus.IN_RECOVER)
            );
    }

    @Test
    public void testNextRollbackIteration() {
        Integer beforeProcessingCounter = offer.getProcessingCounter();

        service.startRollback(List.of(offer), Map.of());
        service.nextRollback(List.of(offer), OfferMappingHistory.RecoverStatus.REJECT_BY_MODERATION);
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(offer.hasRecheckSkuMapping()).isTrue();
        assertThat(offer.getRecheckSkuMapping().getMappingId()).isEqualTo(mappings.get(0).getMappingId());

        var events = offerMappingHistoryRepository.findByOfferId(offer.getId());
        assertThat(events).hasSize(3);
        events.stream()
            .filter(event -> event.getSkuIdAfter() == 103L)
            .forEach(event ->
                assertThat(event.getRecoverStatus()).isEqualTo(OfferMappingHistory.RecoverStatus.REJECT_BY_MODERATION)
            );
        events.stream()
            .filter(event -> event.getSkuIdAfter() == 102L)
            .forEach(event ->
                assertThat(event.getRecoverStatus()).isEqualTo(OfferMappingHistory.RecoverStatus.IN_RECOVER)
            );
        events.stream()
            .filter(event -> event.getSkuIdAfter() == 101L)
            .forEach(event -> assertThat(event.getRecoverStatus()).isNull());

        Assertions.assertThat(beforeProcessingCounter).isNull();

        offerRepository.updateOffer(offer);
        // FIXME: Return to 1 after MBOASSORT-3023
        Assertions.assertThat(offer.getProcessingCounter()).isEqualTo(2);
    }

    @Test
    public void testJobWithFriends() {
        var friend = OfferTestUtils.simpleOkOffer()
            .setShopSku("friend")
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setCategoryId(offerDestinationCalculator, category.getCategoryId(), Offer.BindingKind.APPROVED)
            .addNewServiceOfferIfNotExists(offerDestinationCalculator, supplier)
            .updateApprovedSkuMapping(mappings.get(1), Offer.MappingConfidence.PARTNER_SELF);
        offerRepository.insertOffer(friend);
        friend = offerRepository.getOfferById(friend.getId());
        friend.updateApprovedSkuMapping(mappings.get(2), Offer.MappingConfidence.DEDUPLICATION);
        offerRepository.updateOffer(friend);
        friend = offerRepository.getOfferById(friend.getId());

        var notFriend1 = OfferTestUtils.simpleOkOffer()
            .setShopSku("notFriend1")
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setCategoryId(offerDestinationCalculator, category.getCategoryId(), Offer.BindingKind.APPROVED)
            .addNewServiceOfferIfNotExists(offerDestinationCalculator, supplier)
            .updateApprovedSkuMapping(mappings.get(0), Offer.MappingConfidence.PARTNER_FAST);
        offerRepository.insertOffer(notFriend1);
        notFriend1 = offerRepository.getOfferById(notFriend1.getId());
        notFriend1.updateApprovedSkuMapping(mappings.get(2), Offer.MappingConfidence.DEDUPLICATION);
        offerRepository.updateOffer(notFriend1);
        notFriend1 = offerRepository.getOfferById(notFriend1.getId());

        var notFriend2 = OfferTestUtils.simpleOkOffer()
            .setShopSku("notFriend2")
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setCategoryId(offerDestinationCalculator, category.getCategoryId(), Offer.BindingKind.APPROVED)
            .addNewServiceOfferIfNotExists(offerDestinationCalculator, supplier)
            .updateApprovedSkuMapping(mappings.get(1), Offer.MappingConfidence.PARTNER_SELF);
        offerRepository.insertOffer(notFriend2);
        notFriend2 = offerRepository.getOfferById(notFriend2.getId());
        notFriend2.updateApprovedSkuMapping(mappings.get(2), Offer.MappingConfidence.DEDUPLICATION);
        offerRepository.updateOffer(notFriend2);
        notFriend2 = offerRepository.getOfferById(notFriend2.getId());
        notFriend2.updateApprovedSkuMapping(mappings.get(1), Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffer(notFriend2);
        notFriend2 = offerRepository.getOfferById(notFriend2.getId());

        var offers = List.of(offer, friend, notFriend1, notFriend2);
        offersProcessingStatusService.processOffers(offers);
        offerRepository.updateOffers(offers);
        assertThat(offers.stream().map(Offer::getProcessingStatus).collect(Collectors.toSet()))
            .containsOnly(Offer.ProcessingStatus.PROCESSED);

        var events = offerMappingHistoryRepository.findByOfferId(offer.getId());
        assertThat(events).hasSize(3);
        events.stream()
            .filter(event -> event.getSkuIdAfter() == 103L)
            .forEach(event -> event.setRecoverStatus(OfferMappingHistory.RecoverStatus.INIT));
        offerMappingHistoryRepository.updateBatch(events);

        offer = offerRepository.getOfferById(offer.getId());
        var rollbackAffectedOffers = service.startRollback(List.of(offer), Map.of());
        offerRepository.updateOffers(rollbackAffectedOffers);

        offer = offerRepository.getOfferById(offer.getId());
        friend = offerRepository.getOfferById(friend.getId());
        notFriend1 = offerRepository.getOfferById(notFriend1.getId());
        notFriend2 = offerRepository.getOfferById(notFriend2.getId());

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        assertThat(Offer.Mapping.mappingEqual(offer.getRecheckSkuMapping(), mappings.get(1))).isTrue();
        assertThat(friend.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION);
        //check by approved mapping
        assertThat(friend.getRecheckMappingStatus()).isEqualTo(ON_RECHECK);
        assertThat(friend.hasRecheckSkuMapping()).isTrue();

        assertThat(notFriend1.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(notFriend2.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
    }

    @Test
    public void nullHistoryMappings() {
        offerMappingHistoryRepository.deleteAll();
        service.startRollback(List.of(offer), Map.of());

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.PROCESSED);
        assertThat(offer.hasRecheckSkuMapping()).isFalse();
    }
}
