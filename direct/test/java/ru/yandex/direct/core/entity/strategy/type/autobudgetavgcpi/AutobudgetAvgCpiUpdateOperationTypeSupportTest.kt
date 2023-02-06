package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpi

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpiUpdateOperationTypeSupportTest : StrategyUpdateOperationTestBase() {
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

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply().get(0).result

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(listOf(cid), AutobudgetAvgCpi.CIDS)
            .process(DEFAULT_CPI_GOAL_ID, AutobudgetAvgCpi.GOAL_ID)

        prepareAndApplyValid(listOf(modelChanges))

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

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply().get(0).result

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(listOf(cid), AutobudgetAvgCpi.CIDS)
            .process(6L, AutobudgetAvgCpi.GOAL_ID)

        prepareAndApplyValid(listOf(modelChanges))

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

