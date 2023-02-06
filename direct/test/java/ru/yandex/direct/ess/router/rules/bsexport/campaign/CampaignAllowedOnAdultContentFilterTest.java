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
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType.CAMPAIGN_ALLOWED_ON_ADULT_CONTENT;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignAllowedOnAdultContentFilterTest {
    @Autowired
    private BsExportCampaignRule bsExportCampaignRule;

    @Test
    void insertCampaignTest_flagPresent() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addInsertedColumn(CAMPAIGNS.OPTS, "opt1,is_allowed_on_adult_content,opt2");
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_ALLOWED_ON_ADULT_CONTENT)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void insertCampaignTest_flagAbsent() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addInsertedColumn(CAMPAIGNS.OPTS, "opt1,opt2");
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), INSERT);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, CAMPAIGN_ALLOWED_ON_ADULT_CONTENT);
    }

    @Test
    void updateOptsTest_flagAdded() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.OPTS, "opt1", "opt1,is_allowed_on_adult_content");
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_ALLOWED_ON_ADULT_CONTENT)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void updateOptsTest_flagDeleted() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.OPTS, "opt1,is_allowed_on_adult_content", "opt1");
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportCampaignObject.Builder()
                .setCid(cid)
                .setService("service")
                .setMethod("method")
                .setCampaignResourceType(CAMPAIGN_ALLOWED_ON_ADULT_CONTENT)
                .setReqid(123L)
                .build();
        assertThat(objects).usingRecursiveFieldByFieldElementComparator().contains(expectedObject);
    }

    @Test
    void updateOptsTest_flagNotChanged() {
        long cid = 10L;
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(cid).withClientId(1L).withType(CampaignsType.text);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.OPTS,
                "opt1,is_allowed_on_adult_content", "opt1,is_allowed_on_adult_content");
        BinlogEvent binlogEvent = createCampaignEvent(List.of(campaignsTableChange), UPDATE);
        binlogEvent.setTraceInfoMethod("method");
        binlogEvent.setTraceInfoService("service");
        binlogEvent.setTraceInfoReqId(123L);
        var objects = bsExportCampaignRule.mapBinlogEvent(binlogEvent);
        assertThatNotContainsResourceType(objects, CAMPAIGN_ALLOWED_ON_ADULT_CONTENT);
    }

    private void assertThatNotContainsResourceType(List<BsExportCampaignObject> objects,
                                                   CampaignResourceType resourceType) {
        var objectsWithType = objects.stream()
                .filter(object -> object.getCampaignResourceType().equals(resourceType))
                .collect(Collectors.toList());
        assertThat(objectsWithType).isEmpty();
    }
}
