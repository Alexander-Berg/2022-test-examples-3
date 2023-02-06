package ru.yandex.direct.api.v5.entity.keywords.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.KeywordsGetContainer;
import ru.yandex.direct.api.v5.entity.keywords.container.StatValueAggregatorItem;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsResponse;
import ru.yandex.direct.ytcomponents.statistics.model.StatValueAggregator;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatisticServiceTest {

    private static final long ADGROUP_ID = 10L;
    private static final long CAMPAIGN_ID = 20L;
    private static final long BANNER_ID1 = 30L;
    private static final long BANNER_ID2 = 31L;
    private static final long PHRASE_ID = 40L;

    private static final long SEARCH_SHOWS = 201L;
    private static final long SEARCH_CLICKS = 21L;
    private static final long NETWORK_SHOWS = 301L;
    private static final long NETWORK_CLICKS = 31L;

    private StatisticService statisticService;
    private RecentStatisticsService recentStatisticsService;

    @Before
    public void setUp() {
        recentStatisticsService = mock(RecentStatisticsService.class);
        statisticService = new StatisticService(recentStatisticsService);
    }

    @Test
    public void getPhraseStatistics_empty_whenEmptyRepositoryResponse() {
        when(recentStatisticsService.getPhraseStatistics(any(), any())).thenReturn(emptyMap());
        Map<Long, StatValueAggregatorItem> actual = statisticService.getPhraseStatistics(getPhraseRequests());
        assertThat(actual).isEmpty();
    }

    @Test
    public void getPhraseStatisticsWithOnePhrase_networkStatTest() {
        when(recentStatisticsService.getPhraseStatistics(any(), any()))
                .thenReturn(ImmutableMap.of(
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).phraseStatIndex(),
                        new StatValueAggregator()
                                .addNetworkClicks(NETWORK_CLICKS)
                                .addNetworkShows(NETWORK_SHOWS)
                ));

        Map<Long, StatValueAggregatorItem> actual = statisticService.getPhraseStatistics(getPhraseRequests());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(NETWORK_CLICKS, NETWORK_SHOWS));
    }

    @Test
    public void getPhraseStatisticsWithTwoPhrase_networkStatTest() {
        when(recentStatisticsService.getPhraseStatistics(any(), any()))
                .thenReturn(ImmutableMap.of(
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).withBannerId(BANNER_ID1)
                                .phraseStatIndex(),
                        new StatValueAggregator()
                                .addNetworkClicks(NETWORK_CLICKS)
                                .addNetworkShows(NETWORK_SHOWS),
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).withBannerId(BANNER_ID2)
                                .phraseStatIndex(),
                        new StatValueAggregator()
                                .addNetworkClicks(NETWORK_CLICKS)
                                .addNetworkShows(NETWORK_SHOWS)
                ));

        Map<Long, StatValueAggregatorItem> actual = statisticService.getPhraseStatistics(getPhraseRequests());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(NETWORK_CLICKS * 2, NETWORK_SHOWS * 2));
    }

    @Test
    public void getPhraseStatisticsWithOnePhrase_searchStatTest() {
        when(recentStatisticsService.getPhraseStatistics(any(), any()))
                .thenReturn(ImmutableMap.of(
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).phraseStatIndex(),
                        new StatValueAggregator()
                                .addSearchClicks(SEARCH_CLICKS)
                                .addSearchShows(SEARCH_SHOWS)
                ));

        Map<Long, StatValueAggregatorItem> actual = statisticService.getPhraseStatistics(getPhraseRequests());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(SEARCH_CLICKS, SEARCH_SHOWS));
    }

    @Test
    public void getPhraseStatisticsWithTwoPhrase_searchStatTest() {
        when(recentStatisticsService.getPhraseStatistics(any(), any()))
                .thenReturn(ImmutableMap.of(
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).withBannerId(BANNER_ID1)
                                .phraseStatIndex(),
                        new StatValueAggregator()
                                .addSearchClicks(SEARCH_CLICKS)
                                .addSearchShows(SEARCH_SHOWS),
                        new PhraseStatisticsResponse().withPhraseId(PHRASE_ID).withBannerId(BANNER_ID2)
                                .phraseStatIndex(),
                        new StatValueAggregator()
                                .addSearchClicks(SEARCH_CLICKS)
                                .addSearchShows(SEARCH_SHOWS)
                ));

        Map<Long, StatValueAggregatorItem> actual = statisticService.getPhraseStatistics(getPhraseRequests());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(SEARCH_CLICKS * 2, SEARCH_SHOWS * 2));
    }

    private List<KeywordsGetContainer> getPhraseRequests() {
        return Collections.singletonList(KeywordsGetContainer.createItemForKeyword((
                new Keyword()
                        .withId(0L)
                        .withAdGroupId(ADGROUP_ID)
                        .withCampaignId(CAMPAIGN_ID)
        )));
    }

    private Consumer<StatValueAggregatorItem> networkStatEquality(
            long expectedNetworkClicks, long expectedNetworkShows) {
        return agg -> SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(agg.getNetworkClicks())
                    .describedAs("networkClicks")
                    .isEqualTo(expectedNetworkClicks);
            softly.assertThat(agg.getNetworkShows())
                    .describedAs("networkShows")
                    .isEqualTo(expectedNetworkShows);
        });
    }

    private Consumer<StatValueAggregatorItem> searchStatEquality(
            long expectedSearchClicks, long expectedSearchShows) {
        return agg -> SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(agg.getSearchClicks())
                    .describedAs("searchClicks")
                    .isEqualTo(expectedSearchClicks);
            softly.assertThat(agg.getSearchShows())
                    .describedAs("searchShows")
                    .isEqualTo(expectedSearchShows);
        });
    }

}
