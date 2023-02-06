package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalsSuggestion
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.model.Order.DESC
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.goal.GdAvailableGoalsContainer
import ru.yandex.direct.grid.processing.model.goal.GdGoalsOrderBy
import ru.yandex.direct.grid.processing.model.goal.GdGoalsOrderByField.CONVERSION_VISITS_COUNT
import ru.yandex.direct.grid.processing.service.goal.GoalDataConverter.toGdGoal

@GridProcessingTest
class GetAvailableGoalsTest {
    private lateinit var metrikaGoalsService: MetrikaGoalsService
    private lateinit var goalDataService: GoalDataService

    @Before
    fun before() {
        metrikaGoalsService = mock()
        whenever(metrikaGoalsService.getAvailableMetrikaGoalsForClient(any(), any()))
            .thenReturn(emptySet())
        whenever(metrikaGoalsService.getGoalsSuggestion(any(), any()))
            .thenReturn(GoalsSuggestion().withSortedGoalsToSuggestion(defaultGoals()))
        goalDataService =
            GoalDataService(metrikaGoalsService, null, null, null, null, null, null, null, null, null, null, null)
    }

    @Test
    fun getAvailableGoals_whenNoInput() {
        val result = goalDataService.getAvailableGoals(
            1L, ClientId.fromLong(1L),
            null
        )
        Assertions.assertThat(result.rowset).containsExactlyElementsOf(
            defaultGoals()
                .map { toGdGoal(it, null) }
        )
    }

    @Test
    fun getAvailableGoals_whenOrderBy() {
        val result = goalDataService.getAvailableGoals(
            1L, ClientId.fromLong(1L),
            GdAvailableGoalsContainer(
                null,
                listOf(GdGoalsOrderBy(CONVERSION_VISITS_COUNT, DESC))
            )
        )
        Assertions.assertThat(result.rowset).containsExactlyElementsOf(
            defaultGoals()
                .sortedByDescending { it.conversionVisitsCount }
                .map { toGdGoal(it, null) }
        )
    }

    @Test
    fun getAvailableGoals_whenLimitOffset() {
        val result = goalDataService.getAvailableGoals(
            1L, ClientId.fromLong(1L),
            GdAvailableGoalsContainer(
                GdLimitOffset().withLimit(1).withOffset(1),
                null
            )
        )
        Assertions.assertThat(result.rowset).containsExactlyElementsOf(
            defaultGoals()
                .drop(1)
                .take(1)
                .map { toGdGoal(it, null) }
        )
    }

    @Test
    fun getAvailableGoals_whenLimitOffsetAndOrderBy() {
        val result = goalDataService.getAvailableGoals(
            1L, ClientId.fromLong(1L),
            GdAvailableGoalsContainer(
                GdLimitOffset().withLimit(1).withOffset(1),
                listOf(GdGoalsOrderBy(CONVERSION_VISITS_COUNT, DESC))
            )
        )
        Assertions.assertThat(result.rowset).containsExactlyElementsOf(
            defaultGoals()
                .sortedByDescending { it.conversionVisitsCount }
                .drop(1)
                .take(1)
                .map { toGdGoal(it, null) }
        )
    }

    private fun defaultGoals() = listOf(
        Goal().withId(1).withConversionVisitsCount(10) as Goal,
        Goal().withId(2).withConversionVisitsCount(20) as Goal,
        Goal().withId(3).withConversionVisitsCount(30) as Goal
    )
}
