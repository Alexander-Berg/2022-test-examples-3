package ru.yandex.market.abo.core.feed.search.task.idx;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.feed.search.task.FeedSearchTaskService;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTaskStatus;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.util.idx.ServiceAvailabilityConfig;
import ru.yandex.market.common.report.indexer.IdxAPI;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 22.11.2019
 */
class IdxOfferSearchProcessorTest {
    private static final long SHOP_ID = 774;
    private final LocalDateTime NOW = LocalDateTime.now();
    private final LocalDateTime DEFAULT_SEARCH_AFTER = NOW.minusMinutes(30);
    private final LocalDateTime DEFAULT_LAST_CHECKED = NOW.minusMinutes(40);

    @Mock
    FeedSearchTaskService feedSearchTaskService;
    @Mock
    IdxAPI idxAPI;
    @Mock
    IdxOfferSearchStateManager offerSearchStateManager;
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    FeedSearchTask task;
    @Mock
    OfferDetails idxOffer;
    @Mock
    ServiceAvailabilityConfig serviceAvailabilityConfig;
    @Mock
    MbiApiService mbiApiService;

    @InjectMocks
    IdxOfferSearchProcessor idxOfferSearchProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(task.getShopId()).thenReturn(SHOP_ID);
        when(task.getSearchAfter()).thenReturn(DEFAULT_SEARCH_AFTER);
        when(task.getStatus()).thenReturn(FeedSearchTaskStatus.NEW);

        when(idxOffer.getLastChecked()).thenReturn(DateUtil.asDate(DEFAULT_LAST_CHECKED));
        when(idxAPI.findOffer(any(), any(), any())).thenReturn(idxOffer);

        when(shopInfoService.doNotTouchMyFeedShops()).thenReturn(Set.of());
        when(serviceAvailabilityConfig.dontUseIdxApi()).thenReturn(false);
    }

    @Test
    void testNeedRefreshFeedUsualShop() {
        idxOfferSearchProcessor.processTask(task);
        verify(mbiApiService).refreshFeed(anyLong());
        verify(offerSearchStateManager).feedRefreshed(task, idxOffer);
    }

    @Test
    void testWaitFreshFeedUsualShop() {
        when(task.getStatus()).thenReturn(FeedSearchTaskStatus.IN_PROGRESS);
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).waitFreshFeedForUsualShop(task, idxOffer);
    }

    @Test
    void testWaitFreshFeedUntouchableShop() {
        var requestDate = NOW.minusMinutes(10).truncatedTo(ChronoUnit.MINUTES);
        mockLoadFeedRefreshRequests(requestDate);
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).waitFreshFeedForUntouchableShop(task, idxOffer, requestDate);
    }

    @Test
    void testFreshFeedUntouchableShop() {
        mockLoadFeedRefreshRequests(DEFAULT_LAST_CHECKED.minusMinutes(5));
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).offerFound(task, idxOffer);
    }

    @Test
    void testFreshFeed() {
        when(idxOffer.getLastChecked()).thenReturn(DateUtil.asDate(NOW.minusMinutes(20)));
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).offerFound(task, idxOffer);
    }

    @Test
    void testFreshFeedWithTaskRegions() {
        when(task.getRegionIds()).thenReturn(List.of(225L));
        when(idxOffer.getLastChecked()).thenReturn(DateUtil.asDate(NOW.minusMinutes(20)));
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).logDeliverySearchRequest(task);
        verify(idxAPI).findDefaultDeliveryOptions(any(), any(), any());
        verify(offerSearchStateManager).offerFound(task, idxOffer);
    }

    @Test
    void testFreshFeedWithTaskRegionsException() {
        when(task.getRegionIds()).thenReturn(List.of(225L));
        when(idxOffer.getLastChecked()).thenReturn(DateUtil.asDate(NOW.minusMinutes(20)));
        when(idxAPI.findDefaultDeliveryOptions(any(), any(), any())).thenThrow(new RuntimeException());
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).logDeliverySearchRequest(task);
        verify(offerSearchStateManager).logDeliveryError(task);
        verify(offerSearchStateManager).offerFound(task, idxOffer);
    }

    @Test
    void testExpiredTask() {
        when(task.getSearchAfter()).thenReturn(NOW.minusHours(2));
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).taskExpired(task);
    }

    @Test
    void testOfferNotFound() {
        when(idxAPI.findOffer(any(), any(), any())).thenReturn(null);
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).offerNotFound(task);
    }

    @Test
    void testIdxApiCoreConfig() {
        when(serviceAvailabilityConfig.dontUseIdxApi()).thenReturn(true);
        idxOfferSearchProcessor.processTask(task);
        verify(offerSearchStateManager).dontUseIdxApi(task);
    }

    private void mockLoadFeedRefreshRequests(LocalDateTime requestDate) {
        when(shopInfoService.doNotTouchMyFeedShops()).thenReturn(Set.of(SHOP_ID));
        when(idxAPI.feedRefreshRequests()).thenReturn(Map.of(SHOP_ID, DateUtil.asDate(requestDate)));
    }
}
