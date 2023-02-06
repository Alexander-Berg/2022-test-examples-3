package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalsSuggestion
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaGoalsByStrategyFilter
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaGoalsFilterUnion
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaGoalsByCounterPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.test.utils.RandomNumberUtils

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class GoalGraphQlServiceGoalsByFilterTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var context: GridGraphQLContext

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var userInfo: UserInfo

    private val queryTemplate = """{
      getMetrikaGoalsByFilter(input: %s) {
        top1GoalId
          goals {
            id
            counterId
          }
       }
    }"""

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()
        context = ContextHelper.buildContext(userInfo.user)
        gridContextProvider.gridContext = context
    }

    @Test
    fun `successfully goals for new strategy`() {
        val unionFilter = GdMetrikaGoalsFilterUnion()
            .withMetrikaGoalsByStrategyFilter(GdMetrikaGoalsByStrategyFilter())

        val firstCounterId = RandomNumberUtils.nextPositiveInteger()
        val firstGoalId = RandomNumberUtils.nextPositiveInteger()
        val secondCounterId = RandomNumberUtils.nextPositiveInteger()
        val secondGoalId = RandomNumberUtils.nextPositiveInteger()

        metrikaClientStub.addUserCounter(userInfo.uid, firstCounterId)
        metrikaClientStub.addUserCounter(userInfo.uid, secondGoalId)
        metrikaClientStub.addCounterGoal(firstCounterId, CounterGoal().withId(firstGoalId))
        metrikaClientStub.addCounterGoal(secondCounterId, CounterGoal().withId(secondGoalId))

        val goalsFromMetrika = setOf(
            Goal().withId(
                firstGoalId.toLong(),
            ).withCounterId(firstCounterId) as Goal,
            Goal().withId(
                secondGoalId.toLong(),
            ).withCounterId(secondCounterId) as Goal
        )

        doReturn(goalsFromMetrika).`when`(
            metrikaGoalsService
        ).getMetrikaGoalsByCounters(
            anyLong(),
            any(),
            any(),
            any(),
            anyMap(),
            any(),
            anyBoolean(),
            anyBoolean()
        )

        doReturn(
            GoalsSuggestion()
                .withSortedGoalsToSuggestion(goalsFromMetrika.toList())
                .withTop1GoalId(null)
        ).`when`(metrikaGoalsService).getGoalsSuggestion(
            any(),
            any()
        )

        val result = executeSuccess(unionFilter)
        softly {
            assertThat(result).isNotNull
            assertThat(result.goals.map { it.id }).containsExactlyInAnyOrder(
                firstGoalId.toLong(),
                secondGoalId.toLong()
            )
            assertThat(result.goals.map { it.counterId }).containsExactlyInAnyOrder(
                firstCounterId,
                secondCounterId
            )
        }
    }

    @Test
    fun `successfully goals for strategy`() {
        val firstCounterId = RandomNumberUtils.nextPositiveInteger()
        val firstGoalId = RandomNumberUtils.nextPositiveInteger()
        val secondCounterId = RandomNumberUtils.nextPositiveInteger()
        val secondGoalId = RandomNumberUtils.nextPositiveInteger()

        metrikaClientStub.addUserCounter(userInfo.uid, firstCounterId)
        metrikaClientStub.addUserCounter(userInfo.uid, secondGoalId)
        metrikaClientStub.addCounterGoal(firstCounterId, CounterGoal().withId(firstGoalId))
        metrikaClientStub.addCounterGoal(secondCounterId, CounterGoal().withId(secondGoalId))

        val unionFilter = GdMetrikaGoalsFilterUnion()
            .withMetrikaGoalsByStrategyFilter(
                GdMetrikaGoalsByStrategyFilter()
                    .withCounterIds(listOf(firstCounterId.toLong(), secondCounterId.toLong()))
            )

        val goalsFromMetrika = setOf(
            Goal().withId(
                firstGoalId.toLong(),
            ).withCounterId(firstCounterId) as Goal,
            Goal().withId(
                secondGoalId.toLong(),
            ).withCounterId(secondCounterId) as Goal
        )

        doReturn(goalsFromMetrika).`when`(
            metrikaGoalsService
        )
            .getMetrikaGoalsByCounters(
                anyLong(),
                any(),
                any(),
                any(),
                anyMap(),
                any(),
                anyBoolean(),
                anyBoolean()
            )

        doReturn(
            GoalsSuggestion()
                .withSortedGoalsToSuggestion(goalsFromMetrika.toList())
                .withTop1GoalId(null)
        )
            .`when`(metrikaGoalsService).getGoalsSuggestion(
                any(),
                any()
            )

        val result = executeSuccess(unionFilter)
        softly {
            assertThat(result).isNotNull
            assertThat(result.goals.map { it.id }).containsExactlyInAnyOrder(
                firstGoalId.toLong(),
                secondGoalId.toLong()
            )
            assertThat(result.goals.map { it.counterId }).containsExactlyInAnyOrder(
                firstCounterId,
                secondCounterId
            )
        }
    }

    private fun executeSuccess(filter: GdMetrikaGoalsFilterUnion): GdMetrikaGoalsByCounterPayload {
        val query = String.format(queryTemplate, graphQlSerialize(filter))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()
        val data: Map<String, Any> = result.getData()
        val payload = data["getMetrikaGoalsByFilter"] as Map<String, Any>
        return convertValue(remapGoals(payload), GdMetrikaGoalsByCounterPayload::class.java)
    }

    private fun remapGoals(gdMetrikaGoalsByCounterPayload: Map<String, Any>): Map<String, Any> {
        val newMap = HashMap(gdMetrikaGoalsByCounterPayload)
        newMap.computeIfPresent("goals") { _, goals ->
            val goalsList = goals as List<*>
            goalsList.map { goal ->
                val map = HashMap(goal as Map<*, *>)
                map.computeIfAbsent("_type") { _ -> "GdGoal" }
                map
            }
        }
        return newMap
    }
}
