package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpa;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.WeekdayType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.libs.timetarget.WeekdayType.FRIDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.MONDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.SATURDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.SUNDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.THURSDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.TUESDAY;
import static ru.yandex.direct.libs.timetarget.WeekdayType.WEDNESDAY;

@RunWith(JUnitParamsRunner.class)
public class CampaignDataConverterGetTotalsTest {
    @SuppressWarnings("unused")
    private Object[] parameters() {
        return new Object[][]{
                {"Одна кампания без бюджета", List.of(campaignWithBudget(null, null)), List.of()},
                {"Две кампании без бюджета", List.of(campaignWithBudget(null, null), campaignWithBudget(null, null)),
                        List.of()},
                {"Две кампании, одна с бюджетом", List.of(campaignWithBudget(null, null),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)),
                        List.of(budget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY))},
                {"Две кампании, c одним типом бюджета", List.of(
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)),
                        List.of(budget(BigDecimal.valueOf(2L), GdCampaignBudgetPeriod.DAY))},
                {"Две кампании, c разным типом бюджета", List.of(
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK)),
                        List.of(budget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY),
                                budget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK))},
                {"Несколько кампаний, c разным типом бюджета", List.of(
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK),
                        campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)),
                        List.of(budget(BigDecimal.valueOf(2L), GdCampaignBudgetPeriod.DAY),
                                budget(BigDecimal.valueOf(2L), GdCampaignBudgetPeriod.WEEK),
                                budget(BigDecimal.valueOf(2L), GdCampaignBudgetPeriod.CUSTOM))},
                {"Одна кампания c нулевым бюджетом", List.of(campaignWithBudget(BigDecimal.ZERO,
                        GdCampaignBudgetPeriod.DAY)), List.of()},
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void testCampaignDataConverter_пetTotals(String description, List<GdCampaign> campaigns,
                                                    List<GdCampaignBudget> expected) {
        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertThat(total.getTotalBudgets()).containsExactlyInAnyOrder(expected.toArray(new GdCampaignBudget[0]));
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_cpmCampaigns(){
        var campaigns = new ArrayList<GdCampaign>();
        //cpm with day budget
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)
                .withType(GdCampaignType.CPM_PRICE));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)
                .withType(GdCampaignType.CPM_BANNER));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)
                .withType(GdCampaignType.CPM_DEALS));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)
                .withType(GdCampaignType.CPM_YNDX_FRONTPAGE));

        //cpm with weekly budget
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK)
                .withType(GdCampaignType.CPM_PRICE));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK)
                .withType(GdCampaignType.CPM_BANNER));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK)
                .withType(GdCampaignType.CPM_DEALS));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK)
                .withType(GdCampaignType.CPM_YNDX_FRONTPAGE));

        //cpm with custom budget
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM)
                .withType(GdCampaignType.CPM_PRICE));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM)
                .withType(GdCampaignType.CPM_BANNER));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM)
                .withType(GdCampaignType.CPM_DEALS));
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.CUSTOM)
                .withType(GdCampaignType.CPM_YNDX_FRONTPAGE));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(BigDecimal.ZERO, total.getTotalWeeklyBudget());
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_noDayOrWeeklyBudget(){
        var campaigns = new ArrayList<GdCampaign>();
        campaigns.add(campaignWithBudget(BigDecimal.ZERO, GdCampaignBudgetPeriod.DAY));
        campaigns.add(campaignWithBudget(BigDecimal.ZERO, GdCampaignBudgetPeriod.WEEK));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(BigDecimal.ZERO, total.getTotalWeeklyBudget());
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_defaultTimeTarget(){
        var campaigns = new ArrayList<GdCampaign>();
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY)); //1 x 7 = 7 per week
        campaigns.add(campaignWithBudget(BigDecimal.TEN, GdCampaignBudgetPeriod.WEEK));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(new BigDecimal(17), total.getTotalWeeklyBudget());
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_customTimeTarget(){
        var campaigns = new ArrayList<GdCampaign>();
        var customTimeTarget = new GdTimeTarget().withTimeBoard(timeBoard(20,
                List.of(TUESDAY, WEDNESDAY)));
        //2 days x 1 = 2 per week
        campaigns.add(campaignWithBudgetAndTimeTarget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY, customTimeTarget));
        campaigns.add(campaignWithBudget(BigDecimal.TEN, GdCampaignBudgetPeriod.WEEK));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(new BigDecimal(12), total.getTotalWeeklyBudget());
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_dayBudgetOnly(){
        var campaigns = new ArrayList<GdCampaign>();
        var customTimeTarget = new GdTimeTarget().withTimeBoard(timeBoard(1, List.of(THURSDAY, FRIDAY)));
        //2 days x 1 = 2 per week
        campaigns.add(campaignWithBudgetAndTimeTarget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY, customTimeTarget));
        //7 days x 1 = 7 per week
        campaigns.add(campaignWithBudgetAndTimeTarget(BigDecimal.ONE, GdCampaignBudgetPeriod.DAY, defaultTimeTarget()));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(new BigDecimal(9), total.getTotalWeeklyBudget());
    }

    @Test
    public void testGetTotalCampaigns_totalWeeklyBudget_weeklyBudgetOnly(){
        var campaigns = new ArrayList<GdCampaign>();
        campaigns.add(campaignWithBudget(BigDecimal.ONE, GdCampaignBudgetPeriod.WEEK));
        campaigns.add(campaignWithBudget(BigDecimal.TEN, GdCampaignBudgetPeriod.WEEK));

        var total = CampaignDataConverter.getTotalCampaigns(campaigns);
        assertEquals(new BigDecimal(11), total.getTotalWeeklyBudget());
    }

    private static GdCampaign campaignWithBudget(@Nullable BigDecimal budget,
                                                 @Nullable GdCampaignBudgetPeriod period) {
        return campaignWithBudgetAndTimeTarget(budget, period, defaultTimeTarget());
    }

    private static GdCampaign campaignWithBudgetAndTimeTarget(@Nullable BigDecimal budget,
                                                              @Nullable GdCampaignBudgetPeriod period,
                                                              GdTimeTarget timeTarget) {
        if (budget == null) {
            return new GdTextCampaign()
                    .withStrategy(new GdCampaignStrategyAvgCpa()).withSumRest(BigDecimal.ZERO)
                    .withTimeTarget(timeTarget);
        }
        return new GdTextCampaign()
                .withStrategy(new GdCampaignStrategyAvgCpa().withBudget(budget(budget, period))
                ).withSumRest(BigDecimal.ZERO).withTimeTarget(timeTarget);
    }

    private static GdCampaignBudget budget(BigDecimal budget, GdCampaignBudgetPeriod period) {
        return new GdCampaignBudget()
                .withSum(budget)
                .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                .withPeriod(period);
    }

    private static List<List<Integer>> defaultTimeBoard(){
        return timeBoard(HOURS_PER_DAY, List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY));
    }

    private static List<List<Integer>> timeBoard(int activeHours, List<WeekdayType> activeDays){
        return IntStream.rangeClosed(MONDAY.getInternalNum(), WeekdayType.SUNDAY.getInternalNum())
                .mapToObj(i -> new ArrayList<>(Collections.nCopies(activeHours,
                        activeDays.contains(WeekdayType.getById(i)) ? TimeTarget.PredefinedCoefs.USUAL.getValue() :
                                TimeTarget.PredefinedCoefs.ZERO.getValue())))
                .collect(Collectors.toList());
    }

    private static GdTimeTarget defaultTimeTarget(){
        return new GdTimeTarget().withTimeBoard(defaultTimeBoard());
    }
}
