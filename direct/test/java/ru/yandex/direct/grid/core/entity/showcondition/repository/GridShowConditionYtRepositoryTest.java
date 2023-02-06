package ru.yandex.direct.grid.core.entity.showcondition.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Description;

import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowCondition;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionBaseStatus;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderBy;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderByField;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionPrimaryStatus;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionWithTotals;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderByField.STAT_CONVERSION_RATE;
import static ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderByField.STAT_COST_PER_ACTION;
import static ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionOrderByField.STAT_GOALS;
import static ru.yandex.direct.grid.model.Order.DESC;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.utils.CollectionUtils.orderedSet;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class GridShowConditionYtRepositoryTest {

    private static final int SHARD = 1;
    private static final long CAMPAIGN_ID = 11L;
    private static final long SHOW_CONDITION_ID = 111L;
    private static final long GOAL_ID = 1L;
    private static final int LIMIT = 1000;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final List<GdiShowConditionOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final Set<GdiShowConditionPrimaryStatus> primaryStatuses =
            new TreeSet<>(Set.of(GdiShowConditionPrimaryStatus.ACTIVE, GdiShowConditionPrimaryStatus.ARCHIVED));
    private static final ShowConditionFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(false);
    private static final ShowConditionFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_FULL =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(true);
    private static final String SIMPLE_QUERY_PATH_FILE = "classpath:///showconditions/showconditions-simple.query";
    private static final String STAT_QUERY_PATH_FILE = "classpath:///showconditions/showconditions-stat.query";
    private static final String FULL_QUERY_PATH_FILE = "classpath:///showconditions/showconditions-full.query";
    private static final String FULL_WITHOUT_GOAL_REVENUE_QUERY_PATH_FILE =
            "classpath:///showconditions/showconditions-full-without-goal-revenue.query";

    @Mock
    private YtDynamicSupport ytSupport;

    @Mock
    private GridKeywordsParser keywordsParser;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridShowConditionYtRepository gridShowConditionYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().build()).when(ytSupport)
                .selectRows(eq(SHARD), any(), anyBoolean());

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
    }

    @Test
    public void getShowConditions_Simple() {
        String expectedQuery = LiveResourceFactory.get(SIMPLE_QUERY_PATH_FILE).getContent();

        GdiShowConditionFilter filter = new GdiShowConditionFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        gridShowConditionYtRepository.getShowConditions(SHARD, filter, EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем, что простой запрос без статистики отрабатывает даже если переданы цели")
    public void getShowConditions_SimpleWithGoals() {
        UnversionedRowset rowset = rowsetBuilder().add(
                rowBuilder().withColValue(GdiShowCondition.ID.name(), SHOW_CONDITION_ID)
        ).build();
        when(ytSupport.selectRows(eq(SHARD), any(), anyBoolean())).thenReturn(rowset);

        GdiShowConditionFilter filter = new GdiShowConditionFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        GdiShowConditionWithTotals showConditions = gridShowConditionYtRepository.getShowConditions(SHARD, filter,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), singleton(GOAL_ID), singleton(GOAL_ID),
                AD_FETCHED_FIELDS_RESOLVER_SIMPLE, false, false);
        assertThat(showConditions.getGdiShowConditions()).hasSize(1);
        assertThat(showConditions.getGdiShowConditions().get(0).getId()).isEqualTo(SHOW_CONDITION_ID);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        String expectedQuery = LiveResourceFactory.get(SIMPLE_QUERY_PATH_FILE).getContent();
        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем наличие статистики при наличии фильтра на нее")
    public void getShowConditions_Stat() {
        String expectedQuery = LiveResourceFactory.get(STAT_QUERY_PATH_FILE).getContent();

        GdiShowConditionFilter filter = new GdiShowConditionFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID))
                .withGoalStats(emptyList()); // если goalStats в фильтре != null, статистику нужно выбрать

        gridShowConditionYtRepository.getShowConditions(SHARD, filter, EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), emptySet(), null, AD_FETCHED_FIELDS_RESOLVER_SIMPLE, false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    public void getShowConditions_Full() {
        String expectedQuery = LiveResourceFactory.get(FULL_QUERY_PATH_FILE).getContent();

        getShowConditions_Full(singleton(GOAL_ID), null);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    public void getShowConditions_WithSameGoalsForRevenue() {
        String expectedQuery = LiveResourceFactory.get(FULL_QUERY_PATH_FILE).getContent();

        getShowConditions_Full(singleton(GOAL_ID), singleton(GOAL_ID));

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    public void getShowConditions_WithoutRevenue() {
        String expectedQuery = LiveResourceFactory.get(FULL_WITHOUT_GOAL_REVENUE_QUERY_PATH_FILE).getContent();

        getShowConditions_Full(singleton(GOAL_ID), emptySet());

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    private void getShowConditions_Full(Set<Long> goalIds, @Nullable Set<Long> goalIdsForRevenue) {
        GdiShowConditionFilter filter = new GdiShowConditionFilter()
                .withShowConditionIdIn(singleton(11L))
                .withShowConditionIdNotIn(singleton(22L))
                .withCampaignIdIn(singleton(CAMPAIGN_ID))
                .withAdGroupIdIn(singleton(33L))
                .withKeywordContains("a")
                .withKeywordIn(singleton("b"))
                .withKeywordNotContains("c")
                .withKeywordNotIn(singleton("d"))
                .withKeywordWithoutMinusWordsContains(orderedSet(orderedSet("e", "f"), orderedSet("g", "h")))
                .withMinusWordsContains(orderedSet(orderedSet("i", "j"), orderedSet("k", "l")))
                .withMinPrice(BigDecimal.valueOf(44))
                .withMaxPrice(BigDecimal.valueOf(55))
                .withMinPriceContext(BigDecimal.valueOf(66))
                .withMaxPriceContext(BigDecimal.valueOf(77))
                .withAutobudgetPriorityIn(singleton(GdiShowConditionAutobudgetPriority.MEDIUM))
                .withStatusIn(singleton(GdiShowConditionBaseStatus.ACTIVE))
                .withShowConditionStatusIn(primaryStatuses)
                .withTypeIn(singleton(GdiShowConditionType.KEYWORD))
                .withStats(RepositoryUtil.buildStatsFilter())
                .withGoalStats(singletonList(RepositoryUtil.buildGoalStatFilter()));

        //Сортировка по всем полям условий показа и целей
        List<GdiShowConditionOrderBy> orderList = Stream.concat(
                Arrays.stream(GdiShowConditionOrderByField.values())
                        .map(e -> new GdiShowConditionOrderBy().withField(e).withOrder(DESC)),
                Stream.of(STAT_GOALS, STAT_CONVERSION_RATE, STAT_COST_PER_ACTION)
                        .map(e -> new GdiShowConditionOrderBy().withField(e).withOrder(DESC).withGoalId(GOAL_ID))
        ).collect(toList());

        gridShowConditionYtRepository
                .getShowConditions(SHARD, filter, orderList, DATE, DATE, LimitOffset.limited(LIMIT),
                        goalIds, goalIdsForRevenue, AD_FETCHED_FIELDS_RESOLVER_FULL, false, false);
    }
}
