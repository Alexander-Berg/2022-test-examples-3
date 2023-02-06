package ru.yandex.market.mboc.common.services.offers.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PskuHasContentMappingsServiceTest extends BaseDbTestClass {
    @Autowired
    SupplierRepository supplierRepository;

    @Autowired
    OfferRepository repository;
    PskuHasContentMappingsService service;

    @Before
    public void setUp() {
        service = new PskuHasContentMappingsService(repository);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        repository.insertOffers(
            OfferTestUtils.simpleOffer(1L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(111L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer(2L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(111L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF),
            OfferTestUtils.simpleOffer(3L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(111L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF),
            OfferTestUtils.simpleOffer(4L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(111L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(5L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(111L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(6L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(211L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF),
            OfferTestUtils.simpleOffer(7L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(211L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(8L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(211L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(9L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(311L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(10L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(311L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER),
            OfferTestUtils.simpleOffer(11L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(411L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
                .setPskuHasContentMappings(true),
            OfferTestUtils.simpleOffer(12L)
                .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
                .setApprovedSkuMappingInternal(OfferTestUtils.mapping(511L))
                .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER)
                .setPskuHasContentMappings(true)
            );
    }

    @Test
    public void testUpdateHasPartnerMappingsByOfferIds() {
        service.setPskuHasContentMappings(Arrays.asList(111L, 311L, 411L, 511L));
        var offers = repository.findAll()
            .stream()
            .collect(Collectors.toMap(Offer::getId, Function.identity()));
        assertFalse("Ignore offers with CONTENT confidence", offers.get(1L).isPskuHasContentMappings());
        assertTrue("Update offers with PARTNER_SELF confidence", offers.get(2L).isPskuHasContentMappings());
        assertTrue("Update offers with PARTNER_SELF confidence", offers.get(3L).isPskuHasContentMappings());
        assertFalse("Count offers with PARTNER confidence but not update them",
            offers.get(4L).isPskuHasContentMappings());
        assertFalse("Count offers with PARTNER confidence but not update them",
            offers.get(5L).isPskuHasContentMappings());
        assertFalse("Shouldn't be updated because of filter", offers.get(6L).isPskuHasContentMappings());
        assertFalse("Shouldn't be updated because of filter", offers.get(7L).isPskuHasContentMappings());
        assertFalse("Shouldn't be updated because of filter", offers.get(8L).isPskuHasContentMappings());
        assertFalse("Shouldn't be updated because of confidence", offers.get(9L).isPskuHasContentMappings());
        assertFalse("Shouldn't be updated because of confidence", offers.get(10L).isPskuHasContentMappings());
        assertTrue("Shouldn't be updated to false (ignored)", offers.get(11L).isPskuHasContentMappings());
        assertTrue("Shouldn't be updated because of confidence", offers.get(12L).isPskuHasContentMappings());
        service.setPskuHasContentMappings(Collections.singletonList(211L));
        assertTrue(repository.findOffers(
            OfferCriterias.searchByIds(Collections.singleton(6L))).get(0).isPskuHasContentMappings());
    }
}
