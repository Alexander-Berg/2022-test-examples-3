package ru.yandex.direct.core.entity.strategy.service.add

import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(SpringRunner::class)
class UpdateStrategyLastChangeTest: StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        steps.featureSteps()
            .addClientFeature(getClientId(), FeatureName.PACKAGE_STRATEGIES_STAGE_TWO, true)
    }

    @Test
    fun shouldUpdateLastChangeForOldStrategy() {
        //before
        val campaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val oldStrategyId = campaignInfo.typedCampaign.strategyId
        val oldStrategy = strategyTypedRepository.getTyped(getShard(), listOf(oldStrategyId))[0] as CommonStrategy

        val newStrategy = TestAutobudgetAvgClick.autobudgetAvgClick()
        newStrategy.cids = listOf(campaignInfo.campaignId)
        Thread.sleep(1000)

        //when
        val addOperation = createOperation(listOf(newStrategy), StrategyOperationOptions())
        val result = addOperation.prepareAndApply()

        //then
        val updatedOldStrategy = strategyTypedRepository.getTyped(getShard(), listOf(oldStrategyId))[0] as CommonStrategy
        assertNotEquals(oldStrategy.lastChange, updatedOldStrategy.lastChange)
    }

    override fun getShard(): Int = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid
}
