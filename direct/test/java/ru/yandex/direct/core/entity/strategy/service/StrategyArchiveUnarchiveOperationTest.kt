package ru.yandex.direct.core.entity.strategy.service

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.strategyArchivingNotAllowed
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class StrategyArchiveUnarchiveOperationTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    private lateinit var autobudgetAvgCpa: AutobudgetAvgCpa

    @Before
    fun setUp() {
        autobudgetAvgCpa = autobudgetAvgCpa()
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    @Test
    fun shouldArchiveAndUnarchiveStrategies() {
        val id = addStrategy()
        autobudgetAvgCpa.id = id
        //archive
        var modelChanges = ModelChanges(id, CommonStrategy::class.java)
            .process(true, CommonStrategy.STATUS_ARCHIVED)

        createChangeStatusArchiveOperation(listOf(id), true).prepareAndApply()

        autobudgetAvgCpa.statusArchived = true
        autobudgetAvgCpa.lastBidderRestartTime = null
        autobudgetAvgCpa.lastChange = null

        var actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id), AutobudgetAvgCpa::class.java
        )[id]!!

        check(autobudgetAvgCpa, actualStrategy)

        //un-archive
        modelChanges = modelChanges.process(false, CommonStrategy.STATUS_ARCHIVED)

        createChangeStatusArchiveOperation(listOf(id), false).prepareAndApply()

        autobudgetAvgCpa.statusArchived = false
        actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id), AutobudgetAvgCpa::class.java
        )[id]!!

        check(autobudgetAvgCpa, actualStrategy)
    }

    @Test
    fun shouldReturnValidationError_ifStrategyNotFound() {
        val id = -1L

        val result = createChangeStatusArchiveOperation(listOf(id), true).prepareAndApply()

        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<Long>(validationError(path(index(0)), objectNotFound()))
            )
        )
    }

    @Test
    fun shouldReturnValidationError_ifStrategyHasCampaigns() {
        val campaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        campaignInfo.typedCampaign.strategyId

        val result = createChangeStatusArchiveOperation(listOf(campaignInfo.typedCampaign.strategyId), true)
            .prepareAndApply()

        assertThat(result.validationResult).`is`(
            matchedBy(
                hasDefectDefinitionWith<Long>(validationError(path(index(0)), strategyArchivingNotAllowed()))
            )
        )
    }

    @Test
    fun shouldReturnValidationError_ifStrategyNumberExceeded() {
        val strategy = autobudgetAvgCpa().withClientId(null)
            .withWalletId(null)
            .withIsPublic(true)
            .withStatusArchived(true)

        val id = createAddOperation(listOf(strategy), StrategyOperationOptions()).prepareAndApply()[0].result

        createChangeStatusArchiveOperation(listOf(id), true).prepareAndApply()

        val strategies = List(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER) {
            autobudgetAvgCpa().withClientId(null).withWalletId(null)
                .withIsPublic(true)
        }
        createAddOperation(strategies).prepareAndApply()

        val result = createChangeStatusArchiveOperation(listOf(id), false).prepareAndApply()

        assertThat(result.validationResult.errors).has(
            matchedBy(
                Matchers.contains(
                    StrategyDefects.unarchivedStrategiesNumberLimitExceeded(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER)
                )
            )
        )
    }

    private fun addStrategy(): Long {
        val addOperation = createAddOperation(listOf(autobudgetAvgCpa), StrategyOperationOptions())
        val result = addOperation.prepareAndApply()

        return result[0].result
    }

    private fun check(expectedStrategy: AutobudgetAvgCpa, actualStrategy: AutobudgetAvgCpa) {
        assertThat(actualStrategy).`is`(
            matchedBy(
                BeanDifferMatcher.beanDiffer(expectedStrategy)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    override fun getShard(): Int = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid
}
