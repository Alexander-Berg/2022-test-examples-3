package ru.yandex.direct.core.entity.strategy.type.autobudgetweekbundle

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetWeekBundleStrategyUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetWeekBundle()
            .withLimitClicks(200)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetWeekBundle::class.java)
            .process(399, AutobudgetWeekBundle.LIMIT_CLICKS)

        val result = prepareAndApplyValid(listOf(modelChanges))
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    @Test
    fun `fail on invalid LIMIT_CLICKS`() {
        val strategy = autobudgetWeekBundle()
            .withLimitClicks(100)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetWeekBundle::class.java)
            .process(null, AutobudgetWeekBundle.LIMIT_CLICKS)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetWeekBundle.LIMIT_CLICKS)),
                    CommonDefects.notNull()
                )
            )
        )
    }

    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
