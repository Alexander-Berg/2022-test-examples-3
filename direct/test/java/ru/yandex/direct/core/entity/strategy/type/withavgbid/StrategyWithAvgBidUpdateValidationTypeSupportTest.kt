package ru.yandex.direct.core.entity.strategy.type.withavgbid

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgBidUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: ClientInfo

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetAvgClick()
            .withAvgBid(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetWeekBundle::class.java)
            .process(BigDecimal.TEN, AutobudgetAvgClick.AVG_BID)

        val result = prepareAndApplyValid(listOf(modelChanges))
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    @Test
    fun `fail on invalid AVG_BID`() {
        val strategy = autobudgetAvgClick()
            .withAvgBid(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgClick::class.java)
            .process(null, AutobudgetAvgClick.AVG_BID)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetAvgClick.AVG_BID)),
                    CommonDefects.notNull()
                )
            )
        )
    }

    override fun getShard() = user.shard

    override fun getClientId(): ClientId = user.clientId!!

    override fun getOperatorUid(): Long = user.uid

}
