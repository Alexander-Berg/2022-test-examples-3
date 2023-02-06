package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.utils.JsonUtils

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignWithMinusPhrasesReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientInfo.client!!, listOf())))
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    @Test
    fun createCampaignWithMinusPhrasesTest() {
        val minusPhrases = listOf("Привет", "как", "дела")
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withMinusKeywords(minusPhrases)
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        val grutMinusPhrases = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrases).isNotNull
        assertThat(grutMinusPhrases!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrases.spec.phrasesList).containsExactlyElementsOf(minusPhrases)
    }

    @Test
    fun updateCampaignMinusPhrasesTest() {
        val minusPhrasesBefore = listOf("Привет", "как", "дела")
        val minusPhrasesAfter = listOf("Привет", "как дела")
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withMinusKeywords(minusPhrasesBefore)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        val grutMinusPhrases = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrases).isNotNull
        assertThat(grutMinusPhrases!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrases.spec.phrasesList).containsExactlyElementsOf(minusPhrasesBefore)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMP_OPTIONS)
            .set(CAMP_OPTIONS.MINUS_WORDS, JsonUtils.toJson(minusPhrasesAfter))
            .where(CAMP_OPTIONS.CID.eq(campaign.id))
            .execute()

        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        assertThat(updatedCampaign.spec.minusPhrasesIdsList[0]).isEqualTo(createdCampaign.spec.minusPhrasesIdsList[0])
        val grutMinusPhrasesUpdated = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrasesUpdated).isNotNull
        assertThat(grutMinusPhrasesUpdated!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrasesUpdated.spec.phrasesList).containsExactlyElementsOf(minusPhrasesAfter)
    }

    @Test
    fun campaignMinusPhrasesNotChangedTest() {
        val minusPhrases = listOf("Привет", "как", "дела")
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withMinusKeywords(minusPhrases)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        val grutMinusPhrases = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrases).isNotNull
        assertThat(grutMinusPhrases!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrases.spec.phrasesList).containsExactlyElementsOf(minusPhrases)

        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        assertThat(updatedCampaign.spec.minusPhrasesIdsList[0]).isEqualTo(createdCampaign.spec.minusPhrasesIdsList[0])
        val grutMinusPhrasesUpdated = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrasesUpdated).isNotNull
        assertThat(grutMinusPhrasesUpdated!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrasesUpdated.spec.phrasesList).containsExactlyElementsOf(minusPhrases)
    }

    @Test
    fun deleteCampaignMinusPhrasesTest() {
        val minusPhrasesBefore = listOf("Привет", "как", "дела")
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withMinusKeywords(minusPhrasesBefore)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(1)
        val grutMinusPhrases = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrases).isNotNull
        assertThat(grutMinusPhrases!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        assertThat(grutMinusPhrases.spec.phrasesList).containsExactlyElementsOf(minusPhrasesBefore)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMP_OPTIONS)
            .setNull(CAMP_OPTIONS.MINUS_WORDS)
            .where(CAMP_OPTIONS.CID.eq(campaign.id))
            .execute()

        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(0)
        val grutMinusPhrasesUpdated = replicationApiService.minusPhrasesGrutDao.getMinusPhrase(createdCampaign.spec.minusPhrasesIdsList[0])
        assertThat(grutMinusPhrasesUpdated).isNull()
    }

    @Test
    fun campaignNotInstanceOfCampaignWithMinusKeywordsTest() {
        val minusPhrases = listOf("Привет", "как", "дела")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withMinusKeywords(minusPhrases)
            .withOrderId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.minusPhrasesIdsCount).isEqualTo(0)
    }

}
