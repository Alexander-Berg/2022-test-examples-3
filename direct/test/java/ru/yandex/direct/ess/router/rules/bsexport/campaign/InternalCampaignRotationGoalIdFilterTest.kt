package ru.yandex.direct.ess.router.rules.bsexport.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.CampaignsInternalTableChange
import ru.yandex.direct.test.utils.randomPositiveLong

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class InternalCampaignRotationGoalIdFilterTest {

    @Autowired
    private lateinit var bsExportCampaignRule: BsExportCampaignRule

    @Test
    fun insertInternalCampaignTest() {
        val cid = randomPositiveLong()
        val tableChange = CampaignsInternalTableChange().withCid(cid)
        val binlogEvent = CampaignsInternalTableChange.createCampaignsInternalEvent(listOf(tableChange), Operation.INSERT)
        binlogEvent.traceInfoMethod = "method"
        binlogEvent.traceInfoService = "service"
        binlogEvent.traceInfoReqId = 123L
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CampaignResourceType.INTERNAL_CAMPAIGN_ROTATION_GOAL_ID)
                .setReqid(123L)
                .build()
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

    @Test
    fun updateInternalCampaignTest() {
        val cid = randomPositiveLong()
        val tableChange = CampaignsInternalTableChange().withCid(cid)
        tableChange.addChangedColumn(Tables.CAMPAIGNS_INTERNAL.ROTATION_GOAL_ID, 10L, 11L)
        val binlogEvent = CampaignsInternalTableChange.createCampaignsInternalEvent(listOf(tableChange), Operation.UPDATE)
        binlogEvent.traceInfoMethod = "method"
        binlogEvent.traceInfoService = "service"
        binlogEvent.traceInfoReqId = 123L
        val objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent)
        val expectedObject = BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CampaignResourceType.INTERNAL_CAMPAIGN_ROTATION_GOAL_ID)
                .setReqid(123L)
                .build()
        assertThat(objects)
                .usingRecursiveFieldByFieldElementComparator().contains(expectedObject)
    }

}
