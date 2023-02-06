package ru.yandex.direct.grid.core.entity.smartfilter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.smartfilter.model.GdiSmartFilter;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class GridSmartFilterYtRepositoryTest {

    @Mock
    private YtDynamicSupport ytSupport;

    @InjectMocks
    private GridSmartFilterYtRepository gridSmartFilterYtRepository;

    @Captor
    private ArgumentCaptor<Select> argumentCaptor;

    private final String startOfQueryTemplate = "SELECT \n"
            + "  S.ExportID AS ExportID, \n"
            + "  S.GroupExportID AS GroupExportID, \n"
            + "  S.PhraseExportID AS PhraseExportID, \n"
            + "  IF(is_null(sum(S.Shows)), 0, sum(S.Shows)) AS shows, \n"
            + "  IF(is_null(sum(S.Clicks)), 0, sum(S.Clicks)) AS clicks, \n"
            + "  IF(is_null(sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))), 0, sum(IF(S.CurrencyID" +
            " = 0, ((S.Cost * 100) / 118), S.CostTaxFree))) AS cost, \n"
            + "  IF(is_null(sum(S.CostCur)), 0, sum(S.CostCur)) AS costWithTax, \n"
            + "  IF(is_null(sum(S.PriceCur)), 0, sum(S.PriceCur)) AS revenue, \n"
            + "  IF(is_null(sum(S.GoalsNum)), 0, sum(S.GoalsNum)) AS goals, \n"
            + "  IF(shows > 0, ((cost / shows) * 1000), 0) AS cpmPrice, \n"
            + "  IF(is_null(sum(S.FirstPageClicks)), 0, sum(S.FirstPageClicks)) AS firstPageClicks, \n"
            + "  IF(is_null(sum(S.FirstPageShows)), 0, sum(S.FirstPageShows)) AS firstPageShows, \n"
            + "  IF(is_null(sum(S.FirstPageSumPosClicks)), 0, sum(S.FirstPageSumPosClicks)) AS firstPageSumPosClicks," +
            " \n"
            + "  IF(is_null(sum(S.FirstPageSumPosShows)), 0, sum(S.FirstPageSumPosShows)) AS firstPageSumPosShows, \n"
            + "  IF(is_null(sum(IF(S.IsRMP, S.Clicks, S.SessionNum))), 0, sum(IF(S.IsRMP, S.Clicks, S.SessionNum))) " +
            "AS sessions, \n"
            + "  IF(is_null(sum(S.SessionNumLimited)), 0, sum(S.SessionNumLimited)) AS sessionsLimited, \n"
            + "  IF(is_null(sum(S.Bounces)), 0, sum(S.Bounces)) AS bounces, \n"
            + "  IF(is_null(sum(S.SessionDepth)), 0, sum(S.SessionDepth)) AS sessionDepth, \n"
            + "  IF(shows > 0, ((clicks * 100000000) / shows), 0) AS ctr, \n"
            + "  IF(clicks > 0, (cost / clicks), 0) AS avgClickCost, \n"
            + "  IF(firstPageShows > 0, ((firstPageSumPosShows * 1000000) / firstPageShows), 0) AS avgShowPosition, \n"
            + "  IF(firstPageClicks > 0, ((firstPageSumPosClicks * 1000000) / firstPageClicks), 0) AS " +
            "avgClickPosition, \n"
            + "  IF(sessionsLimited > 0, ((bounces * 100000000) / sessionsLimited), 0) AS bounceRate, \n"
            + "  IF(sessionsLimited > 0, ((sessionDepth * 1000000) / sessionsLimited), 0) AS avgDepth, \n"
            + "  IF(clicks > 0, ((goals * 100000000) / clicks), 0) AS conversionRate, \n"
            + "  IF(goals > 0, (cost / goals), null) AS avgGoalCost, \n"
            + "  IF((\n"
            + "    cost > 0\n"
            + "    AND revenue > 0\n"
            + "  ), ((double((revenue - cost)) * 1000000.0) / double(cost)), null) AS profitability, \n"
            + "  IF((\n"
            + "    cost > 0\n"
            + "    AND revenue > 0\n"
            + "  ), ((double(cost) * 100000000.0) / double(revenue)), null) AS crr\n";

    private final String statsWithoutGoalsTemplate = "FROM yt.DirectPhraseStatV2_bs AS S\n";

    private final String statsWithGoalsTemplate =
            "IF(is_null(sum(GS%s.GoalsNum)), 0, sum(GS%s.GoalsNum)) AS goals%s, \n"
                    + "  IF(clicks > 0, ((goals%s * 100000000) / clicks), 0) AS conversionRate%s, \n"
                    + "  IF(goals%s > 0, (cost / goals%s), 0) AS costPerAction%s\n"
                    + "  IF(is_null(sum(GS%s.PriceCur)), 0, sum(GS%s.PriceCur)) AS revenue%s\n"
                    + "  IF(is_null(sum(GS1.WithShowsGoalsNum)), 0, sum(GS1.WithShowsGoalsNum)) AS goalsWithShows1, \n"
                    + "FROM yt.DirectPhraseStatV2_bs AS S\n"
                    + "  LEFT OUTER JOIN yt.DirectPhraseGoalsStat_bs AS GS%s\n"
                    + "  ON (S.ExportID, S.GroupExportID, S.UpdateTime, S.PhraseExportID, S.PhraseID, "
                    + "S.DirectBannerID, S.GoalContextID, S.IsFlat, S.IsMobile, S.CurrencyID, S.IsRMP,"
                    + " IF(is_null(S.AutobudgetStrategyID), -1, S.AutobudgetStrategyID)"
                    + ", %s) = "
                    + "(GS%s.ExportID, GS%s.GroupExportID, GS%s.UpdateTime, GS%s.PhraseExportID, "
                    + "GS%s.PhraseID, GS%s.DirectBannerID, GS%s.GoalContextID, GS%s.IsFlat, GS%s.IsMobile, "
                    + "GS%s.CurrencyID, GS%s.IsRMP, "
                    + "IF(is_null(GS%s.AutobudgetStrategyID), -1, GS%s.AutobudgetStrategyID), GS%s.GoalID)\n";

    private final String endOfQueryTemplate = "WHERE (\n"
            + "  (ExportID, GroupExportID, PhraseExportID) IN ((%s, %s, %s))\n"
            + "  AND S.UpdateTime IN (\n"
            + "    %s, %s, %s\n"
            + "  )\n"
            + ")\n"
            + "GROUP BY \n"
            + "  ExportID, \n"
            + "  GroupExportID, \n"
            + "  PhraseExportID";

    private static final Long SMART_FILTER_ID = 1L;
    private static final Long AD_GROUP_ID = 2L;
    private static final Long CAMPAIGN_ID = 3L;
    private static final long GOAL_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2019, 1, 1);
    private static final String FULL_QUERY_PATH = "classpath:///smartfilters/smartfilters-full.query";
    private List<GdiSmartFilter> gdiSmartFilters;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(rowsetBuilder().build()).when(ytSupport).selectRows(any(Select.class));

        GdiSmartFilter smartFilter = new GdiSmartFilter()
                .withSmartFilterId(SMART_FILTER_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID);

        gdiSmartFilters = Collections.singletonList(smartFilter);
    }

    @Test
    public void getStatistic_withoutGoals() {
        Set<Long> goalIds = new HashSet<>();
        createQueryAndCheck(goalIds);
    }

    @Test
    public void getStatistic_withGoals() {
        //Проверяем построение запроса только для одной цели
        Set<Long> goalIds = ImmutableSet.of(GOAL_ID);
        createQueryAndCheck(goalIds);
    }

    private void createQueryAndCheck(Set<Long> goalIds) {
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();

        Long day1 = from.atStartOfDay(MSK).toEpochSecond();
        Long day2 = from.plusDays(1).atStartOfDay(MSK).toEpochSecond();
        Long day3 = from.plusDays(2).atStartOfDay(MSK).toEpochSecond();

        String queryTemplate = constructQueryTemplate(goalIds);

        String expectedQuery = String.format(queryTemplate, CAMPAIGN_ID, AD_GROUP_ID, SMART_FILTER_ID,
                day1, day2, day3);

        gridSmartFilterYtRepository.getStatistic(gdiSmartFilters, from, to, goalIds, null);
        verify(ytSupport).selectRows(argumentCaptor.capture());

        String query = argumentCaptor.getValue().toString();
        compareQueries(expectedQuery, query);

    }

    private String constructQueryTemplate(Set<Long> goalIds) {
        if (goalIds.isEmpty()) {
            return startOfQueryTemplate + statsWithoutGoalsTemplate + endOfQueryTemplate;
        } else {
            return startOfQueryTemplate
                    + statsWithGoalsTemplate.replaceAll("%s", goalIds.toArray()[0].toString())
                    + endOfQueryTemplate;
        }
    }

    @Test
    public void getShowConditions_Full() {
        String expectedQuery = LiveResourceFactory.get(FULL_QUERY_PATH).getContent();

        gridSmartFilterYtRepository.getStatistic(gdiSmartFilters, DATE, DATE, singleton(GOAL_ID), null);

        verify(ytSupport).selectRows(argumentCaptor.capture());
        String query = argumentCaptor.getValue().toString();

        compareQueries(expectedQuery, query);
    }
}
