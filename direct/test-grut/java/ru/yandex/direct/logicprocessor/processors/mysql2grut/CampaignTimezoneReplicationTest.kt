package ru.yandex.direct.logicprocessor.processors.mysql2grut

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
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
import ru.yandex.direct.dbschema.ppcdict.tables.GeoTimezones.GEO_TIMEZONES
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class CampaignTimezoneReplicationTest {

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
    fun campaignWithTimeZoneReplication() {
        dslContextProvider.ppcdict()
            .insertInto(GEO_TIMEZONES, GEO_TIMEZONES.TIMEZONE_ID, GEO_TIMEZONES.TIMEZONE, GEO_TIMEZONES.COUNTRY_ID, GEO_TIMEZONES.GROUP_NICK)
            .values(130, "Europe/Moscow", 225, "russia")
            .execute()
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withTimezoneId(130)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, orderId = campaignInfo.campaign.orderId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)

        val softly = SoftAssertions()
        softly.assertThat(createdCampaign).isNotNull
        softly.assertThat(createdCampaign!!.spec.hasTimezone()).isTrue
        softly.assertThat(createdCampaign.spec.timezone.countryId).isEqualTo(225)
        softly.assertThat(createdCampaign.spec.timezone.name).isEqualTo("Europe/Moscow")

        softly.assertAll()

    }

    @Test
    fun campaignWithoutTimeZoneReplication() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withTimezoneId(0)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        processor.withShard(campaignInfo.shard)
        processor.process(
            listOf(Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId, orderId = campaignInfo.campaign.orderId, clientId = campaignInfo.clientId.asLong()))
        )

        val createdCampaign = replicationApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaign.id)
        Assertions.assertThat(createdCampaign!!.spec.hasTimezone()).isFalse
    }
}
