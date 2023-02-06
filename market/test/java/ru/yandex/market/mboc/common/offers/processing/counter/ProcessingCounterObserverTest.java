package ru.yandex.market.mboc.common.offers.processing.counter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.CONTENT_PROCESSING;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_CLASSIFICATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_MODERATION;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_MODERATION_REJECTED;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.IN_PROCESS;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.NEED_CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.nextOffer;

public class ProcessingCounterObserverTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    @Before
    public void setUp() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testProcessingCounterIncrement() {
        testProcessingCounterIncrementCase(NEED_CONTENT, CONTENT_PROCESSING, null, null, null);

        testProcessingCounterIncrementCase(CONTENT_PROCESSING, IN_MODERATION, null, null,1);
        testProcessingCounterIncrementCase(CONTENT_PROCESSING, IN_CLASSIFICATION, null, null, 1);
        testProcessingCounterIncrementCase(CONTENT_PROCESSING, IN_PROCESS, null, null, 1);

        testProcessingCounterIncrementCase(IN_MODERATION, IN_PROCESS, null, 1, 2);
        testProcessingCounterIncrementCase(IN_MODERATION, IN_MODERATION_REJECTED, null, 1, 1);
    }

    private void testProcessingCounterIncrementCase(Offer.ProcessingStatus before, Offer.ProcessingStatus after,
                                                    Integer initial, Integer beforeCnt, Integer afterCnt) {
        var offer = nextOffer()
            .setProcessingStatusInternal(before);
        offer.setProcessingCounter(initial);

        offer = offerRepository.insertAndGetOffer(offer);
        assertThat(offer.getProcessingCounter()).isEqualTo(beforeCnt);

        offer.updateProcessingStatusIfValid(after);

        offerRepository.updateOffer(offer);

        assertThat(offer.getProcessingCounter()).isEqualTo(afterCnt);
    }
}
