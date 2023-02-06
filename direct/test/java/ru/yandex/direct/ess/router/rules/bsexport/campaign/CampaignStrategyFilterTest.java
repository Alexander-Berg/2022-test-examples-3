package ru.yandex.direct.ess.router.rules.bsexport.campaign;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject;
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.CAMPAIGN_STRATEGY;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignStrategyFilterTest {
    @Autowired
    private BsExportCampaignRule bsExportCampaignRule;

    @Test
    void insertCampaignTest() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_STRATEGY)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void insertMcbCampaignTest() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.mcb);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, CAMPAIGN_STRATEGY);
    }


    @Test
    void updateCampaignPlatformTest() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.PLATFORM, CampaignsPlatform.both, CampaignsPlatform.context);
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setReqid(123L)
                .setCampaignResourceType(CAMPAIGN_STRATEGY)
                .setOrderId(null)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }


    private void assertThatNotContainsResourceType(List<BsExportCampaignObject> objects,
                                                   CampaignResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getCampaignResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();

    }
}
