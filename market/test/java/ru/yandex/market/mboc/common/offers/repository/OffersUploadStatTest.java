package ru.yandex.market.mboc.common.offers.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.upload.OfferUploadQueueItem;
import ru.yandex.market.mboc.common.offers.model.upload.OffersUploadStat;
import ru.yandex.market.mboc.common.offers.repository.upload.ErpOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.offers.repository.upload.MdmOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.offers.repository.upload.YtOfferUploadQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;


public class OffersUploadStatTest extends BaseDbTestClass {
    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private YtOfferUploadQueueRepository ytOfferUploadQueueRepository;
    @Autowired
    private ErpOfferUploadQueueRepository erpOfferUploadQueueRepository;
    @Autowired
    private MdmOfferUploadQueueRepository mdmOfferUploadQueueRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    public void testOfferUploadingStat() {
        var testSupplier = supplierRepository.insertOrUpdate(OfferTestUtils.simpleSupplier());
        var from = LocalDateTime.now();
        //in 30 minutes
        for (int i = 0; i <= 10; i++) {
            saveOffer(testSupplier, from.minusMinutes(10 - i));
        }

        var stat = ytOfferUploadQueueRepository.calcOfferUploadingStat();
        assertStat(stat);
        stat = erpOfferUploadQueueRepository.calcOfferUploadingStat();
        assertStat(stat);
        stat = mdmOfferUploadQueueRepository.calcOfferUploadingStat();
        assertStat(stat);
    }

    private void assertStat(OffersUploadStat stat) {
        assertEquals(11, stat.getCount());
        assertEquals(600, stat.getMaxWaitTime().getSeconds());
        //calc postgres percentile
        assertEquals(Duration.parse("PT5M"), stat.getPercentile50WaitTime());
        assertEquals(Duration.parse("PT9M"), stat.getPercentile90WaitTime());
        assertEquals(Duration.parse("PT9M30S"), stat.getPercentile95WaitTime());
    }

    private void saveOffer(Supplier supplier, LocalDateTime requestedAt) {
        Offer offer = OfferTestUtils.nextOffer(supplier);
        offer.setMappingDestination(Offer.MappingDestination.WHITE);
        offerRepository.insertOffer(offer);

        // Clear automatic queue
        ytOfferUploadQueueRepository.delete(List.of(offer.getId()));
        erpOfferUploadQueueRepository.delete(List.of(offer.getId()));
        mdmOfferUploadQueueRepository.delete(List.of(offer.getId()));

        ytOfferUploadQueueRepository.insert(new OfferUploadQueueItem(offer.getId(), requestedAt));
        erpOfferUploadQueueRepository.insert(new OfferUploadQueueItem(offer.getId(), requestedAt));
        mdmOfferUploadQueueRepository.insert(new OfferUploadQueueItem(offer.getId(), requestedAt));
    }
}
