package ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy;

import com.google.common.collect.ImmutableSet;

import ru.yandex.direct.core.entity.retargeting.model.Goal;

class CampaignStrategyTestDataUtils {

    static final long CAMPAIGN_COUNTER_GOAL_1 = 1001;
    static final long CAMPAIGN_COUNTER_GOAL_2 = 1002;
    static final long TURBOLANDING_INTERNAL_COUNTER_GOAL_1 = 6001;
    static final long TURBOLANDING_INTERNAL_COUNTER_GOAL_2 = 6002;

    //цели от счетчиков указанных в кампании
    static final ImmutableSet<Goal> CAMPAIGN_COUNTERS_AVAILABLE_GOALS = ImmutableSet.of(
            (Goal) new Goal().withId(CAMPAIGN_COUNTER_GOAL_1),
            (Goal) new Goal().withId(CAMPAIGN_COUNTER_GOAL_2),
            (Goal) new Goal().withId(TURBOLANDING_INTERNAL_COUNTER_GOAL_1),
            (Goal) new Goal().withId(TURBOLANDING_INTERNAL_COUNTER_GOAL_2));

}
