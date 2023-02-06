package ru.yandex.market.mboc.common.offers.repository.upload;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException;
import ru.yandex.market.mboc.common.availability.mbo_audit.OfferUploadQueueAuditRecorder;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.upload.OfferUploadQueueItem;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.mboc.common.offers.repository.upload.YtOfferUploadQueueRepositoryImpl.TABLE_NAME;

public class AbstractOfferUploadQueueRepositoryTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;

    private OfferUploadQueueRepository repository;

    @Before
    public void setUp() {
        // We can use any queue table, for example YT. To do that we need to clear automatically enqueued item
        var audit = new OfferUploadQueueAuditRecorder(new MboAuditServiceMock(), TABLE_NAME) {
        };
        audit.setAuditEnabled(true);
        repository = new AbstractOfferUploadQueueRepository(
            namedParameterJdbcTemplate, transactionTemplate, audit, TABLE_NAME) {
        };
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void requiresOfferToExist() {
        repository.insert(new OfferUploadQueueItem(373737373L, now()));
    }

    @Test
    public void findsForUpload() {
        // Should find items limited by limit and ordered by last_processed first and enqueued_ts second
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
        repository.delete(List.of(offer1.getId(), offer2.getId(), offer3.getId(), offer4.getId(), offer5.getId()));

        assertThat(repository.findForUpload(1000)).isEmpty();

        var item1 = new OfferUploadQueueItem(offer1.getId(), now().minusMinutes(5));
        var item2 = new OfferUploadQueueItem(offer2.getId(), now().minusMinutes(4));
        var item3 = new OfferUploadQueueItem(offer3.getId(), now().minusMinutes(3));
        var item4 = new OfferUploadQueueItem(offer4.getId(), now().minusMinutes(2));
        var item5 = new OfferUploadQueueItem(offer5.getId(), now().minusMinutes(1));
        repository.insertBatch(List.of(item1, item2, item3, item4, item5));

        assertThat(repository.findForUpload(4)).containsExactly(item1, item2, item3, item4);

        item5.setLastProcessed(now().minusMinutes(5));
        item4.setLastProcessed(now().minusMinutes(4));
        item3.setLastProcessed(now().minusMinutes(3));
        repository.updateBatch(item5, item4, item3);

        assertThat(repository.findForUpload(5)).containsExactly(item5, item4, item3, item1, item2);
    }

    @Test
    public void findsInPeriod() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var offer1 = OfferTestUtils.nextOffer(supplier);
        var offer2 = OfferTestUtils.nextOffer(supplier);
        var offer3 = OfferTestUtils.nextOffer(supplier);
        offerRepository.insertOffers(List.of(offer1, offer2, offer3));
        // Clear auto
        repository.delete(List.of(offer1.getId(), offer2.getId(), offer3.getId()));

        assertThat(repository.findEnqueuedInPeriod(now().minusMinutes(30), now())).isEmpty();

        var items = List.of(
            new OfferUploadQueueItem(offer1.getId(), now().minusMinutes(20)),
            new OfferUploadQueueItem(offer2.getId(), now().minusMinutes(10)),
            new OfferUploadQueueItem(offer3.getId(), now())
        );
        repository.insertBatch(items);

        // Test after
        assertThat(repository.findEnqueuedInPeriod(now().minusMinutes(15), null))
            .containsExactlyElementsOf(items.subList(1, 3));
        // Test before
        assertThat(repository.findEnqueuedInPeriod(null, now().minusMinutes(5)))
            .containsExactlyElementsOf(items.subList(0, 2));
        // Test period
        assertThat(repository.findEnqueuedInPeriod(now().minusMinutes(15), now().minusMinutes(5)))
            .containsExactly(items.get(1));
    }

    @Test
    public void enqueuesByIds() {
        var supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
        var alreadyEnqueuedOffer = OfferTestUtils.nextOffer(supplier)
            .setCategoryIdForTests(123L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(new Offer.Mapping(123, now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        var notEnqueuedOffer = OfferTestUtils.nextOffer(supplier)
            .setCategoryIdForTests(456L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(new Offer.Mapping(456, now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        offerRepository.insertOffers(List.of(alreadyEnqueuedOffer, notEnqueuedOffer));
        // Clear auto
        repository.delete(List.of(notEnqueuedOffer.getId()));

        var alreadyEnqueuedItem = repository.findById(alreadyEnqueuedOffer.getId());
        assertThat(alreadyEnqueuedItem).isNotNull();
        assertThatThrownBy(() -> repository.findById(notEnqueuedOffer.getId()))
            .isInstanceOf(ItemNotFoundException.class);

        var enqueuedAt = now().minusMinutes(15);
        repository.enqueueByIds(List.of(notEnqueuedOffer.getId()), enqueuedAt);

        var alreadyEnqueuedItemFromDb = repository.findById(alreadyEnqueuedOffer.getId());
        assertThat(alreadyEnqueuedItemFromDb).isNotNull().isEqualTo(alreadyEnqueuedItem);

        var enqueuedItemFromDb = repository.findById(notEnqueuedOffer.getId());
        assertThat(enqueuedItemFromDb).isNotNull()
            .extracting(OfferUploadQueueItem::getEnqueuedTs).isEqualTo(enqueuedAt);
    }
}
