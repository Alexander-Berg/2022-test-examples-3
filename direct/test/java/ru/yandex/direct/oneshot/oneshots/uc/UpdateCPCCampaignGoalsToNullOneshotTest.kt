package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import java.util.function.Consumer
import kotlin.reflect.jvm.isAccessible
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.sharding.ShardKey
import ru.yandex.direct.dbutil.sharding.ShardedData
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.uc.repository.OneshotUacCampaignRepository
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow

@OneshotTest
@RunWith(SpringRunner::class)
class UpdateCPCCampaignGoalsToNullOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    lateinit var shardHelper: ShardHelper
    lateinit var ytProvider: YtProvider
    lateinit var operator: YtOperator
    lateinit var bsResyncService: BsResyncService
    lateinit var oneshotUacCampaignRepository: OneshotUacCampaignRepository
    lateinit var oneshot: UpdateCPCCampaignGoalsToNullOneshot

    lateinit var clientInfo: ClientInfo

    @Before
    fun init() {
        ytProvider = mock()
        bsResyncService = mock()
        operator = mock()
        oneshotUacCampaignRepository = mock()
        shardHelper = mock()

        oneshot = UpdateCPCCampaignGoalsToNullOneshot(
            ytProvider,
            dslContextProvider,
            bsResyncService,
            oneshotUacCampaignRepository,
            shardHelper
        )

        clientInfo = steps.clientSteps().createDefaultClient()
        whenever(ytProvider.getOperator(any())).thenReturn(operator)
    }

    @Test
    fun testGeneralFlow_Success() {
        mockData()
        whenever(oneshotUacCampaignRepository.updateCampaignStrategyData(any(), any(), any()))
            .thenReturn(true)
        whenever(bsResyncService.addObjectsToResync(any()))
            .thenReturn(1L)

        val result = oneshot.execute(
            UpdateCPCParam(YtCluster.HAHN, "path", 0, 1),
            null
        )
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка статуса")
                .isNotNull
            assertThat(result?.lastRow)
                .`as`("Проверка чанка")
                .isEqualTo(1)
            assertThat(result?.countUpdated)
                .isEqualTo(1)
        }.assertAll()
    }

    @Test
    fun testGeneralFlow_Fail() {
        mockData()
        whenever(oneshotUacCampaignRepository.updateCampaignStrategyData(any(), any(), any()))
            .thenReturn(false)
        whenever(bsResyncService.addObjectsToResync(any()))
            .thenReturn(0L)

        val result = oneshot.execute(
            UpdateCPCParam(YtCluster.HAHN, "path", 0, 1),
            null
        )
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка статуса")
                .isNotNull
            assertThat(result?.lastRow)
                .`as`("Проверка чанка")
                .isEqualTo(1)
            assertThat(result?.countUpdated)
                .isZero
        }.assertAll()
    }

    private fun mockData() {
        val constructor = ShardedData::class.constructors.first()
        constructor.isAccessible = true
        val data = constructor.call(mutableMapOf(1 to mutableListOf(1000L))) as ShardedData<Long>

        whenever(shardHelper.groupByShard(mutableListOf(1000L), ShardKey.CID))
            .thenReturn(data)

        whenever(operator.exists(any()))
            .thenReturn(true)

        doAnswer {
            val field = YtField("cid", Long::class.javaObjectType)
            val row = YtTableRow(listOf(field))
            row.setValue(field, 1000L)
            val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
            consumer.accept(row)
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())

        val strategyData = StrategyData()
            .withVersion(1L)
            .withSum(BigDecimal("330"))
            .withName("autobudget_avg_click")
            .withAvgBid(BigDecimal("4.2"))
            .withGoalId(0L)

        val strategy = DbStrategy()
        strategy.strategyName = StrategyName.AUTOBUDGET_AVG_CLICK
        strategy.strategyData = strategyData

        whenever(oneshotUacCampaignRepository.getRMPCampaignsByIdAndStrategyNameWithGoalsFromUac(
            any(), any(), any(), any()
        )).thenReturn(mapOf(1000L to strategy))
    }
}
