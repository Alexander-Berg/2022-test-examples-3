package ru.yandex.direct.grid.core.util.stats;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.junit.Test;

import ru.yandex.direct.grid.core.util.stats.completestat.DirectPhraseStatData;
import ru.yandex.direct.grid.schema.yt.tables.DirectphrasegoalsstatBs;
import ru.yandex.direct.grid.schema.yt.tables.Directphrasestatv2Bs;

import static org.assertj.core.api.Assertions.assertThat;

public class GridStatNewTest {
    private static final List<Long> CAMPAIGNS = Collections.singletonList(123L);
    private static final List<Long> GROUP = Arrays.asList(126L, 180L);
    private static final Map<Long, Long> SUBCAMPAIGNS = new LinkedHashMap<>() {{
       put(124L, 123L);
       put(125L, 123L);
    }}; // гарантируем, что порядок элементов не изменится от запуска к запуску
    private static final LocalDate TEST_FROM = LocalDate.parse("2017-10-10");
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(4);

    private final GridStatNew<Directphrasestatv2Bs, DirectphrasegoalsstatBs> gridStat
            = new GridStatNew<>(DirectPhraseStatData.INSTANCE);

    @Test
    public void testConstructStatSelect() {
        Field<Long> statGroupId = gridStat.getTableData().table().GROUP_EXPORT_ID.as("ourGroupId");
        Field<Long> statCampaignId = gridStat.getTableData().table().EXPORT_ID.as("ourCampaignId");

        Select<?> query = gridStat.constructStatSelect(
                Arrays.asList(statCampaignId, statGroupId),
                statCampaignId.in(CAMPAIGNS).and(statGroupId.in(GROUP)),
                TEST_FROM, TEST_TO, Collections.emptySet(), null);

        String expected = "SELECT \n"
                + "  S.ExportID AS ourCampaignId, \n"
                + "  S.GroupExportID AS ourGroupId, \n"
                + "  IF(is_null(sum(S.Shows)), 0, sum(S.Shows)) AS shows, \n"
                + "  IF(is_null(sum(S.Clicks)), 0, sum(S.Clicks)) AS clicks, \n"
                + "  IF(is_null(sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))), 0, sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))) AS cost, \n"
                + "  IF(is_null(sum(S.CostCur)), 0, sum(S.CostCur)) AS costWithTax, \n"
                + "  IF(is_null(sum(S.PriceCur)), 0, sum(S.PriceCur)) AS revenue, \n"
                + "  IF(is_null(sum(S.GoalsNum)), 0, sum(S.GoalsNum)) AS goals, \n"
                + "  IF(shows > 0, ((cost / shows) * 1000), 0) AS cpmPrice, \n"
                + "  IF(is_null(sum(S.FirstPageClicks)), 0, sum(S.FirstPageClicks)) AS firstPageClicks, \n"
                + "  IF(is_null(sum(S.FirstPageShows)), 0, sum(S.FirstPageShows)) AS firstPageShows, \n"
                + "  IF(is_null(sum(S.FirstPageSumPosClicks)), 0, sum(S.FirstPageSumPosClicks)) AS firstPageSumPosClicks, \n"
                + "  IF(is_null(sum(S.FirstPageSumPosShows)), 0, sum(S.FirstPageSumPosShows)) AS firstPageSumPosShows, \n"
                + "  IF(is_null(sum(IF(S.IsRMP, S.Clicks, S.SessionNum))), 0, sum(IF(S.IsRMP, S.Clicks, S.SessionNum))) AS sessions, \n"
                + "  IF(is_null(sum(S.SessionNumLimited)), 0, sum(S.SessionNumLimited)) AS sessionsLimited, \n"
                + "  IF(is_null(sum(S.Bounces)), 0, sum(S.Bounces)) AS bounces, \n"
                + "  IF(is_null(sum(S.SessionDepth)), 0, sum(S.SessionDepth)) AS sessionDepth, \n"
                + "  IF(shows > 0, ((clicks * 100000000) / shows), 0) AS ctr, \n"
                + "  IF(clicks > 0, (cost / clicks), 0) AS avgClickCost, \n"
                + "  IF(firstPageShows > 0, ((firstPageSumPosShows * 1000000) / firstPageShows), 0) AS avgShowPosition, \n"
                + "  IF(firstPageClicks > 0, ((firstPageSumPosClicks * 1000000) / firstPageClicks), 0) AS avgClickPosition, \n"
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
                + "  ), ((double(cost) * 100000000.0) / double(revenue)), null) AS crr\n"
                + "FROM yt.DirectPhraseStatV2_bs AS S\n"
                + "WHERE (\n"
                + "  ourCampaignId IN (123)\n"
                + "  AND ourGroupId IN (\n"
                + "    126, 180\n"
                + "  )\n"
                + "  AND S.UpdateTime IN (\n"
                + "    1507582800, 1507669200, 1507755600, 1507842000, 1507928400\n"
                + "  )\n"
                + ")\n"
                + "GROUP BY \n"
                + "  ourCampaignId, \n"
                + "  ourGroupId";

        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }

    @Test
    public void testConstructCostSelect() {
        Field<Long> statGroupId = gridStat.getTableData().table().GROUP_EXPORT_ID.as("ourGroupId");
        Field<Long> statCampaignId = gridStat.getTableData().table().EXPORT_ID.as("ourCampaignId");

        Select<?> query = gridStat.constructCostSelect(
                Arrays.asList(statCampaignId, statGroupId),
                statCampaignId.in(CAMPAIGNS).and(statGroupId.in(GROUP)),
                TEST_FROM);

        String expected = "SELECT \n"
                + "  S.ExportID AS ourCampaignId, \n"
                + "  S.GroupExportID AS ourGroupId, \n"
                + "  IF(is_null(sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))), 0, sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))) AS cost\n"
                + "FROM yt.DirectPhraseStatV2_bs AS S\n"
                + "WHERE (\n"
                + "  ourCampaignId IN (123)\n"
                + "  AND ourGroupId IN (\n"
                + "    126, 180\n"
                + "  )\n"
                + "  AND S.UpdateTime = 1507582800\n"
                + ")\n"
                + "GROUP BY \n"
                + "  ourCampaignId, \n"
                + "  ourGroupId";

        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }

    @Test
    public void testConstructStatSelectWithSubCampaigns() {
        Field<Long> statCampaignId = gridStat.getTableData().effectiveCampaignId(SUBCAMPAIGNS).as("ourCampaignId");

        Select<?> query = gridStat.constructStatSelect(
                List.of(statCampaignId),
                statCampaignId.in(CAMPAIGNS),
                TEST_FROM, TEST_TO, Collections.emptySet(), null);

        String expected = "SELECT \n"
                + "  IF(is_null((transform(S.ExportID, (124, 125), (123, 123)) AS masterCampaignId)), S.ExportID, masterCampaignId) AS ourCampaignId, \n"
                + "  IF(is_null(sum(S.Shows)), 0, sum(S.Shows)) AS shows, \n"
                + "  IF(is_null(sum(S.Clicks)), 0, sum(S.Clicks)) AS clicks, \n"
                + "  IF(is_null(sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))), 0, sum(IF(S.CurrencyID = 0, ((S.Cost * 100) / 118), S.CostTaxFree))) AS cost, \n"
                + "  IF(is_null(sum(S.CostCur)), 0, sum(S.CostCur)) AS costWithTax, \n"
                + "  IF(is_null(sum(S.PriceCur)), 0, sum(S.PriceCur)) AS revenue, \n"
                + "  IF(is_null(sum(S.GoalsNum)), 0, sum(S.GoalsNum)) AS goals, \n"
                + "  IF(shows > 0, ((cost / shows) * 1000), 0) AS cpmPrice, \n"
                + "  IF(is_null(sum(S.FirstPageClicks)), 0, sum(S.FirstPageClicks)) AS firstPageClicks, \n"
                + "  IF(is_null(sum(S.FirstPageShows)), 0, sum(S.FirstPageShows)) AS firstPageShows, \n"
                + "  IF(is_null(sum(S.FirstPageSumPosClicks)), 0, sum(S.FirstPageSumPosClicks)) AS firstPageSumPosClicks, \n"
                + "  IF(is_null(sum(S.FirstPageSumPosShows)), 0, sum(S.FirstPageSumPosShows)) AS firstPageSumPosShows, \n"
                + "  IF(is_null(sum(IF(S.IsRMP, S.Clicks, S.SessionNum))), 0, sum(IF(S.IsRMP, S.Clicks, S.SessionNum))) AS sessions, \n"
                + "  IF(is_null(sum(S.SessionNumLimited)), 0, sum(S.SessionNumLimited)) AS sessionsLimited, \n"
                + "  IF(is_null(sum(S.Bounces)), 0, sum(S.Bounces)) AS bounces, \n"
                + "  IF(is_null(sum(S.SessionDepth)), 0, sum(S.SessionDepth)) AS sessionDepth, \n"
                + "  IF(shows > 0, ((clicks * 100000000) / shows), 0) AS ctr, \n"
                + "  IF(clicks > 0, (cost / clicks), 0) AS avgClickCost, \n"
                + "  IF(firstPageShows > 0, ((firstPageSumPosShows * 1000000) / firstPageShows), 0) AS avgShowPosition, \n"
                + "  IF(firstPageClicks > 0, ((firstPageSumPosClicks * 1000000) / firstPageClicks), 0) AS avgClickPosition, \n"
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
                + "  ), ((double(cost) * 100000000.0) / double(revenue)), null) AS crr\n"
                + "FROM yt.DirectPhraseStatV2_bs AS S\n"
                + "WHERE (\n"
                + "  ourCampaignId IN (123)\n"
                + "  AND S.UpdateTime IN (\n"
                + "    1507582800, 1507669200, 1507755600, 1507842000, 1507928400\n"
                + "  )\n"
                + ")\n"
                + "GROUP BY ourCampaignId";

        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }

    @Test
    public void testConstructConversionsSelect_WithFilteringStatsByAvailableGoals() {
        List<Long> availableGoalIds = List.of(111L, 112L);
        Select<Record> query = gridStat.constructConversionsSelect(CAMPAIGNS, SUBCAMPAIGNS, TEST_FROM, TEST_TO,
                availableGoalIds, null, false, true, false);
        String expected = "SELECT \n" +
                "  IF(is_null((transform(GS.ExportID, (124, 125), (123, 123)) AS masterCampaignId)), GS.ExportID, " +
                "masterCampaignId) AS effectiveCampaignId, \n" +
                "  GS.UpdateTime AS UpdateTime, \n" +
                "  IF(is_null(IF(sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType " +
                "= 1, GS.GoalsNum, NULL)), sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))), 0, IF(sum(IF(GS" +
                ".CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL))," +
                " sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))) AS GoalsNum, \n" +
                "  IF(is_null(IF(sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)) > 0, sum(IF(GS" +
                ".CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)), sum(IF(GS.CampaignGoalType = 2, (GS.PriceCur" +
                " / 1000000), NULL)))), 0, IF(sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)) > 0, " +
                "sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)), sum(IF(GS.CampaignGoalType = 2, (GS" +
                ".PriceCur / 1000000), NULL)))) AS PriceCur, \n" +
                "  GS.GroupExportID AS GroupExportID\n" +
                "FROM yt.DirectPhraseGoalsStat_bs AS GS\n" +
                "WHERE (\n" +
                "  GS.ExportID IN (\n" +
                "    123, 124, 125\n" +
                "  )\n" +
                "  AND UpdateTime IN (\n" +
                "    1507582800, 1507669200, 1507755600, 1507842000, 1507928400\n" +
                "  )\n" +
                "  AND GS.CampaignGoalType > 0\n" +
                "  AND (GS.GoalID + 0) IN (\n" +
                "    111, 112\n" +
                "  )\n" +
                ")\n" +
                "GROUP BY \n" +
                "  effectiveCampaignId, \n" +
                "  UpdateTime, \n" +
                "  GroupExportID";
        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }

    @Test
    public void testConstructConversionsSelect_WithFilteringRevenueByAvailableGoals() {
        List<Long> availableGoalIds = List.of(111L, 112L);
        Select<Record> query = gridStat.constructConversionsSelect(CAMPAIGNS, SUBCAMPAIGNS, TEST_FROM, TEST_TO,
                availableGoalIds, null, true, false, false);
        String expected = "SELECT \n" +
                "  IF(is_null((transform(GS.ExportID, (124, 125), (123, 123)) AS masterCampaignId)), GS.ExportID, " +
                "masterCampaignId) AS effectiveCampaignId, \n" +
                "  GS.UpdateTime AS UpdateTime, \n" +
                "  IF(is_null(IF(sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType " +
                "= 1, GS.GoalsNum, NULL)), sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))), 0, IF(sum(IF(GS" +
                ".CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL))," +
                " sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))) AS GoalsNum, \n" +
                "  IF(is_null(IF(sum(IF((\n" +
                "    GS.CampaignGoalType = 1\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)) > 0, sum(IF((\n" +
                "    GS.CampaignGoalType = 1\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)), sum(IF((\n" +
                "    GS.CampaignGoalType = 2\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)))), 0, IF(sum(IF((\n" +
                "    GS.CampaignGoalType = 1\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)) > 0, sum(IF((\n" +
                "    GS.CampaignGoalType = 1\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)), sum(IF((\n" +
                "    GS.CampaignGoalType = 2\n" +
                "    AND (GS.GoalID + 0) IN (\n" +
                "      111, 112\n" +
                "    )\n" +
                "  ), (GS.PriceCur / 1000000), NULL)))) AS PriceCur, \n" +
                "  GS.GroupExportID AS GroupExportID\n" +
                "FROM yt.DirectPhraseGoalsStat_bs AS GS\n" +
                "WHERE (\n" +
                "  GS.ExportID IN (\n" +
                "    123, 124, 125\n" +
                "  )\n" +
                "  AND UpdateTime IN (\n" +
                "    1507582800, 1507669200, 1507755600, 1507842000, 1507928400\n" +
                "  )\n" +
                "  AND GS.CampaignGoalType > 0\n" +
                ")\n" +
                "GROUP BY \n" +
                "  effectiveCampaignId, \n" +
                "  UpdateTime, \n" +
                "  GroupExportID";
        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }

    @Test
    public void testConstructConversionsSelect_WithoutFiltering() {
        Select<Record> query = gridStat.constructConversionsSelect(CAMPAIGNS, SUBCAMPAIGNS, TEST_FROM, TEST_TO,
                null, null, false, false, false);
        String expected = "SELECT \n" +
                "  IF(is_null((transform(GS.ExportID, (124, 125), (123, 123)) AS masterCampaignId)), GS.ExportID, " +
                "masterCampaignId) AS effectiveCampaignId, \n" +
                "  GS.UpdateTime AS UpdateTime, \n" +
                "  IF(is_null(IF(sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType " +
                "= 1, GS.GoalsNum, NULL)), sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))), 0, IF(sum(IF(GS" +
                ".CampaignGoalType = 1, GS.GoalsNum, NULL)) > 0, sum(IF(GS.CampaignGoalType = 1, GS.GoalsNum, NULL))," +
                " sum(IF(GS.CampaignGoalType = 2, GS.GoalsNum, NULL)))) AS GoalsNum, \n" +
                "  IF(is_null(IF(sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)) > 0, sum(IF(GS" +
                ".CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)), sum(IF(GS.CampaignGoalType = 2, (GS.PriceCur" +
                " / 1000000), NULL)))), 0, IF(sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)) > 0, " +
                "sum(IF(GS.CampaignGoalType = 1, (GS.PriceCur / 1000000), NULL)), sum(IF(GS.CampaignGoalType = 2, (GS" +
                ".PriceCur / 1000000), NULL)))) AS PriceCur, \n" +
                "  GS.GroupExportID AS GroupExportID\n" +
                "FROM yt.DirectPhraseGoalsStat_bs AS GS\n" +
                "WHERE (\n" +
                "  GS.ExportID IN (\n" +
                "    123, 124, 125\n" +
                "  )\n" +
                "  AND UpdateTime IN (\n" +
                "    1507582800, 1507669200, 1507755600, 1507842000, 1507928400\n" +
                "  )\n" +
                "  AND GS.CampaignGoalType > 0\n" +
                ")\n" +
                "GROUP BY \n" +
                "  effectiveCampaignId, \n" +
                "  UpdateTime, \n" +
                "  GroupExportID";
        assertThat(query.toString().trim())
                .isEqualTo(expected);
    }
}
