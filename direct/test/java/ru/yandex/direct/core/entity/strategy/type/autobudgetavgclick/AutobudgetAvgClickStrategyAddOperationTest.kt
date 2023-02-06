package ru.yandex.direct.core.entity.strategy.type.autobudgetavgclick

import java.math.BigDecimal
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgBid
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgClickStrategyAddOperationTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun shouldCreateStrategy() {
        val strategy = createStrategy()
        strategy.avgBid = BigDecimal.TEN
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun shouldReturnValidationError_ifValueIsNull() {
        val strategy = createStrategy()
        val result = prepareAndApplyInvalid(listOf(strategy))
        assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgBid.AVG_BID)),
                    CommonDefects.notNull()
                )
            )
        )
    }

    private fun createStrategy() = AutobudgetAvgClick()
        .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
        .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
        .withIsPublic(false)
}
