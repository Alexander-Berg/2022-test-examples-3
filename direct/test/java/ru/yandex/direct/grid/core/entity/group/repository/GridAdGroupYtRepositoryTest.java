package ru.yandex.direct.grid.core.entity.group.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Description;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupOrderBy;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupOrderByField;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupPrimaryStatus;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendationSort;
import ru.yandex.direct.grid.core.entity.showcondition.model.GridShowConditionType;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.grid.core.entity.group.model.GdiGroupOrderByField.STAT_CONVERSION_RATE;
import static ru.yandex.direct.grid.core.entity.group.model.GdiGroupOrderByField.STAT_COST_PER_ACTION;
import static ru.yandex.direct.grid.core.entity.group.model.GdiGroupOrderByField.STAT_GOALS;
import static ru.yandex.direct.grid.core.entity.showcondition.model.GridShowConditionType.KEYWORD;
import static ru.yandex.direct.grid.model.Order.DESC;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class GridAdGroupYtRepositoryTest {

    private static final int SHARD = 1;
    private static final long CAMPAIGN_ID = 11L;
    private static final long GROUP_ID = 22L;
    private static final long GOAL_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final AdGroupFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_SIMPLE =
            FetchedFieldsResolverCoreUtil.buildAdGroupFetchedFieldsResolver(false);
    private static final AdGroupFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER_FULL =
            FetchedFieldsResolverCoreUtil.buildAdGroupFetchedFieldsResolver(true);
    private static final int LIMIT = 1000;
    private static final Gson GSON = new Gson();

    @Mock
    private YtDynamicSupport ytSupport;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridAdGroupYtRepository gridAdGroupYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().build()).when(ytSupport)
                .selectRows(eq(SHARD), any(), anyBoolean());

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), anyBoolean());
    }

    @Test
    public void getGroups_Simple() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID));

        checkQuery("groups-simple.query", filter);
    }

    @Test
    public void getGroups_emptyShowConditionTypes() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID))
                .withShowConditionTypeIn(emptySet());

        checkQuery("groups-simple.query", filter);
    }

    @Test
    public void getGroups_oneShowConditionType() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID))
                .withShowConditionTypeIn(singleton(KEYWORD));

        checkQuery("groups-one-showconditontype.query", filter);
    }

    @Test
    public void getGroups_allShowConditionTypes() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID))
                .withShowConditionTypeIn(EnumSet.allOf(GridShowConditionType.class));

        checkQuery("groups-all-showconditontypes.query", filter);
    }

    @Test
    @Description("Проверяем наличие статистики при наличии фильтра на нее")
    public void getGroups_Stat() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(GROUP_ID))
                .withGoalStats(emptyList()); // если goalStats в фильтре != null, статистику нужно выбрать

        checkQuery("groups-stat.query", filter);

    }

    @Test
    public void getGroups_Full() {
        checkFullQuery("groups-full.query", singleton(GOAL_ID), null);
    }

    @Test
    public void getGroups_Full_WithSameGoalsForRevenue() {
        checkFullQuery("groups-full.query", singleton(GOAL_ID), singleton(GOAL_ID));
    }

    @Test
    public void getGroups_Full_WithoutRevenue() {
        checkFullQuery("groups-full-without-goal-revenue.query", singleton(GOAL_ID), emptySet());
    }

    private void checkFullQuery(String queryPathFile, Set<Long> goalIds, @Nullable Set<Long> goalIdsForRevenue) {
        GdiRecommendation recom = new GdiRecommendation()
                .withCid(CAMPAIGN_ID)
                .withPid(GROUP_ID)
                .withKpi(GSON.toJson(
                        new GdiRecommendationSort()
                                .withClicks(BigDecimal.valueOf(40))
                                .withCost(BigDecimal.valueOf(50))));

        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(33L))
                .withGroupIdNotIn(singleton(33L))
                .withCampaignIdIn(singleton(44L))
                .withArchived(true)
                .withTypeIn(singleton(AdGroupType.DYNAMIC))
                .withPrimaryStatusIn(singleton(GdiGroupPrimaryStatus.MODERATION))
                .withNameContains("a")
                .withNameIn(singleton("b"))
                .withNameNotContains("c")
                .withNameNotIn(singleton("d"))
                .withStats(RepositoryUtil.buildStatsFilter())
                .withGoalStats(singletonList(RepositoryUtil.buildGoalStatFilter()))
                .withRecommendations(singletonList(recom));

        //Сортировка по всем полям групп и целей
        List<GdiGroupOrderBy> orderList = Stream.concat(
                Arrays.stream(GdiGroupOrderByField.values())
                        .map(e -> new GdiGroupOrderBy().withField(e).withOrder(DESC)),
                Stream.of(STAT_GOALS, STAT_CONVERSION_RATE, STAT_COST_PER_ACTION)
                        .map(e -> new GdiGroupOrderBy().withField(e).withOrder(DESC).withGoalId(GOAL_ID))
        ).collect(toList());

        checkQuery(queryPathFile, filter, orderList, AD_FETCHED_FIELDS_RESOLVER_FULL, goalIds, goalIdsForRevenue);
    }

    private void checkQuery(String queryPathFile, GdiGroupFilter filter) {
        checkQuery(queryPathFile, filter, emptyList(), AD_FETCHED_FIELDS_RESOLVER_SIMPLE, emptySet(), null);
    }

    private void checkQuery(String queryPathFile, GdiGroupFilter filter, List<GdiGroupOrderBy> orderList,
                            AdGroupFetchedFieldsResolver fields,
                            Set<Long> goalIds, @Nullable Set<Long> goalIdsForRevenue) {
        String expectedQuery = LiveResourceFactory.get("classpath:///groups/" + queryPathFile).getContent();

        gridAdGroupYtRepository.getGroups(SHARD, filter, orderList, DATE, DATE, LimitOffset.limited(LIMIT), goalIds,
                goalIdsForRevenue, fields, true);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), anyBoolean());
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }
}
