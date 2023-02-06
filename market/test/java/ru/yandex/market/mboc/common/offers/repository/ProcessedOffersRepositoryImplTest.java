package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ProcessedOfferMarker;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author moskovkin@yandex-team.ru
 * @since 20.02.19
 */
public class ProcessedOffersRepositoryImplTest extends BaseDbTestClass {
    private static final String PROCESSOR_NAME = "testProcessor";
    private Offer offer;
    private Supplier supplier;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ProcessedOffersRepository processedOffersRepository;

    @Before
    public void init() {
        supplier = new Supplier(1, "s1");
        supplierRepository.insert(supplier);

        offer = OfferTestUtils.nextOffer().setBusinessId(supplier.getId());
        offerRepository.insertOffer(offer);
    }

    @Test
    public void whenInsertingShouldWriteCorrectValues() {
        ProcessedOfferMarker newValue = new ProcessedOfferMarker(
            PROCESSOR_NAME, offer.getId(), DateTimeUtils.dateTimeNow());

        processedOffersRepository.insertOrUpdate(newValue);
        List<ProcessedOfferMarker> storedValues = processedOffersRepository.findAll();

        Assertions.assertThat(storedValues).containsExactlyInAnyOrder(newValue);
    }

    @Test
    public void whenUpdatingShouldWriteCorrectValues() {
        ProcessedOfferMarker newValue = new ProcessedOfferMarker(
            PROCESSOR_NAME, offer.getId(), DateTimeUtils.dateTimeNow());
        processedOffersRepository.insertOrUpdate(newValue);

        newValue.setTimestamp(DateTimeUtils.dateTimeNow().plusDays(1));
        processedOffersRepository.insertOrUpdate(newValue);

        List<ProcessedOfferMarker> storedValues = processedOffersRepository.findAll();
        Assertions.assertThat(storedValues).containsExactlyInAnyOrder(newValue);
    }
}
