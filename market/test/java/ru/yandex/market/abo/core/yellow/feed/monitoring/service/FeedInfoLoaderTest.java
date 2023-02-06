package ru.yandex.market.abo.core.yellow.feed.monitoring.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.feed.ShopFeedService;
import ru.yandex.market.abo.core.feed.model.ShopFeed;
import ru.yandex.market.abo.core.yellow.feed.monitoring.model.FeedInfoHistory;
import ru.yandex.market.abo.core.yellow.feed.monitoring.repository.FeedInfoHistoryRepository;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.common.report.indexer.yellow.YellowIdxApi;
import ru.yandex.market.common.report.indexer.yellow.model.SessionFeedInfo;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.shops.ShopFeatureInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.12.2019
 */
class FeedInfoLoaderTest extends EmptyTest {

    public static final long SHOP_ID = 123L;
    public static final long FEED_ID = 1231L;
    private static final long NOT_ACTIVE_SHOP_ID = 124L;
    private static final long OFFERS_COUNT = 21L;
    private static final LocalDateTime PREVIOUS_REFRESH_TIME = LocalDateTime.parse("2019-12-10T13:10:00");
    private static final LocalDateTime CURRENT_REFRESH_TIME = LocalDateTime.parse("2019-12-12T13:10:05");
    private static final long OLD_YML_DATE_TS = 1575972600L;
    private static final long NEW_YML_DATE_TS = 1576145405L;
    private static final boolean IS_LAST = true;

    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private YellowIdxApi yellowIdxApi;
    @Mock
    private ShopFeedService shopFeedService;

    @Autowired
    private FeedInfoHistoryRepository feedInfoHistoryRepository;
    @Autowired
    private FeedInfoHistoryService feedInfoHistoryService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private FeedInfoLoader feedInfoLoader;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        feedInfoLoader = new FeedInfoLoader(mbiApiService, yellowIdxApi, shopFeedService,
                feedInfoHistoryService, transactionTemplate);
    }

    @Test
    void testProblemFeedInfoRefreshingForNotActiveShop() {
        when(mbiApiService.getAllFmcgShopIds())
                .thenReturn(Set.of(NOT_ACTIVE_SHOP_ID));
        when(mbiApiService.getFeatureInfos(anyList(), anyList()))
                .thenReturn(List.of(createShopFeature(NOT_ACTIVE_SHOP_ID, ParamCheckStatus.FAIL.name())));
        feedInfoLoader.loadFeedInfo();
        assertTrue(feedInfoHistoryRepository.findAll().isEmpty());
    }

    @Test
    void testProblemFeedInfoRefreshing() {
        mockAnswersForActiveShop();
        when(yellowIdxApi.findFeedsInfo(FEED_ID))
                .thenReturn(createSessionFeedInfo(NEW_YML_DATE_TS));
        feedInfoLoader.loadFeedInfo();
        var actualResult = feedInfoHistoryRepository.findAll();
        assertEquals(new HashSet<>(expectedResult()), new HashSet<>(actualResult));
    }

    @Test
    void testProblemFeedInfoRefreshingForNotChangedRefreshTime() {
        mockAnswersForActiveShop();
        when(yellowIdxApi.findFeedsInfo(FEED_ID))
                .thenReturn(createSessionFeedInfo(OLD_YML_DATE_TS));
        feedInfoLoader.loadFeedInfo();
        var actualResult = feedInfoHistoryRepository.findAll();
        assertEquals(expectedResultForSameRefreshTime(), actualResult);
    }

    private void mockAnswersForActiveShop() {
        feedInfoHistoryService.save(initialFeedInfoHistory());
        when(mbiApiService.getAllFmcgShopIds())
                .thenReturn(Set.of(SHOP_ID));
        when(mbiApiService.getFeatureInfos(anyList(), anyList()))
                .thenReturn(List.of(createShopFeature(SHOP_ID, ParamCheckStatus.SUCCESS.name())));
        var shopFeed = new ShopFeed();
        shopFeed.setShopId(SHOP_ID);
        shopFeed.setFeedId(FEED_ID);
        when(shopFeedService.loadByShopIds(anyList())).thenReturn(List.of(shopFeed));
    }

    private static List<FeedInfoHistory> initialFeedInfoHistory() {
        return List.of(createFeedInfoHistory(PREVIOUS_REFRESH_TIME, IS_LAST));
    }

    private static List<FeedInfoHistory> expectedResult() {
        var penultimateFeedInfo = createFeedInfoHistory(PREVIOUS_REFRESH_TIME, !IS_LAST);
        var lastFeedInfo = createFeedInfoHistory(CURRENT_REFRESH_TIME, IS_LAST);
        return List.of(penultimateFeedInfo, lastFeedInfo);
    }

    private static List<FeedInfoHistory> expectedResultForSameRefreshTime() {
        return initialFeedInfoHistory();
    }

    private static ShopFeatureInfoDTO createShopFeature(long shopId, String featureStatus) {
        return new ShopFeatureInfoDTO(shopId, FeatureType.FMCG_PARTNER, featureStatus, true, null, null);
    }

    private static SessionFeedInfo createSessionFeedInfo(long ymlDateTs) {
        var sessionFeedInfo = new SessionFeedInfo();
        sessionFeedInfo.setFeedId(FEED_ID);
        sessionFeedInfo.setOffersCount(OFFERS_COUNT);
        sessionFeedInfo.setYmlDateTs(ymlDateTs);
        return sessionFeedInfo;
    }

    private static FeedInfoHistory createFeedInfoHistory(LocalDateTime refreshTime, boolean isLast) {
        var feedInfo = new FeedInfoHistory(FEED_ID, SHOP_ID, OFFERS_COUNT, refreshTime);
        feedInfo.setLast(isLast);
        return feedInfo;
    }
}
