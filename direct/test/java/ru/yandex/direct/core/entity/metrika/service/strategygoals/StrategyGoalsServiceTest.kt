package ru.yandex.direct.core.entity.metrika.service.strategygoals

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anySet
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.metrika.container.CampaignTypeWithCounterIds
import ru.yandex.direct.core.entity.metrika.service.ForStrategy
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.utils.StrategyModelUtils.campaignIds
import ru.yandex.direct.core.entity.strategy.utils.StrategyModelUtils.metrikaCounterIds
import ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.checkEmpty
import ru.yandex.direct.test.utils.checkEquals

internal class StrategyGoalsServiceTest {

    private val campaignGoalsService: CampaignGoalsService = mock()

    private val strategyGoalsService: StrategyGoalsService = StrategyGoalsService(campaignGoalsService)

    private val clientId = ClientId.fromLong(1L)
    private val operator = 1L

    @Before
    fun before() {
        reset(campaignGoalsService)
    }

    @Test
    fun `no available goals on empty metrika counters`() {
        strategyGoalsService.getAvailableForStrategies(clientId, operator, emptySet()).checkEmpty()
    }

    @Test
    fun `available goals for strategy`() {
        val campaignId1 = 1L
        val campaignId2 = 2L
        val strategy = AutobudgetCrr()
            .withCids(listOf(campaignId1, campaignId2))
            .withId(1)
        val campaignId1Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val campaignId2Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(campaignId1 to campaignId1Goals, campaignId2 to campaignId2Goals)
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), anyMap(), anyOrNull())

        val request = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy.campaignIds().toSet(),
            strategy.metrikaCounterIds().toSet()
        )

        val expected = mapOf(request to campaignId1Goals + campaignId2Goals)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for few strategy`() {
        val campaignId1 = 1L
        val campaignId2 = 2L
        val strategy1 = AutobudgetCrr()
            .withCids(listOf(campaignId1))
            .withId(1)

        val strategy2 = AutobudgetRoi()
            .withCids(listOf(campaignId2))
            .withId(2)

        val campaignId1Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val campaignId2Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(campaignId1 to campaignId1Goals, campaignId2 to campaignId2Goals)

        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), anyMap(), anyOrNull())

        val request1 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy1.campaignIds().toSet(),
            strategy1.metrikaCounterIds().toSet()
        )

        val request2 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy2.campaignIds().toSet(),
            strategy2.metrikaCounterIds().toSet()
        )

        val expected = mapOf(request1 to campaignId1Goals, request2 to campaignId2Goals)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request1, request2))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for strategy without campaign ids and campaign type`() {
        val metrikaCounters = setOf(3L, 4L)
        val strategy = AutobudgetCrr()
            .withId(1)
            .withMetrikaCounters(metrikaCounters.toList())
        val goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(campaignTypeWithCounterIds(metrikaCounters) to goals)
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignType(any(), any(), anySet(), anyOrNull())

        val request = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy.campaignIds().toSet(),
            strategy.metrikaCounterIds().toSet()
        )

        val expected = mapOf(request to goals)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for strategy without campaign ids and with campaign type`() {
        val metrikaCounters = setOf(3L, 4L)
        val strategy = AutobudgetCrr()
            .withId(1)
            .withMetrikaCounters(metrikaCounters.toList())
        val goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(campaignTypeWithCounterIds(metrikaCounters, CampaignType.TEXT) to goals)
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignType(any(), any(), anySet(), anyOrNull())

        val request = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy.campaignIds().toSet(),
            strategy.metrikaCounterIds().toSet(),
            CampaignType.TEXT
        )

        val expected = mapOf(request to goals)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for few strategy without campaign ids`() {
        val metrikaCounters = setOf(3L, 4L)
        val strategy1 = AutobudgetCrr()
            .withId(1)
            .withMetrikaCounters(metrikaCounters.toList())

        val strategy2 = AutobudgetCrr()
            .withId(2)
            .withMetrikaCounters(metrikaCounters.toList())
        val goals1 = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goals2 = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(
            campaignTypeWithCounterIds(metrikaCounters, CampaignType.DYNAMIC) to goals1,
            campaignTypeWithCounterIds(metrikaCounters) to goals2
        )
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignType(any(), any(), anySet(), anyOrNull())

        val request1 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy1.campaignIds().toSet(),
            strategy1.metrikaCounterIds().toSet(),
            CampaignType.DYNAMIC
        )
        val request2 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy2.campaignIds().toSet(),
            strategy2.metrikaCounterIds().toSet()
        )

        val expected = mapOf(request1 to goals1, request2 to goals2)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request1, request2))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for strategy with campaign ids and strategy without`() {
        val metrikaCounters = setOf(3L, 4L)
        val campaignId = 5L
        val strategy1 = AutobudgetCrr()
            .withId(1)
            .withCids(listOf(campaignId))
            .withMetrikaCounters(metrikaCounters.toList())

        val strategy2 = AutobudgetCrr()
            .withId(2)
            .withMetrikaCounters(metrikaCounters.toList())
        val goalsForStrategy1 = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsForStrategy2 = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))

        doReturn(mapOf(campaignTypeWithCounterIds(metrikaCounters, CampaignType.TEXT) to goalsForStrategy2))
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignType(any(), any(), anySet(), anyOrNull())

        doReturn(mapOf(campaignId to goalsForStrategy1))
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), anyMap(), anyOrNull())

        val request1 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy1.campaignIds().toSet(),
            strategy1.metrikaCounterIds().toSet()
        )
        val request2 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy2.campaignIds().toSet(),
            strategy2.metrikaCounterIds().toSet(),
            CampaignType.TEXT
        )

        val expected = mapOf(request1 to goalsForStrategy1, request2 to goalsForStrategy2)

        val result = strategyGoalsService.getAvailableForStrategies(clientId, operator, setOf(request1, request2))
        result.checkEquals(expected)
    }

    @Test
    fun `available goals for few strategy by ids`() {
        val campaignId1 = 1L
        val campaignId2 = 2L
        val strategy1 = AutobudgetCrr()
            .withCids(listOf(campaignId1))
            .withId(1)

        val strategy2 = AutobudgetRoi()
            .withCids(listOf(campaignId2))
            .withId(2)

        val campaignId1Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val campaignId2Goals = setOf(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.GOAL))
        val goalsMap = mapOf(campaignId1 to campaignId1Goals, campaignId2 to campaignId2Goals)

        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), anyMap(), anyOrNull())

        val request1 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy1.campaignIds().toSet(),
            strategy1.metrikaCounterIds().toSet()
        )

        val request2 = StrategyGoalsService.Companion.StrategyAvailableGoalsRequest(
            strategy2.campaignIds().toSet(),
            strategy2.metrikaCounterIds().toSet()
        )

        val expected = mapOf(strategy1.id to campaignId1Goals, strategy2.id to campaignId2Goals)

        val result = strategyGoalsService.getAvailableForStrategyIds(
            clientId,
            operator,
            mapOf(strategy1.id to request1, strategy2.id to request2)
        )
        result.checkEquals(expected)
    }

    private fun campaignTypeWithCounterIds(
        metrikaCounters: Set<Long>,
        campaignType: CampaignType? = null
    ): CampaignTypeWithCounterIds =
        if (campaignType != null) {
            CampaignTypeWithCounterIds()
                .withCounterIds(metrikaCounters)
                .withCampaignType(campaignType)
        } else {
            CampaignTypeWithCounterIds()
                .withCounterIds(metrikaCounters)
                .withMobileGoalsFilter(ForStrategy)
        }
}
