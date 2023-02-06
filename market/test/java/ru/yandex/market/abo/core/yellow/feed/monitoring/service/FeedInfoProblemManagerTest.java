package ru.yandex.market.abo.core.yellow.feed.monitoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.yellow.feed.monitoring.model.FeedInfoHistory;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.yellow.feed.monitoring.service.FeedInfoProblemManager.ALLOWED_OFFERS_DIFFERENCE_ON_SHARE;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.12.2019
 */
class FeedInfoProblemManagerTest {

    public static final long SHOP_ID = 123L;
    public static final long FEED_ID = 1231L;
    public static final long SECOND_FEED_ID = 1232L;
    private static final long PREVIOUS_OFFERS_COUNT = 27L;
    private static final long NOT_PROBLEM_OFFERS_COUNT =
            (long) (PREVIOUS_OFFERS_COUNT * ALLOWED_OFFERS_DIFFERENCE_ON_SHARE + 1);
    private static final long PROBLEM_OFFERS_COUNT =
            (long) (PREVIOUS_OFFERS_COUNT * ALLOWED_OFFERS_DIFFERENCE_ON_SHARE - 1);
    private static final LocalDateTime NOT_PROBLEM_REFRESH_TIME = LocalDateTime.now().minusHours(5L);
    private static final LocalDateTime PROBLEM_REFRESH_TIME = LocalDateTime.now().minusDays(2);

    @Mock
    private FeedInfoHistory previousFeed;
    @Mock
    private FeedInfoHistory firstTestFeed;
    @Mock
    private FeedInfoHistory secondTestFeed;
    @Mock
    private FeedInfoLoader feedInfoLoader;
    @Mock
    private FeedInfoStTicketCreator startrekTicketCreator;
    @Mock
    private FeedInfoHistoryService feedInfoHistoryService;

    @InjectMocks
    private FeedInfoProblemManager feedInfoProblemManager;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(feedInfoHistoryService.getNeedForOffersCountCheckFeeds()).thenReturn(List.of(firstTestFeed));
        when(feedInfoHistoryService.getLastFeeds()).thenReturn(List.of(firstTestFeed));
        when(feedInfoHistoryService.getLastCheckedFeeds(any())).thenReturn(List.of(previousFeed));
        when(previousFeed.getFeedId()).thenReturn(FEED_ID);
        when(previousFeed.getShopId()).thenReturn(SHOP_ID);
        when(previousFeed.getOffersCount()).thenReturn(PREVIOUS_OFFERS_COUNT);
        when(firstTestFeed.getFeedId()).thenReturn(FEED_ID);
        when(firstTestFeed.getShopId()).thenReturn(SHOP_ID);
        when(secondTestFeed.getFeedId()).thenReturn(SECOND_FEED_ID);
        when(secondTestFeed.getShopId()).thenReturn(SHOP_ID);
    }

    @Test
    void checkNotProblemFeedTest() {
        when(firstTestFeed.getRefreshTime()).thenReturn(NOT_PROBLEM_REFRESH_TIME);
        when(firstTestFeed.getOffersCount()).thenReturn(NOT_PROBLEM_OFFERS_COUNT);
        feedInfoProblemManager.checkProblems();
        verify(firstTestFeed, never()).setOffersCountProblem(true);
        verify(firstTestFeed, never()).setRefreshProblem(true);
    }

    @Test
    void checkOffersCountProblemTest() {
        when(firstTestFeed.getOffersCount()).thenReturn(PROBLEM_OFFERS_COUNT);
        feedInfoProblemManager.checkOffersCountProblem();
        verify(firstTestFeed, times(1)).setOffersCountProblem(true);
    }

    @Test
    void checkThatExceptionNotThrowForUncheckedFeedsWithSameId() {
        when(feedInfoHistoryService.getNeedForOffersCountCheckFeeds())
                .thenReturn(List.of(firstTestFeed, secondTestFeed));
        when(secondTestFeed.getFeedId()).thenReturn(FEED_ID);
        when(firstTestFeed.getRefreshTime()).thenReturn(NOT_PROBLEM_REFRESH_TIME);
        when(secondTestFeed.getRefreshTime()).thenReturn(NOT_PROBLEM_REFRESH_TIME.minusHours(1L));
        try {
            feedInfoProblemManager.checkOffersCountProblem();
        } catch (IllegalStateException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void checkRefreshProblemTest() {
        when(firstTestFeed.getRefreshTime()).thenReturn(PROBLEM_REFRESH_TIME);
        feedInfoProblemManager.checkRefreshProblem();
        verify(firstTestFeed, times(1)).setRefreshProblem(true);
    }

    @Test
    void checkStTicketCreationForShopWithSeveralProblemFeedsTest() {
        when(feedInfoHistoryService.getProblemFeedsWithoutStTicket())
                .thenReturn(List.of(firstTestFeed, secondTestFeed));
        feedInfoProblemManager.createStTickets();
        verify(startrekTicketCreator, times(1)).createTicketForShop(anyLong(), any());
    }

    @Test
    void zeroCnt() {
        when(firstTestFeed.getRefreshTime()).thenReturn(PROBLEM_REFRESH_TIME);
        when(previousFeed.getOffersCount()).thenReturn(0L);
        feedInfoProblemManager.checkRefreshProblem();
        verify(firstTestFeed, times(1)).setRefreshProblem(true);
    }
}
