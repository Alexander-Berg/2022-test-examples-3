package ru.yandex.direct.jobs.statistics.activeorders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.statistics.model.ActiveOrderChanges
import ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.jobs.configuration.JobsTest


@JobsTest
@ExtendWith(SpringExtension::class)
class WalletMoneyCalculatorTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    private lateinit var calculator: WalletMoneyCalculator

    @Test
    fun calcMoneyForBannedClient() {
        val client = steps.clientSteps().createClient(
            TestClients.defaultClient()
                .withDebt("0.00".toBigDecimal())
                .withOverdraftLimit("0.00".toBigDecimal())
                .withAutoOverdraftLimit("3.00".toBigDecimal())
                .withStatusBalanceBanned(true)
        )

        val wallet = steps.campaignSteps().createCampaign(
            CampaignInfo()
                .withCampaign(
                    activeWalletCampaign(client.clientId, client.uid)
                        .withBalanceInfo(
                            emptyBalanceInfo(client.client?.workCurrency)
                                .withSumSpent("0".toBigDecimal())
                                .withSum("100".toBigDecimal())
                        )
                )
                .withClientInfo(client)
        )

        val camp = steps.campaignSteps().createCampaignUnderWallet(
            client,
            emptyBalanceInfo(client.client?.workCurrency)
                .withSumSpent("50".toBigDecimal())
                .withSum("0".toBigDecimal())
                .withWalletCid(wallet.campaignId)
        )

        var state = calculator.initState(
            client.shard, listOf(
                ActiveOrderChanges.Builder()
                    .withWalletCid(wallet.campaignId)
                    .build()
            )
        )

        assertThat(state.moneyWas[wallet.campaignId]).isTrue()

        testCampaignRepository.updateSums(client.shard, camp.campaignId, "0".toBigDecimal(), "150".toBigDecimal())

        var changedWalletIds = calculator.getChangedWalletIds(state = state)
        assertThat(changedWalletIds).hasSize(1)
    }
}
