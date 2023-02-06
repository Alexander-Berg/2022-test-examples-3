package ru.yandex.direct.grid.core.entity.banner.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerFilter;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderBy;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannersWithTotals;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@ParametersAreNonnullByDefault
public class GridBannerYtRepositoryTotalStatsTest {

    private static final int SHARD = 1;
    private static final long BANNER_ID_1 = 51L;
    private static final long BANNER_ID_2 = 52L;
    private static final LocalDate DATE = LocalDate.of(2020, 1, 1);
    private static final List<GdiBannerOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final AdFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildAdFetchedFieldsResolver(false);
    private static final int LIMIT = 2;
    private static final Set<String> clientFeatures = Set.of(FeatureName.ADD_WITH_TOTALS_TO_BANNER_QUERY.getName());

    @Mock
    private YtDynamicSupport ytSupport;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridBannerYtRepository gridBannerYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().add(RowBuilder.rowBuilder().withColValue("bid", BANNER_ID_1)).build())
                .when(ytSupport).selectRows(eq(SHARD), any(Select.class), anyBoolean());
        doReturn(rowsetBuilder().add(RowBuilder.rowBuilder().withColValue("bid", BANNER_ID_2)).build())
                .when(ytSupport).selectRows(eq(SHARD), any(Select.class));

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(true));
    }

    /**
     * Если при запросе получаем количество баннеров равное лимиту - получаем итоговую статистику
     */
    @Test
    public void getBanners_GetOverlimit_WithTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(BANNER_ID_1, 5_000_000L),
                Pair.of(BANNER_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(Set.of(BANNER_ID_1, BANNER_ID_2))
                .withStats(RepositoryUtil.buildStatsFilter());

        GdiBannersWithTotals gdiBannersWithTotals = gridBannerYtRepository.getBanners(SHARD, filter,
                EMPTY_ORDER_BY_LIST, DATE, DATE, null, LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, clientFeatures, true);

        assertThat("должна вернуться итоговая статистика", gdiBannersWithTotals.getTotalStats(), notNullValue());
    }

    /**
     * Если при запросе в БД получаем количество баннеров меньше лимита -> итоговую статистику не получаем
     */
    @Test
    public void getBanners_GetUnderLimit_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(BANNER_ID_1, 5_000_000L),
                Pair.of(null, 5_000_000L)));

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(singleton(BANNER_ID_1))
                .withStats(RepositoryUtil.buildStatsFilter());
        GdiBannersWithTotals gdiBannersWithTotals = gridBannerYtRepository.getBanners(SHARD, filter,
                EMPTY_ORDER_BY_LIST, DATE, DATE, null, LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, clientFeatures, true);

        assertThat("нет итоговой статистики", gdiBannersWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если запрос в БД без получения статистики -> итоговую статистику не получаем
     */
    @Test
    public void getBanners_WithoutStats_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(BANNER_ID_1, 5_000_000L),
                Pair.of(BANNER_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(Set.of(BANNER_ID_1, BANNER_ID_2));
        GdiBannersWithTotals gdiBannersWithTotals = gridBannerYtRepository.getBanners(SHARD, filter,
                EMPTY_ORDER_BY_LIST, DATE, DATE, null, LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, clientFeatures, true);

        assertThat("нет итоговой статистики", gdiBannersWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если при запросе в БД получаем пустой список баннеров -> итоговую статистику не получаем
     */
    @Test
    public void getBanners_GetEmptyListOfAds_WithoutTotalStats() {
        doReturnFromYt(emptyList());

        GdiBannerFilter filter = new GdiBannerFilter()
                .withBannerIdIn(Set.of(BANNER_ID_1, BANNER_ID_2))
                .withStats(RepositoryUtil.buildStatsFilter());
        GdiBannersWithTotals gdiBannersWithTotals = gridBannerYtRepository.getBanners(SHARD, filter,
                EMPTY_ORDER_BY_LIST, DATE, DATE, null, LimitOffset.limited(LIMIT),
                emptySet(), null, null, null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, emptySet(), true);

        assertThat("нет итоговой статистики", gdiBannersWithTotals.getTotalStats(), nullValue());
    }

    private void doReturnFromYt(Collection<Pair<Long, Long>> bannerIdAndCosts) {
        doReturn(wrapInRowset(
                StreamEx.of(bannerIdAndCosts)
                        .map(bannerIdAndCost -> YTree.mapBuilder()
                                .key(BANNERSTABLE_DIRECT.BID.getName()).value(bannerIdAndCost.getLeft())
                                .key(GdiEntityStats.COST.name()).value(bannerIdAndCost.getRight())
                                .endMap().build()
                        ).toList()))
                .when(ytSupport).selectRows(eq(SHARD), any(), anyBoolean());
    }

    private UnversionedRowset wrapInRowset(List<YTreeNode> nodes) {
        UnversionedRowset rowset = mock(UnversionedRowset.class);
        doReturn(nodes).when(rowset).getYTreeRows();
        return rowset;
    }
}

