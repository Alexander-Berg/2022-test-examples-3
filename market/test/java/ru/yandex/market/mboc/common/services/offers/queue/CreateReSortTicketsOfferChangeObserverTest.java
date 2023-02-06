package ru.yandex.market.mboc.common.services.offers.queue;

import java.time.LocalDateTime;
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
import static ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService.DELETED_MODEL;

public class CreateReSortTicketsOfferChangeObserverTest {

    public static final long OFFER_ID = 1L;
    public static final long MAPPING_ID = 1L;
    public static final long CATEGORY_ID = 1L;
    public static final long SKU_ID_1 = 10000001L;
    public static final long SKU_ID_2 = 10000002L;

    private static final Offer.Mapping NO_MAPPING = Offer.Mapping.fromSku(DELETED_MODEL);
    private static final Offer.Mapping SKU_MAPPING_1 =
        Offer.Mapping.fromSku(ModelTestUtils.publishedSku().setId(SKU_ID_1));
    private static final Offer.Mapping SKU_MAPPING_2 =
        Offer.Mapping.fromSku(ModelTestUtils.publishedSku().setId(SKU_ID_2));

    private OfferQueueService queueServiceMock;
    private CreateReSortTicketsOfferChangeObserver observer;

    @Before
    public void setUp() {
        queueServiceMock = Mockito.mock(OfferQueueService.class);
        observer = new CreateReSortTicketsOfferChangeObserver(queueServiceMock);
    }

    @Test
    public void whenSupplierChangeApprovedMappingThenIsEnqueued() {
        Offer.Mapping mapping = new Offer.Mapping(MAPPING_ID, LocalDateTime.now());

        Offer offerBefore = createOffer();
        Offer offerAfter = offerBefore.copy()
            .setSupplierSkuMappingStatus(Offer.MappingStatus.RE_SORT)
            .updateApprovedSkuMapping(mapping, Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RE_SORT);

        observe(new EntityChangeEvent<>(offerBefore, offerAfter));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(queueServiceMock, Mockito.times(1))
            .enqueueByIds(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .containsExactly(OFFER_ID);
    }

    @Test
    public void whenAlreadyInReSortAndNotChangedSupplierMappingThenIsNotEnqueued() {
        Offer.Mapping mapping = new Offer.Mapping(MAPPING_ID, LocalDateTime.now());

        Offer offerBefore = createOffer()
            .setSupplierSkuMappingStatus(Offer.MappingStatus.RE_SORT)
            .updateApprovedSkuMapping(mapping, Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RE_SORT);
        Offer offerAfter = offerBefore.copy()
            .setContentComment("test");

        observe(new EntityChangeEvent<>(offerBefore, offerAfter));

        Mockito.verify(queueServiceMock, Mockito.times(0))
            .enqueueByIds(Mockito.anyList());
    }

    @SafeVarargs
    private void observe(EntityChangeEvent<Offer>... events) {
        observer.onBeforeSave(List.of(events));
        observer.onAfterSave(List.of(events));
    }

    private Offer createOffer() {
        return OfferTestUtils.nextOffer()
            .setId(OFFER_ID);
    }
}
