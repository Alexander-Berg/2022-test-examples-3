package ru.yandex.direct.oneshot.oneshots.uc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDateTime
import java.util.function.Consumer
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import kotlin.reflect.jvm.isAccessible
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.GrutTestHelpers
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.TargetStatus
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
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.EBriefStatus.BS_STARTED
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief.EBriefStatus.BS_STOPPED
import ru.yandex.grut.objects.proto.client.Schema

@GrutOneshotTest
@RunWith(JUnitParamsRunner::class)
class UacUpdateCampaignTargetStatusOneshotTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

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

    lateinit var uacUpdateCampaignTargetStatusOneshot: UacUpdateCampaignTargetStatusOneshot
    lateinit var ytProvider: YtProvider
    lateinit var operator: YtOperator
    lateinit var shardHelper: ShardHelper
    lateinit var clientInfo: ClientInfo
    var shard: Int = 0

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        ytProvider = mock()
        operator = mock()
        shardHelper = mock()

        uacUpdateCampaignTargetStatusOneshot = UacUpdateCampaignTargetStatusOneshot(
            ytProvider,
            dslContextProvider,
            shardHelper,
            grutUacCampaingService,
            grutApiService,
            grutTransactionProvider,
            ppcPropertiesSupport,
        )

        whenever(ytProvider.getOperator(any())).thenReturn(operator)
        whenever(operator.exists(any())).thenReturn(true)

        grutApiService.clientGrutDao.createOrUpdateClient(
            ClientGrutModel(
                client = Client()
                    .withId(clientInfo.clientId!!.asLong())
                    .withCreateDate(LocalDateTime.now()),
                ndsHistory = listOf()
            )
        )

        ppcPropertiesSupport.set(PpcPropertyNames.UC_UPDATE_CAMPAIGN_TARGET_STATUS_IDLE_TIME, "1")
    }

    fun testData() = listOf(
        listOf(false, true, false, BS_STARTED),
        listOf(false, false, true, BS_STOPPED),
        listOf(false, true, true, BS_STARTED),
        listOf(false, false, false, BS_STOPPED),
        listOf(true, true, false, BS_STOPPED),
        listOf(true, false, true, BS_STARTED),
        listOf(true, true, true, BS_STOPPED),
        listOf(true, false, false, BS_STOPPED),
        listOf(null, true, false, BS_STARTED),
        listOf(null, false, true, BS_STOPPED),
        listOf(null, true, true, BS_STARTED),
        listOf(null, false, false, BS_STOPPED),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("mysql campaign active: {1}, grut campaign active: {2} and draft: {0}, expect grut status: {3}")
    fun testOneshot(
        isDraft: Boolean?,
        activeCampaign: Boolean,
        activeGrutCampaign: Boolean,
        expectGrutTargetStatus: Campaign.TCampaignBrief.EBriefStatus,
    ) {
        val campaignInfo = createCampaign(activeCampaign)
        createGrutCampaign(campaignInfo, activeGrutCampaign, isDraft)
        mockReturnFromYt(campaignInfo.campaignId)

        val input = UacCampaignsParam(YtCluster.HAHN, "path", 0)

        uacUpdateCampaignTargetStatusOneshot.execute(input, null)

        val actualGrutCampaign = grutUacCampaingService.getCampaigns(listOf(campaignInfo.campaignId.toIdString()))[0]

        SoftAssertions().apply {
            assertThat(actualGrutCampaign.spec.campaignBrief.targetStatus)
                .`as`("Проверка статуса")
                .isEqualTo(expectGrutTargetStatus)
        }.assertAll()
    }

    private fun createCampaign(active: Boolean): CampaignInfo {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        testCampaignRepository.setSource(shard, campaignInfo.campaignId, CampaignSource.UAC)
        if (active) {
            testCampaignRepository.makeCampaignActive(shard, campaignInfo.campaignId)
        } else {
            testCampaignRepository.makeCampaignStopped(shard, campaignInfo.campaignId)
        }

        val constructor = ShardedData::class.constructors.first()
        constructor.isAccessible = true
        val data = constructor.call(mutableMapOf(shard to mutableListOf(campaignInfo.campaignId))) as ShardedData<Long>

        whenever(shardHelper.groupByShard(mutableListOf(campaignInfo.campaignId), ShardKey.CID))
            .thenReturn(data)
        return campaignInfo
    }

    private fun createGrutCampaign(
        campaignInfo: CampaignInfo,
        active: Boolean,
        isDraft: Boolean? = null,
    ): Schema.TCampaign {
        val directCampaignStatus =
            if (isDraft == true) DirectCampaignStatus.DRAFT
            else if (isDraft == false) DirectCampaignStatus.CREATED
            else null
        val campaignSpec = UacGrutCampaignConverter.toCampaignSpec(
            createYdbCampaign(
                targetStatus = if (active) TargetStatus.STARTED else TargetStatus.STOPPED,
                directCampaignStatus = directCampaignStatus,
            )
        )
        val grutCampaign: Schema.TCampaign = GrutTestHelpers.buildCreateCampaignRequest(
            clientId = clientInfo.clientId!!.asLong(),
            campaignId = campaignInfo.campaign.id,
            campaignSpec = campaignSpec
        )

        grutApiService.briefGrutApi.createBrief(grutCampaign)
        return grutCampaign
    }

    private fun mockReturnFromYt(campaignId: Long) {
        doAnswer {
            val field = YtField("cid", Long::class.javaObjectType)
            val row = YtTableRow(listOf(field))
            row.setValue(field, campaignId)
            val consumer = it.getArgument(1, Consumer::class.java) as Consumer<YtTableRow>
            consumer.accept(row)
        }.whenever(operator)
            .readTableByRowRange(any(), any<Consumer<YtTableRow>>(), any(), any(), any())
    }
}
