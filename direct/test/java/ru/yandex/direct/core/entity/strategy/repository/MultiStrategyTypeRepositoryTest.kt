package ru.yandex.direct.core.entity.strategy.repository

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.container.StrategyRepositoryContainer
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpv.autobudgetAvgCpv
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpvCustomPeriodStrategy.clientAutobudgetAvgCpvCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsStrategy.clientAutobudgetMaxImpressionsStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy.autobudgetRoi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy.autobudget
import ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy.clientCpmDefaultStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestPeriodFixBidStrategy.clientPeriodFixBidStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@CoreTest
@RunWith(SpringRunner::class)
class MultiStrategyTypeRepositoryTest {
    @Autowired
    lateinit var ppcDslContextProvider: DslContextProvider

    @Autowired
    lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    lateinit var strategyModifyRepository: StrategyModifyRepository

    @Autowired
    lateinit var steps: Steps

    @Autowired
    private lateinit var walletService: WalletService

    @Autowired
    lateinit var shardHelper: ShardHelper

    private var walletId = 0L

    lateinit var clientInfo: ClientInfo

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    @Test
    fun `add strategies of different type in one batch`() {
        val strategiesToAdd = listOf(
            clientDefaultManualStrategy(),
            autobudgetCrr(),
            autobudgetAvgClick(),
            autobudgetAvgCpaPerCamp(),
            autobudgetAvgCpaPerFilter(),
            autobudgetAvgCpa(),
            autobudgetAvgCpcPerFilter(),
            autobudgetAvgCpi(),
            autobudgetAvgCpv(),
            clientAutobudgetAvgCpvCustomPeriodStrategy(),
            clientAutobudgetMaxImpressionsCustomPeriodStrategy(),
            clientAutobudgetMaxImpressionsStrategy(),
            clientAutobudgetMaxReachCustomPeriodStrategy(),
            clientAutobudgetReachStrategy(),
            autobudgetRoi(),
            autobudgetWeekBundle(),
            autobudget(),
            clientCpmDefaultStrategy(),
            clientPeriodFixBidStrategy()
        ).map { fillCommon(it) }

        val container = StrategyRepositoryContainer(
            clientInfo.shard,
            clientInfo.clientId!!,
            emptyMap(),
            false
        )
        val addedStrategiesCount = strategyModifyRepository.add(
            ppcDslContextProvider.ppc(clientInfo.shard),
            container,
            strategiesToAdd
        )

        val actualStrategies = strategyTypedRepository.getTyped(
            clientInfo.shard,
            strategiesToAdd.map { it.id }
        ).associateBy { it.id }

        SoftAssertions.assertSoftly { softly ->
            //добавили успешно все стратегии
            softly.assertThat(addedStrategiesCount.size).isEqualTo(strategiesToAdd.size)
            strategiesToAdd.forEach { expectedStrategy ->
                val actual = actualStrategies[expectedStrategy.id]
                softly.assertThat(actual?.type).isEqualTo(expectedStrategy.type)
            }
        }
    }

    private fun fillCommon(strategy: CommonStrategy): CommonStrategy =
        strategy
            .withClientId(clientInfo.client?.id)
            .withWalletId(walletId)
}
