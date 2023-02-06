package ru.yandex.direct.grid.core.entity.showcondition.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionWithTotals;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;

public class GridShowConditionYtRepositoryTotalStatsTest {
    private static final int SHARD = 1;
    private static final long SHOW_CONDITION_ID_1 = 55L;
    private static final long SHOW_CONDITION_ID_2 = 56L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final ShowConditionFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(false);
    private static final LimitOffset limitOffset = LimitOffset.limited(2);

    @Mock
    private YtDynamicSupport ytSupport;
    @InjectMocks
    private GridShowConditionYtRepository gridShowConditionYtRepository;
    @Mock
    private GridKeywordsParser keywordsParser;

    private GdiShowConditionFilter filter;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        filter = new GdiShowConditionFilter()
                .withShowConditionIdIn(Set.of(SHOW_CONDITION_ID_1, SHOW_CONDITION_ID_2))
                .withStats(RepositoryUtil.buildStatsFilter());
    }

    /**
     * Если при запросе получаем количество условий показа равное лимиту - получаем итоговую статистику
     */
    @Test
    public void getShowConditions_GetOverlimit_WithTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(SHOW_CONDITION_ID_1, 5_000_000L),
                Pair.of(SHOW_CONDITION_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiShowConditionWithTotals showConditionWithTotals = gridShowConditionYtRepository
                .getShowConditions(SHARD, filter, emptyList(), DATE, DATE,
                        limitOffset, emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true, true);

        assertThat("должна вернуться итоговая статистика", showConditionWithTotals.getTotalStats(), notNullValue());
    }

    /**
     * Если при запросе в БД получаем количество условий показа меньше лимита -> итоговую статистику не получаем
     */
    @Test
    public void getShowConditions_GetUnderLimit_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(SHOW_CONDITION_ID_1, 5_000_000L),
                Pair.of(null, 5_000_000L)));

        GdiShowConditionWithTotals showConditionWithTotals = gridShowConditionYtRepository
                .getShowConditions(SHARD, filter, emptyList(), DATE, DATE,
                        limitOffset, emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true, true);

        assertThat("нет итоговой статистики", showConditionWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если запрос в БД без получения статистики -> итоговую статистику не получаем
     */
    @Test
    public void getShowConditions_WithoutStats_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(SHOW_CONDITION_ID_1, 5_000_000L),
                Pair.of(SHOW_CONDITION_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiShowConditionFilter filterWithoutStats = new GdiShowConditionFilter()
                .withShowConditionIdIn(Set.of(SHOW_CONDITION_ID_1, SHOW_CONDITION_ID_2));

        GdiShowConditionWithTotals showConditionWithTotals = gridShowConditionYtRepository
                .getShowConditions(SHARD, filterWithoutStats, emptyList(), DATE, DATE,
                        limitOffset, emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true, true);

        assertThat("нет итоговой статистики", showConditionWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если при запросе в БД получаем пустой список условий показа -> итоговую статистику не получаем
     */
    @Test
    public void getShowConditions_GetEmptyListOfShowConditions_WithoutTotalStats() {
        doReturnFromYt(emptyList());

        GdiShowConditionWithTotals gdiShowConditionWithTotals = gridShowConditionYtRepository
                .getShowConditions(SHARD, new GdiShowConditionFilter(), emptyList(), DATE, DATE,
                        limitOffset, emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, true, true);

        assertThat("нет итоговой статистики", gdiShowConditionWithTotals.getTotalStats(), nullValue());
    }

    private void doReturnFromYt(Collection<Pair<Long, Long>> showConditionIdAndCosts) {
        doReturn(wrapInRowset(
                StreamEx.of(showConditionIdAndCosts)
                        .map(showConditionIdAndCost -> YTree.mapBuilder()
                                .key(BIDSTABLE_DIRECT.ID.getName()).value(showConditionIdAndCost.getLeft())
                                .key(BIDSTABLE_DIRECT.BID_TYPE.getName()).value(GdiShowConditionType.RELEVANCE_MATCH.name().toLowerCase())
                                .key(GdiEntityStats.COST.name()).value(showConditionIdAndCost.getRight())
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
