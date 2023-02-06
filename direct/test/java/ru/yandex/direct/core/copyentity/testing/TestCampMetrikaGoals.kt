package ru.yandex.direct.core.copyentity.testing

import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.entity.retargeting.model.GoalRole
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.testing.data.TestFullGoals.generateGoalId
import ru.yandex.direct.test.utils.randomPositiveLong
import java.time.LocalDateTime

object TestCampMetrikaGoals {
    fun defaultCampMetrikaGoal(
        cid: Long?,
        goalId: Long = generateGoalId(GoalType.GOAL),
    ) = CampMetrikaGoal()
        .withCampaignId(cid)
        .withGoalId(goalId)
        .withGoalsCount(randomPositiveLong(10000L))
        .withContextGoalsCount(randomPositiveLong(1000L))
        .withStatDate(LocalDateTime.now())
        .withGoalRole(setOf(GoalRole.SINGLE))
}
