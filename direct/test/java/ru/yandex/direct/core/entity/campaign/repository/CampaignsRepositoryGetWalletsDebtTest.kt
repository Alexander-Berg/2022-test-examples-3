package ru.yandex.direct.core.entity.campaign.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(SpringRunner::class)
class CampaignsRepositoryGetWalletsDebtTest {
    @Autowired
    lateinit var campaignRepository: CampaignRepository

    @Autowired
    lateinit var steps: Steps

    @Test
    fun getWalletsDebtSuccessPath() {
        val client = steps.clientSteps().createDefaultClient()
        val wallet = steps.campaignSteps().createWalletCampaign(client)
        val camp = steps.campaignSteps().createCampaignUnderWallet(
            client,
            emptyBalanceInfo(client.client?.workCurrency)
                .withSumSpent("123.45".toBigDecimal())
                .withWalletCid(wallet.campaignId)
        )

        val ret = campaignRepository.getWalletsDebt(camp.shard, listOf(wallet.campaignId))

        assertThat(ret).hasSize(1)
        assertThat(ret[wallet.campaignId]).isEqualByComparingTo("123.45")
    }
}
