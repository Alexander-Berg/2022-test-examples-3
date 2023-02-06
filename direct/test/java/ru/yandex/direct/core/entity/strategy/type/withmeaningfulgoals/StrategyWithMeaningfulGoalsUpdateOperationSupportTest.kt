package ru.yandex.direct.core.entity.strategy.type.withmeaningfulgoals

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.math.BigDecimal

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMeaningfulGoalsUpdateOperationSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo
    private lateinit var strategy: AutobudgetAvgCpa

    private var goalId1: Int = 0
    private var goalId2: Int = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val counterId = RandomNumberUtils.nextPositiveInteger()
        goalId1 = RandomNumberUtils.nextPositiveInteger()
        goalId2 = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId1, goalId2))

        strategy = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA, true)

        val meaningfulGoal = meaningfulGoal(goalId1)
        strategy
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withMeaningfulGoals(
                listOf(
                    MeaningfulGoal()
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID),
                    meaningfulGoal
                )
            )

        val addOperation = createAddOperation(listOf(strategy))
        val prepareAndApply = addOperation.prepareAndApply().get(0).result

    }

    private fun meaningfulGoal(goalId: Int) = MeaningfulGoal()
        .withGoalId(goalId.toLong())
        .withConversionValue(BigDecimal(100))

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    //    @Test нужна доработка в операции чтобы изменение типов стратегий корректно обрабатывалось
    fun `change strategy type and remove metrika value source`() {
    }

    @Test
    fun `update meaningful goals with engaged session goal id without value and get meaningful goals without engaged session`() {
        val meaningfulGoal = meaningfulGoal(goalId2)
        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(
                listOf(
                    MeaningfulGoal()
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID), meaningfulGoal
                ), AutobudgetCrr.MEANINGFUL_GOALS
            )

        prepareAndApplyValid(listOf(modelChanges))

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpa::class.java
        )[strategy.id]!!
        Assertions.assertThat(actualStrategy.meaningfulGoals).containsExactly(meaningfulGoal)
    }

    @Test
    fun `add meaningful engaged session goal id without value and get empty meaningful goals`() {
        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(
                listOf(
                    MeaningfulGoal()
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)
                ), AutobudgetCrr.MEANINGFUL_GOALS
            )

        prepareAndApplyValid(listOf(modelChanges))

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpa::class.java
        )[strategy.id]!!
        Assertions.assertThat(actualStrategy.meaningfulGoals).isNull()

    }
}
