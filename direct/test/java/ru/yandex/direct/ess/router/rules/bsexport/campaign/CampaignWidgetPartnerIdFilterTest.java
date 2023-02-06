package ru.yandex.direct.ess.router.rules.bsexport.campaign;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.WidgetPartnerCampaignsTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.WIDGET_PARTNER_CAMPAIGNS;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.WIDGET_PARTNER_ID;
import static ru.yandex.direct.ess.router.testutils.WidgetPartnerCampaignsTableChange.createWidgetPartnerCampaignsEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignWidgetPartnerIdFilterTest {

    @Autowired
    private BsExportCampaignRule bsExportCampaignRule;

    @Test
    void insertCampaignTest() {
        long cid = 10L;
        WidgetPartnerCampaignsTableChange change = new WidgetPartnerCampaignsTableChange().withCid(cid);
        BinlogEvent binlogEvent = createWidgetPartnerCampaignsEvent(List.of(change), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(WIDGET_PARTNER_ID)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void updateWidgetPartnerIdTest() {
        long cid = 10L;
        WidgetPartnerCampaignsTableChange change = new WidgetPartnerCampaignsTableChange().withCid(cid);
        change.addChangedColumn(WIDGET_PARTNER_CAMPAIGNS.WIDGET_PARTNER_ID, 456L, 759L);
        BinlogEvent binlogEvent = createWidgetPartnerCampaignsEvent(List.of(change), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(WIDGET_PARTNER_ID)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }
}
