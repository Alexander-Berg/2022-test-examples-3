package ru.yandex.direct.core.testing.steps

import com.nhaarman.mockitokotlin2.doReturn
import org.mockito.ArgumentMatchers
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalsSuggestion

class MetrikaServiceSteps(private var metrikaGoalsService: MetrikaGoalsService) {
    fun initTestDataForGoalsSuggestion(goals: Collection<Goal>) {
        doReturn(setOf<Goal>()).`when`(metrikaGoalsService)
            .getMetrikaGoalsByCounters(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyCollection(),
                ArgumentMatchers.anySet(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.anyBoolean()
            )

        doReturn(
            GoalsSuggestion()
                .withSortedGoalsToSuggestion(goals.toList())
                .withTop1GoalId(goals.first().id)
        ).`when`(metrikaGoalsService).getGoalsSuggestion(ArgumentMatchers.any(), ArgumentMatchers.any())
    }
}
