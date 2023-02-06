package ru.yandex.market.abo.core.yellow.feed.monitoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.yellow.feed.monitoring.model.FeedInfoHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.12.2019
 */
class FeedInfoStTicketCreatorTest {

    public static final long FEED_ID = 1231L;
    public static final long SECOND_FEED_ID = 1232L;
    private static final long CURRENT_OFFERS_COUNT = 35L;
    private static final long PREVIOUS_OFFERS_COUNT = 75L;
    private static final LocalDateTime CHECK_TIME = LocalDateTime.parse("2019-12-15T11:17:29");
    private static final LocalDateTime REFRESH_TIME = LocalDateTime.parse("2019-12-14T09:19:23");

    @Mock
    private FeedInfoHistory previousFeed;
    @Mock
    private FeedInfoHistory firstProblemFeed;
    @Mock
    private FeedInfoHistory secondProblemFeed;
    @Mock
    private FeedInfoHistoryService feedInfoHistoryService;

    @InjectMocks
    private FeedInfoStTicketCreator ticketForYellowProblemShopsService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(feedInfoHistoryService.getPreviousFeedInfo(any())).thenReturn(previousFeed);
        when(previousFeed.getOffersCount()).thenReturn(PREVIOUS_OFFERS_COUNT);
        when(firstProblemFeed.getFeedId()).thenReturn(FEED_ID);
        when(firstProblemFeed.getOffersCount()).thenReturn(CURRENT_OFFERS_COUNT);
        when(firstProblemFeed.isOffersCountProblem()).thenReturn(true);
        when(firstProblemFeed.isRefreshProblem()).thenReturn(true);
        when(firstProblemFeed.getRefreshTime()).thenReturn(REFRESH_TIME);
        when(secondProblemFeed.getFeedId()).thenReturn(SECOND_FEED_ID);
        when(secondProblemFeed.getRefreshTime()).thenReturn(REFRESH_TIME);
        when(secondProblemFeed.isRefreshProblem()).thenReturn(true);
    }

    @Test
    void checkDescriptionBuild() {
        var actualDescription = ticketForYellowProblemShopsService
                .buildDescription(CHECK_TIME, List.of(firstProblemFeed, secondProblemFeed));
        assertEquals(getExpectedDescription(), actualDescription);
    }

    private String getExpectedDescription() {
        return "Дата/время проверки: 2019-12-15 11:17:29\n" +
                "\n" +
                "Фид: 1231\n" +
                "Дата/время предыдущего обновления: !!2019-12-14 09:19:23!!\n" +
                "Количество предложений: 35\n" +
                "Количество предложений в предыдущей версии фида: !!75!!\n" +
                "\n" +
                "Фид: 1232\n" +
                "Дата/время предыдущего обновления: !!2019-12-14 09:19:23!!" +
                "\n\n";
    }
}
