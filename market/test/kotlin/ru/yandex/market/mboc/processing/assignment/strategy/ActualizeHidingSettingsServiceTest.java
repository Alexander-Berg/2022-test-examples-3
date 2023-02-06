package ru.yandex.market.mboc.processing.assignment.strategy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.criteria.SimpleCriteria;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.SettingsForOfferProcessingInTolokaType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SettingsForOfferProcessingInToloka;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository;
import ru.yandex.market.mboc.processing.assignment.SettingsForOfferProcessingInTolokaRepository;
import ru.yandex.market.mboc.processing.assignment.TolokaHidingSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;

public class ActualizeHidingSettingsServiceTest extends BaseOfferProcessingTest {
    private static final AtomicLong IDS = new AtomicLong(101L);

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository offerRepositorySpied;

    @Autowired
    private OfferProcessingAssignmentRepository assignmentRepository;

    @Autowired
    private TolokaHidingSettings tolokaHidingSettings;

    @Autowired
    private SettingsForOfferProcessingInTolokaRepository settings;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private ActualizeHidingSettingsService service;

    @Before
    public void setUp() throws Exception {
        offerRepositorySpied = Mockito.spy(offerRepository);
        service = new ActualizeHidingSettingsService(offerRepositorySpied, assignmentRepository, tolokaHidingSettings,
            storageKeyValueService);

        var thirdPartySupplier = OfferTestUtils.simpleSupplier().setId(1001).setType(MbocSupplierType.THIRD_PARTY);
        var firstPartySupplier = OfferTestUtils.simpleSupplier().setId(1002).setRealSupplierId("rsid")
            .setType(MbocSupplierType.REAL_SUPPLIER);
        var dsbsSupplier = OfferTestUtils.simpleSupplier().setId(1003).setType(MbocSupplierType.DSBS);

        supplierRepository.insertBatch(thirdPartySupplier, firstPartySupplier, dsbsSupplier);

        var msku = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.MARKET);
        var psku = new Offer.Mapping(2L, LocalDateTime.now(), Offer.SkuType.PARTNER20);

        Category category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);
        Category root = OfferTestUtils.defaultCategory().setCategoryId(90401L);
        categoryRepository.insert(root);

        var categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance().setHideFromToloka(false);
        categoryInfoRepository.insert(categoryInfo);

        tolokaHidingSettings.reset();

        for (var supplier : List.of(thirdPartySupplier, firstPartySupplier, dsbsSupplier)) {
            for (var suggest : List.of(msku, psku)) {
                var offers = IntStream.range(0, 100)
                    .mapToObj(__ -> createApplicableOffer(supplier).setSuggestSkuMapping(suggest))
                    .collect(Collectors.toList());
                offerRepository.insertOffers(offers);
            }
        }

        storageKeyValueService.putValue(ActualizeHidingSettingsService.HIDING_LIMIT_KEY, 100);
    }

    @Test
    public void offerHidingDoesntRequireJob() {
        assignmentRepository.findAll()
            .forEach(assignment -> assertFalse(assignment.getHideFromToloka()));

        var updated = offerRepository.findAll().stream().limit(10)
            .peek(offer -> offer.setHideFromToloka(true)).collect(Collectors.toList());
        offerRepository.updateOffers(updated);

        assignmentRepository.findByIds(updated.stream().map(Offer::getId).collect(Collectors.toList()))
            .forEach(assignment -> assertTrue(assignment.getHideFromToloka()));
    }

    @Test
    public void hidingBySupplier() {
        var supplier = supplierRepository.findById(1001).setHideFromToloka(true);
        supplierRepository.update(supplier);

        var offerIds = new HashSet<>(offerRepository.findOfferIds(new OffersFilter().setBusinessIds(supplier.getId())));
        assignmentRepository.findByIds(offerIds)
            .forEach(assignment -> assertFalse(assignment.getHideFromToloka()));

        tolokaHidingSettings.reset();
        service.update();

        assignmentRepository.findAll().forEach(assignment ->
            assertEquals(offerIds.contains(assignment.getOfferId()), assignment.getHideFromToloka()));
    }

    @Test
    public void hidingDSBS() {
        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.DSBS)
            .setIsActive(false)));

        var offerIds = new HashSet<>(offerRepository.findOfferIds(new OffersFilter().setBusinessIds(1003)));

        tolokaHidingSettings.reset();
        service.update();

        assignmentRepository.findAll().forEach(assignment ->
            assertEquals(offerIds.contains(assignment.getOfferId()), assignment.getHideFromToloka()));
    }

    @Test
    public void hidingByMsku() {
        settings.updateSettings(List.of(new SettingsForOfferProcessingInToloka()
            .setCriteria(SettingsForOfferProcessingInTolokaType.MSKU)
            .setIsActive(false)));

        var offerIds = new HashSet<>(offerRepository.findOfferIds(new OffersFilter()
            .addCriteria((SimpleCriteria<Offer>) alias -> alias + "suggest_sku_mapping_sku_type = 'MARKET'")));

        tolokaHidingSettings.reset();
        service.update();

        assignmentRepository.findAll().forEach(assignment ->
            assertEquals(offerIds.contains(assignment.getOfferId()), assignment.getHideFromToloka()));
    }

    @Test
    public void lastOfferIdIsPersistedAndSetToZeroOnSuccess() {
        Mockito.when(offerRepositorySpied.getOffersByIds(anyCollection()))
            .thenCallRealMethod()
            .thenThrow(new RuntimeException("test"));

        storageKeyValueService.putValue(ActualizeHidingSettingsService.LAST_OFFER_ID_KEY, 0L);

        var lastOfferId = storageKeyValueService.getLong(ActualizeHidingSettingsService.LAST_OFFER_ID_KEY, null);
        assertThat(lastOfferId).isZero();

        tolokaHidingSettings.reset();
        assertThatThrownBy(() ->
            service.update()
        )
            .hasCause(new RuntimeException("test"));

        // lastOfferId is persisted after job failure
        lastOfferId = storageKeyValueService.getLong(ActualizeHidingSettingsService.LAST_OFFER_ID_KEY, null);
        assertThat(lastOfferId).isPositive();

        Mockito.doCallRealMethod()
            .when(offerRepositorySpied).getOffersByIds(anyCollection());

        tolokaHidingSettings.reset();
        assertThatNoException().isThrownBy(() ->
            service.update()
        );

        // lastOfferId is set to Zero after job success
        lastOfferId = storageKeyValueService.getLong(ActualizeHidingSettingsService.LAST_OFFER_ID_KEY, null);
        assertThat(lastOfferId).isZero();
    }

    private Offer createApplicableOffer(Supplier supplier) {
        var id = IDS.incrementAndGet();
        return OfferTestUtils.simpleOkOffer(supplier)
            .setBusinessId(supplier.getId())
            .setId(id)
            .setShopSku("ssku-" + id)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION);
    }
}
