package ru.yandex.market.mboc.common.services.offers.queue;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.ModelTestUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService.DELETED_MODEL;

public class ForModerationOfferChangeObserverTest {

    public static final long OFFER_ID = 1L;
    public static final long SKU_ID_1 = 10000001L;
    public static final long SKU_ID_2 = 10000002L;

    private static final Offer.Mapping NO_MAPPING = Offer.Mapping.fromSku(DELETED_MODEL);
    private static final Offer.Mapping SKU_MAPPING_1 =
        Offer.Mapping.fromSku(ModelTestUtils.publishedSku().setId(SKU_ID_1));
    private static final Offer.Mapping SKU_MAPPING_2 =
        Offer.Mapping.fromSku(ModelTestUtils.publishedSku().setId(SKU_ID_2));

    private OfferQueueService queueServiceMock;
    private ForModerationOfferChangeObserver observer;

    @Before
    public void setUp() {
        queueServiceMock = Mockito.mock(OfferQueueService.class);
        observer = new ForModerationOfferChangeObserver(queueServiceMock);
    }

    @Test
    public void whenNewOfferWithoutMappingThenIsNotEnqueued() {
        Offer offer = createOffer()
            .setSuggestSkuMapping(NO_MAPPING)
            .setSupplierSkuMapping(NO_MAPPING);

        observe(new EntityChangeEvent<>(null, offer));

        Mockito.verify(queueServiceMock, Mockito.never())
            .enqueueByIds(anyCollection());
    }

    @Test
    public void whenNewOfferWithSuggestMappingThenIsEnqueued() {
        Offer offer = createOffer()
            .setSuggestSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMapping(NO_MAPPING);

        observe(new EntityChangeEvent<>(null, offer));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(queueServiceMock, Mockito.times(1))
            .enqueueByIds(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .containsExactly(OFFER_ID);
    }

    @Test
    public void whenNewOfferWithSupplierMappingIsEnqueued() {
        Offer offer = createOffer()
            .setSuggestSkuMapping(NO_MAPPING)
            .setSupplierSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);

        observe(new EntityChangeEvent<>(null, offer));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(queueServiceMock, Mockito.times(1))
            .enqueueByIds(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .containsExactly(OFFER_ID);
    }

    @Test
    public void whenSuggestMappingChangedNoSupplierMappingThenIsEnqueued() {
        Offer offerBefore = createOffer()
            .setSuggestSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMapping(NO_MAPPING);
        Offer offerAfter = offerBefore.copy()
            .setSuggestSkuMapping(SKU_MAPPING_2);

        observe(new EntityChangeEvent<>(offerBefore, offerAfter));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(queueServiceMock, Mockito.times(1))
            .enqueueByIds(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .containsExactly(OFFER_ID);
    }

    @Test
    public void whenSupplierMappingChangedThenIsEnqueued() {
        Offer offerBefore = createOffer()
            .setSuggestSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);
        Offer offerAfter = offerBefore.copy()
            .setSupplierSkuMapping(SKU_MAPPING_2);

        observe(new EntityChangeEvent<>(offerBefore, offerAfter));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(queueServiceMock, Mockito.times(1))
            .enqueueByIds(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .containsExactly(OFFER_ID);
    }

    @Test
    public void whenSuggestMappingChangedSupplierMappingNotChangedThenIsNotEnqueued() {
        Offer offerBefore = createOffer()
            .setSuggestSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMapping(SKU_MAPPING_1)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW);
        Offer offerAfter = offerBefore.copy()
            .setSuggestSkuMapping(SKU_MAPPING_2);

        observe(new EntityChangeEvent<>(offerBefore, offerAfter));

        Mockito.verify(queueServiceMock, Mockito.never())
            .enqueueByIds(anyCollection());
    }

    @SafeVarargs
    private void observe(EntityChangeEvent<Offer>... events) {
        observer.onBeforeSave(List.of(events));
        observer.onAfterSave(List.of(events));
    }

    private Offer createOffer() {
        return OfferTestUtils.nextOffer()
            .setId(OFFER_ID)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN);
    }
}
