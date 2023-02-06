package ru.yandex.direct.grid.core.entity.showcondition.repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Description;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.RetargetingFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStatsFilter;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingBaseStatus;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingFilter;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingOrderBy;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingOrderByField;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.RepositoryUtil;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.grid.model.Order.DESC;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class GridRetargetingYtRepositoryTest {

    private static final int SHARD = 1;
    private static final long CAMPAIGN_ID = 11L;
    private static final long RET_COND_ID = 21L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(123L);
    private static final int LIMIT = 1000;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final List<GdiRetargetingOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final String SIMPLE_QUERY_PATH_FILE = "classpath:///showconditions/retargetings-simple.query";
    private static final String STAT_QUERY_PATH_FILE = "classpath:///showconditions/retargetings-stat.query";
    private static final String FILTER_CONDITION_QUERY_PATH_FILE = "classpath:///showconditions/retargetings-filter" +
            "-condition.query";
    private static final String FETCHED_CONDITION_QUERY_PATH_FILE = "classpath:///showconditions/retargetings-fetched" +
            "-condition.query";
    private static final String FULL_CONDITION_QUERY_PATH_FILE = "classpath:///showconditions/retargetings-full.query";

    @Mock
    private YtDynamicSupport ytSupport;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridRetargetingYtRepository gridRetargetingYtRepository;
    private RetargetingFetchedFieldsResolver fetchedFieldsResolver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().build()).when(ytSupport)
                .selectRows(eq(SHARD), any(), anyBoolean());

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));

        fetchedFieldsResolver = FetchedFieldsResolverCoreUtil.buildRetargetingFetchedFieldsResolver(false);
    }

    @Test
    @Description("Проверяем запрос без статистики и условий ретаргетинга")
    public void getRetargetings_Simple() {
        String expectedQuery = LiveResourceFactory.get(SIMPLE_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем запрос с фильтром по статистике")
    public void getRetargetings_WithFilterStat() {
        String expectedQuery = LiveResourceFactory.get(STAT_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID))
                .withStats(new GdiEntityStatsFilter());

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем запрос с запрошенной статистикой")
    public void getRetargetings_WithFetchedStat() {
        String expectedQuery = LiveResourceFactory.get(STAT_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        fetchedFieldsResolver.setStats(true);

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем запрос с фильтром по условиям ретаргетинга")
    public void getRetargetings_WithFilterRetargetingConditions() {
        String expectedQuery = LiveResourceFactory.get(FILTER_CONDITION_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(Set.of(CAMPAIGN_ID))
                .withNameContains("name");

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем запрос с запрошенными условиями ретаргетинга")
    public void getRetargetings_WithFetchedRetargetingConditions() {
        String expectedQuery = LiveResourceFactory.get(FETCHED_CONDITION_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(Set.of(CAMPAIGN_ID));

        fetchedFieldsResolver.setRetargetingCondition(true);

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver,
                EMPTY_ORDER_BY_LIST, DATE, DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    @Description("Проверяем запрос со статистикой и условиями ретаргетинга и всеми фильтрами")
    public void getRetargetings_WithStatAndRetargetingConditions() {
        String expectedQuery = LiveResourceFactory.get(FULL_CONDITION_QUERY_PATH_FILE).getContent();

        GdiRetargetingFilter filter = new GdiRetargetingFilter()
                .withCampaignIdIn(Set.of(CAMPAIGN_ID))
                .withAdGroupIdIn(Set.of(2L))
                .withRetargetingIdIn(Set.of(3L))
                .withRetargetingIdNotIn(Set.of(4L))
                .withRetargetingConditionIdIn(Set.of(RET_COND_ID))
                .withRetargetingConditionIdNotIn(Set.of(RET_COND_ID + 1L))
                .withNameContains("name")
                .withNameNotContains("another")
                .withMaxPriceContext(BigDecimal.valueOf(10))
                .withMinPriceContext(BigDecimal.valueOf(20))
                .withStatusIn(Set.of(GdiRetargetingBaseStatus.ACTIVE))
                .withInterest(true)
                .withStats(RepositoryUtil.buildStatsFilter());

        fetchedFieldsResolver
                .withRetargetingCondition(true)
                .withStats(true);

        List<GdiRetargetingOrderBy> orderList = Arrays.stream(GdiRetargetingOrderByField.values())
                .map(e -> new GdiRetargetingOrderBy()
                        .withField(e)
                        .withOrder(DESC))
                .collect(toList());

        gridRetargetingYtRepository.getRetargetings(SHARD, CLIENT_ID, filter, fetchedFieldsResolver, orderList, DATE,
                DATE,
                LimitOffset.limited(LIMIT), false, false);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }
}
