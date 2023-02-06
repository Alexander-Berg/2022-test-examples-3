package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.GdGoalStats;
import ru.yandex.direct.grid.model.GdGoalStatsFilter;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.processing.util.StatHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;

@RunWith(Parameterized.class)
public class CampaignServiceUtilsGoalStatsFilterTest {
    private static final long GOAL_ID_1 = 100L;
    private static final long GOAL_ID_2 = 200L;
    private static final long WRONG_GOAL_ID = 300L;

    private static final GdCampaign CAMP_1 = defaultGdCampaign(1L).withGoalStats(emptyList());

    private static final GdCampaign CAMP_2 = defaultGdCampaign(2L).withGoalStats(singletonList(
            new GdGoalStats()
                    .withGoalId(GOAL_ID_1)
                    .withGoals(20L)
                    .withCostPerAction(BigDecimal.valueOf(200))));

    private static final GdCampaign CAMP_3 = defaultGdCampaign(3L).withGoalStats(asList(
            new GdGoalStats()
                    .withGoalId(GOAL_ID_1)
                    .withGoals(30L)
                    .withCostPerAction(BigDecimal.valueOf(300)),
            new GdGoalStats()
                    .withGoalId(GOAL_ID_2)
                    .withGoals(30L)
                    .withCostPerAction(BigDecimal.valueOf(300))
    ));

    private static final GdCampaign CAMP_4 = defaultGdCampaign(4L).withGoalStats(asList(
            new GdGoalStats()
                    .withGoalId(GOAL_ID_1)
                    .withGoals(40L)
                    .withCostPerAction(BigDecimal.valueOf(400)),
            new GdGoalStats()
                    .withGoalId(GOAL_ID_2)
                    .withGoals(40L)
                    .withCostPerAction(BigDecimal.valueOf(400))
    ));


    private static final List<GdCampaign> CAMPAIGNS = asList(CAMP_1, CAMP_2, CAMP_3, CAMP_4);

    @Parameterized.Parameter(0)
    public List<GdGoalStatsFilter> filters;

    @Parameterized.Parameter(1)
    public List<Long> expectedList;

    @Parameterized.Parameters(name = "filter = {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // фильтр отсутствует
                {null,
                        asList(1L, 2L, 3L, 4L)},
                {emptyList(),
                        asList(1L, 2L, 3L, 4L)},

                // фильтр на минимум - если цели с таким id нет, кампания фильтр не проходит
                {singletonList(new GdGoalStatsFilter().withGoalId(GOAL_ID_1).withMinGoals(30L)),
                        asList(3L, 4L)},
                {singletonList(new GdGoalStatsFilter().withGoalId(WRONG_GOAL_ID).withMinGoals(30L)),
                        emptyList()},

                // фильтр на максимум - если цели с таким id нет, кампания фильтр проходит
                {singletonList(new GdGoalStatsFilter().withGoalId(GOAL_ID_1).withMaxGoals(30L)),
                        asList(1L, 2L, 3L)},
                {singletonList(new GdGoalStatsFilter().withGoalId(WRONG_GOAL_ID).withMaxGoals(30L)),
                        asList(1L, 2L, 3L, 4L)},

                // составной фильтр
                {singletonList(new GdGoalStatsFilter().withGoalId(GOAL_ID_1).withMaxGoals(30L)
                        .withMinCostPerAction(BigDecimal.valueOf(300))),
                        singletonList(3L)},

                // фильтр на несколько целей
                {asList(new GdGoalStatsFilter().withGoalId(GOAL_ID_1).withMinGoals(30L),
                        new GdGoalStatsFilter().withGoalId(GOAL_ID_2).withMaxGoals(30L)),
                        singletonList(3L)},
        });
    }

    @Test
    public void applyGoalsStatFilters() {
        assertEquals(expectedList, filterCampaigns());
    }

    private List<Long> filterCampaigns() {
        return CAMPAIGNS.stream()
                .filter(c -> StatHelper.applyGoalsStatFilters(filters, c))
                .map(GdCampaign::getId)
                .collect(toList());
    }
}
