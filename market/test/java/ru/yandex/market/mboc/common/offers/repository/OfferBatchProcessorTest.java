package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class OfferBatchProcessorTest extends BaseDbTestClass {
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setup() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        offerRepository.insertOffers(IntStream.range(0, 42)
            .mapToObj(i -> OfferTestUtils.nextOffer())
            .collect(Collectors.toList()));
    }

    /**
     * Вообще говоря, из-за SavepointTransactionHelper этот тест по сути не падает там, где должен бы
     * (при отравленной транзакции).
     * Но проверяет некую общую логику с ретраями.
     */
    @Test
    public void testRollbacksInMain() {
        var i = new AtomicInteger(0);
        offerBatchProcessor.processBatchesOnSlave(new OffersFilter(), 30, offers -> {
            if (i.getAndIncrement() < 3) {
                throw new SqlConcurrentModificationException("Yay!", List.of());
            }
        });

        // Expect it works
    }

    /**
     * Вообще говоря, из-за SavepointTransactionHelper этот тест по сути не падает там, где должен бы
     * (при отравленной транзакции).
     * Но проверяет некую общую логику с ретраями.
     */
    @Test
    public void testRollbacksInTail() {
        var i = new AtomicInteger(0);
        offerBatchProcessor.processBatchesOnSlave(new OffersFilter(), 30, offers -> {
            if (offers.size() < 30 && i.getAndIncrement() < 3) {
                throw new SqlConcurrentModificationException("Yay tail!", List.of());
            }
        });

        // Expect it works
    }
}
