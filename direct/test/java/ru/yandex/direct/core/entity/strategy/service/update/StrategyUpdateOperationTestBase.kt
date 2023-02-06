package ru.yandex.direct.core.entity.strategy.service.update

import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.repository.StrategyModifyRepository
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory
import ru.yandex.direct.core.entity.strategy.validation.update.StrategyArchiveValidationService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.ValidationResult

abstract class StrategyUpdateOperationTestBase {
    @Autowired
    protected lateinit var strategyOperationFactory: StrategyOperationFactory

    @Autowired
    protected lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    protected lateinit var strategyModifyRepository: StrategyModifyRepository

    @Autowired
    lateinit var ppcDslContextProvider: DslContextProvider

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var walletService: WalletService

    @Autowired
    lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    lateinit var strategyArchiveValidationService: StrategyArchiveValidationService

    protected inline fun <reified T : BaseStrategy> createUpdateOperation(
        modelChanges: List<ModelChanges<T>>,
        options: StrategyOperationOptions = StrategyOperationOptions()
    ) =
        strategyOperationFactory.createStrategyUpdateOperation(
            getShard(),
            getClientId(),
            getClientUid(),
            getOperatorUid(),
            options,
            modelChanges,
            T::class.java
        )

    protected fun <T : BaseStrategy> createUpdateOperation(
        modelChanges: List<ModelChanges<T>>,
        clazz: Class<T>,
        options: StrategyOperationOptions = StrategyOperationOptions()
    ) =
        strategyOperationFactory.createStrategyUpdateOperation(
            getShard(),
            getClientId(),
            getClientUid(),
            getOperatorUid(),
            options,
            modelChanges,
            clazz
        )

    protected fun createAddOperation(
        strategies: List<BaseStrategy>,
        options: StrategyOperationOptions = StrategyOperationOptions()
    ) =
        strategyOperationFactory.createStrategyAddOperation(
            getShard(),
            getOperatorUid(),
            getClientId(),
            getClientUid(),
            strategies,
            options
        )

    protected fun createChangeStatusArchiveOperation(strategyIds: List<Long>, statusArchive: Boolean) =
        strategyOperationFactory.createChangeStatusArchiveOperation(
            getShard(),
            getClientId(),
            strategyIds,
            statusArchive
        )

    protected fun stubGoals(counterId: Int, goalIds: List<Int>) {
        metrikaClientStub.addUserCounter(getOperatorUid(), counterId)
        goalIds.forEach { metrikaClientStub.addCounterGoal(counterId, it) }
    }

    protected inline fun <reified T : BaseStrategy> prepareAndApplyValid(models: List<ModelChanges<T>>): ValidationResult<Any, Defect<Any>>? {
        val result = createUpdateOperation(models).prepareAndApply()
        val defectsDescription = result.validationResult.flattenErrors().joinToString("\n\t") { it.toString() }
        Assert.assertThat("Unexpected errors: $defectsDescription", result, MassResultMatcher.isFullySuccessful())
        return result.validationResult as ValidationResult<Any, Defect<Any>>?
    }

    protected inline fun <reified T : BaseStrategy> prepareAndApplyInvalid(models: List<ModelChanges<T>>): ValidationResult<Any, Defect<Any>>? {
        val result = createUpdateOperation(models).prepareAndApply()
        Assert.assertThat(result, MassResultMatcher.isSuccessful(false))
        return result.validationResult as ValidationResult<Any, Defect<Any>>?
    }


    protected fun stubGoals(counterId: Int, goalId: Int) = stubGoals(counterId, listOf(goalId))

    abstract fun getShard(): Int

    abstract fun getClientId(): ClientId

    abstract fun getOperatorUid(): Long

    protected open fun getClientUid(): Long = getOperatorUid()
}
