package ru.yandex.direct.grid.core.entity.showcondition.repository;

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

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.RetargetingFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargetingWithTotals;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT;

public class GridRetargetingYtRepositoryTotalStatsTest {
    private static final int SHARD = 1;
    private static final ClientId CLIENT_ID = ClientId.fromLong(529L);
    private static final long RETARGETING_ID_1 = 37L;
    private static final long RETARGETING_ID_2 = 38L;
    private static final LocalDate DATE = LocalDate.of(2020, 1, 1);
    private static final int LIMIT = 2;

    @Mock
    private YtDynamicSupport ytSupport;
    @Mock
    private GridRetargetingYtRepository retargetingYtRepository;

    private RetargetingFetchedFieldsResolver fetchedFieldsResolver;
    private GdiRetargetingFilter filter;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        retargetingYtRepository = new GridRetargetingYtRepository(ytSupport);
        fetchedFieldsResolver = FetchedFieldsResolverCoreUtil.buildRetargetingFetchedFieldsResolver(false);

        filter = new GdiRetargetingFilter()
                .withRetargetingIdIn(Set.of(RETARGETING_ID_1, RETARGETING_ID_2))
                .withStats(RepositoryUtil.buildStatsFilter());
    }

    /**
     * Если при запросе получаем количество ретаргетингов равное лимиту - получаем итоговую статистику
     */
    @Test
    public void getRetargetings_GetOverlimit_WithTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(RETARGETING_ID_1, 5_000_000L),
                Pair.of(RETARGETING_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        GdiBidsRetargetingWithTotals retargetingsWithTotals = retargetingYtRepository
                .getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver, emptyList(), DATE,
                        DATE, LimitOffset.limited(LIMIT), true, true);

        assertThat("должна вернуться итоговая статистика", retargetingsWithTotals.getTotalStats(), notNullValue());
    }

    /**
     * Если при запросе в БД получаем количество ретаргетингов меньше лимита -> итоговую статистику не получаем
     */
    @Test
    public void getRetargetings_GetUnderLimit_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(RETARGETING_ID_1, 5_000_000L),
                Pair.of(null, 5_000_000L)));

        GdiBidsRetargetingWithTotals retargetingsWithTotals = retargetingYtRepository
                .getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver, emptyList(), DATE,
                        DATE, LimitOffset.limited(LIMIT), true, true);

        assertThat("нет итоговой статистики", retargetingsWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если запрос в БД без получения статистики -> итоговую статистику не получаем
     */
    @Test
    public void getRetargetings_WithoutStats_WithoutTotalStats() {
        doReturnFromYt(List.of(
                Pair.of(RETARGETING_ID_1, 5_000_000L),
                Pair.of(RETARGETING_ID_2, 15_000_000L),
                Pair.of(null, 20_000_000L)));

        var filter = new GdiRetargetingFilter()
                .withRetargetingIdIn(Set.of(RETARGETING_ID_1, RETARGETING_ID_2));
        GdiBidsRetargetingWithTotals retargetingsWithTotals = retargetingYtRepository
                .getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver, emptyList(), DATE,
                        DATE, LimitOffset.limited(LIMIT), true, true);

        assertThat("нет итоговой статистики", retargetingsWithTotals.getTotalStats(), nullValue());
    }

    /**
     * Если при запросе в БД получаем пустой список ретаргетингов -> итоговую статистику не получаем
     */
    @Test
    public void getRetargetings_GetEmptyListOfRetargetings_WithoutTotalStats() {
        doReturnFromYt(emptyList());

        GdiBidsRetargetingWithTotals retargetingsWithTotals = retargetingYtRepository
                .getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver, emptyList(), DATE,
                        DATE, LimitOffset.limited(LIMIT), true, true);

        assertThat("нет итоговой статистики", retargetingsWithTotals.getTotalStats(), nullValue());
    }

    private void doReturnFromYt(Collection<Pair<Long, Long>> retargetingIdAndCosts) {
        doReturn(wrapInRowset(
                StreamEx.of(retargetingIdAndCosts)
                        .map(retargetingIdAndCost -> YTree.mapBuilder()
                                .key(BIDS_RETARGETINGTABLE_DIRECT.RET_ID.getName()).value(retargetingIdAndCost.getLeft())
                                .key(GdiEntityStats.COST.name()).value(retargetingIdAndCost.getRight())
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
