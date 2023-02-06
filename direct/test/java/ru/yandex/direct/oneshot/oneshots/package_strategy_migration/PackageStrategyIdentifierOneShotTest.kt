package ru.yandex.direct.oneshot.oneshots.package_strategy_migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.dbschema.ppc.tables.Strategies
import ru.yandex.direct.dbschema.ppcdict.Tables
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.LastPosition
import ru.yandex.direct.oneshot.oneshots.CampaignMigrationBaseOneshot.Companion.Position
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful

@OneshotTest
@RunWith(SpringJUnit4ClassRunner::class)
@Ignore("Тест для локального запуска и проверки ваншота. При запуске в CI афектит стратегии в параллельных тестах.")
class PackageStrategyIdentifierOneShotTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Autowired
    private lateinit var oneshot: PackageStrategyIdentifierOneShot

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    private fun startPosition() = LastPosition(Position(1))

    @Test
    fun `successfully process campaign with correct strategy id`() {
        val campaign = createTextCampaign()

        val actualCampaign = campaignTypedRepository.getTyped(getShard(), listOf(campaign.id))
            .mapNotNull { it as? TextCampaign }
            .first()
        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(campaign.strategyId))
            .first()
        oneshot.execute(
            Unit,
            startPosition(),
            getShard()
        )
        val campaignAfterOneshot = campaignTypedRepository.getTyped(getShard(), listOf(campaign.id))
            .mapNotNull { it as? TextCampaign }
            .first()
        val deletedStrategy =
            strategyTypedRepository.getTyped(getShard(), listOf(actualStrategy.id))
        val strategyAfterOneshot =
            strategyTypedRepository.getTyped(getShard(), listOf(campaignAfterOneshot.strategyId))
                .first()
        assertThat(campaignAfterOneshot.strategyId).isNotEqualTo(actualCampaign.strategyId)
        assertThat(deletedStrategy).isEmpty()
        assertThat(strategyAfterOneshot.id).isEqualTo(campaignAfterOneshot.strategyId)
        assertThat(strategyAfterOneshot.id).isEqualTo(campaignAfterOneshot.orderId)
    }

    @Test
    fun `successfully process campaign without order id (order_id = 0)`() {
        val campaign = createTextCampaign(orderId = 0)

        val actualCampaign = campaignTypedRepository.getTyped(getShard(), listOf(campaign.id))
            .first()
        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(campaign.strategyId))
            .first()
        oneshot.execute(
            Unit,
            startPosition(),
            getShard()
        )
        val campaignAfterOneshot = campaignTypedRepository.getTyped(getShard(), listOf(campaign.id))
            .first()
        val strategyAfterOneshot =
            strategyTypedRepository.getTyped(getShard(), listOf(actualStrategy.id))
                .first()
        assertThat(campaignAfterOneshot).isEqualTo(actualCampaign)
        assertThat(strategyAfterOneshot).isEqualTo(actualStrategy)
    }

    @Test
    fun `successfully update incorrect strategy id (mass operation)`() {
        val count = 10
        val campaigns = (0 until count).map { createTextCampaign() }

        val strategies = campaigns.map {
            val incorrectStrategyId = RandomNumberUtils.nextPositiveInteger().toLong()
            val strategy = createStrategy(listOf(it.id), incorrectStrategyId)
            deleteStrategy(it.strategyId)
            strategy
        }

        oneshot.execute(
            Unit,
            startPosition(),
            getShard()
        )
        val campaignsAfterOneshot = campaignTypedRepository.getTyped(getShard(), campaigns.map { it.id })
            .mapNotNull { it as? TextCampaign }

        val deletedStrategyIds = strategyTypedRepository.getTyped(getShard(), strategies.map { it.id })
        val strategiesAfterOneshot =
            strategyTypedRepository.getTyped(getShard(), campaignsAfterOneshot.map { it.strategyId })
                .associateBy { it.id }
        softly {
            assertThat(deletedStrategyIds).isEmpty()
            assertThat(getStrategyIds(strategies.map { it.id })).isEmpty()
            assertThat(getStrategyIds(strategiesAfterOneshot.keys.toList())).hasSize(count)

            assertThat(campaignsAfterOneshot.map { it.id }).containsExactlyInAnyOrder(*(campaigns.map { it.id }
                .toTypedArray()))
            campaignsAfterOneshot.forEach { campaignAfterOneshot ->
                val strategy = strategiesAfterOneshot[campaignAfterOneshot.strategyId]
                assertThat(campaignAfterOneshot.strategyId).isEqualTo(campaignAfterOneshot.orderId)
                assertThat(strategy?.id).isEqualTo(campaignAfterOneshot.strategyId)
            }
        }
    }

    private fun getStrategyIds(ids: List<Long>): List<Long> {
        return ppcDslContextProvider.ppcdict()
            .select(Tables.SHARD_INC_STRATEGY_ID.STRATEGY_ID)
            .from(Tables.SHARD_INC_STRATEGY_ID)
            .where(Tables.SHARD_INC_STRATEGY_ID.STRATEGY_ID.`in`(ids))
            .fetch()
            .map { it.value1() }
    }

    private fun deleteStrategy(id: Long) {
        ppcDslContextProvider.ppc(getShard())
            .deleteFrom(Strategies.STRATEGIES)
            .where(Strategies.STRATEGIES.STRATEGY_ID.eq(id))
            .execute()
    }

    private fun createStrategy(
        cids: List<Long> = emptyList(),
        id: Long? = null
    ): CommonStrategy {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetCrr()
            .withGoalId(goalId.toLong())
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withCids(cids)
            .withId(id)
        val addOperationResult = createAddOperation(
            listOf(strategy),
            StrategyOperationOptions(isStrategyIdsAlreadyFilled = true)
        ).prepareAndApply()

        assertThat(addOperationResult).`is`(matchedBy(isFullySuccessful<Long>()))
        return strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()
    }

    private fun createTextCampaign(orderId: Long? = null): TextCampaign {
        val actualOrderId = orderId ?: RandomNumberUtils.nextPositiveInteger()
        val textCampaign = TestTextCampaigns.fullTextCampaign()
            .withOrderId(actualOrderId.toLong())
        val info = TextCampaignInfo(clientInfo = clientInfo, typedCampaign = textCampaign)
        val campaignInfo = steps.textCampaignSteps().createCampaign(info)
        return campaignTypedRepository.getTyped(getShard(), listOf(campaignInfo.id))
            .mapNotNull { it as TextCampaign }
            .first()
    }
}
