package ru.yandex.market.mboc.common.offers.repository.queue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.queue.OfferQueueItem;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.mboc.common.offers.repository.queue.ForModerationOfferQueueRepository.TABLE_NAME;

public class AbstractOfferQueueRepositoryTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    private OfferQueueRepository queueRepository;

    @Before
    public void setUp() {
        queueRepository = new AbstractOfferQueueRepository(
            namedParameterJdbcTemplate, transactionTemplate, TABLE_NAME) {
        };
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void requiresOfferToExist() {
        queueRepository.insert(OfferQueueItem.builder().offerId(373737373L).build());
    }

    @Test
    public void finds() {
        // Should find items limited by limit and ordered by last_processed_ts first and enqueued_ts second
        // (Ordered so failed to upload offers won't stop upload from working altogether)

        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer1 = OfferTestUtils.nextOffer(supplier);
        var offer2 = OfferTestUtils.nextOffer(supplier);
        var offer3 = OfferTestUtils.nextOffer(supplier);
        var offer4 = OfferTestUtils.nextOffer(supplier);
        var offer5 = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(List.of(offer1, offer2, offer3, offer4, offer5));
        // Clear auto
        queueRepository.delete(List.of(offer1.getId(), offer2.getId(), offer3.getId(), offer4.getId(), offer5.getId()));

        assertThat(queueRepository.find(1000)).isEmpty();

        var item1 = OfferQueueItem.builder().offerId(offer1.getId()).enqueuedTs(now().minusMinutes(5)).build();
        var item2 = OfferQueueItem.builder().offerId(offer2.getId()).enqueuedTs(now().minusMinutes(4)).build();
        var item3 = OfferQueueItem.builder().offerId(offer3.getId()).enqueuedTs(now().minusMinutes(3)).build();
        var item4 = OfferQueueItem.builder().offerId(offer4.getId()).enqueuedTs(now().minusMinutes(2)).build();
        var item5 = OfferQueueItem.builder().offerId(offer5.getId()).enqueuedTs(now().minusMinutes(1)).build();
        queueRepository.insertBatch(List.of(item1, item2, item3, item4, item5));

        assertThat(queueRepository.find(4)).containsExactly(item1, item2, item3, item4);

        OfferQueueItem item5Processed = item5.toBuilder().lastProcessedTs(now().minusMinutes(5)).build();
        OfferQueueItem item4Processed = item4.toBuilder().lastProcessedTs(now().minusMinutes(4)).build();
        OfferQueueItem item3Processed = item3.toBuilder().lastProcessedTs(now().minusMinutes(3)).build();
        queueRepository.updateBatch(item5Processed, item4Processed, item3Processed);

        assertThat(queueRepository.find(5))
            .containsExactly(item5Processed, item4Processed, item3Processed, item1, item2);
    }

    @Test
    public void findsInPeriod() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer1 = OfferTestUtils.nextOffer(supplier);
        var offer2 = OfferTestUtils.nextOffer(supplier);
        var offer3 = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(List.of(offer1, offer2, offer3));

        queueRepository.delete(List.of(offer1.getId(), offer2.getId(), offer3.getId()));

        assertThat(queueRepository.findEnqueuedBetween(now().minusMinutes(30), now())).isEmpty();

        var items = List.of(
            OfferQueueItem.builder().offerId(offer1.getId()).enqueuedTs(now().minusMinutes(20)).build(),
            OfferQueueItem.builder().offerId(offer2.getId()).enqueuedTs(now().minusMinutes(10)).build(),
            OfferQueueItem.builder().offerId(offer3.getId()).enqueuedTs(now()).build()
        );
        queueRepository.insertBatch(items);

        // Test after
        assertThat(queueRepository.findEnqueuedBetween(now().minusMinutes(15), null))
            .containsExactlyElementsOf(items.subList(1, 3));
        // Test before
        assertThat(queueRepository.findEnqueuedBetween(null, now().minusMinutes(5)))
            .containsExactlyElementsOf(items.subList(0, 2));
        // Test period
        assertThat(queueRepository.findEnqueuedBetween(now().minusMinutes(15), now().minusMinutes(5)))
            .containsExactly(items.get(1));
    }

    @Test
    public void enqueuesByIds() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var alreadyEnqueuedOffer = OfferTestUtils.nextOffer(supplier);
        var notEnqueuedOffer = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(alreadyEnqueuedOffer, notEnqueuedOffer);

        queueRepository.enqueueByIds(List.of(alreadyEnqueuedOffer.getId()), LocalDateTime.now());
        queueRepository.delete(List.of(notEnqueuedOffer.getId()));

        var alreadyEnqueuedItem = queueRepository.findById(alreadyEnqueuedOffer.getId());
        assertThat(alreadyEnqueuedItem).isNotNull();
        assertThatThrownBy(() -> queueRepository.findById(notEnqueuedOffer.getId()))
            .isInstanceOf(ItemNotFoundException.class);

        var enqueuedAt = now().minusMinutes(15);
        queueRepository.enqueueByIds(List.of(notEnqueuedOffer.getId()), enqueuedAt);

        var alreadyEnqueuedItemFromDb = queueRepository.findById(alreadyEnqueuedOffer.getId());
        assertThat(alreadyEnqueuedItemFromDb).isNotNull().isEqualTo(alreadyEnqueuedItem);

        var enqueuedItemFromDb = queueRepository.findById(notEnqueuedOffer.getId());
        assertThat(enqueuedItemFromDb).isNotNull()
            .extracting(OfferQueueItem::getEnqueuedTs).isEqualTo(enqueuedAt);
    }

    @Test
    public void reEnqueuesAlreadyEnqueued() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(offer);

        queueRepository.enqueueByIds(List.of(offer.getId()), now());
        OfferQueueItem enqueuedFirst = queueRepository.findById(offer.getId());
        assertThat(enqueuedFirst).isNotNull();

        queueRepository.enqueueByIds(List.of(offer.getId()), now().plusMinutes(10));
        OfferQueueItem enqueuedSecond = queueRepository.findById(offer.getId());
        assertThat(enqueuedSecond).isNotNull();

        assertThat(enqueuedSecond.getEnqueuedTs())
            .isAfter(enqueuedFirst.getEnqueuedTs());
    }

    @Test
    public void dequeues() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer1 = OfferTestUtils.nextOffer(supplier);
        var offer2 = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(offer1, offer2);

        queueRepository.enqueueByIds(List.of(offer1.getId(), offer2.getId()), now());
        OfferQueueItem enqueued1 = queueRepository.findById(offer1.getId());
        assertThat(enqueued1).isNotNull();
        OfferQueueItem enqueued2 = queueRepository.findById(offer1.getId());
        assertThat(enqueued2).isNotNull();

        queueRepository.enqueueByIds(List.of(offer2.getId()), now().plusMinutes(10));
        OfferQueueItem enqueued2New = queueRepository.findById(offer2.getId());
        assertThat(enqueued2New).isNotNull();
        assertThat(enqueued2New.getEnqueuedTs())
            .isAfter(enqueued2.getEnqueuedTs());

        queueRepository.dequeue(List.of(enqueued1, enqueued2));

        // offer1 is removed from queue
        assertThatThrownBy(() -> queueRepository.findById(offer1.getId()))
            .isInstanceOf(ItemNotFoundException.class);

        // offer2 is still queued as it has been re-enqueued
        OfferQueueItem enqueued2NewFresh = queueRepository.findById(offer2.getId());
        assertThat(enqueued2NewFresh).isNotNull()
            .isEqualTo(enqueued2New);
    }
}
