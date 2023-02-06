package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.function.Consumer
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.smartchangestrategy.repository.OneshotCampaignRepository
import ru.yandex.direct.oneshot.oneshots.uc.repository.OneshotUacCampaignRepository
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow

@OneshotTest
@RunWith(SpringRunner::class)
class UpdateCPCCampaignGoalsToNullOneshotFullTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var oneshotUacCampaignRepository: OneshotUacCampaignRepository

    @Autowired
    lateinit var oneshotCampaignRepository: OneshotCampaignRepository

    @Autowired
    lateinit var campaignRepository: CampaignRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    lateinit var shardHelper: ShardHelper

    lateinit var ytProvider: YtProvider
    lateinit var bsResyncService: BsResyncService
    lateinit var updateCPCCampaignGoalsToNullOneshot: UpdateCPCCampaignGoalsToNullOneshot
    lateinit var operator: YtOperator

    lateinit var oneshot: UpdateCPCCampaignGoalsToNullOneshot

    @Before
    fun init() {
        ytProvider = mock()
        bsResyncService = mock()
        operator = mock()
        whenever(bsResyncService.addObjectsToResync(any())).thenReturn(1L)

        updateCPCCampaignGoalsToNullOneshot = UpdateCPCCampaignGoalsToNullOneshot(
            ytProvider,
            dslContextProvider,
            bsResyncService,
            oneshotUacCampaignRepository,
            shardHelper)

        whenever(ytProvider.getOperator(any())).thenReturn(operator)
        whenever(operator.exists(any())).thenReturn(true)
    }

    @Test
    fun test_JobFlow_successful() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val dummyCampaign = steps.mobileContentCampaignSteps().createDefaultCampaign(clientInfo)
        val campaign = campaignRepository
            .getCampaigns(clientInfo.shard, listOf(dummyCampaign.campaign.id))
            .first()

        testCampaignRepository.setSource(clientInfo.shard, campaign.id, CampaignSource.UAC)
        campaign.strategy.withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK)
        campaign.strategy.strategyData.withGoalId(0L)
        oneshotCampaignRepository.updateCampaignStrategy(
            dslContextProvider.ppc(clientInfo.shard),
            campaign)

        doAnswer {
            val field = YtField("cid", Long::class.javaObjectType)
            val row = YtTableRow(listOf(field))
            row.setValue(field, campaign.id)
            val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
            consumer.accept(row)
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())

        val input = UpdateCPCParam(YtCluster.HAHN, "path", 0, 1)
        val result = updateCPCCampaignGoalsToNullOneshot.execute(input, null)
        val campaignAfterUpdate = campaignRepository
            .getCampaigns(clientInfo.shard, listOf(dummyCampaign.campaign.id))
            .first()

        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка статуса")
                .isNotNull
            assertThat(result?.lastRow)
                .`as`("Проверка чанка")
                .isEqualTo(1)
            assertThat(result?.countUpdated)
                .`as`("Проверка количества удалений")
                .isEqualTo(1)
            assertThat(campaignAfterUpdate.strategy.strategyData.goalId as Long?)
                .`as`("Проверка исправления данных")
                .isEqualTo(null)
        }.assertAll()
    }
}
