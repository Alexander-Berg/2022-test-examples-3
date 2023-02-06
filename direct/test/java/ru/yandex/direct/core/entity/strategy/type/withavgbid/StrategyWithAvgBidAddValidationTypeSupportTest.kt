package ru.yandex.direct.core.entity.strategy.type.withavgbid

import java.math.BigDecimal
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgBid
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgBidAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var user: ClientInfo

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `add valid strategy`() {
        val strategy = autobudgetAvgClick()
            .withAvgBid(BigDecimal.ONE)

        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `fail to add strategy with invalid AVG_BID`() {
        val strategy = autobudgetAvgClick()
            .withAvgBid(null)

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

    override fun getShard() = user.shard

    override fun getClientId(): ClientId = user.clientId!!

    override fun getOperatorUid(): Long = user.uid
}
