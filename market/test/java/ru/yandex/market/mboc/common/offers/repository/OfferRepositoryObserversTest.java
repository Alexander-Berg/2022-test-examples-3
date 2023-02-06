package ru.yandex.market.mboc.common.offers.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mbo.jooq.repo.notification.RepositoryObserver;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author dergachevfv
 * @created 21.07.20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferRepositoryObserversTest extends BaseDbTestClass {

    private static final long CATEGORY_ID = 100L;
    private static final long MODEL_ID = 1111111L;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private TestRepositoryObserver observer;

    @Before
    public void setUp() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        TestRepositoryObserver observerInstance = new TestRepositoryObserver();
        observer = Mockito.spy(observerInstance);
        offerRepository.addObserver(observer);
    }

    @After
    public void tearDown() {
        offerRepository.removeObserver(observer);
    }

    @Test
    public void testInsertOffersObserved() {
        Offer offer1 = OfferTestUtils.nextOffer();
        Offer offer2 = OfferTestUtils.nextOffer();
        Offer offer3 = OfferTestUtils.nextOffer();

        observer.setBeforeHandler(handler(o -> o.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)));

        offerRepository.insertOffers(offer1, offer2, offer3);

        List<Offer> offers = offerRepository.findAll();

        MbocAssertions.assertThat(offers).hasSize(3)
            .allMatch(offer -> Objects.equals(offer.getCategoryId(), CATEGORY_ID));

        Mockito.verify(observer, Mockito.times(1)).onBeforeSave(Mockito.anyList());
        Mockito.verify(observer, Mockito.times(1)).onAfterSave(Mockito.anyList());

        List<Offer> beforeOffers = observer.getLastBefore().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(beforeOffers)
            .hasSize(3)
            .allMatch(offer -> !offer.hasCategoryId());

        List<Offer> afterOffers = observer.getLastAfter().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(afterOffers)
            .hasSize(3)
            .allMatch(offer -> Objects.equals(offer.getCategoryId(), CATEGORY_ID));
    }

    @Test
    public void testUpdateOffersObserved() {
        Offer offer1 = OfferTestUtils.nextOffer();
        Offer offer2 = OfferTestUtils.nextOffer();
        Offer offer3 = OfferTestUtils.nextOffer();

        offerRepository.insertOffers(offer1, offer2, offer3);
        Mockito.clearInvocations(observer);

        List<Offer> updatedOffers = offerRepository.findAll().stream()
            .peek(offer -> offer.setModelId(MODEL_ID))
            .collect(Collectors.toList());

        observer.setBeforeHandler(handler(o -> o.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)));

        offerRepository.updateOffers(updatedOffers);

        List<Offer> offers = offerRepository.findAll();

        MbocAssertions.assertThat(offers).hasSize(3)
            .allMatch(offer -> Objects.equals(offer.getCategoryId(), CATEGORY_ID))
            .allMatch(offer -> Objects.equals(offer.getModelId(), MODEL_ID));

        Mockito.verify(observer, Mockito.times(1)).onBeforeSave(Mockito.anyList());
        Mockito.verify(observer, Mockito.times(1)).onAfterSave(Mockito.anyList());

        List<Offer> beforeOffers = observer.getLastBefore().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(beforeOffers)
            .hasSize(3)
            .allMatch(offer -> !offer.hasCategoryId())
            .allMatch(offer -> Objects.equals(offer.getModelId(), MODEL_ID));

        List<Offer> afterOffers = observer.getLastAfter().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(afterOffers)
            .hasSize(3)
            .allMatch(offer -> Objects.equals(offer.getCategoryId(), CATEGORY_ID))
            .allMatch(offer -> Objects.equals(offer.getModelId(), MODEL_ID));
    }

    @Test
    public void testUpdateUnchangedOffersObserved() {
        Offer offer = OfferTestUtils.nextOffer();

        offerRepository.insertOffers(offer);
        Mockito.clearInvocations(observer);

        observer.setBeforeHandler(handler(o -> o.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)));
        observer.setAfterHandler(handler(o -> o.setModelId(MODEL_ID)));

        offer = offerRepository.findOfferByBusinessSkuKeyWithContent(offer.getBusinessSkuKey());

        // do not change offer, just save it
        offerRepository.updateOffers(offer);

        Offer offerAfter = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());

        MbocAssertions.assertThat(offerAfter)
            .hasCategoryId(CATEGORY_ID) // set by before save handler
            .doesNotHaveModelId(); // after handler changes are not pesisted

        MbocAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_ID) // set by before save handler
            .hasModelId(MODEL_ID); // set by after save handler

        Mockito.verify(observer, Mockito.times(1)).onBeforeSave(Mockito.anyList());
        Mockito.verify(observer, Mockito.times(1)).onAfterSave(Mockito.anyList());

        List<Offer> beforeOffers = observer.getLastBefore().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(beforeOffers)
            .hasSize(1)
            .allMatch(o -> !o.hasCategoryId())
            .allMatch(o -> !o.hasModelId());

        List<Offer> afterOffers = observer.getLastAfter().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(afterOffers)
            .hasSize(1)
            .allMatch(o -> Objects.equals(o.getCategoryId(), CATEGORY_ID))
            .allMatch(o -> !o.hasModelId());
    }

    @Test
    public void testUpdateTrackerTicketAndProcessingStatusObserved() {
        Offer testOffer = OfferTestUtils.nextOffer();

        offerRepository.insertOffers(testOffer);
        Mockito.clearInvocations(observer);

        LocalDateTime now = DateTimeUtils.dateTimeNow().minusSeconds(1);

        observer.setBeforeHandler(handler(o -> o.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)));

        offerRepository.updateTrackerTicket(
            "SOME-1234", Offer.AdditionalTicketType.RECLASSIFICATION,
            1,
            Collections.singletonList(testOffer.getId()));

        Offer updatedOffer = offerRepository.getOfferById(testOffer.getId());

        Assertions.assertThat(updatedOffer.getTrackerTicket()).isEqualTo("SOME-1234");
        Assertions.assertThat(updatedOffer.getProcessingStatusModified()).isAfterOrEqualTo(now);
        Assertions.assertThat(updatedOffer.getAdditionalTickets())
            .contains(Map.entry(Offer.AdditionalTicketType.RECLASSIFICATION, "SOME-1234"));

        Mockito.verify(observer, Mockito.times(1)).onBeforeSave(Mockito.anyList());
        Mockito.verify(observer, Mockito.times(1)).onAfterSave(Mockito.anyList());

        List<Offer> beforeOffers = observer.getLastBefore().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(beforeOffers)
            .hasSize(1)
            .allMatch(offer -> !offer.hasCategoryId());

        List<Offer> afterOffers = observer.getLastAfter().stream()
            .map(EntityChangeEvent::getAfter)
            .collect(Collectors.toList());
        MbocAssertions.assertThat(afterOffers)
            .hasSize(1)
            .allMatch(offer -> Objects.equals(offer.getCategoryId(), CATEGORY_ID));
    }

    private Consumer<List<EntityChangeEvent<Offer>>> handler(Consumer<Offer> offerConsumer) {
        return events -> events.stream()
            .map(EntityChangeEvent::getAfter)
            .forEach(offerConsumer);
    }

    public static class TestRepositoryObserver implements RepositoryObserver<Offer> {

        public static final Consumer<List<EntityChangeEvent<Offer>>> NOOP = __ -> {
        };

        private Consumer<List<EntityChangeEvent<Offer>>> beforeHandler = NOOP;
        private Consumer<List<EntityChangeEvent<Offer>>> afterHandler = NOOP;

        private List<EntityChangeEvent<Offer>> lastBefore;
        private List<EntityChangeEvent<Offer>> lastAfter;

        @Override
        public void onBeforeSave(List<EntityChangeEvent<Offer>> entityChangeEvents) {
            lastBefore = copy(entityChangeEvents);
            beforeHandler.accept(entityChangeEvents);
        }

        @Override
        public void onAfterSave(List<EntityChangeEvent<Offer>> entityChangeEvents) {
            lastAfter = copy(entityChangeEvents);
            afterHandler.accept(entityChangeEvents);
        }

        public void setBeforeHandler(Consumer<List<EntityChangeEvent<Offer>>> beforeHandler) {
            this.beforeHandler = beforeHandler;
        }

        public void setAfterHandler(Consumer<List<EntityChangeEvent<Offer>>> afterHandler) {
            this.afterHandler = afterHandler;
        }

        public List<EntityChangeEvent<Offer>> getLastBefore() {
            return lastBefore;
        }

        public List<EntityChangeEvent<Offer>> getLastAfter() {
            return lastAfter;
        }

        private List<EntityChangeEvent<Offer>> copy(List<EntityChangeEvent<Offer>> events) {
            return events.stream()
                .map(this::copy)
                .collect(Collectors.toList());
        }

        private EntityChangeEvent<Offer> copy(EntityChangeEvent<Offer> event) {
            return new EntityChangeEvent<>(
                event.getBefore() != null ? event.getBefore().copy() : null,
                event.getAfter() != null ? event.getAfter().copy() : null
            );
        }
    }
}
