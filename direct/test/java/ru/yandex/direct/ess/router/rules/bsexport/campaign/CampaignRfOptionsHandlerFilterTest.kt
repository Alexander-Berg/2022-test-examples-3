package ru.yandex.direct.ess.router.rules.bsexport.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.BinlogEvent
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange
import ru.yandex.direct.test.utils.randomPositiveLong

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class CampaignRfOptionsFilterTest {

    @Autowired
    private lateinit var bsExportCampaignRule: BsExportCampaignRule

    private var campaignId: Long = 1
    private lateinit var tableChange: CampaignsTableChange

    @BeforeEach
    fun initTestData() {
        campaignId = randomPositiveLong()
        tableChange = CampaignsTableChange()
            .withCid(campaignId)
            .withType(CampaignsType.internal_autobudget)
    }

    @Test
    fun insertCampaignWithRfTest() {
        tableChange
            .withRf(123)
            .withRfReset(456)

        val binlogEvent = getBinlogEvent(tableChange, Operation.INSERT)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun insertCampaignWithoutRfTest() {
        tableChange
            .withRf(0)

        val binlogEvent = getBinlogEvent(tableChange, Operation.INSERT)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().doesNotContain(expectedObject)
    }

    @Test
    fun insertNotSuitableCampaignTest() {
        tableChange
            .withType(CampaignsType.cpm_banner)
            .withRf(123)
            .withRfReset(456)

        val binlogEvent = getBinlogEvent(tableChange, Operation.INSERT)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().doesNotContain(expectedObject)
    }

    @Test
    fun updateCampaignWithRfTest() {
        tableChange.addChangedColumn(Tables.CAMPAIGNS.RF, 10L, 11L)

        val binlogEvent = getBinlogEvent(tableChange, Operation.UPDATE)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateCampaignWithClearRfTest() {
        tableChange.addChangedColumn(Tables.CAMPAIGNS.RF, 10L, 0L)

        val binlogEvent = getBinlogEvent(tableChange, Operation.UPDATE)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateCampaignWithOnlyRfResetTest() {
        tableChange.addChangedColumn(Tables.CAMPAIGNS.RF_RESET, 9L, 11L)

        val binlogEvent = getBinlogEvent(tableChange, Operation.UPDATE)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateCampaignWithoutRfTest() {
        tableChange.addChangedColumn(Tables.CAMPAIGNS.NAME, "old name", "new name")

        val binlogEvent = getBinlogEvent(tableChange, Operation.UPDATE)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().doesNotContain(expectedObject)
    }

    @Test
    fun updateNotSuitableCampaign() {
        tableChange
            .withType(CampaignsType.cpm_yndx_frontpage)
        tableChange.addChangedColumn(Tables.CAMPAIGNS.RF, 10L, 11L)

        val binlogEvent = getBinlogEvent(tableChange, Operation.UPDATE)
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)

        val expectedObject = getExpectedObject(campaignId, binlogEvent.traceInfoReqId)
        assertThat(objects)
            .usingRecursiveFieldByFieldElementComparator().doesNotContain(expectedObject)
    }

    private fun getBinlogEvent(
        tableChange: CampaignsTableChange,
        operation: Operation
    ): BinlogEvent {
        val binlogEvent = CampaignsTableChange.createCampaignEvent(listOf(tableChange), operation)
        binlogEvent.traceInfoMethod = "method"
        binlogEvent.traceInfoService = "service"
        binlogEvent.traceInfoReqId = randomPositiveLong()

        return binlogEvent
    }

    private fun getExpectedObject(cid: Long, traceInfoReqId: Long): BsExportCampaignObject {
        return BsExportCampaignObject.Builder()
            .setCid(cid)
            .setService("service")
            .setMethod("method")
            .setCampaignResourceType(CampaignResourceType.CAMPAIGN_RF_OPTIONS)
            .setReqid(traceInfoReqId)
            .build()
    }
}
