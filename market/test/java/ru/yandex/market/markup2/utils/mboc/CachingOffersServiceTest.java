package ru.yandex.market.markup2.utils.mboc;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.http.MboCategory.GetOffersPrioritiesRequest.StatusFilter.IN_CLASSIFICATION;
import static ru.yandex.market.mboc.http.MboCategory.GetOffersPrioritiesRequest.StatusFilter.IN_PROCESS;

public class CachingOffersServiceTest {
    private MboCategoryServiceMock mboCategoryServiceMock;
    private CachingOffersService cachingOffersService;

    @Before
    public void setUp() {
        mboCategoryServiceMock = spy(new MboCategoryServiceMock());

        cachingOffersService = new CachingOffersService(
                mboCategoryServiceMock,
                ImmutableMap.of(
                        IN_PROCESS.toString(),
                        new CachingOffersService.CacheLoadingConfig()
                                .setHasDeadlineOrOldProcessingStatus(true)
                ));
    }

    @Test
    public void testDefaultCacheLoadingConfig() {
        cachingOffersService.getOffersByStatus(IN_CLASSIFICATION.toString());

        verify(mboCategoryServiceMock, times(1))
                .loadOffersByStatus(
                        eq(IN_CLASSIFICATION.toString()),
                        argThat(o -> !o.isPresent()),
                        eq(false),
                        any());
    }

    @Test
    public void testInProcessCacheLoadingConfig() {
        cachingOffersService.getOffersByStatus(IN_PROCESS.toString());

        verify(mboCategoryServiceMock, times(1))
                .loadOffersByStatus(
                        eq(IN_PROCESS.toString()),
                        argThat(o -> !o.isPresent()),
                        eq(true),
                        any());
    }
}
