package ru.yandex.direct.ess.router.rules.conversioncenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.ess.logicobjects.conversioncenter.ConversionCenterEventObject
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class ConversionCenterEventRuleTest {
    @Autowired
    private lateinit var rule: ConversionCenterEventRule

    private val clientId = 1L
    private val campaignId = 2L

    @Test
    fun mapBinlogEventTest_insertCampaign() {
        val campaignsTableChanges = mutableListOf<CampaignsTableChange>()

        val campaignsTableChange = CampaignsTableChange().withCid(campaignId).withClientId(clientId)
        campaignsTableChange.addInsertedColumn(
            Tables.CAMPAIGNS.STRATEGY_DATA,
            String.format("{\"goalId\": %s}", 10)
        )
        campaignsTableChanges.add(campaignsTableChange)

        val event = createCampaignEvent(campaignsTableChanges, Operation.INSERT)
        val actualObjects = rule.mapBinlogEvent(event)
        val expectedObject = ConversionCenterEventObject(clientId = clientId, campaignId = campaignId)
        assertThat(actualObjects).containsOnly(expectedObject)
    }

    @Test
    fun mapBinlogEventTest_updateCampaignStrategy() {
        val campaignsTableChanges = mutableListOf<CampaignsTableChange>()

        val campaignsTableChange = CampaignsTableChange().withCid(campaignId).withClientId(clientId)
        campaignsTableChange.addChangedColumn(
            Tables.CAMPAIGNS.STRATEGY_DATA,
            String.format("{\"goalId\": %s}", 10),
            String.format("{\"goalId\": %s}", 30)
        )
        campaignsTableChanges.add(campaignsTableChange)

        val event = createCampaignEvent(campaignsTableChanges, Operation.UPDATE)
        val actualObjects = rule.mapBinlogEvent(event)
        val expectedObject = ConversionCenterEventObject(clientId = clientId, campaignId = campaignId)
        assertThat(actualObjects).containsOnly(expectedObject)
    }

    @Test
    fun mapBinlogEventTest_updateCampaignName() {
        val campaignsTableChanges = mutableListOf<CampaignsTableChange>()

        val campaignsTableChange = CampaignsTableChange().withCid(campaignId).withClientId(clientId)
        campaignsTableChange.addChangedColumn(
            Tables.CAMPAIGNS.NAME,
            "name1",
            "name2"
        )
        campaignsTableChanges.add(campaignsTableChange)

        val event = createCampaignEvent(campaignsTableChanges, Operation.UPDATE)
        val actualObjects = rule.mapBinlogEvent(event)
        assertThat(actualObjects).isEmpty()
    }
}
