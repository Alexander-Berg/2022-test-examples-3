package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import java.util.function.Consumer
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyCollection
import org.mockito.Mockito.anyList
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.util.RepositoryUtils
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.clientTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.uc.repository.OneshotUacCampaignRepository
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow
import ru.yandex.grut.objects.proto.client.Schema

@OneshotTest
@RunWith(SpringRunner::class)
class DeleteGrutMissingUacCampaignsOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var oneshotUacCampaignRepository: OneshotUacCampaignRepository

    @Autowired
    lateinit var campaignRepository: CampaignRepository

    @Autowired
    lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var grutUacCampaingService: GrutUacCampaignService

    @Autowired
    lateinit var campaignService: CampaignService

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var shardHelper: ShardHelper

    lateinit var ytProvider: YtProvider
    lateinit var operator: YtOperator
    lateinit var deleteGrutMissingUacCampaigns: DeleteGrutMissingUacCampaignsOneshot
    lateinit var clientInfo: ClientInfo

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        ytProvider = mock()
        operator = mock()
        grutUacCampaingService = spy(grutUacCampaingService)
        //в окружении нет грута, поэтому не нужно ходить в реальный грут
        val list = listOf<Schema.TCampaign>()
        doReturn(list).`when`(grutUacCampaingService).getCampaigns(anyCollection())

        deleteGrutMissingUacCampaigns = DeleteGrutMissingUacCampaignsOneshot(
            ytProvider,
            dslContextProvider,
            shardHelper,
            oneshotUacCampaignRepository,
            grutUacCampaingService,
            campaignService,
            clientRepository
        )

        whenever(ytProvider.getOperator(any())).thenReturn(operator)
        whenever(operator.exists(any())).thenReturn(true)
    }

    @Test
    fun test_JobFlow_success() {
        val campaign = createTextCampaignAndInitDummies()
        testCampaignRepository.setSource(clientInfo.shard, campaign.id, CampaignSource.UAC)

        val input = DeleteGrutMissingUacCampaignsParam(YtCluster.HAHN, "path", 1, 0)

        val result = deleteGrutMissingUacCampaigns.execute(input, null)
        val testDelete = campaignRepository
            .getCampaigns(clientInfo.shard, listOf(campaign.id)).first()
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка статуса")
                .isNotNull
            assertThat(result?.lastRow)
                .`as`("Проверка чанка")
                .isEqualTo(1)
            assertThat(result?.deletedCount)
                .`as`("Проверка количества удалений")
                .isEqualTo(1)
            assertThat(testDelete.statusEmpty)
                .`as`("Проверка факта удаления")
                .isTrue
        }.assertAll()
    }

    @Test
    fun test_JobFlow_wrongCampaign_no_delete() {
        val campaign = createTextCampaignAndInitDummies()

        val input = DeleteGrutMissingUacCampaignsParam(YtCluster.HAHN, "path", 1, 0)

        val result = deleteGrutMissingUacCampaigns.execute(input, null)
        val testDelete = campaignRepository
            .getCampaigns(clientInfo.shard, listOf(campaign.id)).first()
        SoftAssertions().apply {
            assertThat(result)
                .`as`("Проверка статуса")
                .isNotNull
            assertThat(result?.lastRow)
                .`as`("Проверка чанка")
                .isEqualTo(1)
            assertThat(result?.deletedCount)
                .`as`("Проверка количества удалений")
                .isEqualTo(0)
            assertThat(testDelete.statusEmpty)
                .`as`("Проверка факта отсутствия удаления")
                .isFalse
        }.assertAll()
    }

    private fun createTextCampaignAndInitDummies(): Campaign {
        val textCampaign = createTextCampaignInfo(clientInfo)
        val dummyCampaign = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign)
        val campaign = campaignRepository
            .getCampaigns(clientInfo.shard, listOf(dummyCampaign.campaign.id))
            .first()

        doAnswer {
            val field = YtField("cid", Long::class.javaObjectType)
            val row = YtTableRow(listOf(field))
            row.setValue(field, campaign.id)
            val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
            consumer.accept(row)
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())

        return campaign
    }

    private fun createTextCampaignInfo(clientInfo: ClientInfo): TextCampaignInfo {
        val campaign = clientTextCampaign()
        campaign.fio = "FIO"
        campaign.statusEmpty = false
        campaign.statusArchived = false
        campaign.statusModerate = CampaignStatusModerate.NEW
        campaign.statusPostModerate = CampaignStatusPostmoderate.NEW
        campaign.statusShow = true
        campaign.statusActive = false
        campaign.statusBsSynced = CampaignStatusBsSynced.SENDING
        campaign.timeZoneId = TestCampaigns.DEFAULT_TIMEZONE_ID
        campaign.lastChange = RepositoryUtils.NOW_PLACEHOLDER
        campaign.productId = TestTextCampaigns.TEXT_CAMPAIGN_PRODUCT_ID
        campaign.currency = CurrencyCode.RUB
        campaign.walletId = TestCampaigns.EMPTY_WALLET_ID
        campaign.agencyId = 0L
        campaign.sum = BigDecimal.ZERO
        campaign.sumSpent = BigDecimal.ZERO
        campaign.sumLast = BigDecimal.ZERO
        campaign.sumToPay = BigDecimal.ZERO
        campaign.paidByCertificate = false
        campaign.isServiceRequested = false
        campaign.isSkadNetworkEnabled = false
        campaign.strategyId = 0
        campaign.orderId = 0

        return steps.textCampaignSteps().createCampaign(clientInfo, campaign)
    }
}
