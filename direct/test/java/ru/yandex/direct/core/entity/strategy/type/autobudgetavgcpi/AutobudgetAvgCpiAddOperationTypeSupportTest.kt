package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpi

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpiAddOperationTypeSupportTest : StrategyAddOperationTestBase() {
    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    private lateinit var user: UserInfo

    @Before
    fun setUp() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `mobile content campaign with default cpi goal id`() {
        val cid = user.clientInfo?.let { steps.mobileContentCampaignSteps().createDefaultCampaign(it).id }
        val strategy = autobudgetAvgCpi()
            .withGoalId(DEFAULT_CPI_GOAL_ID)
            .withCids(listOf(cid))

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetAvgCpi::class.java
        )[id]!!

        assert(actualStrategy.goalId == null)
    }

    @Test
    fun `mobile content campaign with not default cpi goal id`() {
        steps.featureSteps()
            .addClientFeature(getClientId(), FeatureName.RMP_STAT_ASSOCIATED_INSTALL_ENABLED, true)
        val cid = user.clientInfo?.let { steps.mobileContentCampaignSteps().createDefaultCampaign(it).id }
        val strategy = autobudgetAvgCpi()
            .withGoalId(6L)
            .withCids(listOf(cid))

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetAvgCpi::class.java
        )[id]!!

        assert(actualStrategy.goalId.equals(6L))
    }

    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}

