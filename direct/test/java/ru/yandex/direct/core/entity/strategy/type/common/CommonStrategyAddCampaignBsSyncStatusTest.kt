package ru.yandex.direct.core.entity.strategy.type.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

@CoreTest
@RunWith(SpringRunner::class)
class CommonStrategyAddCampaignBsSyncStatusTest: StrategyAddOperationTestBase() {

    private lateinit var clientInfo: ClientInfo

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    private var walletId: Long = 0

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

    }

    @Test
    fun addStrategyWithCampaignId_ResetCampaignBsSyncStatus() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withCids(listOf(campaign.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
            .withEnableCpcHold(false)

        prepareAndApplyValid(listOf(strategy))

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(campaign.id),
            CampaignWithPackageStrategy::class.java
        )[0]

        assertThat(actualCampaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
    }



}
