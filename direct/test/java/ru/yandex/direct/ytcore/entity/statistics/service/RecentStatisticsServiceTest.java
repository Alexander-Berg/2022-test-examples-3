package ru.yandex.direct.ytcore.entity.statistics.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.testing.data.TestNewTextBanners;
import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsResponse;
import ru.yandex.direct.ytcomponents.statistics.model.RetargetingStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.ShowConditionStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.StatValueAggregator;
import ru.yandex.direct.ytcore.entity.statistics.repository.RecentStatisticsRepository;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.types.Unsigned.ulong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileBanner;

public class RecentStatisticsServiceTest {

    private final long yaSearchShows = 101L;
    private final long yaSearchClicks = 11L;
    private final double yaSearchEshows = 100.5;
    private final long searchShows = 201L;
    private final long searchClicks = 21L;
    private final double searchEshows = 200.5;
    private final long networkShows = 301L;
    private final long networkClicks = 31L;
    private final double networkEshows = 300.5;

    private RecentStatisticsService serviceUnderTest;
    private RecentStatisticsRepository recentStatisticsRepositoryMock;
    private BannerService bannerServiceMock;
    private long campaignId;
    private long adGroupId;
    private long bannerId;
    private long phraseId;
    private TextBanner textBanner;

    @Before
    public void setUp() {
        recentStatisticsRepositoryMock = mock(RecentStatisticsRepository.class);

        campaignId = 1L;
        adGroupId = 2L;
        bannerId = 3L;
        phraseId = 4L;

        textBanner = TestNewTextBanners.fullTextBanner(campaignId, adGroupId).withId(bannerId);
        // поведение по умолчанию: возвращаем текстовый баннер

        bannerServiceMock = mockBannerService(textBanner);
        serviceUnderTest = new RecentStatisticsService(recentStatisticsRepositoryMock, bannerServiceMock);
    }



    /*
     * getPhraseStatistics
     */

    // пустой ответ из репозитория -- ОК
    @Test
    public void getPhraseStatistics_empty_whenEmptyRepositoryResponse() {
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(emptyList());

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).isEmpty();
    }

    // ответ с bannerId = 0 -- не учитываем
    @Test
    public void getPhraseStatistics_empty_whenZeroBannerIdInResponse() {
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(singletonList(
                        getFilledResponse().withBannerId(0L)));

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(0, 0, 0.0));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(0, 0, 0.0));
    }

    // ответ с bannerId удалённого баннера -- не учитываем
    @Test
    public void getPhraseStatistics_empty_whenAbsentBannerIdInResponse() {
        long absentBannerId = 1_000_000_042L;
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(singletonList(
                        getFilledResponse().withBannerId(absentBannerId)));

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks, networkShows, networkEshows));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(yaSearchClicks, yaSearchShows, yaSearchEshows));
    }

    // несколько ответов с разной мобильностью: сумма всего для сетей
    @Test
    public void getPhraseStatistics_allNetworkStat() {
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks * 2, networkShows * 2, networkEshows * 2));
    }

    // несколько ответов с разной мобильностью: сумма только мобильной статистики для мобильного баннера
    @Test
    public void getPhraseStatistics_mobileSearchStat_forMobileBanner() {
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner.withIsMobile(true);

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(yaSearchClicks, yaSearchShows, yaSearchEshows));
    }

    // несколько ответов с разной мобильностью: сумма всей статистики для РМП баннера
    @Test
    public void getPhraseStatistics_mobileSearchStat_forMobileContentBanner() {
        recentStatisticsRepositoryMock = mock(RecentStatisticsRepository.class);
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        MobileAppBanner newMobileAppBanner = fullMobileBanner(campaignId, adGroupId).withId(bannerId);
        bannerServiceMock = mockBannerService(newMobileAppBanner);
        serviceUnderTest = new RecentStatisticsService(recentStatisticsRepositoryMock, bannerServiceMock);

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(yaSearchClicks * 2, yaSearchShows * 2, yaSearchEshows * 2));
    }

    // несколько ответов с разной мобильностью: сумма только не мобильной статистики для не мобильного баннера
    @Test
    public void getPhraseStatistics_nonMobileSearchStat_forDesktopTextBanner() {
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner
                .withIsMobile(false);

        Map<PhraseStatisticsResponse.PhraseStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(yaSearchClicks, yaSearchShows, yaSearchEshows));
    }

    // агрегация ответов по ключу
    @Test
    public void getPhraseStatistics_extractStatIndexByShowConditionStatIndex_BannersStatAggregate() {
        Long bannerId2 = 104L;
        when(recentStatisticsRepositoryMock.getPhraseStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withBannerId(bannerId),
                        getFilledResponse().withBannerId(bannerId2)
                ));

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getPhraseStatistics(getPhraseRequests(), getDateRange(),
                        PhraseStatisticsResponse::showConditionStatIndex);

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(yaSearchClicks * 2, yaSearchShows * 2, yaSearchEshows * 2));
    }


    /*
     * getShowConditionStatistics
     */

    // ответ с bannerId = 0 -- учитываем не мобильную статистику
    @Test
    public void getShowConditionStatistics_empty_whenZeroBannerIdInResponse() {
        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(singletonList(
                        getFilledResponse().withBannerId(0L)));

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks, networkShows, networkEshows));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));

    }

    // ответ с bannerId удалённого баннера -- не учитываем
    @Test
    public void getShowConditionStatistics_empty_whenAbsentBannerIdInResponse() {
        long absentBannerId = 1_000_000_042L;
        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(singletonList(
                        getFilledResponse().withBannerId(absentBannerId)));

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks, networkShows, networkEshows));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // несколько ответов с разной мобильностью: сумма всего для сетей
    @Test
    public void getShowConditionStatistics_allNetworkStat() {
        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks * 2, networkShows * 2, networkEshows * 2));
    }

    // несколько ответов с разной мобильностью: сумма только мобильной статистики для мобильного баннера
    @Test
    public void getShowConditionStatistics_mobileSearchStat_forMobileBanner() {
        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner.withIsMobile(true);

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // несколько ответов с разной мобильностью: сумма только мобильной статистики для РМП баннера
    @Test
    public void getShowConditionStatistics_mobileSearchStat_forMobileContentBanner() {
        recentStatisticsRepositoryMock = mock(RecentStatisticsRepository.class);

        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        MobileAppBanner newMobileAppBanner = fullMobileBanner(campaignId, adGroupId).withId(bannerId);
        bannerServiceMock = mockBannerService(newMobileAppBanner);
        serviceUnderTest = new RecentStatisticsService(recentStatisticsRepositoryMock, bannerServiceMock);

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks * 2, searchShows * 2, searchEshows * 2));
    }

    // несколько ответов с разной мобильностью: сумма только не мобильной статистики для не мобильного баннера
    @Test
    public void getShowConditionStatistics_nonMobileSearchStat_forDesktopTextBanner() {
        when(recentStatisticsRepositoryMock.getShowConditionStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner
                .withIsMobile(false);

        Map<PhraseStatisticsResponse.ShowConditionStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getShowConditionStatistics(getShowConditionRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }



    /*
     * getRetargetingStatistics
     */


    // ответ с bannerId = 0 -- учитываем только desktop-статистику
    @Test
    public void getRetargetingStatistics_nonEmpty_whenZeroBannerIdInResponse() {
        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(),
                        getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks * 3, networkShows * 3, networkEshows * 3));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // ответ с bannerId удалённого баннера -- не учитываем
    @Test
    public void getRetargetingStatistics_empty_whenAbsentBannerIdInResponse() {
        long absentBannerId = 1_000_000_042L;
        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(singletonList(
                        getFilledResponse().withBannerId(absentBannerId)));

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(),
                        getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks, networkShows, networkEshows));
        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // несколько ответов с разной мобильностью: сумма всего для сетей
    @Test
    public void getRetargetingStatistics_allNetworkStat() {
        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                networkStatEquality(networkClicks * 2, networkShows * 2, networkEshows * 2));
    }

    // несколько ответов с разной мобильностью: сумма только мобильной статистики для мобильного баннера
    @Test
    public void getRetargetingStatistics_mobileSearchStat_forMobileBanner() {
        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner.withIsMobile(true);

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // несколько ответов с разной мобильностью: сумма только мобильной статистики для РМП баннера
    @Test
    public void getRetargetingStatistics_mobileSearchStat_forMobileContentBanner() {
        recentStatisticsRepositoryMock = mock(RecentStatisticsRepository.class);

        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        MobileAppBanner newMobileAppBanner = fullMobileBanner(campaignId, adGroupId).withId(bannerId);
        bannerServiceMock = mockBannerService(newMobileAppBanner);
        serviceUnderTest = new RecentStatisticsService(recentStatisticsRepositoryMock, bannerServiceMock);

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }

    // несколько ответов с разной мобильностью: сумма только не мобильной статистики для не мобильного баннера
    @Test
    public void getRetargetingStatistics_nonMobileSearchStat_forDesktopTextBanner() {
        when(recentStatisticsRepositoryMock.getRetargetingStatistics(any(), any()))
                .thenReturn(asList(
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(true),
                        getFilledResponse().withMobile(false)
                ));

        textBanner = textBanner
                .withIsMobile(false);

        Map<PhraseStatisticsResponse.GoalContextStatIndex, StatValueAggregator> actual =
                serviceUnderTest.getRetargetingStatistics(getRetargetingRequests(), getDateRange());

        assertThat(actual).hasSize(1);

        assertThat(actual.values().iterator().next()).satisfies(
                searchStatEquality(searchClicks, searchShows, searchEshows));
    }


    private Consumer<StatValueAggregator> networkStatEquality(
            long expectedNetworkClicks, long expectedNetworkShows, double expectedNetworkEshows) {
        return agg -> SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(agg.getNetworkClicks())
                    .describedAs("networkClicks")
                    .isEqualTo(expectedNetworkClicks);
            softly.assertThat(agg.getNetworkShows())
                    .describedAs("networkShows")
                    .isEqualTo(expectedNetworkShows);
            softly.assertThat(agg.getNetworkEshows())
                    .describedAs("networkEshows")
                    .isEqualTo(expectedNetworkEshows);
        });
    }

    private Consumer<StatValueAggregator> searchStatEquality(
            long expectedSearchClicks, long expectedSearchShows, double expectedSearchEshows) {
        return agg -> SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(agg.getSearchClicks())
                    .describedAs("searchClicks")
                    .isEqualTo(expectedSearchClicks);
            softly.assertThat(agg.getSearchShows())
                    .describedAs("searchShows")
                    .isEqualTo(expectedSearchShows);
            softly.assertThat(agg.getSearchEshows())
                    .describedAs("searchEshows")
                    .isEqualTo(expectedSearchEshows);
        });
    }

    private List<PhraseStatisticsRequest> getPhraseRequests() {
        return Collections.singletonList(
                new PhraseStatisticsRequest.Builder()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withPhraseId(4L)
                        .build());
    }

    private List<ShowConditionStatisticsRequest> getShowConditionRequests() {
        return Collections.singletonList(
                new ShowConditionStatisticsRequest.Builder()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withShowConditionId(4L)
                        .build());
    }

    private List<RetargetingStatisticsRequest> getRetargetingRequests() {
        return Collections.singletonList(
                new RetargetingStatisticsRequest.Builder()
                        .withCampaignId(1L)
                        .withAdGroupId(2L)
                        .withRetargetingConditionId(5L)
                        .build());
    }

    private PhraseStatisticsResponse getFilledResponse() {
        return new PhraseStatisticsResponse()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerId(bannerId)
                .withPhraseId(phraseId)
                .withBsPhraseId(ulong(5L))
                .withGoalContextId(ulong(5L))
                .withMobile(false)
                .withSearchShows(searchShows)
                .withSearchClicks(searchClicks)
                .withSearchEshows(searchEshows)
                .withYaSearchShows(yaSearchShows)
                .withYaSearchClicks(yaSearchClicks)
                .withYaSearchEshows(yaSearchEshows)
                .withNetworkShows(networkShows)
                .withNetworkClicks(networkClicks)
                .withNetworkEshows(networkEshows);
    }

    private DateRange getDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(28);
        return new DateRange()
                .withFromInclusive(startDate)
                .withToInclusive(endDate);
    }

    private BannerService mockBannerService(Banner newBanner) {
        BannerService mockService = mock(BannerService.class);
        when(mockService.getBannersByIds(any()))
                .thenAnswer(invocation -> {
                    Collection<Long> ids = invocation.getArgument(0);
                    if (ids.contains(newBanner.getId())) {
                        return singletonList(newBanner);
                    }
                    return emptyList();
                });

        return mockService;
    }
}
