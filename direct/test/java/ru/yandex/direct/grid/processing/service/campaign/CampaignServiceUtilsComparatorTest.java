package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.GdEntityStats;
import ru.yandex.direct.grid.model.GdGoalStats;
import ru.yandex.direct.grid.model.GdOrderByParams;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderBy;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.grid.model.Order.ASC;
import static ru.yandex.direct.grid.model.Order.DESC;
import static ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField.ID;
import static ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField.NAME;
import static ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField.STAT_COST_PER_ACTION;
import static ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField.STAT_GOALS;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;

@RunWith(Parameterized.class)
public class CampaignServiceUtilsComparatorTest {
    private static final long GOAL_ID = 111L;

    private static final GdCampaign CAMP_1 = defaultGdCampaign(1L).withName("a")
            .withGoalStats(emptyList());

    private static final GdCampaign CAMP_2 = defaultGdCampaign(2L).withName("a")
            .withGoalStats(emptyList());

    private static final GdCampaign CAMP_3 = defaultGdCampaign(3L).withName("b")
            .withStats(new GdEntityStats().withGoals(1L))
            .withGoalStats(emptyList());

    private static final GdCampaign CAMP_4 = defaultGdCampaign(4L).withName("b")
            .withStats(new GdEntityStats().withGoals(20L))
            .withGoalStats(singletonList(new GdGoalStats().withGoalId(GOAL_ID).withGoals(10L)
                    .withCostPerAction(BigDecimal.ZERO)));

    private static final GdCampaign CAMP_5 = defaultGdCampaign(5L).withName("b")
            .withStats(new GdEntityStats().withGoals(30L))
            .withGoalStats(singletonList(new GdGoalStats().withGoalId(GOAL_ID).withGoals(20L)
                    .withCostPerAction(BigDecimal.ZERO)));

    private static final List<GdCampaign> CAMPAIGNS = asList(CAMP_1, CAMP_2, CAMP_3, CAMP_4, CAMP_5);

    @Parameterized.Parameter(0)
    public List<GdCampaignOrderBy> orderingItems;

    @Parameterized.Parameter(1)
    public List<Long> expectedOrder;

    @Parameterized.Parameter(2)
    public Class<?> exceptionClass;

    @Parameterized.Parameters(name = "comparators = {0}; exception = {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // Сортировка по id
                {singletonList(buildOrderBy(ID, ASC, null)), asList(1L, 2L, 3L, 4L, 5L), null},
                {singletonList(buildOrderBy(ID, DESC, null)), asList(5L, 4L, 3L, 2L, 1L), null},

                // Сортировка по наименованию и id
                {asList(buildOrderBy(NAME, DESC, null), buildOrderBy(ID, ASC, null)),
                        asList(3L, 4L, 5L, 1L, 2L), null},

                // Сортировка по полю из статистики и id
                {asList(buildOrderBy(STAT_GOALS, ASC, null), buildOrderBy(ID, ASC, null)),
                        asList(3L, 4L, 5L, 1L, 2L), null},

                // Сортировка по полю из статистики целей и id
                {asList(buildOrderBy(STAT_GOALS, ASC, GOAL_ID), buildOrderBy(ID, ASC, null)),
                        asList(1L, 2L, 3L, 4L, 5L), null},
                // по убыванию
                {asList(buildOrderBy(STAT_GOALS, DESC, GOAL_ID), buildOrderBy(ID, ASC, null)),
                        asList(5L, 4L, 1L, 2L, 3L), null},

                // отсутствующий компаратор для кампании
                {singletonList(buildOrderBy(STAT_COST_PER_ACTION, ASC, null)), null, IllegalArgumentException.class},

                // отсутствующий компаратор для цели
                {singletonList(buildOrderBy(ID, ASC, GOAL_ID)), null, IllegalArgumentException.class},
        });
    }

    @Test
    public void getComparator() {
        if (exceptionClass == null) {
            assertEquals(expectedOrder, sortCampaigns());
        } else {
            assertThatThrownBy(this::sortCampaigns)
                    .isInstanceOf(exceptionClass);
        }
    }

    private static GdCampaignOrderBy buildOrderBy(GdCampaignOrderByField id, Order desc, Long goalId) {
        return new GdCampaignOrderBy()
                .withField(id)
                .withOrder(desc)
                .withParams(goalId == null ? null : new GdOrderByParams().withGoalId(goalId));
    }

    private List<Long> sortCampaigns() {
        return CAMPAIGNS.stream()
                .sorted(CampaignServiceUtils.getComparator(orderingItems))
                .map(GdCampaign::getId)
                .collect(toList());
    }
}
