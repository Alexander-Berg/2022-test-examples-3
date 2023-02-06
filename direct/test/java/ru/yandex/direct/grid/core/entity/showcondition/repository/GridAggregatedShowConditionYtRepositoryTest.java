package ru.yandex.direct.grid.core.entity.showcondition.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.showcondition.model.GdiAggregatedShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiAggregatedShowConditionOrderBy;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiAggregatedShowConditionOrderByField;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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

public class GridAggregatedShowConditionYtRepositoryTest {

    private static final int SHARD = 1;
    private static final long CAMPAIGN_ID = 11L;
    private static final int LIMIT = 1000;
    private static final long GOAL_ID = 4L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final List<GdiAggregatedShowConditionOrderBy> EMPTY_ORDER_BY_LIST = emptyList();
    private static final String STAT_QUERY_PATH_FILE = "classpath:///showconditions/aggregatedshowconditions-stat.query";
    private static final String FULL_QUERY_PATH_FILE = "classpath:///showconditions/aggregatedshowconditions-full.query";

    @Mock
    private YtDynamicSupport ytSupport;

    @Mock
    private GridKeywordsParser keywordsParser;

    @Captor
    private ArgumentCaptor<Select> queryArgumentCaptor;

    @InjectMocks
    private GridAggregatedShowConditionYtRepository gridAggregatedShowConditionYtRepository;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(rowsetBuilder().build()).when(ytSupport)
                .selectRows(eq(SHARD), any(), anyBoolean());

        ytSupport.selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
    }

    @Test
    public void getAggregatedShowConditions_Stat() {
        String expectedQuery = LiveResourceFactory.get(STAT_QUERY_PATH_FILE).getContent();

        GdiAggregatedShowConditionFilter filter = new GdiAggregatedShowConditionFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        gridAggregatedShowConditionYtRepository.getAggregatedShowConditions(SHARD, filter, EMPTY_ORDER_BY_LIST,
                DATE, DATE, LimitOffset.limited(LIMIT), emptySet(), null);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }

    @Test
    public void getAggregatedShowConditions_Full() {
        String expectedQuery = LiveResourceFactory.get(FULL_QUERY_PATH_FILE).getContent();

        GdiAggregatedShowConditionFilter filter = new GdiAggregatedShowConditionFilter()
                .withCampaignIdIn(singleton(CAMPAIGN_ID));

        //Сортировка по всем полям условий показа и целей
        List<GdiAggregatedShowConditionOrderBy> orderList = Arrays.stream(GdiAggregatedShowConditionOrderByField.values())
                .map(e -> new GdiAggregatedShowConditionOrderBy().withField(e).withOrder(DESC))
                .collect(toList());

        gridAggregatedShowConditionYtRepository
                .getAggregatedShowConditions(SHARD, filter, orderList, DATE, DATE, LimitOffset.limited(LIMIT),
                        singleton(GOAL_ID), null);

        verify(ytSupport).selectRows(eq(SHARD), queryArgumentCaptor.capture(), eq(false));
        String query = queryArgumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }
}
