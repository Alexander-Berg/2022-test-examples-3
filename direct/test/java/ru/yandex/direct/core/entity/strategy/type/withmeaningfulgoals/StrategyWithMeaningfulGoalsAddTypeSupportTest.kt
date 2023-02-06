package ru.yandex.direct.core.entity.strategy.type.withmeaningfulgoals

import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.RandomNumberUtils

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMeaningfulGoalsAddTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    val counterId = RandomNumberUtils.nextPositiveInteger()
    val goalId = RandomNumberUtils.nextPositiveInteger()

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `add meaningful engaged session goal id without value and get empty meaningful goals`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
        //пока в операции не сделано заполнение коммон полей, но уже есть их запись -- заполняю их часть
        strategy
            .withMeaningfulGoals(listOf(MeaningfulGoal().withGoalId(ENGAGED_SESSION_GOAL_ID)))

        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            DefaultManualStrategy::class.java
        )[id]!!
        assertThat(actualStrategy.meaningfulGoals).isNull()
    }

    @Test
    fun `add meaningful goals with engaged session goal id without value and get meaningful goals without engaged session`() {
        val counterId = RandomNumberUtils.nextPositiveInteger()
        val goalId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetCrr()
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA, true)

        val meaningfulGoal = MeaningfulGoal()
            .withGoalId(goalId.toLong())
            .withIsMetrikaSourceOfValue(false)
            .withConversionValue(BigDecimal(100))
        strategy
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withMeaningfulGoals(
                listOf(
                    MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID),
                    meaningfulGoal
                )
            )

        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetCrr::class.java
        )[id]!!
        assertThat(actualStrategy.meaningfulGoals).containsExactly(meaningfulGoal)
    }

}
