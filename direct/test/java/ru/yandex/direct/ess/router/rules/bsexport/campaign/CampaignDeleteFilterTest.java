package ru.yandex.direct.ess.router.rules.bsexport.campaign;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.CAMPAIGN_DELETE;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignDeleteFilterTest {
    @Autowired
    private BsExportCampaignRule bsExportCampaignRule;

    @Test
    void deleteCampaignWithoutOrderId() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addDeletedColumn(CAMPAIGNS.ORDER_ID, 0L);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), DELETE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setReqid(123L)
                .setCampaignResourceType(CampaignResourceType.CAMPAIGN_DELETE)
                .setOrderId(0L)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void deleteCampaignWithOrderId() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addDeletedColumn(CAMPAIGNS.ORDER_ID, 987L);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), DELETE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setReqid(123L)
                .setCampaignResourceType(CampaignResourceType.CAMPAIGN_DELETE)
                .setOrderId(987L)
                .build();

        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void deleteMcbCampaign() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.mcb);
        campaignsTableChange.addDeletedColumn(CAMPAIGNS.ORDER_ID, 0L);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), DELETE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, CAMPAIGN_DELETE);
    }

    private void assertThatNotContainsResourceType(List<BsExportCampaignObject> objects,
                                                   CampaignResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getCampaignResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();

    }
}
