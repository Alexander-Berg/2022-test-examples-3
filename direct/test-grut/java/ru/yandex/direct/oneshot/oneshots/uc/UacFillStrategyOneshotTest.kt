package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.uac.GrutTestHelpers
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.sharding.ShardKey
import ru.yandex.direct.dbutil.sharding.ShardedData
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.GrutOneshotTest
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtField
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTableRow
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.client.Schema
import java.time.LocalDateTime
import java.util.function.Consumer
import kotlin.reflect.jvm.isAccessible

@GrutOneshotTest
@RunWith(SpringRunner::class)
class UacFillStrategyOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var grutUacCampaingService: GrutUacCampaignService

    @Autowired
    lateinit var grutApiService: GrutApiService

    @Autowired
    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    lateinit var grutTransactionProvider: GrutTransactionProvider

    @Autowired
    lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    lateinit var featureService: FeatureService

    lateinit var uacFillStrategyOneshot: UacFillStrategyOneshot
    lateinit var operator: YtOperator
    lateinit var uacConverterYtRepository: UacConverterYtRepository
    lateinit var shardHelper: ShardHelper
    lateinit var clientInfo: ClientInfo
    var shard: Int = 0

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        operator = mock()
        shardHelper = mock()
        uacConverterYtRepository = mock()

        uacFillStrategyOneshot = UacFillStrategyOneshot(
            uacConverterYtRepository,
            grutUacCampaignService,
            shardHelper,
            featureService,
            grutApiService,
            grutTransactionProvider,
        )

        whenever(operator.exists(any())).thenReturn(true)

        grutApiService.clientGrutDao.createOrUpdateClient(
            ClientGrutModel(
                client = Client()
                    .withId(clientInfo.clientId!!.asLong())
                    .withCreateDate(LocalDateTime.now()),
                ndsHistory = listOf()
            )
        )
    }

    @Test
    fun testOneshot() {
        val campaignInfo = createCampaign()
        createGrutCampaign(campaignInfo)

        whenever(uacConverterYtRepository.getCampaignIdsFromYtTable(any(), any(), any(), any())).thenReturn(listOf(campaignInfo.campaignId))
        whenever(shardHelper.getClientIdsByCampaignIds(any())).thenReturn(mapOf(campaignInfo.campaignId to campaignInfo.clientId.asLong()))

        val input = UacFillStrategyParam(YtCluster.HAHN, "path")

        var grutCampaign = grutUacCampaingService.getCampaigns(listOf(campaignInfo.campaignId.toIdString()))[0].toUacYdbCampaign()
        val soft = SoftAssertions()
        soft.assertThat(grutCampaign.strategy).isNull()
        uacFillStrategyOneshot.execute(input, null)

        grutCampaign = grutUacCampaingService.getCampaigns(listOf(campaignInfo.campaignId.toIdString()))[0].toUacYdbCampaign()
        soft.assertThat(grutCampaign.strategy).isNotNull
        soft.assertThat(grutCampaign.strategy!!.uacStrategyName).isEqualTo(UacStrategyName.AUTOBUDGET_AVG_CPI)
        soft.assertThat(grutCampaign.strategy!!.uacStrategyData.payForConversion).isEqualTo(false)
        soft.assertThat(grutCampaign.strategy!!.uacStrategyData.avgCpa).isNull()
        soft.assertThat(grutCampaign.strategy!!.uacStrategyData.avgCpi).isEqualTo(grutCampaign.cpa)
        soft.assertThat(grutCampaign.strategy!!.uacStrategyData.avgBid).isNull()
        soft.assertAll()
    }

    private fun createCampaign(): CampaignInfo {
        val campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo)
        testCampaignRepository.setSource(shard, campaignInfo.campaignId, CampaignSource.UAC)

        val constructor = ShardedData::class.constructors.first()
        constructor.isAccessible = true
        val data = constructor.call(mutableMapOf(shard to mutableListOf(campaignInfo.campaignId))) as ShardedData<Long>

        whenever(shardHelper.groupByShard(mutableListOf(campaignInfo.campaignId), ShardKey.CID))
            .thenReturn(data)
        return campaignInfo
    }

    private fun createGrutCampaign(campaignInfo: CampaignInfo): Schema.TCampaign {
        val campaignSpec = UacGrutCampaignConverter.toCampaignSpec(
            createYdbCampaign(
                targetStatus = TargetStatus.STARTED,
                advType = AdvType.MOBILE_CONTENT
            )
        )
        val grutCampaign: Schema.TCampaign = GrutTestHelpers.buildCreateCampaignRequest(
            clientId = clientInfo.clientId!!.asLong(),
            campaignId = campaignInfo.campaign.id,
            campaignSpec = campaignSpec,
            campaignType = Campaign.ECampaignTypeOld.CTO_MOBILE_APP
        )

        grutApiService.briefGrutApi.createBrief(grutCampaign)
        return grutCampaign
    }
}
