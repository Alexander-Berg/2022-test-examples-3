package ru.yandex.direct.core.entity.metrika.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.GoalStatus;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_BASE;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class MetrikaGoalsUtilsTest {

    @Test
    public void getGoalIdsByCampaignIds() {
        Set<RetargetingCondition> goals = Set.of(new RetargetingCondition()
                        .withCounterId(1)
                        .withId(1L),
                new RetargetingCondition()
                        .withCounterId(1)
                        .withId(2L),
                new RetargetingCondition()
                        .withCounterId(2)
                        .withId(3L),
                new RetargetingCondition()
                        .withCounterId(0)
                        .withCounterIds(List.of(1, 3))
                        .withId(4L));

        long campaignIdFirst = 1L;
        long campaignIdSecond = 2L;
        Map<Long, Set<Goal>> goalIdsByCampaignIds = MetrikaGoalsUtils.getGoalsByCampaignIds(goals,
                Map.of(campaignIdFirst,
                        Set.of(1L, 2L, 3L), campaignIdSecond, Set.of(2L, 3L)));

        List<Long> expectedGoalIdsForFirstCampaign = List.of(1L, 2L, 3L, 4L);
        List<Long> expectedGoalIdsForSecondCampaign = List.of(3L, 4L);

        assertThat(mapList(goalIdsByCampaignIds.get(campaignIdFirst), GoalBase::getId))
                .containsExactlyInAnyOrder(expectedGoalIdsForFirstCampaign.toArray(Long[]::new));

        assertThat(mapList(goalIdsByCampaignIds.get(campaignIdSecond), GoalBase::getId))
                .containsExactlyInAnyOrder(expectedGoalIdsForSecondCampaign.toArray(Long[]::new));
    }

    @Test
    public void getEcommerceGoalFromCounterId() {
        Long counterId = 123L;
        Goal expectedGoal = (Goal) (new Goal()
                .withType(GoalType.ECOMMERCE)
                .withId(METRIKA_ECOMMERCE_BASE + counterId)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                .withStatus(GoalStatus.ACTIVE)
                .withCounterId(counterId.intValue())
                .withName(counterId.toString()));

        Goal actualGoal = MetrikaGoalsUtils.ecommerceGoalFromCounterId(counterId);

        assertThat(actualGoal).isEqualTo(expectedGoal);
    }
}
