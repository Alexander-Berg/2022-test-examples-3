package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.CAMP_ORDER_TYPES
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignOrderTypeReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

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
    fun createCampaign_NoOrderTypeTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull

        assertThat(createdCampaign!!.meta.orderType).isEqualTo(0)
    }

    @Test
    fun createCampaign_GetOrderTypeFromTableTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)

        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(CAMP_ORDER_TYPES, CAMP_ORDER_TYPES.CID, CAMP_ORDER_TYPES.ORDER_TYPE)
            .values(campaignInfo.campaignId, 9)
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

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull

        assertThat(createdCampaign!!.meta.orderType).isEqualTo(9)
    }

    @Test
    fun createCampaign_UpdateOrderTypeTest() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaignWithCalculatedOrderId(clientInfo)
        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(
                    campaignId = campaignInfo.campaignId,
                    clientId = campaignInfo.clientId.asLong()
                )
            )
        )
        dslContextProvider.ppc(campaignInfo.shard)
            .insertInto(CAMP_ORDER_TYPES, CAMP_ORDER_TYPES.CID, CAMP_ORDER_TYPES.ORDER_TYPE)
            .values(campaignInfo.campaignId, 9)
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

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaign(campaignInfo.campaign.orderId)
        assertThat(createdCampaign).isNotNull

        assertThat(createdCampaign!!.meta.orderType).isEqualTo(9)
    }
}
