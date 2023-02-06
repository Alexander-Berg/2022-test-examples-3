package ru.yandex.direct.core.entity.strategy.type.withmeaningfulgoals

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMeaningfulGoals
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMeaningfulGoalsUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val goalId1 = RandomNumberUtils.nextPositiveInteger()
        val goalId2 = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId1, goalId2))
        val meaningfulGoal1 = MeaningfulGoal()
            .withGoalId(goalId1.toLong())
            .withConversionValue(BigDecimal.ONE)
        val meaningfulGoal2 = MeaningfulGoal()
            .withGoalId(goalId2.toLong())
            .withConversionValue(BigDecimal.ONE)

        val strategy = TestAutobudgetCrrStrategy.autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withMeaningfulGoals(listOf(meaningfulGoal1))

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithMeaningfulGoals::class.java)
            .process(listOf(meaningfulGoal1, meaningfulGoal2), StrategyWithMeaningfulGoals.MEANINGFUL_GOALS)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail on unavailable goal in meaningful goals`() {
        val goalId1 = RandomNumberUtils.nextPositiveInteger()
        val goalId2 = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId1))
        val meaningfulGoal1 = MeaningfulGoal()
            .withGoalId(goalId1.toLong())
            .withConversionValue(BigDecimal.ONE)
        val meaningfulGoal2 = MeaningfulGoal()
            .withGoalId(goalId2.toLong())
            .withConversionValue(BigDecimal.ONE)

        val strategy = TestAutobudgetCrrStrategy.autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withMeaningfulGoals(listOf(meaningfulGoal1))

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithMeaningfulGoals::class.java)
            .process(listOf(meaningfulGoal1, meaningfulGoal2), StrategyWithMeaningfulGoals.MEANINGFUL_GOALS)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(
                    index(0),
                    field(StrategyWithMeaningfulGoals.MEANINGFUL_GOALS),
                    index(1),
                    field(StrategyWithConversion.GOAL_ID)
                ),
                CollectionDefects.inCollection()
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(matcher)
    }

    @Test
    fun `fail on not unique meaningful goals set`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId))
        val meaningfulGoal = MeaningfulGoal()
            .withGoalId(goalId.toLong())
            .withConversionValue(BigDecimal.ONE)

        val strategy = TestAutobudgetCrrStrategy.autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withMeaningfulGoals(listOf(meaningfulGoal))

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithMeaningfulGoals::class.java)
            .process(listOf(meaningfulGoal, meaningfulGoal), StrategyWithMeaningfulGoals.MEANINGFUL_GOALS)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val marcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(index(0), field(StrategyWithMeaningfulGoals.MEANINGFUL_GOALS), index(0)),
                CollectionDefects.duplicatedElement()
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(marcher)
    }


    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
