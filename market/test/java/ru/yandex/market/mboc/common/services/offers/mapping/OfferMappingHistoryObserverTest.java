package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferMappingHistory;
import ru.yandex.market.mboc.common.offers.repository.OfferMappingHistoryRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;


public class OfferMappingHistoryObserverTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private OfferMappingHistoryRepository offerMappingHistoryRepository;

    @Before
    public void setup() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        categoryRepository.insert(OfferTestUtils.defaultCategory());
    }

    @Test
    public void singleMappingTest() {
        var noApprovedOffer = OfferTestUtils.nextOffer().setCategoryIdInternal(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        offerRepository.insertOffer(noApprovedOffer);
        var noApprovedEvents = offerMappingHistoryRepository.findByOfferId(noApprovedOffer.getId());
        Assertions.assertThat(noApprovedEvents).isEmpty();

        var toUpdate = offerRepository.getOfferById(noApprovedOffer.getId());
        toUpdate.setApprovedSkuMappingInternal(
            new Offer.Mapping(101L, LocalDateTime.now(), Offer.SkuType.MARKET)
        );
        toUpdate.setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER);
        offerRepository.updateOffers(toUpdate);

        var updatedEvents = offerMappingHistoryRepository.findByOfferId(toUpdate.getId());
        Assertions.assertThat(updatedEvents).hasSize(1);
        var updated = updatedEvents.get(0);
        validateFirst(updated);
    }

    @Test
    public void updatedMappingTest() {
        var initApprovedOffer = OfferTestUtils.nextOffer().setCategoryIdInternal(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        initApprovedOffer.setApprovedSkuMappingInternal(
            new Offer.Mapping(101L, LocalDateTime.now(), Offer.SkuType.MARKET)
        );
        initApprovedOffer.setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER);
        offerRepository.insertOffer(initApprovedOffer);
        var initApprovedEvents = offerMappingHistoryRepository.findByOfferId(initApprovedOffer.getId());
        Assertions.assertThat(initApprovedEvents).hasSize(1);
        var initial = initApprovedEvents.get(0);
        validateFirst(initial);
        var toUpdate = offerRepository.getOfferById(initApprovedOffer.getId());
        toUpdate.setApprovedSkuMappingInternal(new Offer.Mapping(202L, LocalDateTime.now(),
            Offer.SkuType.PARTNER20));
        toUpdate.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offerRepository.updateOffers(toUpdate);

        var updatedTwiceEvents = offerMappingHistoryRepository.findByOfferId(toUpdate.getId());
        Assertions.assertThat(updatedTwiceEvents).hasSize(2);
        var lastUpdate = updatedTwiceEvents.stream()
            .filter(event -> event.getConfidenceAfter() != null)
            .filter(event -> event.getConfidenceBefore() != null)
            .findFirst().get();
        validateLast(lastUpdate);
    }

    private void validateFirst(OfferMappingHistory event) {
        Assertions.assertThat(event.getSkuIdAfter()).isEqualTo(101L);
        Assertions.assertThat(event.getSkuTypeAfter()).isEqualTo(Offer.SkuType.MARKET);
        Assertions.assertThat(event.getConfidenceAfter()).isEqualTo(Offer.MappingConfidence.PARTNER);
        Assertions.assertThat(event.getSkuIdBefore()).isNull();
        Assertions.assertThat(event.getSkuTypeBefore()).isNull();
        Assertions.assertThat(event.getConfidenceBefore()).isNull();
    }

    private void validateLast(OfferMappingHistory event) {
        Assertions.assertThat(event.getSkuIdAfter()).isEqualTo(202L);
        Assertions.assertThat(event.getSkuTypeAfter()).isEqualTo(Offer.SkuType.PARTNER20);
        Assertions.assertThat(event.getConfidenceAfter()).isEqualTo(Offer.MappingConfidence.CONTENT);
        Assertions.assertThat(event.getSkuIdBefore()).isEqualTo(101L);
        Assertions.assertThat(event.getSkuTypeBefore()).isEqualTo(Offer.SkuType.MARKET);
        Assertions.assertThat(event.getConfidenceBefore()).isEqualTo(Offer.MappingConfidence.PARTNER);
    }
}
