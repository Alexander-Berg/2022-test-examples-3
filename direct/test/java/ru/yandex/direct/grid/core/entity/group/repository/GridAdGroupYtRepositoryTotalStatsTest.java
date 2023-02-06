package ru.yandex.direct.grid.core.entity.group.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.multitype.entity.LimitOffset;
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
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;

public class GridAdGroupYtRepositoryTotalStatsTest {

    private static final int SHARD = 1;
    private static final long GROUP_ID_1 = 55L;
    private static final long GROUP_ID_2 = 56L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final AdGroupFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildAdGroupFetchedFieldsResolver(false);
    private static final int LIMIT = 2;

    @Mock
    private YtDynamicSupport ytSupport;
    @Mock
    private GridAdGroupYtRepository gridAdGroupYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        gridAdGroupYtRepository = new GridAdGroupYtRepository(ytSupport);
    }

    /**
     * Если при запросе получаем количество групп равное лимиту - получаем итоговую статистику
     */
    @Test
    public void getGroups_GetOverlimit_WithTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(GROUP_ID_1, 5_000_000L),
                Pair.of(GROUP_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(Set.of(GROUP_ID_1, GROUP_ID_2))
                .withStats(RepositoryUtil.buildStatsFilter());
        GdiGroupsWithTotals groupsWithTotals = gridAdGroupYtRepository.getGroups(SHARD, filter, emptyList(), DATE,
                DATE, LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true);

        assertThat("должна вернуться итоговая статистика", groupsWithTotals.getTotalStats(), notNullValue());
    }

    /**
     * Если при запросе в БД получаем количество групп меньше лимита -> итоговую статистику не получаем
     */
    @Test
    public void getGroups_GetUnderLimit_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(GROUP_ID_1, 5_000_000L),
                Pair.of(null, 5_000_000L)));

        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID_1))
                .withStats(RepositoryUtil.buildStatsFilter());
        GdiGroupsWithTotals groupsWithTotals = gridAdGroupYtRepository.getGroups(SHARD, filter, emptyList(), DATE,
                DATE, LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true);

        assertThat("нет итоговой статистики", groupsWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если запрос в БД без получения статистики -> итоговую статистику не получаем
     */
    @Test
    public void getGroups_WithoutStats_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(GROUP_ID_1, 5_000_000L),
                Pair.of(GROUP_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID_1));
        GdiGroupsWithTotals groupsWithTotals = gridAdGroupYtRepository.getGroups(SHARD, filter, emptyList(), DATE,
                DATE, LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true);

        assertThat("нет итоговой статистики", groupsWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если при запросе в БД получаем пустой список групп -> итоговую статистику не получаем
     */
    @Test
    public void getGroups_GetEmptyListOfGroups_WithoutTotalStats() {
        doReturnFromYt(emptyList());

        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID_1))
                .withStats(RepositoryUtil.buildStatsFilter());
        GdiGroupsWithTotals groupsWithTotals = gridAdGroupYtRepository.getGroups(SHARD, filter, emptyList(), DATE,
                DATE, LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true);

        assertThat("нет итоговой статистики", groupsWithTotals.getTotalStats(), nullValue());
    }

    private void doReturnFromYt(Collection<Pair<Long, Long>> groupIdAndCosts) {
        doReturn(wrapInRowset(
                StreamEx.of(groupIdAndCosts)
                        .map(groupIdAndCost -> YTree.mapBuilder()
                                .key(PHRASESTABLE_DIRECT.PID.getName()).value(groupIdAndCost.getLeft())
                                .key(GdiEntityStats.COST.name()).value(groupIdAndCost.getRight())
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
