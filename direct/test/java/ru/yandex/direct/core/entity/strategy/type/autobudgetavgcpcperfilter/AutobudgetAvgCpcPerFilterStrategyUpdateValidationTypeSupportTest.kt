package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcperfilter

import java.math.BigDecimal
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.strategy.AutobudgetAvgCpcPerFilterInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpcPerFilterStrategyUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetAvgCpcPerFilter()
            .withFilterAvgBid(BigDecimal.ONE)

        steps.autobudgetAvgCpcPerFilterSteps().createStrategy(AutobudgetAvgCpcPerFilterInfo(user.clientInfo!!, strategy))

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpcPerFilter::class.java)
            .process(BigDecimal.ONE, AutobudgetAvgCpcPerFilter.FILTER_AVG_BID)

        val result = prepareAndApplyValid(listOf(modelChanges))
        assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    @Test
    fun `fail on invalid FILTER_AVG_BID`() {
        val strategy = autobudgetAvgCpcPerFilter()

        steps.autobudgetAvgCpcPerFilterSteps().createStrategy(AutobudgetAvgCpcPerFilterInfo(user.clientInfo!!, strategy))

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpcPerFilter::class.java)
            .process(null, AutobudgetAvgCpcPerFilter.FILTER_AVG_BID)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetAvgCpcPerFilter.FILTER_AVG_BID)),
                    CommonDefects.notNull()
                )
            )
        )
    }

    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
