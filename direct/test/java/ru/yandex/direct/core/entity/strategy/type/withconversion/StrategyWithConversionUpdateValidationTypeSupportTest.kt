package ru.yandex.direct.core.entity.strategy.type.withconversion

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithConversionUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
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

        val strategy = autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId1.toLong())

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithConversion::class.java)
            .process(goalId2.toLong(), StrategyWithConversion.GOAL_ID)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(isFullySuccessful())
    }

    @Test
    fun `fail on unavailable goal`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val unavailableGoal = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithConversion::class.java)
            .process(unavailableGoal.toLong(), StrategyWithConversion.GOAL_ID)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        val marcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(index(0), field(StrategyWithConversion.GOAL_ID)),
                objectNotFound()
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(marcher)
    }


    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
