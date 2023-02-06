package ru.yandex.direct.core.entity.strategy.service.add

import org.junit.Assert.assertThat
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.ValidationResult

abstract class StrategyAddOperationTestBase {
    @Autowired
    lateinit var strategyOperationFactory: StrategyOperationFactory

    @Autowired
    lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    lateinit var ppcDslContextProvider: DslContextProvider

    @Autowired
    protected lateinit var walletService: WalletService

    @Autowired
    lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    lateinit var steps: Steps

    fun prepareAndApplyValid(models: List<BaseStrategy>) {
        val result = createOperation(models).prepareAndApply()
        val defectsDescription = result.validationResult.flattenErrors().joinToString("\n\t") { it.toString() }
        assertThat("Unexpected errors: $defectsDescription", result, isFullySuccessful())
    }

    fun prepareAndApplyInvalid(models: List<BaseStrategy>): ValidationResult<Any, Defect<Any>>? {
        val result = createOperation(models).prepareAndApply()
        assertThat(result, isSuccessful(false))
        return result.validationResult as ValidationResult<Any, Defect<Any>>?
    }

    fun createOperation(
        models: List<BaseStrategy>,
        options: StrategyOperationOptions = StrategyOperationOptions()
    ) =
        strategyOperationFactory.createStrategyAddOperation(
            getShard(),
            getOperatorUid(),
            getClientId(),
            getClientUid(),
            models,
            options
        )

    protected fun stubGoals(counterId: Int, goalIds: List<Int>) {
        metrikaClientStub.addUserCounter(getOperatorUid(), counterId)
        goalIds.forEach { metrikaClientStub.addCounterGoal(counterId, it) }
    }

    protected fun stubGoals(counterId: Int, goalId: Int) = stubGoals(counterId, listOf(goalId))

    abstract fun getShard(): Int

    abstract fun getClientId(): ClientId

    abstract fun getOperatorUid(): Long

    protected open fun getClientUid(): Long = getOperatorUid()
}
