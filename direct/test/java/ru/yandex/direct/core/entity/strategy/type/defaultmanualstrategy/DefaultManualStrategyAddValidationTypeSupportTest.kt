package ru.yandex.direct.core.entity.strategy.type.defaultmanualstrategy

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.isNull
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
internal class DefaultManualStrategyAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    @Test
    fun `add valid strategy`() {
        val strategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `validation fail on strategy with invalid cpc hold flag`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val strategy = clientDefaultManualStrategy()
            .withEnableCpcHold(null)
            .withCids(listOf(campaign.campaignId))


        val vr = prepareAndApplyInvalid(listOf(strategy))

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(DefaultManualStrategy.ENABLE_CPC_HOLD)),
                notNull()
            )
        )

        vr.check(matcher)
    }

    @Test
    fun `validation fail on strategy with invalid cpc hold flag with performance campaign`() {
        val campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo)

        val strategy = clientDefaultManualStrategy()
            .withEnableCpcHold(false)
            .withCids(listOf(campaign.campaignId))


        val vr = prepareAndApplyInvalid(listOf(strategy))

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(DefaultManualStrategy.ENABLE_CPC_HOLD)),
                isNull()
            )
        )

        vr.check(matcher)
    }

}
