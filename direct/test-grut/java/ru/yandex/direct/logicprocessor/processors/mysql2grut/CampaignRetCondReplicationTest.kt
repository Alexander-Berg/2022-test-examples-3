package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.RetConditionSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignRetCondReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var retConditionSteps: RetConditionSteps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    clientInfo.client!!,
                    listOf()
                )
            )
        )
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    @Test
    fun testCreateCampaign_NoAbSegmentRetContIdInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val abSegmentRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val abSegmentStatisticRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AB_SEGMENT_RET_COND_ID, abSegmentRetCond.retConditionId)
            .set(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID, abSegmentStatisticRetCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(abSegmentRetCond.retConditionId)
        assertThat(createdCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(abSegmentStatisticRetCond.retConditionId)
    }

    @Test
    fun testCreateCampaign_ExistingRetContIdInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val abSegmentRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val abSegmentStatisticRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentRetCond.retConditionId,
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentStatisticRetCond.retConditionId
                )
            )
        )

        val grutRetCond = replicationApiService.retargetingConditionGrutApi.getRetargetingConditions(
            listOf(
                abSegmentRetCond.retConditionId,
                abSegmentStatisticRetCond.retConditionId
            )
        )
        assertThat(grutRetCond).hasSize(2)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AB_SEGMENT_RET_COND_ID, abSegmentRetCond.retConditionId)
            .set(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID, abSegmentStatisticRetCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(abSegmentRetCond.retConditionId)
        assertThat(createdCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(abSegmentStatisticRetCond.retConditionId)
    }

    @Test
    fun testCreateCampaign_RemoveLinkToAbSegmentRetCondGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val abSegmentRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val abSegmentStatisticRetCond = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AB_SEGMENT_RET_COND_ID, abSegmentRetCond.retConditionId)
            .set(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID, abSegmentStatisticRetCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(abSegmentRetCond.retConditionId)
        assertThat(createdCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(abSegmentStatisticRetCond.retConditionId)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .setNull(CAMPAIGNS.AB_SEGMENT_RET_COND_ID)
            .setNull(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(0L)
        assertThat(updatedCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(0L)
    }

    @Test
    fun testCreateCampaign_updateLinkToAbSegmentRetCondGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val abSegmentRetCond1 = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val abSegmentStatisticRetCond1 = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)

        val abSegmentRetCond2 = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val abSegmentStatisticRetCond2 = retConditionSteps.createDefaultABSegmentRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AB_SEGMENT_RET_COND_ID, abSegmentRetCond1.retConditionId)
            .set(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID, abSegmentStatisticRetCond1.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentRetCond1.retConditionId,
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentStatisticRetCond1.retConditionId
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentRetCond2.retConditionId,
                ),
                Mysql2GrutReplicationObject(
                    retargetingConditionId = abSegmentStatisticRetCond2.retConditionId
                )
            )
        )

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(abSegmentRetCond1.retConditionId)
        assertThat(createdCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(abSegmentStatisticRetCond1.retConditionId)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.AB_SEGMENT_RET_COND_ID, abSegmentRetCond2.retConditionId)
            .set(CAMPAIGNS.AB_SEGMENT_STAT_RET_COND_ID, abSegmentStatisticRetCond2.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign!!.spec.abSegmentRetargetingConditionId).isEqualTo(abSegmentRetCond2.retConditionId)
        assertThat(updatedCampaign.spec.abSegmentStatisticsRetargetingConditionId).isEqualTo(abSegmentStatisticRetCond2.retConditionId)
    }

    @Test
    fun testCreateCampaign_NoBrandSafetyRetContIdInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)

        val retCond = retConditionSteps.createDefaultBrandSafetyRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.BRANDSAFETY_RET_COND_ID, retCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.brandsafetyRetargetingConditionId).isEqualTo(retCond.retConditionId)
    }

    @Test
    fun testCreateCampaign_ExistingBrandSafetyRetContIdInGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val retCond = retConditionSteps.createDefaultBrandSafetyRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    retargetingConditionId = retCond.retConditionId,
                ),
            )
        )

        val grutRetCond = replicationApiService.retargetingConditionGrutApi.getRetargetingConditions(
            listOf(retCond.retConditionId)
        )
        assertThat(grutRetCond).hasSize(1)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.BRANDSAFETY_RET_COND_ID, retCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.brandsafetyRetargetingConditionId).isEqualTo(retCond.retConditionId)
    }

    @Test
    fun testCreateCampaign_RemoveLinkToBrandSafetyRetCondGrut() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaign = TestCampaigns.activeCpmBannerCampaign(null, null)
            .withOrderId(0L)
        val retCond = retConditionSteps.createDefaultBrandSafetyRetCondition(clientInfo)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.BRANDSAFETY_RET_COND_ID, retCond.retConditionId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.brandsafetyRetargetingConditionId).isEqualTo(retCond.retConditionId)

        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .setNull(CAMPAIGNS.BRANDSAFETY_RET_COND_ID)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        assertThat(updatedCampaign!!.spec.brandsafetyRetargetingConditionId).isEqualTo(0L)
    }
}
