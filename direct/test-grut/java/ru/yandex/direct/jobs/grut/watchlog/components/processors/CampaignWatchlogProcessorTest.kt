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
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.grut.client.GrutClient
import ru.yandex.grut.objects.proto.client.Schema.EObjectType
import ru.yandex.grut.objects.proto.client.Schema.TCampaignMeta
import ru.yandex.grut.proto.transaction_context.TransactionContext
import ru.yandex.grut.watchlog.Watch.TEvent

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignWatchlogProcessorTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var grutClient: GrutClient

    private lateinit var campaignWatchlogProcessor: CampaignWatchlogProcessor

    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var campaignInfo2: CampaignInfo

    @BeforeEach
    fun beforeEach() {
        userInfo = steps.userSteps().createDefaultUser()
        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.clientInfo)
        campaignInfo2 = steps.campaignSteps().createDefaultCampaign()

        campaignWatchlogProcessor = CampaignWatchlogProcessor(
            ppcPropertiesSupport,
            grutClient,
            { mock() },
            "watchlogConsumerPrefix",
            shardHelper,
        )
    }

    @Test
    fun enrichEventsWithShardTest() {
        val transactionContextWithShard = TransactionContext.TTransactionContext.newBuilder().apply {
            shard = 15
            operatorUid = 123
        }.build()
        val transactionContextWithoutShard = TransactionContext.TTransactionContext.newBuilder().apply {
            operatorUid = 456
        }.build()

        val eventWithShard = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = 555666L
            }.build().toByteString()
            transactionContext = transactionContextWithShard
        }.build()
        val eventWithShard2 = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = 777888L
            }.build().toByteString()
            transactionContext = transactionContextWithShard
        }.build()
        val eventWithShard3 = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = campaignInfo2.campaignId
            }.build().toByteString()
            transactionContext = transactionContextWithShard
        }.build()
        val eventWithoutShard = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = campaignInfo.campaignId
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()
        val eventWithoutShard2 = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = 777888L
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()
        val eventWithoutShard3 = TEvent.newBuilder().apply {
            objectMeta = TCampaignMeta.newBuilder().apply {
                id = campaignInfo2.campaignId
            }.build().toByteString()
            transactionContext = transactionContextWithoutShard
        }.build()

        val events = listOf(
            eventWithShard,
            eventWithoutShard,
            eventWithShard2,
            eventWithShard3,
            eventWithoutShard2,
            eventWithoutShard3,
            eventWithoutShard,
        )

        val enrichedEvents = campaignWatchlogProcessor.enrichEvents(events)

        SoftAssertions().apply {
            assertThat(enrichedEvents).hasSameSizeAs(events)
            assertThat(enrichedEvents.map { it.objectType }).isEqualTo(List(enrichedEvents.size) { EObjectType.OT_CAMPAIGN })
            assertThat(enrichedEvents.map { it.event.transactionContext.shard })
                .containsExactly(15, campaignInfo.shard, 15, 15, 0, campaignInfo2.shard, campaignInfo.shard)
            assertThat(enrichedEvents.map { TCampaignMeta.parseFrom(it.event.objectMeta).id })
                .containsExactly(555666, campaignInfo.campaignId, 777888, campaignInfo2.campaignId, 777888, campaignInfo2.campaignId, campaignInfo.campaignId)
        }.assertAll()
    }
}
