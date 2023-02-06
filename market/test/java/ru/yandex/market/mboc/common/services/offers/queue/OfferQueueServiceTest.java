package ru.yandex.market.mboc.common.services.offers.queue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.queue.OfferQueueItem;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.ForModerationOfferQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OfferQueueServiceTest extends BaseDbTestClass {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final long OFFER_ID = 1L;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private ForModerationOfferQueueRepository offerQueueRepository;

    private OfferQueueService offerQueueService;

    @Before
    public void setUp() {
        supplierRepository.insertBatch(OfferTestUtils.simpleSupplier());
        offerRepository.insertOffers(OfferTestUtils.simpleOffer().setId(OFFER_ID));
        // delete possible auto-inserted entities
        offerQueueRepository.deleteAll();

        offerQueueService = new OfferQueueService(
            offerQueueRepository,
            DEFAULT_BATCH_SIZE, DEFAULT_BATCH_SIZE
        );
    }

    @Test
    public void whenProcessingFailedThenQueueItemIsUpdated() {
        offerQueueRepository.enqueueByIds(List.of(OFFER_ID), now().minusMinutes(10));

        OfferQueueItem queueItemBefore = offerQueueRepository.findById(OFFER_ID);
        assertThat(queueItemBefore).isNotNull();

        assertThatThrownBy(() ->
            offerQueueService.handleQueueBatch(offerIds -> {
                throw new RuntimeException("test");
            })
        ).hasMessageContaining("test");

        OfferQueueItem queueItemAfter = offerQueueRepository.findById(OFFER_ID);
        assertThat(queueItemAfter)
            .matches(i -> i.getLastProcessedTs().isAfter(queueItemBefore.getEnqueuedTs()),
                "updated lastProcessedTs")
            .matches(i -> i.getLastError() != null && i.getLastError().contains("test"),
                "updated lastError")
            .matches(i -> i.getAttempt() > 0, "updated attempt");
    }

    @Test
    public void whenProcessingSucceedsThenQueueItemIsDequeued() {
        offerQueueRepository.enqueueByIds(List.of(OFFER_ID), now().minusMinutes(10));

        OfferQueueItem queueItemBefore = offerQueueRepository.findById(OFFER_ID);
        assertThat(queueItemBefore).isNotNull();

        offerQueueService.handleQueueBatch(offerIds -> {
        });

        assertThatThrownBy(() -> offerQueueRepository.findById(OFFER_ID))
            .isInstanceOf(ItemNotFoundException.class);
    }
}
