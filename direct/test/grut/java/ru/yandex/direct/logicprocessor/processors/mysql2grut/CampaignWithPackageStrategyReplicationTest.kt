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
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.enums.StrategiesType
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignWithPackageStrategyReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

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
    fun createCampaignWithNotExistingStrategyTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        val strategyId = createStrategy()
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_ID, strategyId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        // Так как для кампании еще нет стратегии в груте, она должна автоматически досоздаться
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                )
            )
        )

        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        assertThat(createdStrategy).isNotNull
        assertThat(createdStrategy!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.strategyId).isEqualTo(strategyId)
    }

    @Test
    fun createCampaignWithExistingStrategyTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        val strategyId = createStrategy()
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_ID, strategyId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()
        processor.withShard(campaignInfo.shard)
        // сначала создаем стратегию в груте
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId
                )
            )
        )

        val createdStrategy = replicationApiService.strategyGrutApi.getStrategy(strategyId)
        assertThat(createdStrategy).isNotNull
        assertThat(createdStrategy!!.meta.clientId).isEqualTo(clientInfo.clientId!!.asLong())

        // Затем создаем кампанию
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.strategyId).isEqualTo(strategyId)
    }

    @Test
    fun updateCampaignStrategyTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.CAMPAIGN_REPLICATION_FILTER_WITHOUT_FOREIGN, "false")
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        val strategyId1 = createStrategy()
        val strategyId2 = createStrategy()

        processor.withShard(campaignInfo.shard)
        // создаем обе стратегии в груте
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    strategyId = strategyId1
                ),
                Mysql2GrutReplicationObject(
                    strategyId = strategyId2
                )
            )
        )

        // сначала на кампании одна стратегия
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_ID, strategyId1)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        // Затем создаем кампанию
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(createdCampaign).isNotNull
        assertThat(createdCampaign!!.spec.strategyId).isEqualTo(strategyId1)

        // затем меняется на другую стратегию
        dslContextProvider.ppc(clientInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_ID, strategyId2)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                )
            )
        )

        val updatedCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(updatedCampaign).isNotNull
        assertThat(updatedCampaign!!.spec.strategyId).isEqualTo(strategyId2)
    }

    private fun createStrategy(): Long {
        val id = shardHelper.generateStrategyIds(clientInfo.clientId!!.asLong(), 1)[0]
        dslContextProvider.ppc(clientInfo.shard)
            .insertInto(Tables.STRATEGIES)
            .set(Tables.STRATEGIES.STRATEGY_ID, id)
            .set(Tables.STRATEGIES.STRATEGY_DATA, "{\"sum\": \"700\", \"name\": \"autobudget\"}")
            .set(Tables.STRATEGIES.TYPE, StrategiesType.autobudget)
            .set(Tables.STRATEGIES.CLIENT_ID, clientInfo.clientId!!.asLong())
            .execute()
        return id
    }
}
