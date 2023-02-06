package ru.yandex.direct.grid.core.entity.banner.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerFilter;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderBy;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderByField;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerPrimaryStatus;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKey;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderByField.PRIMARY_STATUS;
import static ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderByField.STAT_CONVERSION_RATE;
import static ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderByField.STAT_COST_PER_ACTION;
import static ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderByField.STAT_GOALS;
import static ru.yandex.direct.grid.model.Order.DESC;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@RunWith(JUnitParamsRunner.class)
public class GridBannerYtRepositoryTest {

    private static final int SHARD = 1;
    private static final long BANNER_ID = 11L;
    private static final long GOAL_ID = 1L;
    private static final long EXPORT_ID = 66L;
    private static final int LIMIT = 1000;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final List<GdiBannerOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final AdFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildAdFetchedFieldsResolver(false);
    private static final AdFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_FULL =
            FetchedFieldsResolverCoreUtil.buildAdFetchedFieldsResolver(true);
    private static final String SIMPLE_QUERY_PATH_FILE = "classpath:///banners/banners-simple.query";
    private static final String SIMPLE_CREATIVE_FREE_QUERY_PATH_FILE =
            "classpath:///banners/banners-simple-creative-free.query";
    private static final String STAT_QUERY_PATH_FILE = "classpath:///banners/banners-stat.query";
    private static final String FULL_QUERY_PATH_FILE = "classpath:///banners/banners-full.query";
    private static final String FULL_CREATIVE_FREE_QUERY_PATH_FILE =
            "classpath:///banners/banners-full-creative-free.query";
    private static final String FULL_QUERY_WITH_STATS_BY_DAYS_PATH_FILE =
            "classpath:///banners/banners-full-with-stats-by-days.query";
    private static final String FULL_CREATIVE_FREE_QUERY_WITH_STATS_BY_DAYS_PATH_FILE =
            "classpath:///banners/banners-full-creative-free-with-stats-by-days.query";
    private static final String FULL_WITHOUT_GOAL_REVENUE_QUERY_PATH_FILE =
            "classpath:///banners/banners-full-without-goal-revenue.query";

    @Mock
    private YtDynamicSupport ytSupport;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridBannerYtRepository gridBannerYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().add(RowBuilder.rowBuilder()
                .withColValue("bid", BANNER_ID)
                .withColValue("cost", 0)).build())
                .when(ytSupport).selectRows(eq(SHARD), any(Select.class), anyBoolean());
        doReturn(rowsetBuilder().add(RowBuilder.rowBuilder().withColValue("bid", BANNER_ID)).build())
                .when(ytSupport).selectRows(eq(SHARD), any(Select.class));

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
    }

    private static Object[] simpleQueryParameters() {
        return new Object[][]{
                {emptySet(), SIMPLE_QUERY_PATH_FILE},
                {singleton(FeatureName.CREATIVE_FREE_INTERFACE.getName()), SIMPLE_CREATIVE_FREE_QUERY_PATH_FILE}
        };
    }

    @Test
    @Parameters(method = "simpleQueryParameters")
    public void getBanners_Simple(Set<String> features, String queryPath) {
        String expectedQuery = LiveResourceFactory.get(queryPath).getContent();

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(singleton(BANNER_ID));

        gridBannerYtRepository.getBanners(SHARD, filter, EMPTY_ORDER_BY_LIST, DATE, DATE, null,
                LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, features, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    public void getBanners_Stat() {
        String expectedQuery = LiveResourceFactory.get(STAT_QUERY_PATH_FILE).getContent();

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(singleton(BANNER_ID))
                .withGoalStats(emptyList()); // если goalStats в фильтре != null, статистику нужно выбрать

        gridBannerYtRepository.getBanners(SHARD, filter, EMPTY_ORDER_BY_LIST, DATE, DATE, null,
                LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, emptySet(), false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    private static Object[] fullQueryParameters() {
        return new Object[][]{
                {emptySet(), FULL_QUERY_PATH_FILE, FULL_QUERY_WITH_STATS_BY_DAYS_PATH_FILE},
                {singleton(FeatureName.CREATIVE_FREE_INTERFACE.getName()), FULL_CREATIVE_FREE_QUERY_PATH_FILE,
                        FULL_CREATIVE_FREE_QUERY_WITH_STATS_BY_DAYS_PATH_FILE}
        };
    }

    @Test
    @Parameters(method = "fullQueryParameters")
    public void getBanners_Full(Set<String> features, String queryPath, String statsByDaysQueryPath) {
        String expectedQuery = LiveResourceFactory.get(queryPath).getContent();

        getBanners_Full(features, singleton(GOAL_ID), null);

        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture());

        List<Select> selects = queryArgumentCaptor.getAllValues();
        String query = selects.get(0).toString();
        compareQueries(expectedQuery, query);

        expectedQuery = LiveResourceFactory.get(statsByDaysQueryPath).getContent();
        query = selects.get(1).toString();
        compareQueries(expectedQuery, query);
    }

    @Test
    @Parameters(method = "fullQueryParameters")
    public void getBanners_Full_WithSameGoalsForRevenue(Set<String> features,
                                                        String queryPath, String statsByDaysQueryPath) {
        String expectedQuery = LiveResourceFactory.get(queryPath).getContent();

        getBanners_Full(features, singleton(GOAL_ID), singleton(GOAL_ID));

        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture());

        List<Select> selects = queryArgumentCaptor.getAllValues();
        String query = selects.get(0).toString();
        compareQueries(expectedQuery, query);

        expectedQuery = LiveResourceFactory.get(statsByDaysQueryPath).getContent();
        query = selects.get(1).toString();
        compareQueries(expectedQuery, query);
    }

    private static Object[] fullQueryWithoutRevenueParameters() {
        return new Object[][]{
                {FULL_WITHOUT_GOAL_REVENUE_QUERY_PATH_FILE, FULL_QUERY_WITH_STATS_BY_DAYS_PATH_FILE},
        };
    }

    @Test
    @Parameters(method = "fullQueryWithoutRevenueParameters")
    public void getBanners_Full_WithoutRevenue(String queryPath, String statsByDaysQueryPath) {
        String expectedQuery = LiveResourceFactory.get(queryPath).getContent();

        getBanners_Full(emptySet(), singleton(GOAL_ID), emptySet());

        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        verify(ytSupport, times(1))
                .selectRows(eq(SHARD), queryArgumentCaptor.capture());

        List<Select> selects = queryArgumentCaptor.getAllValues();
        String query = selects.get(0).toString();
        compareQueries(expectedQuery, query);

        expectedQuery = LiveResourceFactory.get(statsByDaysQueryPath).getContent();
        query = selects.get(1).toString();
        compareQueries(expectedQuery, query);
    }

    private void getBanners_Full(Set<String> features, Set<Long> goalIds, @Nullable Set<Long> goalIdsForRevenue) {
        GdRecommendationKey recom = new GdRecommendationKey()
                .withCid(10L)
                .withPid(20L)
                .withBid(30L);

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(singleton(BANNER_ID))
                .withBannerIdNotIn(singleton(22L))
                .withCampaignIdIn(singleton(44L))
                .withAdGroupIdIn(singleton(33L))
                .withTypeIn(singleton(BannersBannerType.image_ad))
                .withArchived(true)
                .withTitleContains("a")
                .withTitleNotContains("b")
                .withTitleNotIn(singleton("c"))
                .withTitleExtensionContains("d")
                .withTitleExtensionIn(singleton("e"))
                .withTitleNotContains("f")
                .withTitleExtensionNotIn(singleton("g"))
                .withBodyContains("h")
                .withBodyIn(singleton("i"))
                .withBodyNotContains("j")
                .withBodyNotIn(singleton("k"))
                .withHrefContains("l")
                .withHrefIn(singleton("m"))
                .withHrefNotContains("n")
                .withHrefNotIn(singleton("o"))
                .withInternalAdTitleContains(singleton("p"))
                .withInternalAdTitleIn(singleton("q"))
                .withInternalAdTitleNotContains(singleton("r"))
                .withInternalAdTitleNotIn(singleton("s"))
                .withTitleOrBodyContains("t")
                .withInternalAdTemplateIdIn(singleton(55L))
                .withExportIdIn(singleton(EXPORT_ID))
                .withExportIdNotIn(singleton(77L))
                .withExportIdContainsAny(singleton("88"))
                .withPrimaryStatusContains(singleton(GdiBannerPrimaryStatus.MODERATION))
                .withVcardExists(true)
                .withImageExists(true)
                .withSitelinksExists(true)
                .withTurbolandingsExist(true)
                .withStats(RepositoryUtil.buildStatsFilter())
                .withGoalStats(singletonList(RepositoryUtil.buildGoalStatFilter()))
                .withRecommendations(singletonList(recom));

        //Сортировка по всем полям баннеров и целей
        List<GdiBannerOrderBy> orderList = Stream.concat(
                Arrays.stream(GdiBannerOrderByField.values())
                        .filter(e -> e != PRIMARY_STATUS) // todo по этому полю сортировки нет DIRECT-92976
                        .map(e -> new GdiBannerOrderBy().withField(e).withOrder(DESC)),
                Stream.of(STAT_GOALS, STAT_CONVERSION_RATE, STAT_COST_PER_ACTION)
                        .map(e -> new GdiBannerOrderBy().withField(e).withOrder(DESC).withGoalId(GOAL_ID))
        ).collect(toList());

        gridBannerYtRepository.getBanners(SHARD, filter, orderList, DATE, DATE, true, LimitOffset.limited(LIMIT),
                goalIds, goalIdsForRevenue, DATE, DATE, AD_FETCHED_FIELDS_RESOLVER_FULL, features, false);
    }
}
