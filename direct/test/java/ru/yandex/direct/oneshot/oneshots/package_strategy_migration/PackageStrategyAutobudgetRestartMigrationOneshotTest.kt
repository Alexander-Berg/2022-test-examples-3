package ru.yandex.direct.oneshot.oneshots.package_strategy_migration

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.autobudget.restart.repository.CampRestartData
import ru.yandex.direct.autobudget.restart.repository.CampaignAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.PackageStrategyAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.autobudget.restart.repository.StrategyRestartData
import ru.yandex.direct.autobudget.restart.service.Reason
import ru.yandex.direct.autobudget.restart.service.StrategyState
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.CampaignAutobudgetRestartUtils.getStrategyDto
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.PublicPackageStrategyAutobudgetRestartService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtOperator
import java.time.LocalDateTime
import java.util.function.Consumer

@OneshotTest
@RunWith(SpringJUnit4ClassRunner::class)
class PackageStrategyAutobudgetRestartMigrationOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var strategyAutobudgetMigrationService: StrategyAutobudgetMigrationService

    @Autowired
    lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    lateinit var walletService: WalletService

    @Autowired
    lateinit var campaignAutobudgetRestartRepository: CampaignAutobudgetRestartRepository

    @Autowired
    lateinit var strategyAutobudgetRestartRepository: PackageStrategyAutobudgetRestartRepository

    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    lateinit var oneshot: PackageStrategyAutobudgetRestartMigrationOneshot

    lateinit var clientInfo: ClientInfo

    lateinit var operator: YtOperator

    var walletId: Long = 0L

    @Before
    fun before() {
        val ytProvider = mock<YtProvider>()
        ppcPropertiesSupport = mock()
        val relaxTime = mock<PpcProperty<Int>>()
        val chunkSize = mock<PpcProperty<Long>>()
        operator = mock<YtOperator>()
        whenever(ytProvider.getOperator(any()))
            .thenReturn(operator)

        whenever(relaxTime.getOrDefault(any())).thenReturn(0)
        whenever(chunkSize.getOrDefault(any())).thenReturn(500L)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.PACKAGE_STRATEGY_AUTOBUDGET_RESTART_MIGRATION_BATCH_SIZE)))
            .thenReturn(chunkSize)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.PACKAGE_STRATEGY_AUTOBUDGET_RESTART_MIGRATION_RELAX_TIME)))
            .thenReturn(relaxTime)
        oneshot = PackageStrategyAutobudgetRestartMigrationOneshot(
            ytProvider,
            campaignTypedRepository,
            dslContextProvider,
            strategyAutobudgetMigrationService,
            ppcPropertiesSupport
        )
        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    fun inputData(rewriteOnDuplicate: Boolean = false) =
        PackageStrategyAutobudgetRestartMigrationOneshot.Companion.InputData(
            YtCluster.HAHN.name,
            "/test",
            rewriteOnDuplicate
        )

    @Test
    fun `do nothing on empty set`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)

        val result = oneshot.execute(inputData(), null, clientInfo.shard)

        MatcherAssert.assertThat(result, Matchers.`is`(Matchers.nullValue()))
        verify(operator, times(0)).readTableByKeyRange(any(), any(), any(), any<Long>(), any())
    }

    @Test
    fun `migrate campaign restarts`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        createCampaignAutobudgetRestart(campaign.typedCampaign)
        mock(listOf(campaign.id))
        oneshot.execute(inputData(), null, clientInfo.shard)

        checkRestarts(campaign.typedCampaign.strategyId, campaign.campaignId)
    }
    
    @Test
    fun `rewrite on duplicate`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = strategyTypedRepository
            .getTyped(clientInfo.shard, listOf(campaign.typedCampaign.strategyId))
            .mapNotNull { it as? CommonStrategy }
            .first()
        createCampaignAutobudgetRestart(campaign.typedCampaign)
        val strategyRestart = createStrategyAutobudgetRestart(strategy)

        mock(listOf(campaign.id))

        val actualStrategyRestart = strategyAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(campaign.typedCampaign.strategyId)
        ).first()

        val expectedCampaignRestart = campaignAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(campaign.id)
        ).first()

        oneshot.execute(inputData(rewriteOnDuplicate = true), null, clientInfo.shard)

        softly {
            assertThat(actualStrategyRestart.times).isNotEqualTo(expectedCampaignRestart)
            assertThat(actualStrategyRestart.state).isNotEqualTo(expectedCampaignRestart.state)
        }
    }

    @Test
    fun `migrate few campaign restarts`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)
        val cnt = 10
        val campaigns = (0 until cnt).map {
            val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
            createCampaignAutobudgetRestart(campaign.typedCampaign)
            campaign
        }
        mock(campaigns.map { it.campaignId })
        oneshot.execute(inputData(), null, clientInfo.shard)

        campaigns.forEach { campaign ->
            checkRestarts(campaign.typedCampaign.strategyId, campaign.campaignId)
        }
    }

    @Test
    fun `do not update strategy restart if exists`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = strategyTypedRepository
            .getTyped(clientInfo.shard, listOf(campaign.typedCampaign.strategyId))
            .mapNotNull { it as? CommonStrategy }
            .first()
        createCampaignAutobudgetRestart(campaign.typedCampaign)
        val expectedStrategyRestart = createStrategyAutobudgetRestart(strategy)

        mock(listOf(campaign.id))

        val actualStrategyRestart = strategyAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(campaign.typedCampaign.strategyId)
        ).first()

        val actualCampaignRestart = campaignAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(campaign.id)
        ).first()

        oneshot.execute(inputData(), null, clientInfo.shard)

        softly {
            assertThat(actualStrategyRestart).isEqualTo(expectedStrategyRestart)
            assertThat(actualStrategyRestart.times).isNotEqualTo(actualCampaignRestart)
            assertThat(actualStrategyRestart.state).isNotEqualTo(actualCampaignRestart.state)
        }
    }

    private fun mock(campaigns: List<Long>) {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(campaigns.size.toLong())

        whenever(operator.readTableByRowRange(any(), any(), any(), any<Long>(), any()))
            .thenAnswer { invocation ->
                campaigns.forEach {
                    val row = mock<PackageStrategyAutobudgetRestartMigrationOneshot.Companion.Row>()
                    whenever(row.cid).thenReturn(it)
                    invocation.getArgument<Consumer<PackageStrategyAutobudgetRestartMigrationOneshot.Companion.Row>>(1)
                        .accept(row)
                }
            }
    }

    private fun createCampaignAutobudgetRestart(campaign: TextCampaign) {
        val dto = getStrategyDto(
            campaign,
            true,
            false
        )
        val now = LocalDateTime.now()
        val restart = CampRestartData(
            campaign.id,
            campaign.orderId,
            dto,
            RestartTimes(
                now.minusHours(1),
                now,
                Reason.AUTOBUDGET_START.name
            ),
            StrategyState(now.plusHours(1))
        )
        campaignAutobudgetRestartRepository.saveAutobudgetRestartData(
            clientInfo.shard,
            listOf(restart)
        )
    }

    private fun createStrategyAutobudgetRestart(strategy: CommonStrategy): StrategyRestartData {
        val dto = PublicPackageStrategyAutobudgetRestartService.toStrategyDto(
            true,
            strategy
        )
        val now = LocalDateTime.now()
        val restart = StrategyRestartData(
            strategy.id,
            dto,
            RestartTimes(
                now.minusHours(2),
                now,
                Reason.AUTOBUDGET_START.name
            ),
            StrategyState(now.plusHours(2))
        )
        strategyAutobudgetRestartRepository.saveAutobudgetRestartData(
            dslContextProvider.ppc(clientInfo.shard),
            listOf(restart)
        )
        return strategyAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(strategy.id)
        ).first()
    }

    private fun checkRestarts(strategyId: Long, campaignId: Long) {
        val campaignRestart = campaignAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(campaignId)
        ).first()
        val strategyRestart = strategyAutobudgetRestartRepository.getAutobudgetRestartData(
            clientInfo.shard,
            listOf(strategyId)
        ).first()

        softly {
            assertThat(strategyRestart.state).isEqualTo(campaignRestart.state)
            assertThat(strategyRestart.times).isEqualTo(campaignRestart.times)
        }
    }
}
