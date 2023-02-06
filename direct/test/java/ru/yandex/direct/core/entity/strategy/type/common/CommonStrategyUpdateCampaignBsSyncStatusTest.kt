package ru.yandex.direct.core.entity.strategy.type.common

import java.math.BigDecimal
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
class CommonStrategyUpdateCampaignBsSyncStatusTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    private var walletId: Long = 0

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var campaignRepository: CampaignRepository

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    @Test
    fun updateCampaignIds_ResetCampaignBsSync() {
        val strategy = steps.autobudgetAvgCpaSteps().createDefaultStrategy(clientInfo);
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val modelChanges = ModelChanges(strategy.strategyId, CommonStrategy::class.java)
            .process(listOf(campaign.id), CommonStrategy.CIDS)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        updateOperation.prepareAndApply()

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(campaign.id),
            CampaignWithPackageStrategy::class.java
        )[0]

        Assertions.assertThat(actualCampaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
    }

    @Test
    fun updateField_ResetCampaignBsSync() {
        val strategy = steps.autobudgetAvgCpaSteps().createDefaultStrategyWithCampaign(clientInfo);

        val modelChanges = ModelChanges(strategy.strategyId, AutobudgetAvgCpa::class.java)
            .process(strategy.typedStrategy.avgCpa.add(BigDecimal.ONE), AutobudgetAvgCpa.AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        updateOperation.prepareAndApply()

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(strategy.campaignIds.first()),
            CampaignWithPackageStrategy::class.java
        )[0]

        Assertions.assertThat(actualCampaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
    }

    @Test
    fun updateWithoutRealChanges_NotResetCampaignBsSync() {
        val strategy = steps.autobudgetAvgCpaSteps().createDefaultStrategyWithCampaign(clientInfo);

        val modelChanges = ModelChanges(strategy.strategyId, AutobudgetAvgCpa::class.java)
            .process(strategy.campaignIds, CommonStrategy.CIDS)
            .process(strategy.typedStrategy.avgCpa, AutobudgetAvgCpa.AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        updateOperation.prepareAndApply()

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(strategy.campaignIds.first()),
            CampaignWithPackageStrategy::class.java
        )[0]

        Assertions.assertThat(actualCampaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.YES)
    }
}
