package ru.yandex.market.mboc.common.offers.processing.counter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_MODERATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_PROCESS;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

public class ProcessingCounterChangeObserverTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private ProcessingCounterLogRepositoryImpl processingCounterLogRepository;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.businessSupplier());
    }

    @Test
    public void testLogsCounterChange() {
        assertThat(processingCounterLogRepository.findAll()).isEmpty();
        offerRepository.insertOffers(
            offer(0, BIZ_ID_SUPPLIER, "offer1", IN_MODERATION, 11L, 1, null)
        );
        assertThat(processingCounterLogRepository.findAll()).isEmpty();

        var offer1 = offerRepository.findAll().get(0);
        offer1.updateProcessingStatusIfValid(IN_PROCESS);
        offerRepository.updateOffer(offer1);
        assertThat(processingCounterLogRepository.findAll()).hasSize(1);
    }

    private static Offer offer(long id, int businessId, String shopSku, Offer.ProcessingStatus status,
                               Long categoryId, Integer groupId, String barCode) {
        return Offer.builder()
            .id(id)
            .isDataCampOffer(true)
            .businessId(businessId)
            .shopSku(shopSku)
            .title(shopSku + " title")
            .shopCategoryName(categoryId + " cat name")
            .processingStatus(status)
            .categoryId(categoryId)
            .dataCampContentVersion(0L)
            .groupId(groupId)
            .barCode(barCode)
            .acceptanceStatus(Offer.AcceptanceStatus.OK)
            .offerContent(OfferContent.builder().build())
            .build();
    }
}
