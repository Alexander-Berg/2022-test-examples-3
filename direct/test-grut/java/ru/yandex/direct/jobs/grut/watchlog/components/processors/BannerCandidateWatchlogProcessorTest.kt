package ru.yandex.direct.jobs.grut.watchlog.components.processors

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.uac.grut.NonTransactionalGrutContext
import ru.yandex.direct.core.grut.api.CampaignGrutApi
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.grut.client.GrutClient
import ru.yandex.grut.objects.proto.client.Schema.EObjectType
import ru.yandex.grut.objects.proto.client.Schema.TBannerCandidateMeta
import ru.yandex.grut.proto.transaction_context.TransactionContext.TTransactionContext
import ru.yandex.grut.watchlog.Watch.TEvent

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCandidateWatchlogProcessorTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var grutClient: GrutClient

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var grutSteps: GrutSteps

    private lateinit var campaignGrutApi: CampaignGrutApi
    private lateinit var bannerCandidateWatchlogProcessor: BannerCandidateWatchlogProcessor

    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo

    private lateinit var campaignInfo2: CampaignInfo

    @BeforeEach
    fun beforeEach() {
        campaignGrutApi = CampaignGrutApi(NonTransactionalGrutContext(grutClient))

        userInfo = steps.userSteps().createDefaultUser()
        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.clientInfo)
        createCampaignInGrut(campaignInfo)
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)

        campaignInfo2 = steps.campaignSteps().createDefaultCampaign()
        createCampaignInGrut(campaignInfo2)

        bannerCandidateWatchlogProcessor = BannerCandidateWatchlogProcessor(
            ppcPropertiesSupport,
            grutClient,
            { mock() },
            "watchlogConsumerPrefix",
            shardHelper,
        )
    }

    private fun createCampaignInGrut(campaignInfo: CampaignInfo) {
        grutSteps.createClient(campaignInfo.clientInfo)
        val campaign = campaignTypedRepository.getTyped(campaignInfo.shard, listOf(campaignInfo.campaignId)).first() as CommonCampaign
        campaignGrutApi.createOrUpdateCampaign(CampaignGrutModel(campaign, 0))
        campaignInfo.campaign.orderId = campaignGrutApi.getCampaignByDirectId(campaignInfo.campaignId)!!.meta.id
    }

    @Test
    fun shardEnricherTest() {
        val transactionContextWithShard = TTransactionContext.newBuilder().apply {
            shard = 15
            operatorUid = 123
        }.build()
        val transactionContextWithoutShard = TTransactionContext.newBuilder().apply {
            operatorUid = 456
        }.build()

        val eventWithShard = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 1L
                campaignId = 555666L
            }.build().toByteString()
            transactionContext = transactionContextWithShard
        }.build()
        val eventWithShard2 = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 2L
                campaignId = 777888L
            }.build().toByteString()
            transactionContext = transactionContextWithShard
        }.build()
        val eventWithoutShard = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 3L
                campaignId = campaignInfo.orderId
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()
        val eventWithoutShard2 = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 4L
                campaignId = campaignInfo2.orderId
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()
        val eventWithoutShard3 = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 5L
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()

        val events = listOf(
            eventWithShard,
            eventWithoutShard,
            eventWithShard2,
            eventWithoutShard2,
            eventWithoutShard3,
            eventWithoutShard,
        )

        val enrichedEvents = bannerCandidateWatchlogProcessor.enrichEvents(events)

        SoftAssertions().apply {
            assertThat(enrichedEvents).hasSameSizeAs(events)
            assertThat(enrichedEvents.map { it.event.transactionContext.shard })
                .containsExactly(15, 1, 15, 1, 0, 1)
            assertThat(enrichedEvents.map { TBannerCandidateMeta.parseFrom(it.event.objectMeta).id })
                .containsExactly(1, 3, 2, 4, 5, 3)
        }.assertAll()
    }

    @Test
    fun directCampaignIdEnricherTest() {
        val eventWithCampaign = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 1L
                campaignId = campaignInfo.orderId
            }.build().toByteString()
        }.build()
        val eventWithCampaign2 = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 2L
                campaignId = campaignInfo2.orderId
            }.build().toByteString()
        }.build()
        val eventWithoutCampaign = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 3L
                campaignId = 555666L
            }.build().toByteString()
        }.build()
        val eventWithoutCampaign2 = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 5L
            }.build().toByteString()
        }.build()

        val events = listOf(
            eventWithCampaign,
            eventWithCampaign2,
            eventWithoutCampaign,
            eventWithCampaign,
            eventWithoutCampaign2,
        )

        val enrichedEvents = bannerCandidateWatchlogProcessor.enrichEvents(events)

        SoftAssertions().apply {
            assertThat(enrichedEvents).hasSameSizeAs(events)
            assertThat(enrichedEvents.map { it.directCampaignId })
                .containsExactly(campaignInfo.campaignId, campaignInfo2.campaignId, 0, campaignInfo.campaignId, 0)
            assertThat(enrichedEvents.map { TBannerCandidateMeta.parseFrom(it.event.objectMeta).id })
                .containsExactly(1, 2, 3, 1, 5)
        }.assertAll()
    }

    @Test
    fun allEnrichersTest() {
        val transactionContextWithoutShard = TTransactionContext.newBuilder().apply {
            operatorUid = 456
        }.build()

        val event = TEvent.newBuilder().apply {
            objectMeta = TBannerCandidateMeta.newBuilder().apply {
                id = 1L
                campaignId = campaignInfo.orderId
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()

        val events = listOf(event)

        val enrichedEvents = bannerCandidateWatchlogProcessor.enrichEvents(events)

        SoftAssertions().apply {
            assertThat(enrichedEvents).hasSameSizeAs(events)
            assertThat(enrichedEvents.map { it.objectType }).isEqualTo(List(enrichedEvents.size) { EObjectType.OT_BANNER_CANDIDATE })
            assertThat(enrichedEvents.map { it.event.transactionContext.shard })
                .containsExactly(1)
            assertThat(enrichedEvents.map { TBannerCandidateMeta.parseFrom(it.event.objectMeta).id })
                .containsExactly(1)
            assertThat(enrichedEvents.map { it.directCampaignId })
                .containsExactly(campaignInfo.campaignId)
        }.assertAll()
    }
}
