package ru.yandex.direct.core.entity.strategy.type.autobudgetweekbundle

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetWeekBundleStrategyAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun shouldCreateStrategy() {
        val strategy = autobudgetWeekBundle()
            .withLimitClicks(200L)
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun shouldReturnValidationError_ifLimitClicksIsNull() {
        val strategy = autobudgetWeekBundle()
            .withLimitClicks(null)
        val result = prepareAndApplyInvalid(listOf(strategy))
        assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetWeekBundle.LIMIT_CLICKS)),
                    CommonDefects.notNull()
                )
            )
        )
    }

}
