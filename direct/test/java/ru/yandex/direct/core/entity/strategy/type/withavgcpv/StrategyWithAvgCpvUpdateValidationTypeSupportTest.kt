package ru.yandex.direct.core.entity.strategy.type.withavgcpv

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpv
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpv.autobudgetAvgCpv
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgCpvUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: ClientInfo

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetAvgCpv()
            .withAvgCpv(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpv::class.java)
            .process(BigDecimal.valueOf(2), StrategyWithAvgCpv.AVG_CPV)

        val result = prepareAndApplyValid(listOf(modelChanges))
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    @Test
    fun `fail on invalid AVG_CPV`() {
        val strategy = autobudgetAvgCpv()
            .withAvgCpv(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpv::class.java)
            .process(null, StrategyWithAvgCpv.AVG_CPV)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgCpv.AVG_CPV)),
                    CommonDefects.notNull()
                )
            )
        )
    }

    override fun getShard() = user.shard

    override fun getClientId(): ClientId = user.clientId!!

    override fun getOperatorUid(): Long = user.uid

}
