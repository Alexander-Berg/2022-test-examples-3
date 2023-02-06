package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.CheckCategorySyncQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;

public class CheckCategorySyncObserverTest extends BaseDbTestClass {
    @Autowired
    private CheckCategorySyncQueueRepository checkCategorySyncQueueRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void processEventsOk() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        var offer = OfferTestUtils.simpleOffer(supplier);
        offerRepository.insertOffer(offer);

        assertEquals(checkCategorySyncQueueRepository.findAll().size(), 0);

        offer = offerRepository.getOfferById(offer.getId());
        offer.setCategoryIdInternal(1L);
        offer.setMappedCategoryId(1L);
        offer.setApprovedSkuMappingInternal(new Offer.Mapping(1L, LocalDateTime.now()));
        offer.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offer.setMappedModelId(1L);
        offerRepository.updateOffer(offer);

        var queue = checkCategorySyncQueueRepository.findAll();
        assertEquals(queue.size(), 1);
        var offerQueueItem = queue.get(0);
        assertEquals(offerQueueItem.getOfferId(), offer.getId());
    }
}
