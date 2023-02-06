package ru.yandex.market.mboc.common.services.offers.upload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.upload.OfferUploadQueueItem;
import ru.yandex.market.mboc.common.offers.repository.upload.OfferUploadQueueRepository;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractOfferUploadQueueServiceTest {
    OfferUploadQueueRepository repo = mock(OfferUploadQueueRepository.class);
    OfferUploadQueueService service = new AbstractOfferUploadQueueService(repo) {
    };

    @Before
    public void setUp() {
        reset(repo);
    }

    @Test
    public void getForUploadCallsRepo() {
        service.getForUpload(200);
        verify(repo, times(1)).findForUpload(eq(200));
    }

    @Test
    public void dequeueOfferIdsCallsRepo() {
        service.dequeueOfferIds(List.of(1L, 2L, 3L));
        verify(repo, times(1)).delete(eq(List.of(1L, 2L, 3L)));
    }

    @Test
    public void dequeueOffersCallsRepo() {
        var offers = List.of(OfferTestUtils.nextOffer(), OfferTestUtils.nextOffer());
        service.dequeueOffers(offers);
        verify(repo, times(1)).delete(eq(List.of(offers.get(0).getId(), offers.get(1).getId())));
    }

    @Test
    public void updateInQueueCallsRepo() {
        var item = new OfferUploadQueueItem(123, now());
        service.updateInQueue(List.of(item));
        verify(repo, times(1)).updateBatch(eq(List.of(item)));
    }

    @Test
    public void getEnqueuedInPeriodCallsRepo() {
        var after = now().minusMinutes(10);
        var before = now();
        service.getEnqueuedInPeriod(after, before);
        verify(repo, times(1)).findEnqueuedInPeriod(eq(after), eq(before));
    }

    @Test
    public void areAllOfferIdsInQueueChecksAll() {
        var idsIn = List.of(12L, 34L);
        var idsNotIn = List.of(56L, 78L);
        var all = new ArrayList<Long>(idsIn);
        all.addAll(idsNotIn);

        doReturn(List.of(new OfferUploadQueueItem(idsIn.get(0), now()),
                new OfferUploadQueueItem(idsIn.get(1), now()))).when(repo).findByIds(eq(new HashSet<>(idsIn)));

        assertThat(service.areAllOfferIdsInQueue(idsIn)).isTrue();
        assertThat(service.areAllOfferIdsInQueue(idsNotIn)).isFalse();
        assertThat(service.areAllOfferIdsInQueue(all)).isFalse();
    }
}
